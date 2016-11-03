package org.grails.plugin.queuemail

import grails.util.Holders

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.helpers.QueueHelper

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
	// Keeps actual counter when current date day is different to senderLastSent all is reset
	private static final ConcurrentMap<String, Integer> senderCounter = new ConcurrentHashMap<String, Integer>()
	//Keeps day of date and Sender String
	private static final ConcurrentMap<String, Integer> senderLastSent = new ConcurrentHashMap<String, Integer>()
	public BasicExecutor() {
		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
			new PriorityBlockingQueue<Runnable>(maxQueue,new EmailComparator()) 
		)
	}

	static String getSenderCount(jobConfigurations) {
		int currentCounter=1
		String sendAccount
		int lastSentDay
		boolean exists
		jobConfigurations?.each { String sender, int limit ->
			if (!sendAccount) {
				def currentCount = senderCounter.get(sender)
				int today = ((new Date()).format('dd') as int)
				lastSentDay = senderLastSent.get(sender)
				if (currentCount) {
					exists=true
					if (currentCount+1 <= limit) {
						if (today != lastSentDay) {
							currentCounter = 1
							lastSentDay=today
						} else {
							currentCounter=currentCount+1
						}
						sendAccount=sender
					} else {
						if (today != lastSentDay) {
							currentCounter = 1
							lastSentDay=today
							sendAccount=sender
						}
					}
				} else {
					currentCounter = 1
					lastSentDay=today
					sendAccount=sender
				}
			}
		}
		if (sendAccount) {
			if (exists) {
				senderLastSent.remove(sendAccount)
				senderCounter.remove(sendAccount)
			}
			senderLastSent.put(sendAccount,lastSentDay)
			senderCounter.put(sendAccount,currentCounter)
		}
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