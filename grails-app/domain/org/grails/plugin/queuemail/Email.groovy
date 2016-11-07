package org.grails.plugin.queuemail

import static org.grails.plugin.queuemail.enums.MessageStatus.*
import grails.gsp.PageRenderer
import grails.plugin.mail.GrailsMailException
import grails.plugin.mail.MailMessageContentRenderer
import grails.util.Holders

import javax.activation.FileTypeMap

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.io.support.FileSystemResource
import org.grails.plugin.queuemail.enums.MessageStatus
import org.springframework.core.io.InputStreamSource
import org.springframework.util.Assert


/**
 * Re-using asyncmail plugin 
 * Most of AsynchronousMailMessage.groovy 
 */

class Email {
	
	def grailsApplication
	def configuration = grailsApplication?.config?.queuemail
	private static final int MAX_EMAIL_ADDR_SIZE = 256
	boolean smtpValidation = configuration?.smtpValidation?:false

	String from
	String replyTo
	List<String> to
	List<String> cc
	List<String> bcc
	List<EmailAttachment> attachments

	Map<String, String> headers
	MessageStatus status = CREATED
	String envelopeFrom
	String subject
	String text
	String alternative
	boolean htmlEmail = false


	/** Date when message was sent */
	Date sentDate

	final boolean mimeCapable
	private FileTypeMap fileTypeMap

	static constraints = {
		def mailboxValidator = { String value ->
			return value == null || Validator.isMailbox(value)
		}
		from(nullable: true, maxSize: MAX_EMAIL_ADDR_SIZE, validator: mailboxValidator)
		replyTo(nullable: true, maxSize: MAX_EMAIL_ADDR_SIZE, validator: mailboxValidator)
		def emailList = { List<String> list, reference, errors ->
			boolean flag = true
			if (list != null) {
				list.each { String addr ->
					if (!Validator.isMailbox(addr)) {

						errors.rejectValue(propertyName, 'mail.mailbox.invalid')
						flag = false
					}
				}
			}
			return flag
		}

		def atLeastOneRecipientValidator = { List<String> value, reference, errors ->
			// It's needed to access to propertyName
			emailList.delegate = delegate

			// Validate address list
			if (!emailList(value, reference, errors)) {
				return false
			}

			boolean hasRecipients = reference.to || reference.cc || reference.bcc
			if (!hasRecipients) {
				errors.reject('mail.one.recipient.required')
			}
			return hasRecipients
		}

		// The nullable constraint isn't applied for collections by default.
		to(nullable: true, validator: atLeastOneRecipientValidator)
		cc(nullable: true, validator: emailList)
		bcc(nullable: true, validator: emailList)

		headers(nullable: true, validator: { Map<String, String> map ->
			boolean flag = true
			map?.each { String key, String value ->
				if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
					flag = false
				}
			}
			return flag
		})

		envelopeFrom(nullable: true, maxSize: MAX_EMAIL_ADDR_SIZE, validator: mailboxValidator)

