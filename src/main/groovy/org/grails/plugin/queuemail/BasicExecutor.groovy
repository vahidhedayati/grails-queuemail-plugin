package org.grails.plugin.queuemail

import grails.util.Holders
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.helpers.QueueHelper
import org.grails.plugin.queuemail.monitor.ServiceConfigs

import java.util.concurrent.*

class BasicExecutor extends ThreadPoolExecutor {

	private static long keepAliveTime=((Holders.grailsApplication.config.queuemail?.keepAliveTime ?: 300) as Long)
	private static TimeUnit timeoutUnit=TimeUnit.SECONDS
	private static int corePoolSize = Holders.grailsApplication.config.queuemail?.corePoolSize ?: 1
	private static final int actualPoolSize = Holders.grailsApplication.config.queuemail?.maximumPoolSize ?: 3
	private static int maximumPoolSize = actualPoolSize
	private static int maxQueue = Holders.grailsApplication.config.queuemail.maxQueue?:100
	private static int minPreserve = Holders.grailsApplication.config.queuemail?.preserveThreads ?: 0
	private static Priority definedPriority = Holders.grailsApplication.config.queuemail?.preservePriority ?: Priority.MEDIUM
	private static boolean defaultComparator = Holders.grailsApplication.config.queuemail.defaultComparator
	static final Set<ArrayList> runningJobs = ([] as Set).asSynchronized()


	/**
	 * Keeps a tab on current sender configuration
	 * a count of how many sent per account configuration as per your service definition
	 * this binds to each service you have created and their accounts
	 * When an account reaches it's limit for the day, it will be set to use the next one
	 *
	 * If a configured account fails consequently for configured fail values it will
	 * mark host as down and use the next account in list. Account will be re-checked as per
	 * configured recheck value
	 */
	private static final ConcurrentMap<Class, List<ServiceConfigs>> senderMap = new ConcurrentHashMap<Class, List<ServiceConfigs>>()

	//Amount of QueueId's from currentID, if host was down to reset - for another attempt
	private static int elapsedQueue = Holders.grailsApplication.config.queuemail?.elapsedQueue ?: 300
	//Or amount of time elapsed in seconds between last failure and now
	private static int elapsedTime = Holders.grailsApplication.config.queuemail?.elapsedTime ?: 1800 // 30 minutes
	private static int failuresTolerated = Holders.grailsApplication.config.queuemail?.failuresTolerated ?: 5 // 5  failures in a row

