<%@ page import="org.grails.plugin.queuemail.enums.QueueStatus;org.grails.plugin.queuemail.enums.QueueTypes;" %>
<g:set var="entityName" value="${message(code: 'queuemail.reportDownload.label')}" scope="request" />

<g:render template="/queueMail/search" />

<g:set var="dateFormat" value="${message(code: 'queuemail.reportDate.format')}"/>
<div class="content" role="main" id="results">	
<g:if test="${instanceList.reportJobs}">
<section class="jobList container-fluid" id="section2" style="display: none;">
			<g:each in="${instanceList.reportJobs}" var="reportJob">
			<div class="col-sm-12 well">
				<g:message code="reportThreadLimit${reportJob.isAdvanced?'Advanced':''}.${reportJob.queueType}${reportJob.minPreserve?'':'.label'}" 
				args="${[reportJob.maxPoolSize,reportJob.running,reportJob.queued,reportJob.minPreserve,reportJob.priority,
					reportJob.elapsedTime,reportJob.elapsedQueue,reportJob.failuresTolerated,reportJob.maxQueue]}"/>
				<g:if test="${reportJob.executorCount}">
					<div class="alert alert-success"><span class="circle"></span> 
						<g:message code="userThreadbreakDown.label" args="${[reportJob.executorCount.userRunningBelow,reportJob.executorCount.userRunningAbove,
							reportJob.executorCount.userBelow,reportJob.executorCount.userAbove,reportJob.priority?:'']}"/>
					</div>
					<div class="alert alert-warning">
						<span class="circle"></span> <g:message code="reportThreadbreakDown.label" args="${[reportJob.executorCount.runningBelow,reportJob.executorCount.runningAbove,
							reportJob.executorCount.queuedBelow,reportJob.executorCount.queuedAbove,reportJob.priority?:'']}"/>
					</div>
					<div >
					<g:each in="${reportJob.serviceConfigs}" var="serviceConfig">
						<div class="col-sm-12">
						<div class="col-sm-4">
						${serviceConfig.service}
						</div>
						<div class="col-sm-6">
						<g:each in="${serviceConfig.info}" var="info">
							<div class="alert alert-success">
								<span class="circle"></span>
								<g:message code="configBreakown.label" args="${[info.jobName,info.limit,info.currentCount,info.failTotal,info.failCount,info.actioned]}"/>
							</div>
						</g:each>
						</div>
						</div>
					</g:each>
					</div>

				</g:if>
			</div>
			<br/>
			</g:each>
	
	</section>
</g:if>
<div id="message" class="message" role="status" 
	<g:unless test="${flash.message }"> style="display:none" </g:unless>
>${flash.message }</div>
<g:if test="${instanceList.results}">

