package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic

@CompileStatic
public enum MessageExceptions {
    AuthenticationFailedException, FolderClosedException, FolderNotFoundException,
    IllegalWriteException, MessageRemovedException, MethodNotSupportedException,
    NoSuchProviderException, ParseException, ReadOnlyFolderException, SearchException,
    SendFailedException, StoreClosedException,AddressException,MessagingException,IOException,
    Exception,MailAuthenticationException

    /**
     * Decides on if Messaging Exception should trigger a resend of the email
     * and or if it should also attempt an alternative config/smtp provider
     * Authentication failure we expect to move on and mark that config as unusable
     * @param input
     * @return
     */
    static Map verifyStatus(MessageExceptions input) {
        //By default false so where break happens their all false
        //Last few have no break and pass into default
        boolean alternateProvider
        boolean resend
        switch (input) {
            case "${FolderClosedException}":
                break
            case "${FolderNotFoundException}":
                break
            case "${MessageRemovedException}":
                break
            case "${MethodNotSupportedException}":
                break
            case "${NoSuchProviderException}":
                break
            case "${ParseException}":
                break
            case "${ReadOnlyFolderException}":
                break
            case "${StoreClosedException}":
                break
            case "${AddressException}":
                break
            case "${IOException}":
                break
            case "${AuthenticationFailedException}":
                alternateProvider=true
            case "${MailAuthenticationException}":
                alternateProvider=true
            case "${SendFailedException}":
            case "${MessagingException}":
            case "${Exception}":
            default:
              resend=true
              break
        }
        return [alternateProvider:alternateProvider,resend:resend]
    }

    /**
     * As per above all those that set alternateProvider to true are by default treated as inactive
     * No point in attempting to re-activate a connection with username/password failure.
     * You can reset the status of a provider through web ui admin drop down
     * @param input
     * @return
     */
    static boolean defineActive(MessageExceptions input) {
        return verifyStatus(input)?.alternateProvider ? false : true
    }
}
