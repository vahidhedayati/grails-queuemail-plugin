package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic;

@CompileStatic
public enum SearchTypes {
	
	USER('US'),
	FROM('FR'),
	TO('TO'),
	SUBJECT('SU')
	
	
	String value
		
	SearchTypes(String val) {
		this.value = val
	}	
	public String getValue(){
		return value
	}
	static SearchTypes byValue(String val) {
		values().find { it.value == val }
	}
}
