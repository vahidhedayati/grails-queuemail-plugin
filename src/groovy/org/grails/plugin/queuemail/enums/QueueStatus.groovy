package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic

@CompileStatic
public enum QueueStatus {
	ACTIVE('AC'),		//ALL STATUSES
	QUEUED('QU'),
	ERROR('ER'),
	RUNNING('RU'),
	CANCELLED('CA'),
	COMPLETED('CO'),
	DELETED('DE'),
	SENT('SE'),
	NORESULTS('NR')
	
	String value

	QueueStatus(String val) {
		this.value = val
	}

	public String getValue(){
		return value
	}
	
	static QueueStatus byValue(String val) {
		values().find { it.value == val }
	}
}
