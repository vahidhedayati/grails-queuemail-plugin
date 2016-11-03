package org.grails.plugin.queuemail

import grails.plugin.mail.MailMessageBuilder
import grails.plugin.mail.MailMessageBuilderFactory

import org.springframework.mail.MailSender

class QueueMailMessageBuilderFactory extends MailMessageBuilderFactory {
	
	MailMessageBuilder createBuilder(MailSender mailSendr, ConfigObject config) {
		//this.mailSender=mailSender
		new MailMessageBuilder(mailSendr?:mailSender, config, mailMessageContentRenderer)
	}

}
