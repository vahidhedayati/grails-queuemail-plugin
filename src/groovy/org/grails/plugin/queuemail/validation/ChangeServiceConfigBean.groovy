package org.grails.plugin.queuemail.validation

import grails.validation.Validateable
import org.grails.plugin.queuemail.enums.ConfigTypes
import org.grails.plugin.queuemail.enums.MessageExceptions
import org.grails.plugin.queuemail.enums.QueueTypes
import org.grails.plugin.queuemail.monitor.ServiceConfigs

@Validateable
class ChangeServiceConfigBean {

    String changeType=ConfigTypes.SERVICECONFIGS
    String serviceClazz
    List<ServiceConfigs> serviceConfigs
    List classes
    QueueTypes queueType

    String template='/queueMail/changeServiceConfig'

    int limit
    MessageExceptions currentException
    boolean active
    String jobName


    static constraints = {
        limit(nullable:true)
        currentException(nullable:true)
        queueType(nullable:true)
        classes(nullable:true)
        serviceConfigs(nullable:true)
        serviceClazz(nullable:true)
        jobName(nullable:true)

    }
    List getQueueList() {
        return QueueMailLists.queueTypes
    }

}
