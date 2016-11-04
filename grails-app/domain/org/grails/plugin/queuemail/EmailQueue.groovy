package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.QueueStatus.*
import static org.grails.plugin.queuemail.enums.QueueTypes.*

import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueStatus
import org.grails.plugin.queuemail.enums.QueueTypes
import org.grails.plugin.queuemail.validation.QueueMailLists

/**
 * When a request is made to save or run something on the reportsQueue
 * it is saved here. The main object extends over 3 different calls and classes 
 * found in this folder
 * 
 * @author Vahid Hedayati
 *
 */
//@GrailsCompileStatic
class EmailQueue {

	String emailService        //This will be the service variant that binds in to extract additional info from

	Email email
	Long userId                //could be bound to spring security userId or apacheShiro
	Locale locale

	Date start                // null means never send
	Date created
	Date requeued
	Integer retries            //keep a tab on user clicking requeue
	Integer queuePosition
	Priority priority
	Date finished

	QueueStatus status= QUEUED
	QueueTypes queueType = ENHANCED

	List<String> errorLogs=[]

	private static final long serialVersionUID = 1L

	static hasMany = [errorLogs: String]
	static final List REPORT_STATUS_ALL = [ACTIVE, QUEUED, RUNNING, COMPLETED, DELETED, ERROR, CANCELLED, NORESULTS]
	static final List REPORT_STATUS = REPORT_STATUS_ALL - [DELETED]

	static constraints = {
		status(inList: REPORT_STATUS_ALL)
		requeued(nullable: true)
		start(nullable: true)
		finished(nullable: true)
		retries(nullable: true)
		queuePosition(nullable: true)
		userId(nullable: true)
		priority(nullable: true)
		errorLogs(nullable: true)
	}
	
	/**
	 * defaults to 'ReportingService'
	 * can be overriden by classes that will use different convention
	 * i.e. EmailService
	 * @return
	 */
	String getServiceLabel() {
		return 'MailingService'
	}
	
	String getQueueLabel() {
		return ENHANCED
	}
	
	Priority getDefaultPriority() {
		return QueueMailLists.sortPriority(emailService) ?: Priority.LOW
	}
	
	static mapping={
		//status(sqlType:'char(2)')
		start(index:'email_queue_start_idx')
		priority(enumType:'string',sqlType:'char(20)')
		errorLogs(
				indexColumn: 'error_logs_idx',
				fetch: 'join',
				joinTable: [
						name  : 'error',
						length: 255,
						key   : 'email_queue_id',
						column: 'error_logs_string'
				]
		)
	}

	void errorLogs(String t) {
		this.errorLogs << t
	}
	
	String toString() {
		return "${emailService}-${created}"
	}
	
	Boolean hasPriority() {
		return false
	}
	
	Boolean isEnhancedPriority() {
		return true
	}
	
	
}
