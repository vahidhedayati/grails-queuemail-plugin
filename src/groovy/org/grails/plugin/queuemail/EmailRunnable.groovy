package org.grails.plugin.queuemail

import grails.util.Holders


class EmailRunnable implements Runnable {


	private EmailQueue queue

	public EmailRunnable(EmailQueue queue){
		this.queue=queue
	}

	@Override
	public void run() {
		
		String name = queue.emailService+queue.serviceLabel
		try {
			def currentService =  Holders.grailsApplication.mainContext.getBean(name)
			currentService.executeReport(queue)

		} catch (InterruptedException e) {
			e.printStackTrace()
		}
	}
}