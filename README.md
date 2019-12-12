Grails QueueMail plugin
=========================

### Side note - similar / related projects
- [grails-queuekit-plugin](https://github.com/vahidhedayati/grails-queuekit-plugin)

- [grails-queuemail-plugin (this)](https://github.com/vahidhedayati/grails-queuemail-plugin)


Queuemail plugin is a centralised email queueing system configurable for many providers all centrally controlled and limited to either daily limit or failures exceeding failureTolerance limit (in a row). By default all email's passing through are priority driven and configured by overall customService name.  Two methods of priority queueing are provided `BASIC` and `ENHANCED (default)`.  Enhanced launches an additional thread for each running task and will attempt to kill any running process considered as stuck (if time taken exceeds `killLongRunningTasks` configuration period). 
  
Email's that arrive in queue are processed through priority rules, please refer to example configuration, each new service you create can be configured to have a specific priority. Higher ones run in preference of lower ones.
 
Configure how many active concurrent email threads can run at any one time and how many
can wait in the queue to be served.  Each email is then bound to the `emailService` that you create and within it you define
the `configNames` and `limit`. The configName will then need to be created in your applications Config.groovy/application.groovy
and essentially contains the SMTP configuration required to connect through and send email's.

The queueing system will use the first provided configuration for every email requested. If this first `configName` goes offline or
was configured incorrectly it will hit a threshold and plugin will mark configuration as `inactive` 

If an email send attempt fails the sole email is re-attempted until it reaches 
 `failuresTolerated` level. Once this happens current `configName` is marked as `inactive` and the next `configName` is attempted to deliver this email. 
 All new email's will now be going through second `configName`. The `configName` that was made `inactive` will automatically re-join active pool after either setPeriod of time or amount of queueId's passing through. Please refer to notes/configuration and specific segment on message exceptions below.
 
Please check with your SMTP provider to ensure you are not violating any TOC's whilst attempting to keep within their set limits/boundaries and consequently/possibly having to switch accounts/providers.

# Please use this plugin responsibly

## 1. Installation/Configuration:

### Grails 3: [source](https://github.com/vahidhedayati/grails-queuemail-plugin/) [configuration](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/master/grails-app/conf/SampleConfig.groovy)
```groovy
compile "org.grails.plugins:queuemail:1.3"
```


### Grails 2: [source](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2) [configuration](https://github.com/vahidhedayati/grails-queuemail-plugin/tree/grails2/grails-app/conf/SampleConfig.groovy)
```groovy
compile ":queuemail:1.0"
```

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
provide will become `replyTo` and from will  be set as `{configName}.fromAddress`. If you do not provide this then
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
All the additional smtp configuration required whilst testing gmail (only under grails 3): 

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
			def queue = queueMailApiService.buildEmail('queueMailExample', userId, locale, message)


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
			def queue = queueMailApiService.buildEmail('queueMailExample', userId, locale, message)

			//To Many recipients:
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

```


You store an `Email` in it's domain class. Then you call `buildEmail` which
 
`buildEmail(EXAMPLE_SERVICE, userId, locale, message)` 
```
queueMailExample=maps up to QueueMailExampleMailingService (you create this) 
userId=current userId
locale=current locale/user locale
message=That email above you just saved
```


## Configuration for unreliable SMTP services
Please visit above configuration links and read through the comments provided. At the very bottom it covers
host failures and how to limit / restrict host failures.


## Binding plugin with your application userbase
Feel free to refer to [queuekit plugin](https://github.com/vahidhedayati/grails-queuekit-plugin) which may give more
insight into some of the additional values not covered such as binding your application with the plugin.
This way each user can only view their own email queue and admin or super users can view all as per default screen.
The queuekit plugin discusses `queuekitUserService` change that to `queueMailUserService` and any reference to how you
override it for this plugin.

## Many services for a given scenario
You could have multiple services that have totally different sets of email configurations to pickup and depending on
 your scenario then traffic the email to use serviceA or serviceB.

## Interface to queueing system
The plugin also provides  `queueMail/listQueue`  controller / action that gives you an overview of how your
email's are being processed. It provides detailed information as to each emailService triggered and their underlying
configuration status/health.


## Message errors / Exceptions and configuration activation
1.3 introduced `enums.MessageExceptions` and `monitor.ServiceConfigs`. 
SMTP providers that trigger an exception depending on exception type can trigger actual provider(itself)
to become inactive. The most obvious example used is when there is an authentication failure. There is no point
in giving this provider a 2nd chance to join the pool since it is obviously incorrectly configured. 
An override feature has been added to the web interface which provides you with option to change a specific configuration limit, active status and MessageException status.
If it has failed and you wish to re-activate it - you should remove Message Exception and set active to true. 
These are dynamic values that are over-written upon application restart.
 

## Configuring sendgrid or other API's/mail plugins to work with queuemail plugin
Please note this is an example tested and working, whilst this covers sendGrid, this theory could be expanded 
over other mail plugin's or mail api's. 


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
So in this example `'mailConfigExample2'` will hit the else block after 2 email's was sent via `sendGrid` because
`executor.getSenderCount` would mark  `sendGrid` down and return `mailConfigExample2` for the next email's. 


```groovy

import org.grails.plugin.queuemail.EmailQueue
import org.grails.plugin.queuemail.QueueMailBaseService
import org.grails.plugin.queuemail.enums.MessageExceptions

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
    def sendMail(executor,queue,jobConfigurations,Class clazz,MessageExceptions currentException=null) {
   		boolean failed=true
   		boolean resend=false
   		String sendAccount
   		String error=''
   		String code
   		List args
   		try {
   			if (jobConfigurations) {
   				sendAccount = executor.getSenderCount(clazz, jobConfigurations, queue.id,currentException)
   				if (sendAccount) {
   					if (sendAccount=='sendGrid') {
   						println "Sending via sendGrid"
   						try {
   							sendGridService.sendMail {
   								from "${queue.email.from}"
   								queue.email?.to?.each { t ->
   									println "sending to ${t}"
   									to "${t}"
   								}
   								subject queue.email.subject+"-- from sendgrid"
   								body " from sendGrid"
   							}
   						} catch (Exception e) {
   							println "SendGrid had error E: ${e}"
   							failed=true
   							code='sendgrid.failed'
   						}
   					} else {
   						println "Returning it back to how plugin was doing things"
   						queueMailService.sendEmail(sendAccount,queue)
   						failed=false
   					}
   				} else {
   					code = 'queuemail.dailyLimit.label'
   					args = [jobConfigurations]
   				}
   			} else {
   				code = 'queuemail.noConfig.label'
   			}
   		}catch (e) {
   			failed=true
   			String currentError = e.getClass().simpleName
   			if (MessageExceptions.values().any{it.toString() == currentError}) {
   				currentException=currentError
   				def errors = MessageExceptions.verifyStatus(currentException)
   				if (errors) {
   					resend=errors.resend
   				}
   			}
   		} finally {
   			if (failed) {
   				actionFailed(executor, queue, jobConfigurations, clazz, sendAccount, error, code, args,resend,currentException)
   			}
   		}
   	}
}
```


