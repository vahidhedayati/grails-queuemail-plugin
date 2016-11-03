package org.grails.plugin.queuemail

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware

class QueueTestInterceptor implements GrailsApplicationAware {
    def config
    GrailsApplication grailsApplication

    public QueueTestInterceptor() {
        match controller: 'queueTest'
    }

    boolean before() {
        if (config.disableExamples) {
            redirect controller: 'queueTest', action: 'notFound'
            return false
        }
        return true
    }

    void setGrailsApplication(GrailsApplication ga) {
        config = ga.config.queuekit
    }


}
