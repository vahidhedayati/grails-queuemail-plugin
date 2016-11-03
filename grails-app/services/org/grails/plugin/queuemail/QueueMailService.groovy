package org.grails.plugin.queuemail

import grails.plugin.mail.MailMessageBuilder
import grails.plugin.mail.MailService

import org.springframework.mail.MailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl

class QueueMailService extends MailService {
	
	def queueMailMessageBuilderFactory

	MailMessage send(String accountSource, queue) {
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
	
	
	MailMessage sendMail(def config, Closure callable)  {
		if (isDisabled()) {
			log.warn("Sending emails disabled by configuration option")
			return
		}
		MailMessageBuilder messageBuilder = queueMailMessageBuilderFactory.createBuilder(getSender(config), config)
		callable.delegate = messageBuilder
		callable.resolveStrategy = Closure.DELEGATE_FIRST
		callable.call(messageBuilder)
		messageBuilder.sendMessage(mailExecutorService)
	}
	
	private JavaMailSenderImpl getSender(ConfigObject config) {
		if (config) {
			JavaMailSenderImpl mailSender = new JavaMailSenderImpl()
			mailSender.setHost(config?.host)
			mailSender.setPort(config?.port)
			mailSender.setUsername(config?.username)
			mailSender.setPassword(config?.password)
			if (config.props && config.props instanceof Map) {
				mailSender.javaMailProperties = config.props
			}
			/*mailSender.setJavaMailProperties(new Properties() { {
					config?.props?.each { k,v ->
						put(k,v)
					}
			}})	*/	
			return mailSender
		}
	}
	
	/**
	 * override of getMailConfig to allow a specific account 
	 * to be loaded at time of sendMail 
	 * @param accountSource
	 * @return
	 */
	ConfigObject getMailConfig(String accountSource) {
		grailsApplication.config.queuemail[accountSource]
	}

}