	public BasicExecutor() {
		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
			new PriorityBlockingQueue<Runnable>(maxQueue,new EmailComparator()) 
		)
	}

	static void registerSenderFault(Class clazz, Long queueId, String sendAccount) {
		List<ServiceConfigs> serviceConfigs = senderMap.get(clazz)
		ServiceConfigs serviceConfig =serviceConfigs?.find{it.jobName==sendAccount}
		if (serviceConfig) {
			ServiceConfigs serviceConfig1 = serviceConfig.clone()
			serviceConfig1.failCount=1

			if (serviceConfig.lastFailedQueueId && serviceConfig.lastFailedQueueId + 1 == queueId||serviceConfig.lastFailedQueueId== queueId) {
				serviceConfig1.failCount=serviceConfig.failCount ? (serviceConfig.failCount+1): 1
			}
			if (serviceConfig1.failCount >= failuresTolerated) {
				serviceConfig1.active=false
			}
			serviceConfig1.lastFailedQueueId=queueId
			serviceConfig1.lastQueueId=queueId
			serviceConfig1.currentCount=serviceConfig1.currentCount ? (serviceConfig1.currentCount+1) : 1
			serviceConfig1.failTotal=serviceConfig.failTotal ? (serviceConfig.failTotal+1) : 1
			serviceConfig1.lastFailed=new Date()
			serviceConfigs.remove(serviceConfig)
			serviceConfigs.add(serviceConfig1)
			senderMap.remove(clazz)
			senderMap.put(clazz,serviceConfigs)
		}
	}
	/**
	 * Complex binding of current Class being your serviceClass and its underlying configuration
	 * Your configuration mapped back in - confirmed if job is there / available for usage
	 * @param clazz
	 * @param jobConfigurations
	 * @param queueId
	 * @return
	 */
	static String getSenderCount(Class clazz, jobConfigurations,Long queueId) {
		String sendAccount
		boolean exists
		List<ServiceConfigs> serviceConfigs = senderMap.get(clazz)
		Date now = new Date()
		Date lastFailed
		int today = now.format('dd') as int
		int lastSendDay,currentCounter,failTotal

		if (serviceConfigs) {
			jobConfigurations?.each { String sender, int limit ->
				currentCounter=1
				failTotal=0
				lastSendDay=today
				if (!sendAccount) {
					ServiceConfigs serviceConfig = serviceConfigs.find { it.jobName == sender }
					now = new Date()
					if (serviceConfig && serviceConfig.currentCount) {
						if (serviceConfig.active) {
							exists = true
							if (serviceConfig.currentCount + 1 <= limit) {
								if (today == serviceConfig.lastDay) {
									currentCounter = serviceConfig.currentCount + 1
								}
								sendAccount = sender
							} else {
								if (today != serviceConfig.lastDay) {
									sendAccount = sender
								}
							}
						} else {
							if (elapsedQueue && queueId - serviceConfig.lastQueueId > elapsedQueue ||
									elapsedTime && serviceConfig.lastFailed &&\
									  now.time - serviceConfig.lastFailed.time > (elapsedTime*1000)) {
								sendAccount = sender
								lastFailed = serviceConfig.lastFailed
								failTotal = serviceConfig.failTotal
							}
						}
					} else {
						if (serviceConfig.active) {
							sendAccount = sender
						}
					}
					if (sendAccount) {
						if (exists) {
							serviceConfigs?.remove(serviceConfig)
						}
						ServiceConfigs serviceConfig1 = serviceConfig.clone()
						serviceConfig1.lastDay = lastSendDay
						serviceConfig1.jobName = sendAccount
						if (lastFailed) {
							serviceConfig1.lastFailed = lastFailed
						}
						if (failTotal) {
							serviceConfig1.failTotal = failTotal
						}
						serviceConfig1.active = true
						serviceConfig1.lastQueueId = queueId
						serviceConfig1.currentCount = currentCounter
						serviceConfig1.limit=limit
						serviceConfig1.actioned= now
						serviceConfigs.remove(serviceConfig)
						serviceConfigs.add(serviceConfig1)
					}
				}
			}
		}  else  {
			jobConfigurations?.eachWithIndex { String sender, int limit, int i ->
				ServiceConfigs serviceConfig1 = new ServiceConfigs()
				serviceConfig1.lastDay = today
				serviceConfig1.jobName = sender
				serviceConfig1.failTotal = 0
				serviceConfig1.active = true
				serviceConfig1.actioned= now
				serviceConfig1.lastQueueId = queueId
				serviceConfig1.currentCount = 0
				serviceConfig1.limit=limit
				if (i==0) {
					serviceConfig1.currentCount = 1
					sendAccount = sender
					serviceConfigs=[serviceConfig1]
				} else {
					serviceConfigs.add(serviceConfig1)
				}
			}
		}
		senderMap?.remove(clazz)
		senderMap.put(clazz,serviceConfigs)
		return sendAccount ?: ''
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		runningJobs.add(r)
		super.beforeExecute(t, r)
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		runningJobs.remove(r)
		super.afterExecute(r, t)
	}

	public ComparableFutureTask execute(Runnable command, int priority) {

		boolean slotsFree
		if (!defaultComparator) {
			slotsFree=QueueHelper.changeMaxPoolSize(this,command.queue.userId,actualPoolSize,minPreserve,priority,definedPriority.value,
					super.getActiveCount(),super.getCorePoolSize())
		}
		ComparableFutureTask task = new ComparableFutureTask(command,null,this,priority,definedPriority.value, actualPoolSize, minPreserve,slotsFree)

		super.execute(task)
	}
	
	void setMaximumPoolSize(int i) {
		this.maximumPoolSize=i
	}
	void setMaxQueue(int i) {
		this.maxQueue=i
	}
	void setMinPreserve(int i) {
		this.minPreserve=i
	}
	void setDefinedPriority(Priority p) {
		this.definedPriority=p
	}

	void setDefaultComparator(boolean b) {
		this.defaultComparator=b
	}
}