<%@ page import="org.grails.plugin.queuemail.enums.ConfigTypes;org.grails.plugin.queuemail.enums.QueueTypes;org.grails.plugin.queuemail.validation.QueueMailLists; org.grails.plugin.queuemail.enums.Priority;org.grails.plugin.queuemail.EmailQueue;org.grails.plugin.queuemail.validation.ChangeConfigBean" %>
<div id="error">
</div>
<form id="changeConfigForm">
	<g:hiddenField name="changeType" value="${instance.changeType}"/>
	<g:if test="${instance.queue}">
		<g:hiddenField name="queue.id" value="${instance.queue.id}"/> 
	</g:if>	
	<h4>
		<g:if test="${instance.queueType}">
		<g:message code="queuemail.queueType.${instance.queueType}"/> :
		</g:if>
		<g:message code="queuemail.changeType.${instance.changeType}.label" args="${[g.message(code:'queuemail.configure.label')]}"/>
	</h4>
	<div class="form-group">	
		<g:if test="${!instance.queue}">
			<g:select name="queueType" noSelection="${['':'']}" required="required" 
			from="${instance.queueList}"
			 valueMessagePrefix="queuemail.queueType" onChange="configureFields(this.value)"/>	
		</g:if>
	</div>	
	<span id="priorityGroup">
		<span class="form-group">
			<label>
			<g:message code="queuemail.changePriority.${instance.changeType}.label" args="${[g.message(code:'queuemail.modify.label')]}"/>		
			</label>	
			<g:select name="priority" class="form-control" from="${Priority.values()}" value="${instance.priority}"/>
		</span>
	</span>
	<span id="changeValueGroup">
		<span class="form-group">
			<label>
				<g:message code="queuemail.changeType.${instance.changeType}.label" args="${[g.message(code:'queuemail.modify.label')]}"/>		
			</label>	
			<g:textField name="changeValue" required="" value="${instance.changeValue?:instance.currentValue?:0}"/>
		</span>
	</span>
	
	
	<span id="defaultComparatorGroup">
		<span class="form-group">
			<label>
				<g:message code="queuemail.changeType.${instance.changeType}.label" args="${[g.message(code:'queuemail.enable.label')]}"/>		
			</label>
			<g:select name="defaultComparator" class="form-control" from="${[true,false]}" value="${instance.defaultComparator}" valueMessagePrefix="queuemail.status"/>
		</span>
	</span>
	<div class="form-group">
		<span id="buttons">	
			<a class="btn btn-warning" onclick="closeModal()"><g:message code="queuemail.cancel.label"/></a>
			<g:submitButton name="submit" value="${g.message(code:'submit.label') }" class="btn btn-success"/>	
		</span>
		<a class="btn btn-danger" style="display:none" id="closeForm" onclick="closeModal()"><g:message code="queuemail.close.label"/></a>
	</div>
<form>
<g:if test="${instance.changeType=='MAXQUEUE'}">
<g:message code="queuemail.maxQueue1.message"/>
</g:if>
<g:if test="${instance.changeType=='POOL'}">
<g:message code="queuemail.maxPool1.message"/>
</g:if>
<g:if test="${instance.changeType=='CHECKQUEUE'}">
<g:message code="queuemail.checkQueue1.message"/>
<g:message code="queuemail.checkQueue2.message"/>
<g:message code="queuemail.checkQueue3.message"/>
<g:message code="queuemail.checkQueue4.message"/>
<g:message code="queuemail.checkQueue5.message"/>
<g:message code="queuemail.checkQueue6.message"/><br/>
<g:message code="queuemail.checkQueue7.message"/>
<g:message code="queuemail.checkQueue8.message"/>
</g:if>

<g:if test="${instance.changeType=='STOPEXECUTOR'}">
<g:message code="queuemail.stopExecutor1.message"/>
<g:message code="queuemail.stopExecutor2.message"/>
<g:message code="queuemail.stopExecutor3.message"/><br/>
<g:message code="queuemail.stopExecutor4.message"/>
<g:message code="queuemail.stopExecutor5.message"/>
<g:message code="queuemail.stopExecutor6.message"/>
</g:if>
<g:if test="${instance.changeType=='DEFAULTCOMPARATOR'}">
<g:message code="queuemail.defaultComparator1.message"/>
</g:if>

