package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic


@CompileStatic
public enum QueueTypes {
	ENHANCED('E'),
	BASIC('B')
	
	String value

	QueueTypes(String val) {
		this.value = val
	}

	public String getValue(){
		return value
	}
	
	static QueueTypes byValue(String val) {
		values().find { it.value == val }
	}
}
