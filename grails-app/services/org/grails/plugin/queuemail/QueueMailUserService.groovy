package org.grails.plugin.queuemail

import grails.util.Holders
import org.grails.plugin.queuemail.enums.Priority
import org.grails.plugins.web.taglib.ApplicationTagLib

/**
 * ! - IMPORTANT NOTICE - !
 * 
 * The only thing that you should need to extend / 
 * modify behaviour of is all in this class.
 * 
 * So In grails 2 create a new Service i.e :
 *  test/MyUserService
 *  
 *  Open conf/spring/resources.groovy and add following 
 import test.MyUserService
 beans = {	
 queueMailUserService(MyUserService)
 }
 * 
 * @author Vahid Hedayati
 *
 */

class QueueMailUserService {

	def g = Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)

	/*
	 * Override this service and method to return your real user
	 * must return their userId 
	 */
	Long getCurrentuser() {
		return 1L
	}

	/*
	 * Overrider this method to then ensure superUser
	 * Privileges are only given to superUser's as per your definition
	 * if it is a security group or some user role.
	 */
	boolean isSuperUser(Long userId) {
		return userId==1L
		
	}

	/*
	 * Override this to get the real users UserName
	 * Return String username bound to userId (long digit)
	 */
	String getUsername(Long userId) {
		return ''
	}
	
	Long getRealUserId(String searchBy) {
		/*
		 * 
		 * We are looking for the real user Id that is bound 
		 * to given search username - you may need to UsernameLike 
		 * or something that matches how you want the search to work
		 * best
		 * 
		 * User = User.findByUsername(searchBy)
		 * if (user)
		 * return user.id
		 * 
		 */
		return 1L
	} 

	/*
	 * Override this to return a locale for your actual user
	 * when running reports if you have save their locale on the DB
	 * you can override here it will be defaulted to null and set to 
	 * predfined plugin value in this case
	 * 
	 */
	Locale  getUserLocale(Long userId) {
		return null
	}

	/**
	 * !! ATTENTION !! 
	 * You are not required to override  :
	 * reportPriority or checkReportPriority
	 * 
	 * If you have not enabled standardRunnable or 
	 * set standardRunnable = false
	 * then this aspect does not need to be touched.
	 * 
	 * If you decide to use standardRunnable=true
	 * 
	 * Then override this method in your local extended
	 * version of this class. 
	 *  
	 * This will be your last chance for this scenario to capture
	 * and override the report status.
	 * 
	 * This is an alternative method than using the provided default.
	 * If you wish to configure reports in this manner centralised.
	 * 
	 * You will still need to declare 
	 * Priority getQueuePriority in your extends reportService. 
	 * 
	 * But take a look at ParamsExampleReportingService since 
	 * you can keep it plain
	 * 
	 * -------------------------------------------------------------
	 * How it works
	 * 
	 * Whilst you can configure a report to have LOW priority
	 * It could be that it needs to be LOW for long term date range
	 * but HIGH for a short 1 day lookup
	 * 
	 * This is a final stage before actual priority is selected
	 * which if not found here will be actual report default
	 * as defined in configuration if not by plugin default choice LOW
	 */
	Priority reportPriority(EmailQueue queue, Priority givenPriority, Email email) {
		Priority priority = queue.priority ?: queue.defaultPriority
		if (givenPriority < priority) {
			priority = givenPriority
		}
		switch (queue.emailService) {
			case 'default':
				priority = checkReportPriority(priority,email)
				break
		}		
		return priority
	}

	Priority checkReportPriority(Priority priority,email) {
		return priority
	}

}
