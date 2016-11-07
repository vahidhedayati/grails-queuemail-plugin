

package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.ConfigTypes.*
import static org.grails.plugin.queuemail.enums.QueueStatus.*
import static org.grails.plugin.queuemail.enums.QueueTypes.*
import static org.grails.plugin.queuemail.enums.SearchTypes.*
import groovy.time.TimeCategory
import groovy.time.TimeDuration

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuemail.enums.MessageExceptions
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.events.BasicQueuedEvent
import org.grails.plugin.queuemail.events.EmailQueuedEvent
import org.grails.plugin.queuemail.helpers.QueueHelper
import org.grails.plugin.queuemail.monitor.ServiceConfigs
import org.grails.plugin.queuemail.validation.EmailQueueBean
import org.grails.plugin.queuemail.validation.QueueMailBean
import org.grails.plugin.queuemail.validation.QueueMailLists

/**
 * QueueReportService is the main service that interacts with 
 * QueueKitController -
 * It carries out user UI selection i.e. Queue listing.
 * 
 * It also is the service called by your report calls 
 * buildReport  -> Two ways of being called  
 *
 */
class QueueMailApiService {

	def grailsApplication
	def exeutorBaseService

	def emailExecutor
	def basicExecutor
	
	def queueMailUserService



	/**
	 * Attempts to delete actual file as well as DB entry for EmailQueue
	 * @param id
	 * @param user
	 * @param authorized
	 * @return
	 */
	def delete(Long id,Long userId, boolean authorized,boolean safeDel) {
		EmailQueue c=EmailQueue.get(id)
		if (!c) return null
		boolean deleted=false
		if ((c.userId==userId||authorized) && (!safeDel||(safeDel && c.status==SENT))) {


			deleted=true

			/**
			 * EnhancedPriorityBlockingEmailQueue can stop running tasks
			 * This block attempts to kill the underlying thread launched
			 * with the main call. Thus stopping a live task from execution
			 * 
			 */
			if (c.isEnhancedPriority()) {
				boolean cancelled=false
				/*
				 * Check to see if is a running task ?
				 */
				ComparableFutureTask task = EmailExecutor?.runningJobs?.find{it.queueId == c.id}
				if (task) {
					statusDeleted(c)
					task.cancel(true)
					cancelled=true
				} else {
					/*
					 *  Confirm task is not sitting on queue ?
					 */
					ComparableFutureTask fTask = emailExecutor?.getQueue()?.find{it.queueId == c.id}
					if (fTask) {
						/*
						 *  It was found on queue cancel task
						 */						
						fTask.cancel(true)
						cancelled=true
					}
					statusDeleted(c)
				}

				/*
				 *  Refresh queue elements
				 */
				if (cancelled) {
					emailExecutor.purge()
				}
			} else {
				/*
				 * All other ThreadExecutors 
				 */
				switch (c.queueLabel) {					
					case "${BASIC}":
						basicExecutor?.getQueue()?.find{it.queueId == c.id}?.cancel(true)
						basicExecutor.purge()
						break
				}
				statusDeleted(c)
			}
		}
		return deleted
	}

	private void statusDeleted(EmailQueue c) {
		EmailQueue.withTransaction {
			c.status=DELETED
			c.save()
		}
	}

	/**
	 * Cancels a request from the queue by setting status to error
	 * @param queueId
	 * @param params
	 * @return
	 */
	public boolean cancel(Long queueId,Map params) {
		boolean result=false
		EmailQueue queue=EmailQueue.get(queueId)
		if (queue?.status==RUNNING) {
			queue.status=ERROR
			queue.save()
			result=true
		}
		return result
	}


