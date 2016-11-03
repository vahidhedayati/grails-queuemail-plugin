import org.grails.plugin.queuemail.enums.QueueTypes

queuemail {
	
	//standardRunnable = true
	emailPriorities = [
					defaultExample:org.grails.plugin.queuemail.enums.Priority.REALLYSLOW
	]

	// This is an override of grails { mail { configuration method allowing many mail senders
	
	// The configuration for DefaultExampleMailingService has set this to be 2 emails
	// Meaning after 2 it will fall over to 2nd Configuration

	mailConfigExample1 {
		host = "smtp.gmail.com"
		port = 465
		username = "USERA@gmail.com"
		password = "PASSWORDA"
		props = ["mail.debug":"true",
			  "mail.smtp.auth":"true",
			"mail.smtp.socketFactory.port":"465",
			"mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			"mail.smtp.socketFactory.fallback":"false"]
	}
	
	// In our example we only have 2 examples both set to 2 emails.
	// After 4 emails all jobs bound to defaultExampleMailingService will not be
	// sent instead status changed to error in the queue list
	// In effect setting a daily cap per account
	// The caps are checked over a daily basis whilst app is running
	// so if app is running for 2 days on 2nd day caps are reset
	mailConfigExample2 {
		
	  host = "smtp.gmail.com"
	  port = 465
	  username = "USERB@gmail.com"
	  password = "PASSWORDB"
	  props = ["mail.debug":"true",
			  "mail.smtp.auth":"true",
			"mail.smtp.socketFactory.port":"465",
			"mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			"mail.smtp.socketFactory.fallback":"false"]
	}
	
	/*
	 * keepAliveTime in seconds
	 */
	keepAliveTime=300
	
	/*
	 * corePoolSize this should match maximumPoolSize
	 * 
	 */
	corePoolSize=3
	/*
	 * maxPoolSize
	 *
	 */
	maximumPoolSize=3
	
	/*
	 * Amount of elements that can queue
	 */
	maxQueue=100
	
	
	/*
	 * If you have 3 threads and there are 6 reports launched
	 * 
	 * If after (reportThreads - preserveThreads) =
	 * 3 - 1 = 2
	 * 
	 * After 2 threads all report priorities above or equal
	 * perservePriority Permission  = Priority.MEDIUM
	 * will be left in queued state.
	 * This means all those below Priority.MEDIUM will have a spare slot 
	 * to run within. In short an fast lane left open always for those 
	 * below medium and 2 slow lanes. You should be able to configure 6 report 
	 * Threads and 2 reserveThreads. Configure to suite your needs  
	 */
	preserveThreads = 1
	
	// Explained in preseverThreads
	preservePriority = org.grails.plugin.queuemail.enums.Priority.MEDIUM

	defaultEmailQueue=QueueTypes.ENHANCED // org.grails.plugin.queuemail.enums.queueTypes.BASIC


	/*
	 * Enhanced Priority task killer
	 * Kill a running task that runs more than ?
	 * in seconds - which gives you a chance to
	 * give more accuracy than just minutes alone
	 * 60 = 1 minute
	 * 600 = 10 minutes
	 * 3600 = 1 hour
	 *
	 * By default this is 0 = off
	 *
	 */
	killLongRunningTasks=300

	/*
	 * defaultComparator will use out of the box 
	 * comparator method for either of PriorityBlocking
	 * or EnhancedPriorityBlocking
	 * 
	 * If enabled all of the rest of the experimental options below it won't kick in
	 * By default it is off
	 *  
	 */
	defaultComparator=false

	
	/*
	 * Configure this value if you have enabled in BootStrap:
	 *
	 * executorBaseService.rescheduleRequeue()
	 *
	 * This will then attempt to re-trigger all outstanding jobs upon an
	 * application restart.
	 *
	 * Running the above task without below enabled will simply set the
	 * status of any jobs of running back to queued. Making them ready to be
	 * processed. They were tasks that had been running whilst application was
	 * interrupted/stopped.
	 *
	 * You could also use the manual checkQueue method provided on the listing UI
	 *
	 */
	checkQueueOnStart=true
	
	
	/*
	 * DisableExamples basically disables the examples controller
	 * so when you have tested and don't wish to allow this controller to be available on your app
	 * then turn it off through this config set it to true. By default it is false
	 */
	disableExamples=false
	
	/*
	 * if you no longer wish to display queue type on listing screen set this to true
	 */
	hideQueueType=false
	
	/*
	 * If you no longer wish to show report priority on report screen switch this to true 
	 */
	hideQueuePriority=false
}

