Grails QueueMail plugin
=========================

Queuemail plugin will queue your mail and limited to your pool configuration. You can configure multiple mail accounts
to send from and create services that bind the relevant accounts for a given task i.e.:
 
```		
mailConfig1   --> This binds to Config.groovy object: mailConfig1:{username:abc, password:bbc, host:smtp.mymailservice.com  }
mailConfig2  --> This binds to Config.groovy object: mailConfig2: {username:bbc, password:bbc1, host:smtp.mymailservice.com }
```

You can configure multiple mail accounts like shown above. The example has 2 email accounts that it uses to send email's from, once the first has hit limit of 2 it will from there on try to use next mailConfig in list. This could be a list of many other mailConfigurations and obviously higher limits per account.

Whilst making this plugin the issue of TOC violation came up. Please check with your SMTP provider to ensure you are not violating any TOC's whilst attempting to keep within their set limits/boundaries and consequently switching accounts/providers.

# Please use this plugin responsibly

## 1. Installation:

### Grails 3:
```groovy
compile "org.grails.plugins:queuemail:1.1"
```

##### [source](https://github.com/vahidhedayati/grails-queuemail-plugin/) 

### Grails 2:
```groovy
compile ":queuemail:1.0"
```

##### [source](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2) 

## 2. Configuration
[Configuration for grails 3 application.groovy](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/master/grails-app/conf/SampleConfig.groovy)

[Configuration for grails 2 Config.groovy](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2/grails-app/conf/SampleConfig.groovy)



## Configuration for unreliable SMTP services

Please visist above configuration links and read through the comments of configuration examples provided.
You can make the plugin mark an SMTP provider that fails to send an email down and also let it then try to re-enable
 itself after a given amount of queueuId increments or given time period.
If an email fails to be sent from configExample1 (due to host down bad username/password) it will attempt that same
configuration element for the set `failuresTolerated`. If this is set to 2 then after 2 failures of the same attempt it
will move to 2nd configExample and send Email. So email is sent and the process of marking a configuration
(SMTP service) as down kind of happens through 1 email just retrying to re-send. This probably needs more work.
As mentioned it is given further chances and you can control that. So do read on.


## Basic service the defines SMTP configurations (that are binded to Config objects and limitations per day)
```groovy
class QueueMailExampleService extends QueueMailBaseService {

	def configureMail(executor,EmailQueue queue) {
		/**
		 * Contains a list of configuration names : daily limit for account
		 * So mailConfigExample1 will bind to Config.groovy SMTP configuration
		 * assuming it is google it may have 3000 daily limit 
		 * A listing is provided so it can fall over between list elements starting from top
		 * working it's way down and when limit exceeds it will use the next element
		 */
		def jobConfigurations = [
				'mailConfigExample1': 2,
				'mailConfigExample2': 2,				
			]
        sendMail(executor,queue,jobConfigurations,QueueMailExampleService.class)
	}
}
```

When using this plugin you can configure `mailConfig1.fromAddress` for each configuration. If this is configured when
 you create an email as per example's below. The actual `from` address you provide will become `replyTo` and from will
 be set as `mailConfig1.fromAddress`.
If you do not provide this then it will be set to the `from` address provided as per email.

Now with queueMailExample service created, refer to `SampleConfig.groovy` add the relevant accounts to the
configuration as shown to match with above names. In the most basic form if you add the following to your
Config.groovy or application.groovy

## Example configuration for gmail on grails 2 application
```groovy

queuemail {

	mailConfigExample1 {
		host = "smtp.internal.com"
		port = 465
		username = "USERA@internal.com"
		password = "PASSWORDA"
		props = ["mail.debug":"true",
			  "mail.smtp.auth":"true",
			"mail.smtp.socketFactory.port":"465",
			"mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			"mail.smtp.socketFactory.fallback":"false"]
	}
	mailConfigExample1.fromAddress="USERA@internal.com"

	// In our example we only have 2 examples both set to 2 email's.
	// After 4 email's all jobs bound to defaultExampleMailingService will not be
	// sent instead status changed to error in the queue list
	// In effect setting a daily cap per account
	// The caps are checked over a daily basis whilst app is running
	// so if app is running for 2 days on 2nd day caps are reset
	mailConfigExample2 {
		
	  host = "external.smtp.com"
	  port = 465
	  username = "USERB@smtp.com"
	  password = "PASSWORDB"
	  props = ["mail.debug":"true",
			  "mail.smtp.auth":"true",
			"mail.smtp.socketFactory.port":"465",
			"mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
			"mail.smtp.socketFactory.fallback":"false"]
	}
	mailConfigExample2.fromAddress="USERB@smtp.com"
}	
```	


