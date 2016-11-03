package queuemail

import grails.plugins.Plugin
import org.grails.plugin.queuemail.BasicExecutor
import org.grails.plugin.queuemail.EmailExecutor
import org.grails.plugin.queuemail.QueueMailMessageBuilderFactory

class QueuemailGrailsPlugin extends Plugin {
	def version = "1.0"
	def grailsVersion = "2.4 > *"
	def title = "queuemail plugin"
	def description = """Queuemail plugin will control outgoing email through a queue and control throughput as well as daily limit, 
 priority can be set"""
	def documentation = "https://github.com/vahidhedayati/grails-queuemail-plugin"
	def license = "APACHE"
	def developers = [name: 'Vahid Hedayati', email: 'badvad@gmail.com']
	def issueManagement = [system: 'GITHUB', url: 'https://github.com/vahidhedayati/grails-queuemail-plugin/issues']
	def scm = [url: 'https://github.com/vahidhedayati/grails-queuemail-plugin']
	Closure doWithSpring() {
		{ ->
			emailExecutor(EmailExecutor)
			basicExecutor(BasicExecutor)
			queueMailMessageBuilderFactory(QueueMailMessageBuilderFactory){
				it.autowire = true
			}
		}
	}
}
