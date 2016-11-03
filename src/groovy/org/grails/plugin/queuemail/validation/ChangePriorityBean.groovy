package org.grails.plugin.queuemail.validation

import grails.validation.Validateable

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.enums.Priority

@Validateable
class ChangePriorityBean {

	Priority priority
	EmailQueue queue

	static constraints = {
		priority(nullable: true)
	}

}
