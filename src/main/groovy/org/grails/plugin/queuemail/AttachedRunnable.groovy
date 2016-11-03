package org.grails.plugin.queuemail

import grails.util.Holders


class AttachedRunnable implements Runnable {
	private volatile boolean shutdown
	//def basicExecutor = grailsApplication.mainContext.getBean('basicExecutor')
	//private EmailExecutor emailExecutor
	def emailExecutor = Holders.grailsApplication.mainContext.getBean('emailExecutor')
	private EmailQueue queue
	private Email email

	public AttachedRunnable(EmailQueue queue, Email email){
		this.queue = queue
		this.email = email
	}

	@Override
	public void run() {		
		String name = queue.emailService + queue.serviceLabel
		Thread t
		try {
			def currentService=Holders.grailsApplication.mainContext.getBean(name)
			t = new Thread({currentService.configureMail(emailExecutor,queue)} as Runnable)
			int i=0
			while (!t.isInterrupted() || !shutdown) {
				if (i==0) {
					t.start()
					i++
				}				
				if (t.isInterrupted()||shutdown||!t.isAlive()) {					
					break
				}
			}
			if (shutdown) {
				t.interrupt()
				t.stop()
			}
		} catch (InterruptedException e) {
			e.printStackTrace()
			t.interrupt()
			Thread.currentThread().interrupt()
			return
		}
	}

	public void shutdown() {
		shutdown = true
	}
}
