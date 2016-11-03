package org.grails.plugin.queuemail.executors

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.EmailRunnable
import org.grails.plugin.queuemail.enums.Priority
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.util.concurrent.RunnableFuture

import static org.grails.plugin.queuemail.enums.QueueStatus.ERROR
import static org.grails.plugin.queuemail.enums.QueueStatus.QUEUED

/**
 * Priority Blocking uses priorityBlockingExecutor to manage report queue.
 * This is the default listener provided by the plugin and report priority can be either provided 
 * per call or by Config.groovy
 *
 *
 */
@Consumer
class EmailExecutorQueueService extends ExecutorBaseService {

	def emailExecutor

	@Selector('method.emailExecutor')
	void emailExecutor(Long eventId) {
		log.debug "Received ${eventId}"
		EmailQueue.withTransaction {
			EmailQueue queue = EmailQueue.read(eventId)
			if (queue && (queue.status == QUEUED || queue.status == ERROR)) {
				Priority priority = queue.priority ?: queue.defaultPriority
				EmailRunnable currentTask = new EmailRunnable(queue)
				RunnableFuture task = emailExecutor.execute(currentTask, priority.value)
				task?.get()
			}
		}
	}
}