## Example configuration for application.groovy on grails 3 (whilst testing gmail all these extra keys required)

```groovy
import org.grails.plugin.queuemail.enums.QueueTypes

queuemail {

	//standardRunnable = true
	emailPriorities = [
					defaultExample:org.grails.plugin.queuemail.enums.Priority.REALLYSLOW
	]

	// This is an override of grails { mail { configuration method allowing many mail senders

	// The configuration for DefaultExampleMailingService has set this to be 2 email's
	// Meaning after 2 it will fall over to 2nd Configuration

	exampleFrom="usera <userA@gmail.com>"
	exampleTo="userA_ReplyTo <userA@gmail.com>"

	mailConfigExample1 {
		host = "smtp.internal.com"
		port = 587
		username = "userA@internal.com"
		password = 'PASSWORD'
		props = ["mail.debug":"true",
				 "mail.smtp.user":"userA@internal.com",
				 "mail.smtp.host": "smtp.internal.com",
				 "mail.smtp.port": "587",
				 "mail.smtp.auth": "true",
				 "mail.smtp.starttls.enable":"true",
				 "mail.smtp.EnableSSL.enable":"true",
				 "mail.smtp.socketFactory.class":"javax.net.ssl.SSLSocketFactory",
				 "mail.smtp.socketFactory.fallback":"false",
				 "mail.smtp.socketFactory.port":"465"
		]
	}
	mailConfigExample1.fromAddress="USERAAA <userA@internal.com>"

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

[Here are some ways you now call your service:](https://github.com/vahidhedayati/grails-queuemail-plugin/blob/master/grails-app/controllers/org/grails/plugin/queuemail/QueueTestController.groovy)



```groovy

            //BASIC TEXT
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

            //HTML TEMPLATE
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

			//BODY HTML
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


            /**
             * In order to get your email addresses validated use this method
             * @return
             */
			Email message = new Email(
					from: config.exampleFrom,
					subject: 'Subject',
					body: "<html>HTML text ${new Date()}</html>"
			)
			message.to([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			//BCC
			//message.bcc([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			//CC
            //message.cc([config.exampleTo, config.exampleTo, config.exampleTo, config.exampleTo])
			if (!message.save(flush:true)) {
				log.error message.errors
			}
			def queue = queueMailApiService.buildEmail(EXAMPLE_SERVICE,userId, locale, message)
			flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['testListTo', queue?.id])


```


in buildEmail it calls queueMailExample which maps up to QueueMailExampleMailingService and pulls in relevant
configuration and runs the email through it's queuing system.Queues are limited to defined queue limitations as
per configuration.


Long outstanding jobs should be killed off automatically if ENHANCED method is used.


Feel free to refer to [queuekit plugin](https://github.com/vahidhedayati/grails-queuekit-plugin) which may give more
insight into some of the configuration values.


In short your email's will go through a queueing system with limitations as to how many email threads can run
concurrently, regardless of email traffic sent through queueing system. You can change account configurations
 and send via different accounts if required.

To send through accounts for the same email task, you may have to create multiple services like above example each
covering the account(s) to be used then when scenario A is hit you use serviceA when scenarioB is hit you use serviceB.
 Each will then route email's according to account configurations like shown above.


You also get an interface to view what has been sent / queued. Jobs that fail due to what ever reason are marked as
error and error results are captured and can be viewed via the display pop up screen on queuemail listing.
For example change one of your account's password so that it fails then look at the email you sent through it
through the listing screen / display option.


## Configuring grails sendgrid plugin to with queuemail plugin
Please note this is an example tested and working, whilst the theory of this was only tested with sendgrid. You will
easily be able to repeat the same process to use other third party plugins/methods you already use by changing the bit
that does the work for sendGrid plugin.

Added the plugin to test site:
```groovy
 compile 'desirableobjects.grails.plugins:grails-sendgrid:2.0.1'
```
Configured application.yml
```groovy
sendgrid:
        username: 'myUsername'
        password: 'bigSecret'
```

A new controller action:
```groovy
 def testSendGrid() {
            String myService='myExample'
            Long userId = queueMailUserService.currentuser
            def locale = RequestContextUtils.getLocale(request)
            Email message = new Email(
                    from: "userA <userA@myDomain.com>",
                    to: ['userB <userB@gmail.com>'],
                    subject: 'Subject-------------------------',
                    text: 'Testing text message being sent via plugin'
                    //html: params
            ).save(flush: true)
            def queue = queueMailApiService.buildEmail(myService, userId, locale, message)
            flash.message = g.message(code: 'queuemail.reportQueued.label', args: ['TestTextEmail', queue?.id])
            render  "all done"
            return
        }
```

Now the MyExampleMailingService is a little like [example provided]
(https://github.com/vahidhedayati/grails-queuemail-plugin/blob/master/grails-app/services/org/grails/plugin/queuemail/examples/QueueMailExampleMailingService.groovy)

but it also defines `@Override
                    	def sendMail(executor,queue,jobConfigurations,Class clazz) {`

If you look through that bit you will see I manually intefered with the configName(sendAccount) (verified via queuemail plugin).
If sendAccount was 'sendGrid' (as per first top definition that has a daily limit of 2).
This is then manually called through my check and email sent that way. The rest of the code and including the else block
 will be needed. This will then keep the process flowing via the plugin. After 2 emails my next email was sent through
 normal plugin mailConfigExample2 which uses the default grails mail plugin method to send emails
 (as discussed all of above)

 So simply change the bit under`	println "Sending via sendGrid"` to go through your other third party service.

 That is all there is to it

```groovy
package test

import grails.web.context.ServletContextHolder
import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.QueueMailBaseService
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils

class MyExampleMailingService extends QueueMailBaseService {

	def sendGridService

	def configureMail(executor,EmailQueue queue) {
		def jobConfigurations = [
				'sendGrid': 2,
				'mailConfigExample2': 100,
			]

		sendMail(executor,queue,jobConfigurations,MyExampleMailingService.class)
	}
	@Override
	def sendMail(executor,queue,jobConfigurations,Class clazz) {
		boolean failed=true
		String sendAccount
		String error=''
		String code
		List args
		try {
			if (jobConfigurations) {
				sendAccount = executor.getSenderCount(clazz,jobConfigurations,queue.id)
				if (sendAccount) {
					//This bit has change from what is in QueueMailBaseService
					if (sendAccount=='sendGrid') {
						println "Sending via sendGrid"
						try {
							EmailQueue.withTransaction {
								sendGridService.sendMail {
									from queue.email.from
									to queue.email.to


									subject queue.emai.subject
									body queue.email.text+"----------------------------------------AAAAAA"
								}

							}
						} catch (Exception e) {
							println "------------------------------------------ E: ${e}"
						}
					} else {
						println "Returning it back to how plugin was doing things"
						queueMailService.sendEmail(sendAccount,queue)
					}
					failed=false
				} else {
					code='queuemail.dailyLimit.label'
					args=[jobConfigurations]
				}
			} else {
				code='queuemail.noConfig.label'
			}
		}catch (Exception e) {
			failed=true
			error=e.message
		} finally {
			if (failed) {
				/**
				 * If we have an account meaning also mark down those that peaked limit
				 * make them inactive as well all those that failed to send in a row over failuresTolerated
				 * configured value
				 */
				if (sendAccount) {
					executor.registerSenderFault(clazz, queue.id, sendAccount)
				}
				if (code) {
					def webRequest = RequestContextHolder.getRequestAttributes()
					if(!webRequest) {
						def servletContext  = ServletContextHolder.getServletContext()
						def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
						webRequest =  grails.util.GrailsWebMockUtil.bindMockWebRequest(applicationContext)
					}
					if (args) {
						error = g.message(code: code,args:args)
					} else {
						error = g.message(code: code)
					}
				}
				if (sendAccount) {
					//Try again to see if it can be sent
					logError(queue, error)
					sendMail(executor,queue,jobConfigurations, clazz)
				} else {
					// no options left here
					errorReport(queue, error)
				}
			}
		}
	}
}

```