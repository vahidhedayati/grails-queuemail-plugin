package org.grails.plugin.queuemail.events

import org.springframework.context.ApplicationEvent

class BasicQueuedEvent extends ApplicationEvent {
	
	BasicQueuedEvent(source) {
		super(source)
	}
}
