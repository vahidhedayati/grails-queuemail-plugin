

<div class="alert alert-danger" >

These are various examples when clicked will trigger an email, please ensure you have configured queueMail.exampleFrom and queueMail.exampleTo configuration values
If you do click an option, a message will flash to say job has been queued. Check <g:link controller="queueMail" action="listQueue"><g:message code="queuemail.listQueue.label"/></g:link>
to see how the email has got on


 <div class="nav" role="navigation">
</ul>
	<ul>
			<li><g:link class="btn btn-default" controller="queueTest" action="testTextEmail" ><g:message code="queuemail.testTextEmail.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="queueTest" action="testTemplateEmail" ><g:message code="queuemail.testTemplateEmail.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="queueTest" action="testBodyEmail" ><g:message code="queuemail.testBodyEmail.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="queueTest" action="testListTo" ><g:message code="queuemail.testListTo.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="queueTest" action="testListCc" ><g:message code="queuemail.testListCc.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="queueTest" action="testListBcc" ><g:message code="queuemail.testListBcc.label"/></g:link></li>
			</ul>
		<ul>
		<br/>

	</ul>
</div>
</div>	