package org.grails.plugin.queuemail.validation

import static org.grails.plugin.queuemail.enums.QueueStatus.*
import static org.grails.plugin.queuemail.enums.QueueTypes.*
import grails.util.Holders
import grails.validation.Validateable

import org.grails.plugin.queuemail.Email
import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueStatus
import org.grails.plugin.queuemail.enums.QueueTypes

/**
 * This controls report listing
 * 
 * @author Vahid Hedayati
 *
 */

class EmailQueueBean  implements Validateable {

	def queueMailUserService = Holders.grailsApplication.mainContext.getBean('queueMailUserService')
	def grailsLinkGenerator = Holders.grailsApplication.mainContext.getBean('grailsLinkGenerator')

	def id
	String emailService		//This will be the service variant that binds in to extract additional info from	
	Email email
	Long userId
	Locale locale
	
	Date start				// null means never send
	Date created
	Date requeued
	Integer retries
	Date finished
	Priority priority
	String username

	String error
	String reportType		// Required only for binding to buildReport request

	QueueStatus status=QUEUED
	QueueTypes queueType=ENHANCED

	private static final long serialVersionUID = 1L


	static constraints={
		id(nullable:true,bindable:true)
		status(maxSize:1,inList:EmailQueue.REPORT_STATUS)
		requeued(nullable:true)
		start(nullable:true)
		finished(nullable:true)
		userId(nullable:true)
		priority(nullable:true, inList:Priority.values())
		username(nullable:true)
		queueType(nullable:true)
		reportType(nullable:true)
		error(nullable:true)
	}

	/*
	 * sets up the bean according to a DB entry 
	 */
	def formatBean(queue) {
		id=queue.id
		emailService=queue.emailService
		error=queue.error
		email=queue.email
		userId=queue.userId
		locale=queue.locale
		start=queue.start
		status=queue.status
		queueType=queue.queueType		
		retries=queue.retries				
		priority = queue?.priority ?:queue?.defaultPriority		
		username=queueMailUserService.getUsername(userId)
		return this
	}

	/*
	 * When you use the buildReport(bean) method
	 * you first create a new instance of this bean and add
	 * your values to it.
	 * When it buildsReport it calls this bindReport
	 * which gives back the required values as map to 
	 * ongoing function
	 * 
	 */
	protected Map bindReport() {
		def values=[:]
		values.with {
			emailService=this.emailService
			userId=this.userId
			locale=this.locale
			priority=this.priority
			reportType=this.reportType
		}
		values.email=email
		return values
	}

	/*
	 * Binds bean to a given params
	 * Converts a real params map to a JSON string
	 * which is then compatible with object type
	 */
	protected def bindBean(Map values) {
		if (values.id) {
			id=values.id
		}
		emailService=values.emailService
		email=values.email
		userId=values.userId
		locale=values.locale
		start=values.start
		if (values.priority) {
			priority=values.priority
		}
		status=QUEUED
		return this
	}
}