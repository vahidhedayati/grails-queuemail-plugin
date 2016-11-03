package org.grails.plugin.queuemail.executors

import static org.grails.plugin.queuemail.enums.QueueStatus.*

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.EmailRunnable
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.events.EmailQueuedEvent
import org.springframework.context.ApplicationListener

/**
 * Priority Blocking uses priorityBlockingExecutor to manage report queue.
 * This is the default listener provided by the plugin and report priority can be either provided 
 * per call or by Config.groovy
 * 
 * 
 */
class EmailExecutorQueueService extends ExecutorBaseService implements ApplicationListener<EmailQueuedEvent> {

	def emailExecutor

	void onApplicationEvent(EmailQueuedEvent event) {
		log.debug "Received ${event.source}"
		EmailQueue queue=EmailQueue.read(event.source)
		if (queue && (queue.status==QUEUED||queue.status==ERROR)) {
			Priority priority = queue.priority ?: queue.defaultPriority
			EmailRunnable currentTask = new EmailRunnable(queue)
			RunnableFuture task = emailExecutor.execute(currentTask,priority.value)			
			task?.get()
		}

	}

}