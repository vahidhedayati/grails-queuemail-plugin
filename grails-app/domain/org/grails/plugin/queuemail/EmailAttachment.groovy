package org.grails.plugin.queuemail


class EmailAttachment implements Serializable {

    static final DEFAULT_MIME_TYPE = 'application/octet-stream'

    private static final SIZE_30_MB = 30*1024*1024

    String attachmentName
    String mimeType = DEFAULT_MIME_TYPE
    byte[] content
    boolean inline = false

    static belongsTo = [message:Email]

    static mapping = {
        table 'async_mail_attachment'
        version false
    }

    static constraints = {
        attachmentName(blank:false)
        //mimeType()
        content(maxSize:SIZE_30_MB)
    }
}