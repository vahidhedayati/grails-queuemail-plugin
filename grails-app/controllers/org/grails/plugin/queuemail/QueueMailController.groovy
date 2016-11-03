package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.QueueTypes.*

import org.grails.plugin.queuemail.validation.ChangeConfigBean
import org.grails.plugin.queuemail.validation.ChangePriorityBean
import org.grails.plugin.queuemail.validation.EmailQueueBean
import org.grails.plugin.queuemail.validation.QueueMailBean
import org.grails.plugin.queuemail.validation.QueueMailLists
import org.springframework.dao.DataIntegrityViolationException

/**
 * Main controller that provides a visible front end
 * to your reports queues.
 * 
 * Control report output, extend QueuekitUserService 
 * and define actual userId, userName, user.locale  
 * + authority -> 
 * 		superUser can view all user reports + deleted records 
 *  	normalUser -> only interacts with own reports + no deleted record history
 *  
 *  With correct output from overridden QueuekitUserService 
 *  the results of this report then matches returned service calls
 *  i.e : bean.userId = queueMailUserService.currentuser
 *  
 * @author Vahid Hedayati
 *
 */
class QueueMailController {
	
	static defaultAction = 'listQueue'
	
	static allowedMethods = [save: 'POST']
	
	def queueMailApiService
	def queueMailUserService
	def emailExecutor

	/**
	 * Display a given report Queue record
	 * @return
	 */
	def display() {
		if (params.id) {
			def queue
			switch (params?.queueType) {
				case BASIC:
					queue = EmailQueue.load(params.long('id'))
					break				
				default:
					queue = EmailQueue.load(params.long('id'))
			}
			EmailQueueBean bean = new EmailQueueBean().formatBean(queue)
			if (request.xhr) {
				render template:'showContent',model:[instance:bean]
			} else {
				render view:'show',model:[instance:bean]
			}
			return
		}
		render status:response.SC_NOT_FOUND
	}

	/*
	 * Default action returns reports produced
	 * for current userId
	 */
	def listQueue(QueueMailBean bean) {
		bean.userId = queueMailUserService.currentuser
		if (bean.userId) {
			def results=queueMailApiService.list(bean)
			def search = bean.search
			results.search=search
			//session.queueMailSearch=search
			if (!results.instanceList.results) {
				flash.message = message(code: 'queuemail.noRecordsFound.message')				
			}
			if (request.xhr) {
				render template:'list', model:results, status: response.SC_OK
				return
			}
			render view:'main',model:results
			return			
		}		
		render status:response.SC_NOT_FOUND
	}



	
	/**
	 * Deletes a given record ID - front end queueMail listing delete button action
	 * @param bean
	 * @return
	 */
	def delRecord(QueueMailBean bean) {
		try {
			flash.message = message(code: 'deletion.failure.message')
			bean.userId = queueMailUserService.currentuser
			boolean authorizeduser=bean.superUser ? true : false
			def deleted=queueMailApiService.delete(params.id as Long,bean.userId,authorizeduser,bean.safeDel)
			if (deleted) {
				flash.message = message(code: 'default.deleted.message', args: [message(code: 'queuemail.record.label'), params.id])
				redirect(action: "listQueue",params:bean.search)
				return
			}
		} catch (DataIntegrityViolationException e) {
		} catch (Throwable t) {
			flash.message= t.toString()
		}
		listQueue()
	}



	
	/**
	 * Cancel action from queueMail listing screen
	 * @return
	 */
	def cancel() {
		// id is the id of the queue entry
		boolean success=queueMailApiService.cancel(params.long('id'),params)
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}

	/**
	 * Main queueMail listing deletAll button action
	 * @param bean
	 * @return
	 */
	def deleteAll(QueueMailBean bean) {
		bean.userId = queueMailUserService.currentuser
		boolean success
		if (QueueMailLists.deleteList.contains(bean.deleteBy)) {
			success = queueMailApiService.clearUserReports(bean,bean.deleteBy)
		}
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}


	/**
	 * 
	 * ADMIN / SUPERUSER RELATED TASKS 
	 * 
	 * 
	 * 
	 */
	def changePriority(ChangePriorityBean bean) {
		if (queueMailUserService.isSuperUser(queueMailUserService.currentuser)) {
			if (request.xhr && bean.queue) {
				bean.priority = bean.queue.priority ?: bean.queue.defaultPriority
				render template:'/queueMail/changePriority',model:[instance:bean]
				return
			}
		}
		render status:response.SC_NOT_FOUND
	}

	def modifyPriority(ChangePriorityBean bean) {
		if (queueMailUserService.isSuperUser(queueMailUserService.currentuser)) {
			if (bean.validate()) {
				queueMailApiService.changeQueuePriority(bean.queue, bean.priority)
				response.status=response.SC_CREATED
				return
			}
			response.status=response.SC_CONFLICT
			listQueue()
			return
		}
		response.status=response.SC_CONFLICT
	}

	def changeConfig(ChangeConfigBean bean) {
		if (queueMailUserService.isSuperUser(queueMailUserService.currentuser) && request.xhr) {
				bean.formatBean()
				render template:'/queueMail/changeConfig',model:[instance:bean]
				return
		}
		flash.message = bean?.errors?.allErrors.collect{message(error : it)}
		render status:response.SC_NOT_FOUND
		return
	}

	def modifyConfig(ChangeConfigBean bean) {
		if (queueMailUserService.isSuperUser(queueMailUserService.currentuser) &&bean.validate()) {
			queueMailApiService.modifyConfiguration(bean.queueType?:bean.queue,bean.changeValue, bean.changeType,bean.priority, bean.defaultComparator)
			response.status=response.SC_CREATED
			listQueue()
			return
		}
		flash.message = bean?.errors?.allErrors.collect{message(error : it)}
		response.status=response.SC_CONFLICT
	}
	
	def loadConfig(ChangeConfigBean bean) {
		if (queueMailUserService.isSuperUser(queueMailUserService.currentuser) && bean.validate()) {
			render bean.loadConfig()
			return
		}
		response.status=response.SC_CONFLICT
	}
	
}
