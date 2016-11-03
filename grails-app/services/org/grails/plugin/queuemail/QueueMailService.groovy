package org.grails.plugin.queuemail

import grails.config.Config
import grails.core.GrailsApplication
import grails.core.support.GrailsConfigurationAware
import grails.plugins.mail.MailMessageBuilder
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.mail.MailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * This was rather simple under grails 2
 * Possibly due to CompileStatic in grails 3
 * I was unable to extend MailService and had to recreate it but hack it to change providers
 * A lot of effort really due to complications faced around JavaMailSender mailSender
 * Maybe there is a much easier way than this VS grails 2 way of doing all of this
 *
 * either way it appears to be working
 *
 */
class QueueMailService   implements InitializingBean, DisposableBean,  GrailsConfigurationAware {
	static transactional = false
	private static final Integer DEFAULT_POOL_SIZE = 5
	ThreadPoolExecutor mailExecutorService
	Config configuration
	GrailsApplication grailsApplication
	// MailMessageBuilderFactory mailMessageBuilderFactory
	// Replaced above to custom method which binds current mailSender to job
	QueueMailMessageBuilderFactory queueMailMessageBuilderFactory

	MailMessage sendEmail(String accountSource, queue) {
		def configuration = getMailConfig(accountSource)
		if (!configuration) {
			throw new Exception('No email configuration found for '+accountSource)
		}

		EmailQueue.withNewTransaction {
			Email message = Email.get(queue.emailId)
			Closure closure ={
				if (message.attachments) {
					multipart true
				}
				if(message.to && !message.to.isEmpty()){
					to message.to
				}
				subject message.subject
				if (message.headers && !message.headers.isEmpty() && isMimeCapable()) {
					headers message.headers
				}
				if (message.htmlEmail && isMimeCapable()) {
					html message.text
					if(message.alternative){
						text message.alternative
					}
				} else {
					body message.text
				}
				if (message.bcc && !message.bcc.isEmpty()) {
					bcc message.bcc
				}
				if (message.cc && !message.cc.isEmpty()) {
					cc message.cc
				}
				if (configuration.fromAddress) {
					from configuration.fromAddress
					if (!message.replyTo && message.from) {
						replyTo message.from
					}
				} else {
					if (message.replyTo) {
						replyTo message.replyTo
					}
					if (message.from) {
						from message.from
					}
				}
				if(message.envelopeFrom){
					envelopeFrom message.envelopeFrom
				}
				if (isMimeCapable()) {
					message.attachments.each {EmailAttachment attachment ->
						if (!attachment.inline) {
							attachBytes attachment.attachmentName, attachment.mimeType, attachment.content
						} else {
							inline attachment.attachmentName, attachment.mimeType, attachment.content
						}
					}
				}
			}
			return sendMail (configuration,closure)
		}
	}


	MailMessage sendMail(def config,@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = MailMessageBuilder) Closure callable) {
		if (isDisabled()) {
			log.warn("Sending emails disabled by configuration option")
			return
		}
		MailMessageBuilder messageBuilder = queueMailMessageBuilderFactory.createBuilder(mailSender(config), configuration)
		callable.delegate = messageBuilder
		callable.resolveStrategy = Closure.DELEGATE_FIRST

		callable.call(messageBuilder)
		messageBuilder.sendMessage(mailExecutorService)
	}

	/**
	 * Under Grails 3 this all worked out to be rather painful
	 * Whilst testing gmail and this was the only method that worked
	 * I thought by iterating through the config object and assigning would
	 * work. Even something like this
	 * 	config?.props?.each { k,v ->
	 		props.put("${k}","${v}")
	 		props.setProperty("${k}", "${v}")
	 	}
	 * Unfortunately it had to be defined like this
	 * @param config
	 * @return
     */
	public JavaMailSender mailSender(ConfigObject config) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		String user="${config?.username}"
		String host="${config.host?:'localhost'}"
		int port=config.port?:25
		mailSender.setHost("${host}")
		mailSender.setPort(port)
		mailSender.setUsername("${user}")
		mailSender.setPassword("${config?.password}")
		Properties props = System.getProperties()
		if (config?.props?.mail.debug) {
			props.put("mail.debug", config.props.mail.debug)
		}
		def smtp = config?.props?.mail.smtp
		if (smtp) {
			if (smtp.user) {
				props.put("mail.smtp.user", "${smtp.user}")
			}
			if (smtp.host) {
				props.put("mail.smtp.host", "${smtp.host}")
			}
			if (smtp.port) {
				props.put("mail.smtp.port", smtp.port)
			}
			if (smtp.auth) {
				props.put("mail.smtp.auth", "${smtp.auth}")
			}
			if (smtp.starttls.enable) {
				props.put("mail.smtp.starttls.enable", "${smtp.starttls.enable}")
			}
			if (smtp.EnableSSL.enable) {
				props.put("mail.smtp.EnableSSL.enable", "${smtp.EnableSSL.enable}")
			}
			if (smtp.socketFactory.class) {
				props.setProperty("mail.smtp.socketFactory.class", "${smtp.socketFactory.class}")
			}
			if (smtp.socketFactory.fallback) {
				props.setProperty("mail.smtp.socketFactory.fallback", "${smtp.socketFactory.fallback}")
			}
			if (smtp.socketFactory.port) {
				props.setProperty("mail.smtp.socketFactory.port", "${smtp.socketFactory.port}")
			}
		}
		mailSender.javaMailProperties=props
		return mailSender
	}


	ConfigObject getMailConfig(String accountSource) {
		return configuration.queuemail[accountSource]
	}


	/**
	 * Below methods are as per MailService - nothing different here
	 *
     */

	boolean isDisabled() {
		configuration.getProperty('grails.mail.disabled',Boolean, false)
	}

	void setPoolSize(Integer poolSize){
		mailExecutorService.setCorePoolSize(poolSize ?: DEFAULT_POOL_SIZE)
		mailExecutorService.setMaximumPoolSize(poolSize ?: DEFAULT_POOL_SIZE)
	}

	@Override
	public void destroy() throws Exception {
		mailExecutorService.shutdown();
		mailExecutorService.awaitTermination(10, TimeUnit.SECONDS);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mailExecutorService = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		Integer poolSize = configuration.getProperty('grails.mail.poolSize', Integer)
		try{
			((ThreadPoolExecutor)mailExecutorService).allowCoreThreadTimeOut(true)
		}catch(MissingMethodException e){
			log.info("ThreadPoolExecutor.allowCoreThreadTimeOut method is missing; Java < 6 must be running. The thread pool size will never go below ${poolSize}, which isn't harmful, just a tiny bit wasteful of resources.", e)
		}
		setPoolSize(poolSize)
	}
}
