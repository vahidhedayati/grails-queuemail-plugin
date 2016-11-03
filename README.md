Grails QueueMail plugin
=========================

Queuemail plugin will queue your mail and limited to your pool configuration. You can configure multiple mail accounts to send from and create services that bind the relevant accounts for a given task, so finance may have
 
```		
mailConfig1   --> This binds to Config.groovy object: mailConfig1:{username:abc, password:bbc, host:smtp.mymailservice.com  }
mailConfig2  --> This binds to Config.groovy object: mailConfig2: {username:bbc, password:bbc1, host:smtp.mymailservice.com }
```
``
Here is a sample service created for Finance, it has 2 email accounts that it uses to send email's from, once the first has hit limit of 2 it will from there on try to use next mailConfig in list. This could be a list of many other mailConfigurations and obviously higher limits per account.

Whilst making this plugin the issue of TOC violation came up. My advice is to check with your SMTP provider to ensure you are not violating any TOC's whilst attempting to keep within their limits/boundaries and consequently switching accounts/providers.

#### As an example: [Gmail](https://support.google.com/a/answer/166852?hl=en).

# Please use this plugin responsibly


## 1. Installation:

### Grails 3:
```groovy
compile "org.grails.plugins:queuemail:1.0"
```

##### [source](https://github.com/vahidhedayati/grails-queuemail-plugin/) |
 [demo](https://github.com/vahidhedayati/test-queuemail3/)

### Grails 2:
```groovy
compile ":queuemail:1.0"
```

##### [source](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2) |
 [demo](https://github.com/vahidhedayati/test-queuemail)


## 2. Configuration
[Configuration grails 3](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/master/grails-app/conf/SampleConfig.groovy)
[Configuration grails 2](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2/grails-app/conf/SampleConfig.groovy)
The configuration provided would be added to | Config.groovy (grails 2)  | application.groovy (grails 3).


When using this plugin you can configure `mailConfig1.fromAddress` for each configuration. If this is confgured when you create an email as per example's below. The actual `from` address you provide will become `replyTo` and from will be set as `mailConfig1.fromAddress`.
If you do not provide this then it will be set to the `from` address provided as per email.


```groovy
package org.grails.plugin.queuemail.examples

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.QueueMailBaseService

class QueueMailExampleService extends QueueMailBaseService {

	def configureMail(executor,EmailQueue queue) {
		/**
		 * Contains a list of configuration names : daily limit for account
		 * So mailConfigExample1 will bind to Config.groovy smtp configuration
		 * assuming it is google it may have 3000 daily limit 
		 * A listing is provided so it can fall over between list elements starting from top
		 * working it's way down and when limit exceeds it will use the next element
		 */
		def jobConfigurations = [
				'mailConfigExample1': 2,
				'mailConfigExample2': 2,				
			]		
		
		sendMail(executor,queue,jobConfigurations)
	}
}
```

Now with queueMailExample service created, refer to `SampleConfig.groovy` add the relevant accounts to the configuration as shown to match with above names. In the most basic form if you add the following to your Config.groovy or application.groovy

## Example configuration for gmail on grails 2 application
```groovy

queuemail {
	//Used by TestController to send emails from/to
	exampleFrom="usera <userA@gmail.com>"
	exampleTo="userA_ReplyTo <userA@gmail.com>"
	
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
	mailConfigExample1.fromAddress="USERA@gmail.com"
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
	mailConfigExample2.fromAddress="USERB@gmail.com"
}	
```	


##Example configuration for application.groovy on grails 3
## Gmail had to be configured differently to work via this plugin:
```groovy
queuemail {
        exampleFrom="badvad <userA@gmail.com>"
        exampleTo="badvad <userB@gmail.com>"
        //standardRunnable = true
        emailPariorities = [
                        defaultExample:org.grails.plugin.queuemail.enums.Priority.REALLYSLOW
        ]

        mailConfigExample1 {
             host = "smtp.gmail.com"
             port = 587
             username = "userA@gmail.com"
             password = 'PASSWORD'
             props = ["mail.debug":"true",
			    "mail.smtp.user":"userA@gmail.com",
                "mail.smtp.host": "smtp.gmail.com",
                "mail.smtp.port": "587",
                "mail.smtp.auth": "true",
                "mail.smtp.starttls.enable":"true",
                "mail.smtp.EnableSSL.enable":"true",
                "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
                "mail.smtp.socketFactory.fallback":"false",
                "mail.smtp.socketFactory.port":"465"
             ]
        }
        mailConfigExample1.fromAddress="USERAAA <userA@gmail.com>"

        mailConfigExample2 {
            host = "smtp.gmail.com"
            port =587
            username = "userB@gmail.com"
            password = 'PASSWORD'
		    props = ["mail.debug":"true",
                "mail.smtp.user":"userB@gmail.com",
                "mail.smtp.host": "smtp.gmail.com",
                "mail.smtp.port": "587",
                "mail.smtp.auth": "true",
                "mail.smtp.starttls.enable":"true",
                "mail.smtp.EnableSSL.enable":"true",
                "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
                "mail.smtp.socketFactory.fallback":"false",
                "mail.smtp.socketFactory.port":"465"
            ]
        }
        mailConfigExample2.fromAddress="userBB<userB@gmail.com>"
}

```

Here are  some ways you now call your service:


```groovy

package org.grails.plugin.queuemail

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import org.springframework.web.servlet.support.RequestContextUtils

class TestController implements GrailsApplicationAware {

	static defaultAction = 'index'
	def config
	GrailsApplication grailsApplication

	def queueMailApiService
	def queueMailUserService


	private String VIEW = '/examples/index'

    /**
    * This binds to QueueMailExamplesService
    * Which contains a list of smtp configuration and daily limit
    */
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
```


in buildEmail it calls queueMailExample which maps up to FinanceMailingService and pulls in relevant configuration and runs the email through it's queuing system.Queues are limited to defined queue limitations as per configuration.


Long outstanding jobs should be killed off automatically if ENHANCED method is used.


Feel free to refer to [queuekit plugin](https://github.com/vahidhedayati/grails-queuekit-plugin) which may give more insight into some of the configuration values.


In short your email's will go through a queueing system with limitations as to how many email threads can run concurrently, regardless of email traffic sent through queueing system. You can change account configurations and send via different accounts if required.

To send through accounts for the same email task, you may have to create multiple services like above example each covering the account(s) to be used then when scenario A is hit you use serviceA when scenarioB is hit you use serviceB. Each will then route emails according to account configurations like shown above.


You also get an interface to view what has been sent / queued. Jobs that fail due to what ever reason are marked as error and error results are captured and can be viewed via the display pop up screen on queuemail listing. For example change one of your account's password so that it fails then look at the email you sent through it through the listing screen / display option.

   