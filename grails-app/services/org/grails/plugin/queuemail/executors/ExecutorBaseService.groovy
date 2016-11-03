package org.grails.plugin.queuemail.executors

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import org.grails.plugin.queuemail.EmailQueue

import static org.grails.plugin.queuemail.enums.QueueStatus.QUEUED
import static org.grails.plugin.queuemail.enums.QueueStatus.RUNNING
import static org.grails.plugin.queuemail.enums.QueueTypes.BASIC
import static org.grails.plugin.queuemail.enums.QueueTypes.ENHANCED

/**

 * @author Vahid Hedayati
 *
 */
class ExecutorBaseService  implements GrailsApplicationAware {

	def config
	GrailsApplication grailsApplication

	void checkQueue(Long id=null) {
		def inputParams=[:]
		String addon=''
		if (id) {
			addon='and rq.id!=:id'
			inputParams.id=id
		}
		processCheckQueue(addon,inputParams)
	}
	void checkQueue(Class clazz) {
		String addon=''
		def inputParams=[:]
		if (clazz) {
			addon="and rq.class = :className"
		}
		inputParams.className=clazz.name
		processCheckQueue(addon,inputParams)
	}

	void processCheckQueue(String addon,Map inputParams=[:]) {
		inputParams.status=QUEUED
		def query="""select new map(rq.id as id,
				(case 
					when rq.class=EmailQueue then '${ENHANCED}'
					when rq.class=BasicQueue then '${BASIC}'				
					end) as queueType
			) from EmailQueue rq where rq.status=:status
				$addon order by rq.queuePosition asc, id asc
			"""
		def metaParams=[readOnly:true,timeout:15,max:-1,cache: false]
		def waiting
		EmailQueue.withNewTransaction {
			waiting=EmailQueue.executeQuery(query,inputParams,metaParams)
		}
		log.debug "waiting reports ${waiting.size()}"
		waiting?.each{queue ->

			new Thread({
				sleep(500)
				switch (queue.queueType) {
					case "${ENHANCED}":
						notify( "method.emailExecutor",queue.id)
						break
					case "${BASIC}":
						notify( "method.basicExecutor",queue.id)
						break
				}
			} as Runnable ).start()
		}
	}


	/**
	 * This is called by Bootstrap to ensure no tasks are left running from last restart
	 * @return
	 */
	def rescheduleRequeue() {
		EmailQueue.withNewTransaction{
			def running = EmailQueue.where{status==RUNNING}.findAll()
			running.each { EmailQueue queue ->
				log.debug "Job ${queue.id} had been running. Setting status to queued"
				queue.status=QUEUED
				queue.save(flush:true)
			}
		}
		if (config.checkQueueOnStart) {
			checkQueue()
		}
	}

	void setGrailsApplication(GrailsApplication ga) {
		config = ga.config.queuemail
	}
}