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
	
	private static final int MAX_EMAIL_ADDR_SIZE = 256
	
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

	// Send interval
	Date beginDate = new Date()


	/** Mark this message for deletion after it's sent */
	boolean markDelete = false

	boolean markDeleteAttachments = false

	final boolean mimeCapable
	private FileTypeMap fileTypeMap
	final MailMessageContentRenderer mailMessageContentRenderer


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
						errors.rejectValue(propertyName, 'asynchronous.mail.mailbox.invalid')
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
				errors.reject('asynchronous.mail.one.recipient.required')
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
	}


	static hasMany = [to: String, cc: String, bcc: String, attachments: EmailAttachment]
	def html
	def body
	
	static transients = ['mimeCapable','fileTypeMap','mailMessageContentRenderer', 'html','body']
	static mapping = {
		table 'async_mail_mess'

		from column: 'from_column'

		to(
				indexColumn: 'to_idx',
				fetch: 'join',
				joinTable: [
					name: 'async_mail_to',
					length: MAX_EMAIL_ADDR_SIZE,
					key: 'message_id',
					column: 'to_string'
				]
				)

		cc(
				indexColumn: 'cc_idx',
				fetch: 'join',
				joinTable: [
					name: 'async_mail_cc',
					length: MAX_EMAIL_ADDR_SIZE,
					key: 'message_id',
					column: 'cc_string'
				]
				)

		bcc(
				indexColumn: 'bcc_idx',
				fetch: 'join',
				joinTable: [
					name: 'async_mail_bcc',
					length: MAX_EMAIL_ADDR_SIZE,
					key: 'message_id',
					column: 'bcc_string'
				]
				)

		headers(
				indexColumn: [name: 'header_name', length: 255],
				fetch: 'join',
				joinTable: [
					name: 'async_mail_header',
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

	// Field "to"
	void to(CharSequence recipient) {

		Assert.notNull(recipient, "Field to can't be null.")
		to([recipient])
	}

	void to(Object[] recipients) {
		Assert.notNull(recipients, "Field to can't be null.")
		to(recipients*.toString())
	}
	
	void to(List<? extends CharSequence> recipients) {
		this.to =validateAndConvertAddrList('to', recipients)
	}

	private List<String> validateAndConvertAddrList(String fieldName, List<? extends CharSequence> recipients) {
		Assert.notNull(recipients, "Field $fieldName can't be null.")
		Assert.notEmpty(recipients, "Field $fieldName can't be empty.")

		List<String> list = new ArrayList<String>(recipients.size())
		recipients.each {CharSequence seq ->
			String addr = seq.toString()
			assertEmail(addr, fieldName)
			list.add(addr)
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

	// Field "bcc"
	void bcc(CharSequence val) {
		Assert.notNull(val, "Field bcc can't be null.")
		bcc([val])
	}

	void bcc(Object[] recipients) {
		Assert.notNull(recipients, "Field bcc can't be null.")
		bcc(recipients*.toString())
	}

	void bcc(List<? extends CharSequence> recipients) {
		this.bcc = validateAndConvertAddrList('bcc', recipients)
	}

	// Field "cc"
	void cc(CharSequence val) {
		Assert.notNull(val, "Field cc can't be null.")
		cc([val])
	}

	void cc(Object[] recipients) {
		Assert.notNull(recipients, "Field cc can't be null.")
		cc(recipients*.toString())
	}

	void cc(List<? extends CharSequence> recipients) {
		this.cc = validateAndConvertAddrList('cc', recipients)
	}

	// Field "replyTo"
	void replyTo(CharSequence val) {
		def addr = val?.toString()
		assertEmail(addr, 'replyTo')
		this.replyTo = addr
	}

	// Field "from"
	void from(CharSequence sender) {
		def addr = sender?.toString()
		assertEmail(addr, 'from')
		this.from = addr
	}

	// Field "envelope from"
	void envelopeFrom(CharSequence envFrom) {
		def addr = envFrom?.toString()
		assertEmail(addr, 'envelopeFrom')
		this.envelopeFrom = envFrom
	}

	// Field "subject"
	void title(CharSequence subject1) {
		subject(subject1)
	}

	void subject(CharSequence subject) {
		String string = subject?.toString()
		Assert.hasText(string, "Field subject can't be null or blank.")
		this.subject = string
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

	void locale(String localeStr) {
		Assert.hasText(localeStr, "Locale can't be null or empty.")

		locale(new Locale(localeStr.split('_', 3).toArrayString()))
	}

	void locale(Locale locale) {
		Assert.notNull(locale, "Locale can't be null.")

		this.locale = locale
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
