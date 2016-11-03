package org.grails.plugin.queuemail.events

import org.springframework.context.ApplicationEvent

class EmailQueuedEvent extends ApplicationEvent {
	
	EmailQueuedEvent(source) {
		super(source)
	}

}
