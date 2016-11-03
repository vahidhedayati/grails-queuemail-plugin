package org.grails.plugin.queuemail

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import org.springframework.web.servlet.support.RequestContextUtils

class QueueTestController implements GrailsApplicationAware {

	static defaultAction = 'index'
	def config
	GrailsApplication grailsApplication

	def queueMailApiService
	def queueMailUserService


	private String VIEW = '/examples/index'
	private String EXAMPLE_SERVICE='queueMailExample'
	private String FAILURE = "please configure config keys exampleFrom exampleTo"


	def notFound() {
		render status: response.SC_NOT_FOUND
		return
	}

	def index() {
		render view: VIEW
	}

	def testTextEmail() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId = queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			Email message = new Email(
					from: config.exampleFrom,
					to: [config.exampleTo],
					subject: 'Subject',
					text: 'Testing text message being sent via plugin'
					//html: params
			).save(flush: true)
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE, userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['TestTextEmail', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}

	def testTemplateEmail() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId = queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)

			//This loads in a template and provides model which is the instance list for template
			def paramsMap = [:]
			paramsMap.view = "/examples/testTemplate"
			paramsMap.model = [var1: "hello", var2: "there"]
			// Or Like this
			//Map paramsMap =  [view:"/examples/testTemplate",model:[var1:"hello", var2:"there"]]

			Email message = new Email(
					from: config.exampleFrom,
					to: [config.exampleTo],
					subject: 'Subject',
					html: paramsMap
			)
			if (!message.save(flush: true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE, userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testTemplateEmail', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}


	def testBodyEmail() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId = queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			Email message = new Email(
					from: config.exampleFrom,
					to: [config.exampleTo],
					subject: 'Subject',
					body: '<html>HTML text</html>'
			)
			if (!message.save(flush: true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE, userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testBodyEmail', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}

	/**
	 * In order to get your email addresses validated use this method
	 * @return
	 */
	def testListTo() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId = queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			List
			Email message = new Email(
					from: config.exampleFrom,
					subject: 'Subject',
					body: "<html>HTML text ${new Date()}</html>"
			)
			message.to([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			if (!message.save(flush:true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE,userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testListTo', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}
	
	def testListBcc() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId= queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			List
			Email message = new Email(from:config.exampleFrom,
					subject: 'Subject',
					body:  " <html>HTML text ${new Date()}</html>"
			)
			message.bcc([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			if (!message.save(flush:true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE,userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testListBcc', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}
	
	def testListCc() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId= queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			List
			Email message = new Email(
				from:config.exampleFrom,
				subject: 'Subject',
				body:  "<html>HTML text ${new Date()}</html>"
			)			
			message.cc([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			if (!message.save(flush:true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE,userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testListCc', queue?.id])
			render view:VIEW
			return
		}
		render FAILURE
	}

	void setGrailsApplication(GrailsApplication ga) {
		config = ga.config.queuemail
	}
}