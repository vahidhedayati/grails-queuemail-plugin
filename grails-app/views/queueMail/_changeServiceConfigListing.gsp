<%@ page import="org.grails.plugin.queuemail.enums.MessageExceptions" %>

<g:each in="${instance.serviceConfigs}" var="serviceConfig" >

    <g:each in="${serviceConfig.info}" var="sconf">
        <g:form name="${sconf.jobName}" class="form-horizontal"  >
            <div class="col-sm-12">
                <div class="form-group">
                    <g:hiddenField name="jobName" value="${sconf.jobName}"/>
                    <g:hiddenField name="queueType" value="${instance.queueType}"/>
                    <g:hiddenField name="serviceClazz" value="${instance.serviceClazz}"/>
                    <span class="col-md-2 control-label extra-center"><h4>${sconf.jobName}</h4></span>
                    <div class="form-group row">
                        <div class="col-md-2">
                            <label for="limit" class="col-md-1 control-label"><g:message code="queuemail.limit.label"/></label>
                            <g:textField class="form-control"  name="limit" value="${sconf.limit}"
                                         pattern="(?!0*\$)^[0-9]{0,4}\$" maxlength="4" size="4"/>
                        </div>
                        <div class="col-md-2">
                            <label for="active" class="col-md-1 control-label">
                                <g:message code="queuemail.reportType.ACTIVE"/>
                            </label>
                            <g:select name="active" class="form-control" from="${[true,false]}" value="${sconf.active}"
                                      valueMessagePrefix="queuemail.status"/>
                        </div>
                        <div class="col-md-3">
                            <label for="currentException" class="col-md-1 control-label">
                                <g:message code="queuemail.reportType.ERROR"/>
                            </label>
                            <g:select name="currentException" noSelection="${['':'']}"  class="form-control"
                                      value="${sconf.currentException}" from="${org.grails.plugin.queuemail.enums.MessageExceptions.values()}" />
                        </div>
                        <div class="col-md-2">
                            <span class="col-md-2 control-label extra-center">
                                <a class="btn btn-success"  onclick="submitForm('${sconf.jobName}')">
                                    <g:message code="default.button.update.label"/></a>
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </g:form>
    </g:each>
</g:each>

<script>

</script>