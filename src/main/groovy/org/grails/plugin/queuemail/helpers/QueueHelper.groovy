package org.grails.plugin.queuemail.helpers

import java.util.concurrent.PriorityBlockingQueue

import org.grails.plugin.queuemail.ComparableFutureTask

class QueueHelper {

	static boolean changeMaxPoolSize(executor,Long userId,int maxPoolSize,int minPreserve,int priority,int definedPriority,int running,int coreSize) {
		boolean slotsFree=true
		int available = maxPoolSize - running
		int poolSize = (coreSize+1 <= maxPoolSize) ? (coreSize+1) : maxPoolSize
		int minSize = maxPoolSize - minPreserve
		if (minPreserve > 0) {
			def executorCount = executorCount(executor.runningJobs,executor.getQueue(), definedPriority,userId)
			if (priority >= definedPriority) {
				available = coreSize - running
				slotsFree=available > minPreserve ? true : false
				if (slotsFree) {
					int counter = executorCount.runningAbove + 1
					def group = (executor.limitUserAbovePriority ? executor.limitUserAbovePriority : coreSize)
					if (executorCount.othersWaitingAbove > 0 &&  executorCount.userRunningBelow + executorCount.userRunningAbove >= coreSize){
						slotsFree=false
					}
				}
			} else {
				slotsFree=maxPoolSize > coreSize ? true : false
				if (slotsFree) {					
					int counter = (executorCount.runningBelow + 1)
					def group = (executor.limitUserBelowPriority? executor.limitUserBelowPriority : coreSize)
					if ((executorCount.queuedAbove > 0  || executorCount.othersWaiting) &&
					executorCount.userRunningBelow + executorCount.userRunningAbove >= coreSize) {
						slotsFree=false
					}
				}
			}
		}

		int runSize = slotsFree ? poolSize : minSize
		if (runSize && executor) {
			executor.setCorePoolSize(runSize)
			executor.setMaximumPoolSize(runSize)
		}
		return slotsFree
	}


	public static Map executorCount(Collections.SynchronizedSet runningJobs,PriorityBlockingQueue waitingQueue,
			int definedPriority=EnhancedPriorityBlockingExecutor.definedPriority.value, Long userId=0) {

		def runBelowPriority = runningJobs?.findAll{it.priority < definedPriority}
		int runningBelowPriority =runBelowPriority?.size() ?: 0

		def runAbovePriority =runningJobs?.findAll{it.priority >= definedPriority}
		int runningAbovePriority=runAbovePriority?.size() ?:  0

		def queueAbovePriority=waitingQueue?.findAll{k-> if (k?.priority) {k.priority?.value >= definedPriority}}
		int queuedAbovePriority = queueAbovePriority?.size() ?: 0

		def queueBelowPriority=waitingQueue?.findAll{k-> if (k?.priority) {k.priority?.value < definedPriority}}
		int queuedBelowPriority = queueBelowPriority?.size() ?: 0

		int userQueuedAbove,userQueuedBelow,userRunningAbove,userRunningBelow,othersWaitingAbove,othersWaitingBelow
		boolean othersWaiting
		if (userId) {
			userQueuedAbove = queueAbovePriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userQueuedBelow = queueBelowPriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userRunningBelow = runBelowPriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userRunningAbove = runAbovePriority.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			othersWaitingBelow = queueAbovePriority?.findAll{ComparableFutureTask t ->  t.userId != userId}?.size() ?: 0
			othersWaitingAbove = queueBelowPriority?.findAll{ComparableFutureTask t ->  t.userId != userId}?.size() ?: 0
			othersWaiting = othersWaitingBelow+othersWaitingAbove>0
		}
		return [runningBelow: runningBelowPriority, runningAbove: runningAbovePriority,
			queuedBelow: queuedBelowPriority, queuedAbove: queuedAbovePriority,
			userBelow: userQueuedBelow, userAbove: userQueuedAbove,
			userRunningAbove:userRunningAbove,userRunningBelow:userRunningBelow,
			othersWaiting:othersWaiting,othersWaitingBelow:othersWaitingBelow,othersWaitingBelow:othersWaitingBelow
		]

	}
			
}