<table class="table table-list-search">
		<thead>
			<tr>
				<g:sortableColumn property="emailService" titleKey="queuemail.reportName.label" params="${search}" />
				<g:sortableColumn property="created" titleKey="queuemail.created.label" params="${search}" />
				<g:sortableColumn property="startDate" titleKey="queuemail.startDate.label" params="${search}" />
				<g:sortableColumn property="initiation" titleKey="queuemail.initiation.label" params="${search}" />
				<g:sortableColumn property="finishDate" titleKey="queuemail.finishDate.label" params="${search}" />
				<g:sortableColumn property="duration" titleKey="queuemail.duration.label" params="${search}" />
				<g:sortableColumn property="status" titleKey="queuemail.status.label" params="${search}" />
				<g:sortableColumn property="from" titleKey="queuemail.searchType.FROM" params="${search}" />
				<g:sortableColumn property="to" titleKey="queuemail.searchType.TO" params="${search}" />
				<g:sortableColumn property="subject" titleKey="queuemail.searchType.SUBJECT" params="${search}" />
				<g:if test="${showUserField}">
				<g:sortableColumn property="userId" titleKey="queuemail.username.label" params="${search}" />
				</g:if>
				<g:unless test="${hideQueueType}">
				<g:sortableColumn property="queueType" titleKey="queuemail.queueType.label" params="${search}" />
				</g:unless>
				<g:unless test="${hideQueuePriority}">
				<g:sortableColumn property="priority" titleKey="queuemail.reportPriority.label" params="${search}" />
				</g:unless>


				<th>&nbsp;</th>
			</tr>
		</thead>
		<tbody>
			<g:set var="moreLabel" value="${message(code: 'queuemail.moreoptions.label')}"/>
			<g:set var="downloadLabel" value="${message(code: 'queuemail.download.label')}"/>
			<g:set var="queuedLabel" value="${message(code: 'queuemail.reportType.QU')}"/>
			<g:set var="deleteLabel" value="${message(code: 'queuemail.delete.label')}"/>
			<g:set var="cancelLabel" value="${message(code: 'queuemail.download.label')}"/>
			<g:set var="requeueLabel" value="${message(code: 'queuemail.requeue.label')}"/>
			<g:each in="${instanceList.results}" status="i" var="reportInstance">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					<td>${reportInstance.emailService}</td>
					<td class="small"><g:formatDate format="${dateFormat}" date="${reportInstance.created}"/></td>
					<td class="small"><g:formatDate format="${dateFormat}" date="${reportInstance.startDate}"/></td>
					<td class="small" style="background: ${reportInstance.initiationColor?:'transparent'}">${reportInstance.initiation?:''}</td>
					<td class="small"><g:formatDate format="${dateFormat}" date="${reportInstance.finishDate}"/></td>
					<td class="small" style="background: ${reportInstance.color?:'transparent'}">${reportInstance.duration?:''}</td>
					<td class="small">${reportInstance.email.from}</td>
					<td class="small">${reportInstance.email.to}</td>
					<td class="small">${reportInstance.email.subject}</td>
					<g:if test="${showUserField}">
					<td>${reportInstance.username?:reportInstance.userId}</td>
					</g:if>
					<g:unless test="${hideQueueType}">
					<td class="small">${g.message(code:'queuemail.queueType.'+reportInstance.queueType)}</td>
					</g:unless>
					<g:unless test="${hideQueuePriority}">
					<td>${reportInstance?.priority?:''}</td>
					</g:unless>
					<td><g:message code="queuemail.reportType.${reportInstance.status}"/></td>
					<td class="dropdown queuekit">

					<i  class="btn btn-default" id="${reportInstance.id}">
					<g:message code="queuemail.reportType.${reportInstance.status}"/>

					</i>

					<a  class="btn btn-default actionButton" data-toggle="dropdown"
					  data-row-id="${reportInstance.id}" data-queueType="${reportInstance.queueType}" data-row-status="${reportInstance.status}">
					   <span class="arrow-down"></span>						  
					</a>					  
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>
	
		<ul id="contextMenu" class="dropdown-menu" role="menu" >
		<li id="queueDisplay"><a><g:message code="queuemail.display.label"/></a></li>
		<li id="queueRequeue"><a>${requeueLabel}</a></li>					
		<li id="queueDelete"> <a>${deleteLabel}</a></li>
		
		<g:if test="${superUser}">
		<li id="qeuePriority"><a><g:message code="queuemail.changePriority.label"/></a></li>		
		</g:if>
		</ul>
	
	<div class="center-block pagination">
		<span class="listTotal"><g:message code="queuemail.count.message" args="${[instanceTotal]}"/></span>
		<g:paginate total="${instanceTotal}" params="${search}" />
	</div>

	<script>
	$(function() {		
		toggleBlock('#jobCtrl','.jobList');		
		var showJobControl="${search.jobControl}";
		if (showJobControl=='true') {
			var message="<g:message code="queuemail.jobControl.label" args="${[g.message(code:'queuemail.hide.label')]}"/>";
 			$('#jobCtrl').html(message).fadeIn('slow');
 			$('.jobList').toggle();
		}
		function toggleBlock(caller,called,calltext) {
			$(caller).click(function() {
				if($(called).is(":hidden")) {
					var message="<g:message code="queuemail.jobControl.label" args="${[g.message(code:'queuemail.hide.label')]}"/>";
		 			$(caller).html(message).fadeIn('slow');
		 			$('#jobControl').val(true);
		    	}else{
		    		var message="<g:message code="queuemail.jobControl.label" args="${[g.message(code:'queuemail.show.label')]}"/>";			    	
		        	$(caller).html(message).fadeIn('slow');	
		        	$('#jobControl').val(false);
		    	}
		 		$(called).slideToggle("slow");
		  	});
		  }
		$dropdown = $("#contextMenu");
		$(".actionButton").click(function() {
			var id = $(this).attr('data-row-id');
			var status = $(this).attr('data-row-status');
			var queuetype = $(this).attr('data-queuetype');
			$(this).after($dropdown);	
			$dropdown.find("#queueDelete").attr("onclick", "javascript:doDelete("+id+");");
			$dropdown.find("#queueDisplay").attr("onclick", "javascript:doDisplay('"+id+"','"+queuetype+"');");	
			$dropdown.find("#queueRequeue").attr("onclick", "javascript:doRequeue('"+id+"');");
			$dropdown.find("#qeuePriority").attr("onclick", "javascript:doPriority('"+id+"');");
			$(this).dropdown();				
			var running= status=='${QueueStatus.RUNNING}';
			var deleted= status=='${QueueStatus.DELETED}';
			var queued= status=='${QueueStatus.QUEUED}';
			var enhancedQueue = queuetype=='${QueueTypes.ENHANCED}';
			var priorityQueue = queuetype=='${QueueTypes.BASIC}';
			$('#queueDelete')[(!deleted && enhancedQueue)||(!enhancedQueue && !running) ?'show':'hide']();
			$('#qeuePriority')[(queued && enhancedQueue) ?'show':'hide']();			
			var issue = (status=='${QueueStatus.QUEUED}' || status=='${QueueStatus.ERROR}');
			$('#queueRequeue')[issue ?'show':'hide']();													
		});
	});
	</script>
</g:if>
</div>
