package org.grails.plugin.queuemail.executors

import static org.grails.plugin.queuemail.enums.QueueStatus.*

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuemail.BasicQueue
import org.grails.plugin.queuemail.EmailRunnable
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.events.BasicQueuedEvent
import org.springframework.context.ApplicationListener

class BasicExecutorQueueService extends ExecutorBaseService  implements ApplicationListener<BasicQueuedEvent> {

	def basicExecutor

	void onApplicationEvent(BasicQueuedEvent event) {
		log.debug "Received ${event.source}"
		BasicQueue queue=BasicQueue.read(event.source)
		if (queue && (queue.status==QUEUED||queue.status==ERROR)) {
			Priority priority = queue.priority ?: queue.defaultPriority
			EmailRunnable currentTask = new EmailRunnable(queue)
			RunnableFuture	task = basicExecutor.execute(currentTask,priority.value)
			task?.get()
		}
	}
}