package org.grails.plugin.queuemail

import grails.util.Holders

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueStatus
import org.grails.plugin.queuemail.helpers.QueueHelper



class EmailExecutor extends ThreadPoolExecutor {

	private static long keepAliveTime=((Holders.grailsApplication.config.queuemail?.keepAliveTime ?: 300) as Long)
	private static TimeUnit timeoutUnit=TimeUnit.SECONDS
	private static int corePoolSize = Holders.grailsApplication.config.queuemail?.corePoolSize ?: 3
	private static final int actualPoolSize = Holders.grailsApplication.config.queuemail?.maximumPoolSize ?: 3
	private static int maximumPoolSize = actualPoolSize
	private static int maxQueue = Holders.grailsApplication.config.queuemail.maxQueue?:100
	private static int minPreserve = Holders.grailsApplication.config.queuemail?.preserveThreads ?: 0
	private static Priority definedPriority = Holders.grailsApplication.config.queuemail?.preservePriority ?: Priority.MEDIUM
	private static boolean defaultComparator = Holders.grailsApplication.config.queuemail.defaultComparator
	
	private static int killLongRunningTasks = Holders.grailsApplication.config.queuemail?.killLongRunningTasks ?: 0
	private static final ConcurrentMap<Runnable, RunnableFuture> runningTasks = new ConcurrentHashMap<Runnable, RunnableFuture>()
	
	// Keeps actual counter when current date day is different to senderLastSent all is reset	
	private static final ConcurrentMap<String, Integer> senderCounter = new ConcurrentHashMap<String, Integer>()
	//Keeps day of date and Sender String
	private static final ConcurrentMap<String, Integer> senderLastSent = new ConcurrentHashMap<String, Integer>()
	
	private static final ConcurrentMap<Long, Runnable> waitingQueue = new ConcurrentHashMap<Long, Runnable>()
	static final Set<ArrayList> runningJobs = ([] as Set).asSynchronized()


	public EmailExecutor() {
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

	static void endRunningTask(Long id, ExecutorService timeoutExecutor) {
		def  r = waitingQueue.get(id)
		if (r) {
			removeRunningTask(r)
		}
		removeWaitingQueue(id)
		timeoutExecutor.shutdownNow()
	}

	static void addScheduledTask(Long id,Runnable r,RunnableFuture scheduled) {
		addRunningTask(r,scheduled)
		addWaitingQueue(id,r)
	}

	static boolean removeRunningTask(AttachedRunnable r) {
		RunnableFuture timeoutTask = runningTasks.get(r)
		if(timeoutTask) {
			timeoutTask.cancel(true)
			runningJobs.remove(r)
			r.shutdown()
			return true
		}
		runningTasks.remove(r)
		return false
	}

	static boolean addWaitingQueue(Long id, Runnable r) {
		waitingQueue.put(id, r)
		return true
	}

	static boolean removeWaitingQueue(Long id) {
		Runnable timeoutTask = waitingQueue.remove(id)
		if(timeoutTask) {
			return true
		}
		return false
	}

	static boolean addRunningTask(Runnable r,RunnableFuture scheduled) {
		runningTasks.put(r, scheduled)
		return true
	}

	void setLatestPriority(Long queueId, Priority priority) {
		EmailQueue.withNewTransaction {
			EmailQueue queue3=EmailQueue.get(queueId)
			queue3.priority=priority
			queue3.save(flush:true)
		}
	}


	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if (killLongRunningTasks > 0) {
			Long now = (new Date()).time
			boolean found=false
			runningJobs.findAll{k->((now - k.startTime) / 1000.0) >  killLongRunningTasks}?.each {ComparableFutureTask k->
				endRunningTask(k.queueId,k.timeoutExecutor)
				k.cancel(true)
				runningJobs.remove(k)
				sleep(600)
				found=true
				EmailQueue.withTransaction {
					EmailQueue c = EmailQueue.get(k.queueId)
					c.status=QueueStatus.CANCELLED
					c.save(flush:true)
				}
			}
			if (found) {
				super.purge()
			}
		}
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
			slotsFree=QueueHelper.changeMaxPoolSize(this,command.queue.userId,maximumPoolSize,minPreserve,priority,definedPriority.value,
					super.getActiveCount(),super.getCorePoolSize())
		}

		ScheduledThreadPoolExecutor timeoutExecutor= new ScheduledThreadPoolExecutor(1)
		timeoutExecutor.setRemoveOnCancelPolicy(true)
		ComparableFutureTask task = new ComparableFutureTask(command,null,this,timeoutExecutor,priority,definedPriority.value,
				maximumPoolSize, minPreserve,slotsFree)

		super.execute(task)
	}
	
	@Override
	public void shutdown() {
		super.shutdown()
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