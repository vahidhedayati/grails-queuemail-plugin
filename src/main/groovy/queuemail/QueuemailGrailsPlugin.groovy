package queuemail

import grails.plugins.Plugin
import org.grails.plugin.queuemail.BasicExecutor
import org.grails.plugin.queuemail.EmailExecutor
import org.grails.plugin.queuemail.QueueMailMessageBuilderFactory

class QueuemailGrailsPlugin extends Plugin {
	def version = "1.0"
	def grailsVersion = "2.4 > *"
	def title = "queuemail plugin"
	def description = """Queuemail plugin is a centralised email queueing system configurable for many providers all 
centrally controlled and limited to either daily limit or failures exceeding failureTolerance 
limit (in a row). By default all email's passing through are priority driven and configured by overall 
customService name.  Two methods of priority queueing are provided BASIC and ENHANCED (default). 
 Enhanced launches an additional thread for each running task and will attempt to kill any running process 
considered as stuck (if time taken exceeds killLongRunningTasks configuration period). """
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