	/**
	 * modify configuration types of given TaskExecutor mode
	 * modifies max thread / preserve values + preserve groups
	 * also controls shutdown and manual re-queue 
	 * @param queue
	 * @param changeValue
	 * @param changeType
	 * @param priority
	 * @return
	 */
	def modifyConfiguration(String queueLabel,int changeValue, String changeType, Priority priority=Priority.MEDIUM,  boolean defaultComparator) {
		switch (queueLabel) {
			case "${BASIC}":
				BasicExecutor ex = new BasicExecutor()
				actionModifyType(changeType,changeValue,priority,ex,basicExecutor, defaultComparator)
				break
			case "${ENHANCED}":
				EmailExecutor ex = new EmailExecutor()
				actionModifyType(changeType,changeValue,priority,ex,emailExecutor, defaultComparator)
				break
		}
	}

	private void actionModifyType(String changeType,int changeValue,Priority priority=Priority.MEDIUM,ex,executor,boolean defaultComparator) {
		switch (changeType) {
			case "${POOL}":
				ex.maximumPoolSize=changeValue
				break
			case "${MAXQUEUE}":
				ex.maxQueue=changeValue
				break
			case "${PRESERVE}":
				if (changeValue < ex.maximumPoolSize) {
					ex.minPreserve=changeValue
					ex.definedPriority=priority
				}
				break
			case "${CHECKQUEUE}":
				if (changeType == CHECKQUEUE) {
					exeutorBaseService.checkQueue(ex.class)
				}
				break
			case "${DEFAULTCOMPARATOR}":
				ex.defaultComparator=defaultComparator
				break
			case "${STOPEXECUTOR}":
				executor.shutdown()
				break
			
		}
	}
	/**
	 * As per name when user selects
	 * modify priority on a given queue item
	 * it in turn runs this which - re-shuffles the priority 
	 * if it was low task and now high priority maybe it will run next
	 * @param queue
	 * @param priority
	 * @return
	 */
	def changeQueuePriority(queue, Priority priority) {
		if (queue?.isEnhancedPriority() && priority) {
			try {

				/*
				 *  To shuffle a queue Position it must therefore be in the queue and in QUEUED state
				 *  Confirm queueId exists in the queue collection
				 *  
				 *  if found cancel the task then reschedule it all over again this time with the updated priority
				 *   
				 */

				ComparableFutureTask fTask = emailExecutor?.getQueue()?.find{it.queueId == queue.id}
				if (fTask) {
					fTask.cancel(true)

					new Thread({												
						def currentTask = new EmailRunnable(queue)						
						RunnableFuture task = emailExecutor.execute(currentTask,priority.value)
						task?.get()
					} as Runnable ).start()

					if (queue.priority && queue.priority != priority) {
						queue.priority=priority
						queue.save()
					}

					emailExecutor.purge()
				}

			}catch(e) {
			}
		}

	}

	/**
	 * binds with front end user call to delete all or downloaded reports
	 * @param bean
	 * @param downloadedOnly optional if provided will only delete downloaded
	 * @return
	 */
	boolean clearUserReports(QueueMailBean bean, String deleteType) {
		def query=" from EmailQueue rq where rq.userId =:currentUser and "
		def whereParams=[:]
		if (deleteType==QueueMailLists.DELALL) {
			query +="rq.status != :running and rq.status!=:deleted"
			whereParams.running=RUNNING
			whereParams.deleted=DELETED
		} else {
			query +="rq.status = :requestType"
			whereParams.requestType=deleteType
		}
		def metaParams=[readOnly:true,timeout:15,max:-1]
		whereParams.currentUser=bean.userId
		def results=EmailQueue.executeQuery(query,whereParams,metaParams)
		def found = results?.size()
		results?.each {EmailQueue queue ->
			EmailQueue.withTransaction{
				queue.status=DELETED
				queue.save()
			}

			/*
			 * Iterate through  each getQueue = listing of ComparableFutureTask (s)
			 * if current queue.id matches queueId of  ComparableFutureTask.queueId collection then remove it
			 * finally purge executor - resetting queue
			 *
			 */
			switch (queue?.queueLabel) {
				case "${BASIC}":
					basicExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					basicExecutor.purge()
					break
				case "${ENHANCED}":
					emailExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					emailExecutor.purge()
					break
			}
		}
		return (found>0)
	}

