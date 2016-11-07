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

	def testListTo() {
		if (config.exampleFrom && config.exampleTo) {
			Long userId = queueMailUserService.currentuser
			def locale = RequestContextUtils.getLocale(request)
			List
			Email message = new Email(
					from: config.exampleFrom,
					//EITHER TO CC OR BCC
					to: [config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo],
					//FOR CC
					//cc:[config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo],
					//FOR BCC:
					//bcc: [config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo],
					subject: 'Subject',
					body: "<html>HTML text ${new Date()}</html>"
			)
			/**
			 * Above methods will fail to save, if you prefer you can use cleanTo cleanCc or cleanBcc
			 * same rules as above you must define one.
			 *
			 * If this method is used, the bad addresses are silently removed so object will save and
			 * only those with a good email address with be emailed (the last 2) in this example
			 *
			 *
			 * if you have port 25 open to make outgoing SMTP connections you could try enabling
			 *
			 * queuemail.smtpValidation=true
			 *
			 * This will attempt to check the email address of the recipient from the first MX bound
			 * to their email address. If valid then the email address is silently added.
			 *
			 * This is a pre-delivery confirmation (Experimental)
			 */

			//message.cleanTo(['aa <aa@aa>','bb','cc','dd <dd@example.com>','ee <ee@example.com>'])
			//message.cleanBcc(['aa <aa@aa>','bb','cc','dd <dd@example.com>','ee <ee@example.com>'])
			//message.cleanCc(['aa <aa@aa>','bb','cc','dd <dd@example.com>','ee <ee@example.com>'])
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

	void setGrailsApplication(GrailsApplication ga) {
		config = ga.config.queuemail
	}
}