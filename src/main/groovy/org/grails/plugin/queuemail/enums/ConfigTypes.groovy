package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic;

@CompileStatic
public enum ConfigTypes {
	POOL('PO'),
	PRESERVE('PR'),
	CHECKQUEUE('CQ'),
	STOPEXECUTOR('ST'),
	DEFAULTCOMPARATOR('DC'),
	MAXQUEUE('MQ')
	
	String value
		
	ConfigTypes(String val) {
		this.value = val
	}	
	public String getValue(){
		return value
	}
	static ConfigTypes byValue(String val) {
		values().find { it.value == val }
	}
}
