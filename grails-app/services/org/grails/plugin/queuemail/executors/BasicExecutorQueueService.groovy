package org.grails.plugin.queuemail.executors

import org.grails.plugin.queuemail.BasicQueue
import org.grails.plugin.queuemail.EmailRunnable
import org.grails.plugin.queuemail.enums.Priority
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.util.concurrent.RunnableFuture

import static org.grails.plugin.queuemail.enums.QueueStatus.ERROR
import static org.grails.plugin.queuemail.enums.QueueStatus.QUEUED

@Consumer
class BasicExecutorQueueService extends ExecutorBaseService   {

	def basicExecutor
	@Selector('method.basicExecutor')
	void basicExecutor(Long eventId) {
		log.debug "Received ${eventId}"
		BasicQueue.withTransaction {
			BasicQueue queue = BasicQueue.read(eventId)
			if (queue && (queue.status == QUEUED || queue.status == ERROR)) {
				Priority priority = queue.priority ?: queue.defaultPriority
				EmailRunnable currentTask = new EmailRunnable(queue)
				RunnableFuture task = basicExecutor.execute(currentTask, priority.value)
				task?.get()
			}
		}
	}
}