<script>
	$("#changeConfigForm").submit(function( event ) {
		var data = $("#changeConfigForm").serialize();
		 $.ajax({
	         type: 'post',
	         url: '${createLink(controller:'queueMail',action:'modifyConfig')}',
	         data: data,
	         success: function (response) {
	        	 closeModal();
	        	 $('#results').html(data);
	         },
	         error: function(xhr,status,error){
	             $('#error').html(status);             
	         }    
	     });
	});
	$(function() {
		hideField('priority');
		hideField('changeValue');
		hideField('defaultComparator');
	})
	var changeType="${instance.changeType}";
	var isPool = changeType=='${ConfigTypes.POOL}';
	var isQueue = changeType=='${ConfigTypes.MAXQUEUE}';
	var isPreserve = changeType=='${ConfigTypes.PRESERVE}';
	var isConfig = changeType=='${ConfigTypes.PRESERVE}';
	var isQueueCheck = changeType=='${ConfigTypes.CHECKQUEUE}';
	var isDefaultComparator = changeType=='${ConfigTypes.DEFAULTCOMPARATOR}';
	function configureFields(queueType) {
		if (isConfig||isPool||isQueue) {			
			showButtons(changeType,queueType,isPreserve);			
		} else if (isDefaultComparator) {
			showType(changeType,queueType,'defaultComparator');	
		} else {
			if (isConfig) {
				showButtons(changeType,queueType,isPreserve);
				hideField('priority');
			}
		}
	}
	function hideButtons() {
		$('#closeForm').show();
		$('#buttons').hide();	
		hideField('priority');
		hideField('changeValue');

		hideField('defaultComparator');
	}
	function hideField(called) {
		$('#'+called+'Group').hide();
		$('#'+called).attr('prop','disabled',true).attr('required',false);
	}
	function showField(called) {
		$('#'+called+'Group').show();
		$('#'+called).attr('prop','disabled',false).attr('required',true);
		if (isQueue) {
			$('#'+called).attr('pattern', '^(?!0*\$)^[0-9]{0,4}\$').attr('maxlength', '4').attr('size','4');
		} else if (isPool) {			
			$('#'+called).attr('pattern', '^(?!0*\$)^[0-9]{0,2}\$').attr('maxlength', '2').attr('size','2');
		} else if (isPreserve && called=='changeValue') {			
			$('#'+called).attr('pattern', '^[0-9]{0,2}\$').attr('maxlength', '2').attr('size','2');
		}		
	}
	function showType(changeType,queueType,field) {
		$('#closeForm').hide();
		$('#buttons').show();
		hideField('changeValue');
		hideField('priority');
		showField(field);
		postButton(changeType,queueType);
	}
	function showButtons(changeType,queueType,isPreserve) {
		$('#closeForm').hide();
		$('#buttons').show();
		showField('changeValue');
		if (isPreserve) {
			showField('priority');		
		} else {
			hideField('priority');
		}
		postButton(changeType,queueType);
	}
	function postButton(changeType,queueType) {
		$.ajax({
	        type: 'post',
	        url: '${createLink(controller:'queueMail',action:'loadConfig')}',
	        data: {changeType:changeType,queueType:queueType},
	        success: function (data) {
	            var jsonValue = JSON.parse(data.value);
	            if (jsonValue) {
	                $('#changeValue').val(jsonValue);
	            }
	            if (data.priority) {
	                $('#priority option[value="'+data.priority.name+'"]').prop('selected', true);
	            }	            
	            var jsonDefaultComparator = JSON.parse(data.defaultComparator);
	            if (jsonDefaultComparator) {
	            	$('#defaultComparator option[value="'+jsonDefaultComparator+'"]').prop('selected', true);
	            }           
	        }
	    });
	}
</script>