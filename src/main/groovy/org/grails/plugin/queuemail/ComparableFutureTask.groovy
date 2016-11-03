package org.grails.plugin.queuemail

import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledThreadPoolExecutor


class ComparableFutureTask<T> extends FutureTask<T>  {
	
	volatile int priority = 0
	volatile int definedPriority = 0
	volatile int maxPoolSize = 0
	volatile int minPreserve = 0
	volatile boolean slotsFree = true
	volatile Email email
	volatile Long startTime = 0
	volatile Long userId = 0
	volatile Long queueId = 0
	EmailQueue queue
	EmailExecutor emailExecutor
	BasicExecutor basicExecutor
	volatile ScheduledThreadPoolExecutor timeoutExecutor


	public ComparableFutureTask(Runnable runnable, T result, EmailExecutor emailExecutor,ScheduledThreadPoolExecutor timeoutExecutor, 
		int priority, int definedPriority,int maxPoolSize,int minPreserve,boolean slotsFree) {

		super(runnable, result)
		this.emailExecutor=emailExecutor
		this.timeoutExecutor=timeoutExecutor
		updatePriority(priority,definedPriority,maxPoolSize,minPreserve,slotsFree)
		updateDefaults(runnable)
	}


	public ComparableFutureTask(Runnable runnable, T result,BasicExecutor basicExecutor,
		int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {
	
			super(runnable, result)
			this.basicExecutor=basicExecutor
			updatePriority(priority,definedPriority,maxPoolSize,minPreserve,slotsFree)
			updateDefaults(runnable)
		}


	private void updatePriority(int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {
		this.priority = priority
		this.definedPriority = definedPriority
		this.maxPoolSize = maxPoolSize
		this.minPreserve = minPreserve
		this.slotsFree = slotsFree
	}

	private void updateDefaults(Runnable runnable) {
		this.email =runnable.queue.email
		this.queueId =  runnable.queue.id
		this.queue = runnable.queue
		this.startTime=(new Date()).time
		this.userId = runnable.queue.userId
	}
}