	/**
	 * Quartz Schedule: this task
	 * @return
	 */
	def deleteOldEmails() {
		def whereParams=[:]
		def query="""from EmailQueue rq where (
				(rq.created <=:delDate and rq.status!=:sent) 
				or (rq.created <=:sentDate and rq.status=:sent)
			)
			"""
		int removalDay = config.removalDay ?: 5
		int removalDownloaded = config.removalDownloadedDay ?: 1
		whereParams.delDate=new Date()-removalDay
		whereParams.sentDate=new Date() -removalDownloaded
		whereParams.sent=SENT
		def metaParams=[readOnly:true,timeout:15,max:-1]
		def results=EmailQueue.executeQuery(query,whereParams,metaParams)
		if (config.deleteEntryOnDelete) {
			results*.delete()
		}
	}



	/**
	 * Generates listing view for controller: queueKit, action: listQueue
	 * @param bean
	 * @return
	 */
	def list(QueueMailBean bean) {
		def start=System.currentTimeMillis()
		def query
		def where=''
		def whereParams=[:]
		def sorts=['emailService', 'created', 'startDate', 'finishDate' ,'user', 'status', 'priority','queueType','duration','initiation','userId','from','to','subject']
		def sorts2=['rq.emailService', 'rq.created', 'rq.start','rq.finished','rq.userId','rq.status', 'rq.priority','rq.class',
			'internalDuration','internalInitiation','rq.userId','e.from','e.to','e.subject']
			
		def sortChoice=sorts.findIndexOf{it==bean.sort}
		boolean showUserField
		
		query="""select new map(rq.id as id, rq.retries as retries, e as email,
		rq.emailService as emailService, 
		rq.userId as userId,
			(case 
				when rq.class=EmailQueue then '${ENHANCED}'				
				when rq.class=BasicQueue then '${BASIC}'				
			end) as queueType,
			rq.priority as priority,			
			CONVERT(concat(hour(rq.finished)*60*60+minute(rq.finished)*60+second(rq.finished)) ,INTEGER) - 
				CONVERT(concat(hour(rq.start)*60*60+minute(rq.start)*60+second(rq.start)) ,INTEGER) 
			 as internalDuration,
			CONVERT(concat(hour(rq.start)*60*60+minute(rq.start)*60+second(rq.start)) ,INTEGER) - 
				CONVERT(concat(hour(rq.created)*60*60+minute(rq.created)*60+second(rq.created)) ,INTEGER) 			
			as internalInitiation,
			concat(hour(rq.finished)*60*60+minute(rq.finished)*60+second(rq.finished)*60) as aa,			
		 rq.start as startDate, rq.created as created, 
		rq.finished as finishDate, rq.status as status
	)
	from EmailQueue rq  join rq.email e """
		boolean superUser = bean.superUser
		if (!superUser || (superUser && (bean.searchBy && bean.searchBy!=USER||bean.hideUsers))) {
			where=addClause(where,'rq.userId=:userId')
			whereParams.userId=bean.userId
		}
		if (!bean.hideUsers && superUser) {
			showUserField=true
		}
		def statuses=[]
		String add=''
		if (bean.status) {
			if (bean.status == ACTIVE) {
				statuses= superUser ? EmailQueue.REPORT_STATUS_ALL : EmailQueue.REPORT_STATUS
			} else {
				statuses << bean.status
			}
		} else {
			add='not'
			statuses << SENT
			statuses << DELETED
		}
		where=addClause(where,"rq.status ${add} in (:statuses) ")
		whereParams.statuses=statuses
		if (bean.searchBy) {
			if (bean.searchBy==FROM) {
				where = addClause(where, 'lower(e.from) like :from')
				whereParams.from = '%' + bean.searchFor.toLowerCase() + '%'
			}else if (bean.searchBy==TO) {
				where = addClause(where, 'lower(e.to) like :to')
				whereParams.to = '%' + bean.searchFor.toLowerCase() + '%'
			}else if (bean.searchBy==SUBJECT) {
				where = addClause(where, 'lower(e.subject) like :subject')
				whereParams.subject = '%' + bean.searchFor.toLowerCase() + '%'
			}else {
				if (bean.searchBy==USER && bean.superUser) {
					Long userId = queueMailUserService.getRealUserId(bean.searchFor)
					if (userId) {
						where=addClause(where,'rq.userId=:userId')
						whereParams.userId=userId
						showUserField=true
					}
				}
			}
		}
		
		query+=where
		def metaParams=[readOnly:true,timeout:15,offset:bean.offset?:0,max:bean.max?:-1]
		if (sortChoice>0) {
			query+=" order by ${sorts2[sortChoice]} $bean.order"
		} else {
			query+=" order by rq.created $bean.order"
		}
		def results=EmailQueue.executeQuery(query,whereParams,metaParams)
		int total=results.size()
		if (total>=metaParams.max) {
			total=EmailQueue.executeQuery("select count(*) from EmailQueue rq "+where,whereParams,[readOnly:true,timeout:15,max:1])[0]
		} else {
			total+=metaParams.offset as Long
		}
		/*
		 * load up Config.groovy durationThreshHold
		 * recollect setting any unset values to 0
		 */
		def durationThreshHold=config.durationThreshHold
		def threshHold = durationThreshHold?.collect{[hours: it.hours?:0,minutes:it.minutes?:0,seconds:it.seconds?:0,color:it.color?:'']}

		results=results?.each { instance ->
			TimeDuration duration=getDifference(instance?.startDate,instance?.finishDate)
			instance.duration=formatDuration(duration)
			if (duration && threshHold) {
				instance.color=returnColor(threshHold,duration)
			}
			TimeDuration initiation=getDifference(instance?.created,instance?.startDate)
			instance.initiation=formatDuration(initiation)			
			if (initiation && threshHold) {
				instance.initiationColor=returnColor(threshHold,initiation)
			}
			instance.username = queueMailUserService.getUsername(instance.userId)
		}
		def instanceList=[results:results]

		def runTypes =results?.collect{it.queueType}.sort()?.unique()
		def queueTypes=[]
		int defaultPriority = (config.preservePriority ?: Priority.MEDIUM).value
		runTypes?.each {queueType->
			queueTypes << getJobsAvailable(queueType,defaultPriority,bean.userId)
		}
		instanceList << [reportJobs:queueTypes]
		
		return [instanceList:instanceList, instanceTotal:total, superUser:bean.superUser, statuses:bean.statuses,
				searchList  :bean.searchList, deleteList:QueueMailLists.deleteList, adminButtons:bean.adminButtons,
				hideQueueType:config.hideQueueType,hideQueuePriority:config.hideQueuePriority,
				showUserField:showUserField]
	}
	
