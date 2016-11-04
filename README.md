Grails QueueMail plugin
=========================

Queuemail plugin is a centralised email queueing system, provides a `BASIC` and `ENHANCED` mode. difference enhanced has
and inner thread launched but then attempts to kill stuck inner threads. Emails that arrive are processed through priority
rules configured by you. You can define how many active concurrent email threads can be running at any one time and how many
can await in the queue to be served.  Each email is then bound to `emailService` that you create and within it you define
the `email configuration names` the email can use and what each email configuration's `limit is per day`.
The queueing system will use the first provided configuration to run from. If this first or top element goes offline or
was configured incorrectly it will hit a threshold and plugin will mark it down. When either it is marked down due to errors
or it has reached defined capacity the plugin will then return the next config and work this way.

If a host is down or not configured correctly the very first email to hit this issue is re-attempted again through it for
the amount of times `failuresTolerated` is configured for. This sole email will be then mark it as down by re-repeating and
when it is marked down it will finally be attempted through the next configuration itself. Meaning it won't give up there
for that email. My tests of 2 failure and a badly configured account ended up in 3 attempts to send 1st email then 1 attempt
for the email after that through seconday configuration.


Whilst making this plugin the issue of TOC violation came up. Please check with your SMTP provider to ensure you are not violating any TOC's whilst attempting to keep within their set limits/boundaries and consequently switching accounts/providers.

# Please use this plugin responsibly

## 1. Installation:

### Grails 3:
```groovy
compile "org.grails.plugins:queuemail:1.2"
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

Please visit above configuration links and read through the comments provided. At the very bottom it covers
how to mark down a configuration element if it fails to consecutively send emails.

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
Please configure `{configName}.fromAddress` as shown below. Please note when this is set the  actual `from` address you
provide will become `replyTo` and from will  be set as `mailConfig1.fromAddress`. If you do not provide this then
nothing is changed.

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


## Example configuration for application.groovy on grails 3
whilst testing gmail all these extra keys were required  (only under grails 3)

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

[Here are some examples](https://github.com/vahidhedayati/grails-queuemail-plugin/blob/master/grails-app/controllers/org/grails/plugin/queuemail/QueueTestController.groovy)
you now call your service. Please refere to examples for further ways of using the `new Email` method

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

```


`buildEmail` calls `queueMailExample` aka `EXAMPLE_SERVICE` which maps up to QueueMailExampleMailingService and pulls
in relevant configuration and runs the email through it's queueing system.

Queues are limited to defined queue limitations as per configuration.
Long outstanding jobs should be killed off automatically if ENHANCED method is used. (refer to configuration links)


Feel free to refer to [queuekit plugin](https://github.com/vahidhedayati/grails-queuekit-plugin) which may give more
insight into some of the additional values not covered such as binding your application with the plugin.
This way each user can only view their own email queue and admin or super users can view all as per default screen.
The queuekit plugin discusses `queuekitUserService` change that to `queueMailUserService` and any reference to how you
override it for this plugin.

You could have multiple services that have totally different sets of email configurations to pickup and depending on
 your scenario then traffic the email to use serviceA or serviceB.

The plugin also provides  `queueMail/listQueue`  controller / action that gives you an overview of how your
emails are being processed. It provides detailed information as to each emailService triggered and their underlying
configuration status/health.


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

Now the MyExampleMailingService is enhancements on above [QueueMailExampleService]
(https://github.com/vahidhedayati/grails-queuemail-plugin/blob/master/grails-app/services/org/grails/plugin/queuemail/examples/QueueMailExampleMailingService.groovy)

but it also defines `def sendMail(executor,queue,jobConfigurations,Class clazz) {`
My first configured job is
In short if you look for  `'sendGrid': 2,`. Then in the sendMail segment `if (sendAccount=='sendGrid') {`.
When it hits this it will come out of the process the plugin takes and do my new additional work with sendGrid.
You can obviously make this into a case statement or wrap further if's to do other third party email sending methods.
It then falls back into else and proceeds with normal plugin stuff. The result is after 2 emails `executor.getSenderCount`
will no longer return  `sendGrid` and therefore it will hit the else block and carry on to `mailConfigExample2` procssed
by actual plugin.

## That is all there is to it :)

```groovy

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.QueueMailBaseService

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
							sendGridService.sendMail {
								from "${queue.email.from}"
								queue.email?.to?.each { t ->
									to "${t}"
								}

								subject queue.email.subject
								body queue.email.text+" from sendGrid"
							}
						} catch (Exception e) {
							println "SendGrid had error E: ${e}"
							failed=true
							code='sendgrid.failed'
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
				actionFailed(executor, queue, jobConfigurations, clazz, sendAccount, error, code, args)
			}
		}
	}
}
```