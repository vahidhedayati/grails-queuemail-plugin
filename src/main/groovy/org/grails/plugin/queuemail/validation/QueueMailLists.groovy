package org.grails.plugin.queuemail.validation

import static org.grails.plugin.queuemail.enums.ConfigTypes.*
import static org.grails.plugin.queuemail.enums.QueueStatus.*
import static org.grails.plugin.queuemail.enums.SearchTypes.*
import grails.util.Holders
import groovy.transform.CompileStatic

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugin.queuemail.enums.QueueTypes

/**
 * @author Vahid Hedayati
 */
@CompileStatic
class QueueMailLists {
    static final List CHANGE_TYPES=[POOL,MAXQUEUE,PRESERVE,DEFAULTCOMPARATOR,CHECKQUEUE,STOPEXECUTOR,SERVICECONFIGS]
    static final String DELALL='AL'
    static final def deleteList = EmailQueue.REPORT_STATUS_ALL-[DELETED, RUNNING,ACTIVE]+[DELALL]	
    static final List SEARCH_TYPES=[USER,FROM,TO,SUBJECT]
	static final List queueTypes = [QueueTypes.ENHANCED,QueueTypes.BASIC]
	
	static Priority sortPriority(String emailService) {
		Priority priority = Priority.LOW
		Priority configProp =getConfigPriority(emailService)
		if (configProp) {			
			priority = configProp 
		}
		return priority
	}
	static Priority getConfigPriority(String emailService) {
		def conf = getConfig('emailPriorities')
		if (conf) {
			return ((conf as Map)?.find{k,v-> k==emailService}?.value as Priority ?: Priority.LOW)  
		} else {
			return Priority.LOW
		}
	}
	static def getConfig(String configProperty) {
		 Holders.grailsApplication.config.queuemail[configProperty] ?: ''
	}
}