	private String formatDuration(TimeDuration input) {
		return input?.toString()?.replace('.000','')
	}
	private TimeDuration getDifference(Date startDate,Date endDate) {
		if (startDate && endDate) {
			//return TimeCategory.minus(endDate, startDate)
			TimeDuration customPeriod = use(TimeCategory ) {
				customDuration( endDate - startDate )
			}
			return customPeriod
		}
	}
	TimeDuration customDuration( TimeDuration tc ) {
		new TimeDuration(tc.days , tc.hours, tc.minutes, tc.seconds, ((tc.seconds > 0||tc.minutes > 0||tc.hours > 0) ? 0 : tc.millis))
	}
	private String returnColor(List threshHold,TimeDuration duration) {
		String color=''
		/*
		 * Find the nearest match from durationThreshHold
		 */
		def match = threshHold.findAll{ k-> k.hours <= duration.hours && \
			k.minutes <= duration.minutes && k.seconds <= duration.seconds}\
		.sort{a,b-> a.hours<=>b.hours ?: a?.minutes<=>b?.minutes?: a?.seconds<=>b?.seconds  }
		if (match) {
			color=match.last().color
		}
		return color
	}

	/**
	 * Works out and returns a map of total jobs queued / running and available limit
	 */
	Map getJobsAvailable(String queueType=null,int defaultPriority=null,Long userId=null) {
		int queued,running,maxPoolSize,minPreserve,maxQueue,elapsedQueue,elapsedTime,failuresTolerated
		boolean isAdvanced
		List serviceConfigs=[]
		Priority priority
		def executorCount=[:]
		switch (queueType) {
			case "${BASIC}":
				executorCount = QueueHelper.executorCount(EmailExecutor.runningJobs,emailExecutor.getQueue(),defaultPriority,userId)
				queued=basicExecutor.getQueue().size()
				running=basicExecutor.getActiveCount()
				maxPoolSize=basicExecutor.maximumPoolSize
				maxQueue=basicExecutor.maxQueue
				minPreserve=basicExecutor.minPreserve
				elapsedQueue=basicExecutor.elapsedQueue
				elapsedTime=basicExecutor.elapsedTime
				failuresTolerated=basicExecutor.failuresTolerated
				serviceConfigs=listServiceConfigurations(basicExecutor)
				isAdvanced=true
				break
			case "${ENHANCED}":
				executorCount = QueueHelper.executorCount(EmailExecutor.runningJobs,emailExecutor.getQueue(),defaultPriority,userId)
				queued=emailExecutor.getQueue().size()
				running=emailExecutor.getActiveCount()
				maxPoolSize=EmailExecutor.maximumPoolSize
				maxQueue=EmailExecutor.maxQueue
				minPreserve=EmailExecutor.minPreserve
				elapsedQueue=emailExecutor.elapsedQueue
				elapsedTime=emailExecutor.elapsedTime
				failuresTolerated=emailExecutor.failuresTolerated
				serviceConfigs=listServiceConfigurations(emailExecutor)
				isAdvanced=true
				if (minPreserve>0) {
					priority=EmailExecutor.definedPriority
				}
				break
		}
		return [queueType:queueType ?: ENHANCED  , maxPoolSize:maxPoolSize,serviceConfigs:serviceConfigs,
				elapsedTime:elapsedTime,elapsedQueue:elapsedQueue,
				failuresTolerated:failuresTolerated,isAdvanced:isAdvanced,maxQueue:maxQueue,
			running:running,queued:queued,minPreserve:minPreserve,priority:priority,executorCount:executorCount]
	}

