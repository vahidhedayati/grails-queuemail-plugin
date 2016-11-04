package org.grails.plugin.queuemail.monitor

/**
 * Created by Vahid Hedayati on 03/11/16.
 */
class ServiceConfigs {

    String jobName
    int limit

    Date actioned
    int lastDay

    int currentCount

    boolean active

    int failTotal=0
    int failCount=0
    Long lastQueueId
    Date lastFailed

}
