package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.QueueTypes.*

class BasicQueue extends EmailQueue {
	String getQueueLabel() {
		return BASIC
	}
	Boolean isEnhancedPriority() {
		return false
	}
}
