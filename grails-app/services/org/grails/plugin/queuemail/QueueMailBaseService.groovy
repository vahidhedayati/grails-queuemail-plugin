package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.QueueStatus.*
import grails.gsp.PageRenderer
import grails.util.Holders

import java.util.concurrent.RunnableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils

abstract class QueueMailBaseService  {

	def grailsApplication = Holders.grailsApplication
	def g = grailsApplication.mainContext.getBean(ApplicationTagLib)
	def queueMailService = grailsApplication.mainContext.getBean('queueMailService')
	def queueMailUserService= grailsApplication.mainContext.getBean('queueMailUserService')
	def basicExecutor = grailsApplication.mainContext.getBean('basicExecutor')
	//private BasicExecutor basicExecutor
	
	
	PageRenderer groovyPageRenderer

	abstract def configureMail(executor, EmailQueue queue)

	def sendMail(executor,queue,jobConfigurations,Class clazz) {
		boolean failed=true
		String sendAccount
		String error=''
		String code
		List args
		try {
			if (jobConfigurations) {
				sendAccount = executor.getSenderCount(clazz,jobConfigurations,queue.id)
				if (sendAccount) {
					queueMailService.send(sendAccount,queue)
					failed=false							
				} else {
					code='queuemail.dailyLimit.label'
					args=[jobConfigurations]
				}
			} else {
				code='queuemail.noConfig.label'
			}
		}catch (Exception e) {
			failed=true
			error=e.message
		} finally {
			if (failed) {
				/**
				 * If we have an account meaning also mark down those that peaked limit
				 * make them inactive as well all those that failed to send in a row over failuresTolerated
				 * configured value
				 */
				if (sendAccount) {
					executor.registerSenderFault(clazz, queue.id, sendAccount)
				}
				if (code) {
					def webRequest = RequestContextHolder.getRequestAttributes()
					if(!webRequest) {
						def servletContext  = ServletContextHolder.getServletContext()
						def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
						webRequest = grails.util.GrailsWebUtil.bindMockWebRequest(applicationContext)
					}
					if (args) {
						error = g.message(code: code,args:args)
					} else {
						error = g.message(code: code)
					}
				}
				if (sendAccount) {
					//Try again to see if it can be sent
					logError(queue, error)
					sendMail(executor,queue,jobConfigurations, clazz)
				} else {
					// no options left here
					errorReport(queue, error)
				}
			}
		}
	}


	def executeReport(EmailQueue queue) {
		boolean validStatus=verifyStatusBeforeStart(queue.id)
		if (validStatus && !threadInterrupted) {
			boolean hasException=false
			try {
				if (queue.isEnhancedPriority()) {
						def task = EmailExecutor?.runningJobs?.find{it?.queueId == queue.id}
						if (task) {
							RunnableFuture rFuture
							ScheduledThreadPoolExecutor timeoutExecutor = task.timeoutExecutor
							AttachedRunnable attachedRunnable= new AttachedRunnable(queue,queue.email)
							try {						
								rFuture = timeoutExecutor.submit(attachedRunnable) 								
								EmailExecutor.addScheduledTask(queue.id,attachedRunnable,rFuture)
								rFuture?.get()
							}catch (e) {
								attachedRunnable.shutdown()
								timeoutExecutor.shutdownNow()
								timeoutExecutor.shutdown()
								EmailExecutor.endRunningTask(queue.id,timeoutExecutor)
								rFuture?.cancel(true)
								EmailExecutor?.runningJobs.remove(task)
							} finally {
								log.debug " Finished: Open Tasks: ${timeoutExecutor?.shutdownNow()?.size()}"
							}
						}
					
				} else {
					configureMail(basicExecutor, queue)
				}
			} catch (Exception e) {
				hasException=true
				log.error(e,e)
			}
			setCompletedState(queue.id,hasException,queue.status,threadInterrupted)
		}
	}
	
	Priority getQueuePriority(EmailQueue queue, Map params) {
		Priority priority = queue?.priority ?: queue.defaultPriority
		return priority
	}

	static boolean verifyStatusBeforeStart(Long queueId) {
		boolean validStatus=false
		EmailQueue.withNewTransaction {
			EmailQueue queue3=EmailQueue.get(queueId)
			if (queue3.status==QUEUED||queue3.status==ERROR) {
				validStatus=true
				if (queue3.status==ERROR) {
					queue3.retries=queue3.retries ? queue3.retries+1 : 1
				}
				queue3.status=RUNNING
				queue3.start=new Date()
				queue3.save(flush:true)
			}
		}
		return validStatus
	}

	static void setCompletedState(Long queueId, boolean hasException,QueueStatus status,boolean threadInterrupted=false) {
		EmailQueue.withNewTransaction {
			EmailQueue queue2=EmailQueue.get(queueId)
			if (queue2 && queue2.status == RUNNING) {
				if (!hasException && !threadInterrupted) {
					queue2.status=COMPLETED
				} else {
					queue2.status=ERROR
				}
				queue2.finished=new Date()
				queue2.save(flush:true)
			} else {
				log.debug "Queue ${queueId} has real status of ${queue2.status} had been ${status}. Task will be interrupted"
				Thread.currentThread().interrupt()
			}
		}
	}


	boolean getThreadInterrupted() {
		return Thread.currentThread().isInterrupted()
	}

	private void logError(EmailQueue queue, String error) {
		EmailQueue.withNewTransaction {
			EmailQueue queue2=EmailQueue.get(queue.id)
			if (queue2 && queue2.status == RUNNING && !threadInterrupted) {
				queue2.errorLogs(error?.size()>255 ? (error.substring(0,255)) : (error))
				queue2.save(flush:true)
			}
		}
	}
	private void errorReport(EmailQueue queue, String error) {
		EmailQueue.withNewTransaction {
			EmailQueue queue2=EmailQueue.get(queue.id)
			if (queue2 && queue2.status == RUNNING && !threadInterrupted) {
				queue2.status=ERROR
				queue2.errorLogs(error?.size()>255 ? (error.substring(0,255)) : (error))
				queue2.save(flush:true)
			}
		}
	}

	ConfigObject getConfig() {
		return grailsApplication.config.queuemail ?: ''
	}
}