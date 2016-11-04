package org.grails.plugin.queuemail.examples

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.QueueMailBaseService

class QueueMailExampleMailingService extends QueueMailBaseService {
	
		def configureMail(executor,EmailQueue queue) {
			/**
			 * Contains a list of configuration names : daily limit for account
			 * So mailConfigExample1 will bind to Config.groovy smtp configuration
			 * assuming it is google it may have 3000 daily limit
			 * A listing is provided so it can fall over between list elements starting from top
			 * working it's way down and when limit exceeds it will use the next element
			 */
			def jobConfigurations = [
					'mailConfigExample1': 3,
					'mailConfigExample2': 3,
				]
			
			sendMail(executor,queue,jobConfigurations,QueueMailExampleMailingService.class)
		}
		
		
	}
	