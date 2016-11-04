package org.grails.plugin.queuemail.monitor

/**
 * Created by Vahid Hedayati on 03/11/16.
 */
class ServiceConfigs implements Cloneable {

    String jobName
    int limit

    Date actioned
    int lastDay

    int currentCount

    boolean active

    int failTotal=0
    int failCount=0
    Long lastQueueId
    Long lastFailedQueueId
    Date lastFailed

    Map getResults() {
        Map results=[:]
        results.jobName=jobName
        results.limit=limit
        results.actioned=actioned
        results.lastDay=lastDay
        results.currentCount=currentCount
        results.active=active
        results.failTotal=failTotal
        results.failCount=failCount
        results.lastQueueId=lastQueueId
        results.lastQueueId=lastFailedQueueId
        results.lastFailed=lastFailed
        return results
    }

    public Object clone()throws CloneNotSupportedException{
        return (ServiceConfigs)super.clone()
    }

}
