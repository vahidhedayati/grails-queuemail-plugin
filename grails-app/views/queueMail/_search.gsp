<%@ page import="org.grails.plugin.queuemail.enums.ConfigTypes;org.grails.plugin.queuemail.validation.QueueMailLists; org.grails.plugin.queuemail.enums.Priority;org.grails.plugin.queuemail.EmailQueue;org.grails.plugin.queuemail.validation.ChangeConfigBean;org.grails.plugin.queuemail.validation.QueueMailLists" %>
<g:form name='search' controller='queueMail' class="navbar-trans" action='listQueue' method='post' >
	<section class="container-fluid" id="section1">
		<div id="reportResults" class="form-inline nav navbar-trans col-sm-6">			
		<ul>
			<li><a class="home homeButton" href="${createLink(uri: '/')}"><g:message code="default.home.label" /></a></li>
			<li><g:select class="form-control small" name="deleteOption" from="${deleteList}"
						  noSelection="${['':"${g.message(code:'queuemail.chooseDeleteType.label')}"]}"
						  valueMessagePrefix="queuemail.deleteType"/>
			</li>
			<g:if test="${superUser}">
				<li><g:select class="form-control small" name="adminButtons" from="${adminButtons}"
							  noSelection="${['':"${g.message(code:'queuemail.chooseAdminAction.label')}"]}"
							  valueMessagePrefix="queuemail.adminButton"/></li>
			</g:if>
			<g:if test="${superUser||instanceList.reportJobs}">
			<li>
				<g:if test="${superUser}">
					<g:checkBox name="hideUsers" onChange="reloadPage();" value="${search.hideUsers}"/> <g:message code="queuemail.hideOtherUsers.label"/>
				</g:if>
				<g:if test="${instanceList.reportJobs}">
					<a class="jobButton" id="jobCtrl"><g:message code="queuemail.jobControl.label" args="${[g.message(code:'queuemail.show.label')]}"/></a>
				</g:if>
			</li>
			</g:if>
		</ul>
	</div>	
	<div class="form-inline nav navbar-trans col-sm-6">
		<ul>
			<li>
			<g:textField name="searchFor" class="form-control small" size="20" maxlength="50"
			value="${search?.searchFor}" placeholder="${g.message(code:'queuemail.searchFor.label')}" />
			<g:hiddenField name="userSearchId" value="${search?.userSearchId}" />
		</li>
		<li>
		<g:select name="searchBy" class="form-control small" noSelection="${['':"${g.message(code:'queuemail.searchBy.label')}"]}"
		from="${searchList}"  value="${search?.searchBy}" valueMessagePrefix="queuemail.searchType" />
		</li>
		<li>
		<g:select name="status" class="form-control small" from="${statuses}" noSelection="${['':"${g.message(code:'queuemail.chooseStatus.label')}"]}"
		value="${search?.status}" onChange="reloadPage();" valueMessagePrefix="queuemail.reportType" />
		</li>
		<li>
			<g:hiddenField name="jobControl" value="${search?.jobControl}"/>
			<button type="submit" class="submitButton" name="submit" >${message(code: 'queuemail.search.label')}</button>	
		</li>
		</ul>
	</div>

	</section>
	
 	<g:hiddenField name="sort" value="${search?.sort}"/>
 	<g:hiddenField name="order" value="${search?.order}"/>
</g:form>
<script>
$(function() {
	$('#searchFor').on('change', function() {		
		var value = $('#searchFor').val();
		var searchBy=$('#searchBy').val();
		if (value!='' && searchBy=='') {
			$('#searchBy').attr('required',true);
		} else {
			$('#searchBy').attr('required',false);
		}
	});
	$('#deleteOption').on('change', function() {
		var value = $('#deleteOption').val();
		if (value=='${QueueMailLists.DELALL}') {
			if ( confirm('${message(code: 'queuemail.DeleteAllConfirm.message')}')) { 
				postAction(value);
			}
		} else {
			postAction(value);
		}
	});
	var adminMessages = {"${ConfigTypes.POOL}":"${g.message(code:'queuemail.adminButton.PO')}",
            "${ConfigTypes.PRESERVE}":"${g.message(code:'queuemail.adminButton.PR')}",
            "${ConfigTypes.CHECKQUEUE}":"${g.message(code:'queuemail.adminButton.CQ')}",
            "${ConfigTypes.STOPEXECUTOR}":"${g.message(code:'queuemail.adminButton.ST')}",
			"${ConfigTypes.SERVICECONFIGS}":"${g.message(code:'queuemail.adminButton.SC')}",
            "${ConfigTypes.DEFAULTCOMPARATOR}":"${g.message(code:'queuemail.adminButton.DC')}"
            
	}
	$('#adminButtons').on('change', function() {
		var value = $('#adminButtons').val();
		if (value!='') {
			var params=$.param({changeType:value});
			var url='${createLink(controller:'queueMail',action:'changeConfig')}?';
			if (value=="${ConfigTypes.SERVICECONFIGS}") {
				url='${createLink(controller:'queueMail',action:'changeServiceConfig')}?'
			}
			return showDialog(url+params,adminMessages[value]);
		}
	});
})

</script>