	void modifyJob(executor, String serviceClazz, String jobName,int limit,boolean active,MessageExceptions currentException=null) {
		def configCheck  = executor.senderMap?.find{it.key.simpleName.toString()==serviceClazz}
		Class currentClazz = configCheck?.key
		List<ServiceConfigs> serviceConfigs = configCheck?.value
		ServiceConfigs serviceConfig=serviceConfigs?.find{it.jobName==jobName}
		if (serviceConfig) {
			ServiceConfigs serviceConfig1 = serviceConfig.clone()
			if (!currentException) {
				serviceConfig1.currentException=null
			} else {
				serviceConfig1.currentException=currentException
			}
			serviceConfig1.limit=limit
			serviceConfig1.active=active
			serviceConfigs.remove(serviceConfig)
			serviceConfigs.add(serviceConfig1)
			executor.senderMap?.remove(currentClazz)
			executor.senderMap?.put(currentClazz,serviceConfigs)
		}
	}

	List listServiceClasses(executor) {
		return executor.senderMap*.key.collect{it.simpleName}
	}

	List listServiceConfigurations(executor, String clazzName) {
		List<ServiceConfigs> serviceConfigs = executor.senderMap?.find{it.key.simpleName.toString()==clazzName}.value
		List results=[]
		Map result = [:]
		result.service=clazzName
		result.info=[]
		serviceConfigs?.each { ServiceConfigs serviceConfig ->
			result.info << serviceConfig.getResults()
		}
		results << result
		return results
	}

	List listServiceConfigurations(executor) {
		List results=[]
		executor.senderMap?.each { Class clazz, List<ServiceConfigs> serviceConfigs ->
				Map result = [:]
				result.service=clazz.simpleName
				result.info=[]
				serviceConfigs?.each { ServiceConfigs serviceConfig ->
					result.info << serviceConfig.getResults()
				}
			results << result
		}
		return results
	}

	
	def buildEmail(EmailQueueBean bean) {
		functionReport(bean.bindReport())
	}