		subject(blank: false, maxSize: 988)
		text(blank: false)
		alternative(nullable: true)
		sentDate(nullable: true)
		cleanTo(nullable:true)
		cleanCc(nullable:true)
		cleanBcc(nullable:true)
	}


	static hasMany = [to: String, cc: String, bcc: String, attachments: EmailAttachment]
	def html
	def body

	List<String> cleanTo
	List<String> cleanCc
	List<String> cleanBcc
	static transients = ['mimeCapable','fileTypeMap', 'html','body','cleanTo', 'cleanCc', 'cleanBcc','smtpValidation','configuration','grailsApplication']
	static mapping = {
		//table 'queuemail_email'

		from column: 'from_column'

		to(
				indexColumn: 'to_idx',
				fetch: 'join',
				joinTable: [
						name: 'mail_to',
						length: MAX_EMAIL_ADDR_SIZE,
						key: 'message_id',
						column: 'to_string'
				]
		)

		cc(
				indexColumn: 'cc_idx',
				fetch: 'join',
				joinTable: [
						name: 'mail_cc',
						length: MAX_EMAIL_ADDR_SIZE,
						key: 'message_id',
						column: 'cc_string'
				]
		)

		bcc(
				indexColumn: 'bcc_idx',
				fetch: 'join',
				joinTable: [
						name: 'mail_bcc',
						length: MAX_EMAIL_ADDR_SIZE,
						key: 'message_id',
						column: 'bcc_string'
				]
		)

		headers(
				indexColumn: [name: 'header_name', length: 255],
				fetch: 'join',
				joinTable: [
						name: 'mail_header',
						key: 'message_id',
						column: 'header_value'
				]
		)

		text type: 'text'

		attachments cascade: "all-delete-orphan"
	}


	// Mail message headers
	void headers(Map headers) {
		Assert.notEmpty(headers, "Headers can't be null.")

		if(!mimeCapable){
			throw new GrailsMailException("You must use a JavaMailSender to customise the headers.")
		}

		Map map = new HashMap()

		headers.each{key, value->
			String keyString = key?.toString()
			String valueString = value?.toString()

			Assert.hasText(keyString, "Header name can't be null or empty.")
			Assert.hasText(valueString, "Value of header ${keyString} can't be null or empty.")

			map.put(keyString, valueString)
		}

		this.headers = map
	}


	void cleanTo(List<String> recipients) {
		this.to =validateAndCleanAddrList(recipients)
	}

	void setCleanBcc(List<String>  recipients) {
		this.bcc = validateAndCleanAddrList(recipients)
	}

	void setCleanCc(List<String> recipients) {
		this.cc = validateAndCleanAddrList(recipients)
	}

	/**
	 * isMailboxAndResolves will ensure email address is valid and that the end hostname
	 * that email resolves to has an MX record bound to it.
	 * If none of those rules match the email address will not be included in actual To/Cc/Bcc field
	 * when saving/sending
	 * @param recipients
	 * @return
	 */
	private List<String> validateAndCleanAddrList(List<? extends CharSequence> recipients) {
		List<String> list = new ArrayList<String>(recipients.size())
		recipients.each {CharSequence seq ->
			String addr = seq.toString()
			if (Validator.isMailboxAndResolves(this.from, addr, this.smtpValidation)) {
				list.add(addr)
			}
		}
		return list
	}

	private assertEmail(String addr, String fieldName) {
		Assert.notNull(addr, "Value of $fieldName can't be null.")
		Assert.hasText(addr, "Value of $fieldName can't be blank.")
		if (!Validator.isMailbox(addr)) {
			throw new GrailsMailException("Value of $fieldName must be email address.")
		}
	}


	void setBody(CharSequence seq) {
		def string = seq?.toString()
		Assert.hasText(string, "Body text can't be null or blank.")

		if(this.text==null || !this.htmlEmail) {
			this.htmlEmail = false
			this.text = string
		} else if(this.htmlEmail){
			this.alternative = string
		}
	}

	void setHtml(Map paramsMap) {
		this.htmlEmail = true
		this.text=(doRender(paramsMap) as String)
	}

	protected def doRender(Map params) {
		if (!params.view) {
			throw new GrailsMailException("No view specified.")
		}
		return Holders.grailsApplication.mainContext.groovyPageRenderer.render(template: params.view, model: params.model)
	}


	// Attachments
	void attachBytes(String name, String mimeType, byte[] content) {
		Assert.hasText(name, "Attachment name can't be blank.")
		Assert.notNull(content, "Attachment content can't be null.")

		if(!mimeCapable){
			throw new GrailsMailException("You must use a JavaMailSender to add attachment.")
		}

		this.addToAttachments(
				new EmailAttachment(
						attachmentName: name, mimeType: mimeType, content: content
				)
		)
	}

	void attach(String fileName, String contentType, byte[] bytes) {
		attachBytes(fileName, contentType, bytes)
	}

	void attach(File file) {
		attach(file.name, file)
	}

	void attach(String fileName, File file) {
		attach(fileName, fileTypeMap.getContentType(file), file)
	}

	void attach(String fileName, String contentType, File file) {
		if (!file.exists()) {
			throw new FileNotFoundException("Can't use $file as an attachment as it does not exist.")
		}

		attach(fileName, contentType, new FileSystemResource(file))
	}

	void attach(String fileName, String contentType, InputStreamSource source) {
		InputStream stream = source.inputStream
		try {
			attachBytes(fileName, contentType, stream.bytes)
		} finally {
			stream.close()
		}
	}

	void inline(String name, String mimeType, byte[] content) {
		Assert.hasText(name, "Inline id can't be blank.")
		Assert.notNull(content, "Inline content can't be null.")

		if(!mimeCapable){
			throw new GrailsMailException("You must use a JavaMailSender to add inlines.")
		}

		this.addToAttachments(
				new EmailAttachment(
						attachmentName: name, mimeType: mimeType, content: content, inline: true
				)
		)
	}

	void inline(File file) {
		inline(file.name, file)
	}

	void inline(String fileName, File file) {
		inline(fileName, fileTypeMap.getContentType(file), file)
	}

	void inline(String contentId, String contentType, File file) {
		if (!file.exists()) {
			throw new FileNotFoundException("Can't use $file as an attachment as it does not exist.")
		}
		inline(contentId, contentType, new FileSystemResource(file))
	}

	void inline(String contentId, String contentType, InputStreamSource source) {
		InputStream stream = source.inputStream
		try {
			inline(contentId, contentType, stream.bytes)
		} finally {
			stream.close()
		}
	}

}
