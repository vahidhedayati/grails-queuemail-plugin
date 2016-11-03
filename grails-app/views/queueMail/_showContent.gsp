<g:set var="dateFormat" value="${message(code: 'default.date.format')}"/>

<div class="content">
	<div>
		<label class="bold"><g:message code="queuemail.reportName.label"/></label>
		${instance.emailService}
	</div>
	
	
	<div>
		<label class="bold"><g:message code="queuemail.locale.label"/></label>
		${instance.locale}
	</div>

	<div>
		<label class="bold"><g:message code="queuemail.username.label"/></label>
		${instance.userId}
	</div>
	
	<g:if test="${instance.created}">
	<div>
		<label class="bold"><g:message code="queuemail.created.label"/></label>
		<g:formatDate date="${instance.created}" format="${dateFormat}"/>
	</div>
	</g:if>
	<g:if test="${instance.start}">
		<div>
			<label class="bold"><g:message code="queuemail.startDate.label"/></label>
			<g:formatDate date="${instance.start}" format="${dateFormat}"/>
		</div>
	</g:if>
	<g:if test="${instance.requeued}">
		<div>
			<label class="bold"><g:message code="queuemail.requeued.label"/></label>
			<g:formatDate date="${instance.requeued}" format="${dateFormat}"/>
		</div>
	</g:if>
	<g:if test="${instance.retries}">
		<div>
			<label class="bold"><g:message code="queuemail.retries.label"/></label>
			${intance.retries }
		</div>
	</g:if>
	<g:if test="${instance.finished}">
		<div>
			<label class="bold"><g:message code="queuemail.finished.label"/></label>
			<g:formatDate date="${instance.finished}" format="${dateFormat}"/>
		</div>
	</g:if>
	
	<div>
		<label class="bold"><g:message code="queuemail.status.label"/></label>
		<g:message code="queuemail.reportType.${instance.status}"/>
	</div>
	
	<div>
		<label class="bold"><g:message code="queuemail.queueType.label"/></label>
		<g:message code="queuemail.queueType.${instance.queueType}"/>
	</div>
	
	<div>
		<label class="bold"><g:message code="queuemail.reportPriority.label"/></label>
		${instance?.priority?:''}
	</div>
	<g:if test="${instance.error}">
	<div>
		<label class="bold"><g:message code="queuemail.reportPriority.label"/></label>
		${instance.error}
	</div>
	</g:if>
</div>