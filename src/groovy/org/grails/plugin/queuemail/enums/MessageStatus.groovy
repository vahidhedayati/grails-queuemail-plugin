package org.grails.plugin.queuemail.enums

import groovy.transform.CompileStatic

@CompileStatic
public enum MessageStatus {
    CREATED, ATTEMPTED, SENT, ERROR, EXPIRED, ABORT
}