	def buildEmail(String emailService, Long userId, Locale locale, Email email,String reportType=null) {
		def values=[:]
		values.emailService=emailService
		values.userId=userId
		values.locale=locale
		values.email=email
		values.reportType=reportType
		functionReport(values)
	}

	def buildEmail(String emailService,Long userId, Locale locale, Email email,Priority priority,String reportType=null) {
		def values=[:]
		values.emailService=emailService
		values.userId=userId
		values.locale=locale
		values.email=email
		values.priority=priority
		values.reportType=reportType
		if (values.reportType !=  ENHANCED && values.reportType !=  BASIC) {
			values.reportType = ENHANCED
		}
		functionReport(values)
	}

	def functionReport(Map values) {
		/*
		 * If no reportType defined default to ENHANCED
		 */
		if (!values.reportType) {
			values.reportType = config.defaultEmailQueue ?: ENHANCED
		}
		def queue
		/*
		 * Decide which reportType it is and set correct queue class type
		 */
		switch (values.reportType) {
			case "${BASIC}":
				queue = basicQueue(values)
				break
			default:
			/*
			 *  Default to enhancedPriorityBlockingDomainClass
			 */
				queue = enhancedQueue(values)
		}
		save(values,queue)
	}



	/**
	 * Return queue as PriorityBlockingEmailQueue domainClass
	 * @param values
	 * @return
	 */
	def basicQueue(values) {
		BasicQueue queue
		if (values.id) {
			queue = BasicQueue.get(values.id)
		} else {
			queue = new BasicQueue()
		}
		return queue
	}

	/**
	 * Return queue as EnhancedPriorityBlockingEmailQueue domainClass
	 * @param values
	 * @return
	 */
	def enhancedQueue(values) {
		EmailQueue queue
		if (values.id) {
			queue = EmailQueue.get(values.id)
		} else {
			queue = new EmailQueue()
		}
		return queue
	}

	/**
	 * Generic save method that should be called from any report that requires queueing
	 * @param params
	 * @return
	 */
	def save(values,queue) {
		update(values,queue)
	}


	/**
	 * override method of update passing a map into EmailQueueBean
	 * @param values
	 * @param queue
	 * @return
	 */
	def update(Map values, queue) {
		EmailQueueBean bean = new EmailQueueBean()
		bean.bindBean(values)
		update(bean,queue)
	}

	/**
	 * this is the main update method that actually updates the DB and triggers event
	 * @param bean
	 * @param queue
	 * @return
	 */

	def update(EmailQueueBean bean, EmailQueue queue) {
		queue.emailService=bean.emailService
		queue.email=bean.email
		queue.userId=bean.userId
		queue.locale=bean.locale		
		
		
		
		if (config.standardRunnable) {
			if (!config.disableUserServicePriorityCheck) {
				queue.priority= bean.priority ?: queueMailUserService.reportPriority(queue,bean.priority,bean.email)
			}
		}

		if (!queue.priority) {
			queue.priority = bean.priority ?:  QueueMailLists.sortPriority(bean.emailService)
		}			
		
		queue.created=new Date()
		if (!queue.save(flush:true)) {
			log.error queue.errors
		}


		/*
		 * Generation of new records specially if
		 * end user is hammering reports button can 
		 * sometimes cause item to be missed
		 * lets capture it and slow down the pace a little
		 *  
		 */
		new Thread({
			sleep(500)
			switch (queue?.queueLabel) {
				case "${BASIC}":
					publishEvent(new BasicQueuedEvent(queue.id))
					break
				case "${ENHANCED}":
					publishEvent(new EmailQueuedEvent(queue.id))
					break
			}
		} as Runnable ).start()
		return queue
	}

	private String addClause(String where,String clause) {
		return (where ? where + ' and ' : 'where ') + clause
	}

	ConfigObject getConfig() {
		return grailsApplication.config.queuemail ?: ''
	}
}
