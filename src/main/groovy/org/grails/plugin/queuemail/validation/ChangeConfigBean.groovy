package org.grails.plugin.queuemail.validation

import static org.grails.plugin.queuemail.enums.ConfigTypes.*
import static org.grails.plugin.queuemail.enums.QueueTypes.*
import grails.converters.JSON
import grails.validation.Validateable

import org.grails.plugin.queuemail.BasicExecutor
import org.grails.plugin.queuemail.EmailExecutor
import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.enums.ConfigTypes
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueTypes

/**
 * ChangeConfigBean used for front end user interaction
 * to accept input from the change configuration 
 * 
 * @author Vahid Hedayati
 *
 */

class ChangeConfigBean extends ChangePriorityBean  implements Validateable {

	int changeValue
	String changeType
	String queueType
	int currentValue
	
	boolean defaultComparator

	Priority currentPriority

	static constraints = {
		//changeType(inList:QueueMailLists.CHANGE_TYPES)
		changeValue(validator: checkValue ) //nullable:true,blank:true,
		queueType(nullable:true)//,inList:QueueTypes.values())		
		currentPriority(nullable:true)
		defaultComparator(nullable:true)
	}

	String getQueueType() {
		if (!queueType && queue) {
			return queue.queueLabel
		} else {
			return queueType
		}
	}

	List getQueueList() {
		switch (changeType) {
			case STOPEXECUTOR:				
			case POOL:				
			case MAXQUEUE:				
			case CHECKQUEUE:
			case PRESERVE:
			case DEFAULTCOMPARATOR:
			default:
				QueueMailLists.queueTypes
				break
		}
	}
	protected def formatBean() {
		def map = loadDefaultValues(queue?.queueLabel)
		if (!currentValue) {
			currentValue= map?.value
		}
		if (!currentPriority) {
			currentPriority=map?.priority	?: Priority.MEDIUM
		}
		defaultComparator=map?.defaultComparator
	}

	JSON loadConfig() {
		def results = loadDefaultValues(queueType)
		return [value: results.value, priority:results.priority, defaultComparator:results.defaultComparator] as JSON
	}

	Map loadDefaultValues(String queueLabel) {
		Map results=[:]
		results.priority=Priority.MEDIUM
		results.value = 0
		results.defaultComparator=false
		def clazz
		switch (queueLabel) {		
			case "${BASIC}":
				results=formatAdvanced(changeType,results,BasicExecutor)
				break
			case "${ENHANCED}":
				results=formatAdvanced(changeType,results,EmailExecutor)
				break
		}
		return results
	}

	private Map formatAdvanced(String changeType,Map results, Class executor) {
		switch (changeType) {
			case "${POOL}":
				results.value = executor?.maximumPoolSize
				break
			case "${MAXQUEUE}":
				results.value = executor?.maxQueue
				break			
			case "${DEFAULTCOMPARATOR}":
				results.defaultComparator= executor?.defaultComparator
				break
			default:
				results.value = executor?.minPreserve
				results.priority=executor?.definedPriority
				break
		}
		return results
	}

	static def checkValue= { val, obj, errors ->
		if (val && val < 0 && obj.changeType != CHECKQUEUE) {
			errors.rejectValue(propertyName, "queuemail.invalidConfigValue.error")
		}
	}
}
