package org.grails.plugin.queuemail

import grails.plugins.mail.MailMessageBuilder
import grails.plugins.mail.MailMessageBuilderFactory
import org.grails.config.PropertySourcesConfig
import org.springframework.mail.MailSender

class QueueMailMessageBuilderFactory extends MailMessageBuilderFactory {

	/**
	 * Variation from grails 2 fussy about configuration element
	 * @param mailSender
	 * @param config
     * @return
     */
	MailMessageBuilder createBuilder(MailSender mailSender, PropertySourcesConfig config) {
		//this.mailSender=mailSender
		new MailMessageBuilder(mailSender, config, mailMessageContentRenderer)
	}

}
