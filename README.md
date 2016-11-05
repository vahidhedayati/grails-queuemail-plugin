Grails QueueMail plugin
=========================

Queuemail plugin is a centralised email queueing system. It provides two queueing mechanism's `BASIC` and `ENHANCED (default)`. 
Enhanced launches an additional thread for each running task and it will attempt to kill the process if it 
exceeds `killLongRunningTasks` period (it will be considered as stuck).
  
Arriving emails are processed through priority rules configured by you. 
You can define how many active concurrent email threads can be running at any one time and how many
can wait in the queue to be served.  Each email is then bound to the `emailService` that you create and within it you define
the `configNames` and `limit`. The configName will then need to be created in your applications Config.groovy/application.groovy
and essentially contains the SMTP configuration required to connect through and send email's.

The queueing system will use the first provided configuration for every email requested. If this first element goes offline or
was configured incorrectly it will hit a threshold and plugin will mark it down. 

If there is a problem the very email that hits the problem is retried over and over until it reaches 
 `failuresTolerated` level. At this point the element is marked as down and the email that same email is then sent to next element.
 All new email's will now be going through second configured element.
 
Whilst making this plugin the issue of `Violating TOC` came up, so please check with your SMTP provider to ensure you are not violating any TOC's whilst attempting to keep within their set limits/boundaries and consequently/possibly having to switch accounts/providers.

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
host failures and limiting / restricting host failures.

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

## Example configuration for grails 2
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
whilst testing gmail  (only under grails 3) all these extra keys were required 

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

[QueueTestController](https://github.com/vahidhedayati/grails-queuemail-plugin/blob/master/grails-app/controllers/org/grails/plugin/queuemail/QueueTestController.groovy) used to show some of the examples below:

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


You store an `Email` in it's domain class. Then you call `buildEmail` which
 
`buildEmail(EXAMPLE_SERVICE, userId, locale, message)` 
```
EXAMPLE_SERVICE=queueMailExample
userId=current userId
locale=current locale/user locale
message=That email above you just saved
```
The `queueMailExample`  maps up to `QueueMailExampleMailingService` that you would create to match your rule 
name and described above here. 

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

`MyExampleMailingService` enhances or modifies the behaviour of `sendMail` function 
that resides in `QueueMailBaseService` and is what this service extends from. 


My first configured job is `'sendGrid': 2,`.  If you look within `sendMail` segment below you will notice 
`if (sendAccount=='sendGrid') {`.
Whilst it is within the limitation of 2 jobs it will the if statement and use `sendGridService.sendMail`. 
Feel free to add other if statements and expand on the idea to other third party 
email plugin's or custom mail api's. 
The final else should be left as it is since it will return only when it hits a job that no longer matches your defined if statements. 
So in this example `'mailConfigExample2'` will hit the else block after 2 emails was sent via `sendGrid` because
`executor.getSenderCount` would mark  `sendGrid` down and return `mailConfigExample2` for the next email's. 


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

## That is all there is to it :)
