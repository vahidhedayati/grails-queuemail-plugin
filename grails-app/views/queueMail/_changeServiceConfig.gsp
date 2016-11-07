<style>
.queueKitModal .modal-content {
    background: rgba(125, 159, 203, 4.47);
}
.queueKitModal  .modal-body {
    width: 80%;
    height: 80%;
}
.queueKitModal  .modal-dialog {
    position:absolute;
    left: 10px;
    width: 80%;
    height: 80%;
    background: rgba(125, 159, 203, 4.47);
}
</style>

<g:message code="queuemail.changeType.${instance.changeType}.label" args="${[g.message(code:'queuemail.configure.label')]}"/>
<g:form name="changeServiceConfigForm" action="modifyServiceConfig">
    <div id="topForm">
        <g:render template="/queueMail/changeServiceConfigTop"/>
    </div>
    <div id="configListing">

    </div>
</g:form>

<script>
    function configureFields(value) {
        if (value!='') {
            var data = $("#changeServiceConfigForm").serialize();
            $.ajax({
                type: 'post',
                url: '${createLink(controller:'queueMail',action:'changeServiceConfig')}',
                data: data,
                success: function (response) {
                    $('#topForm').html(response);
                },
                error: function (xhr, status, error) {
                    $('#error').html(status);
                }
            });
        } else {
            $('#configListing').html('');
        }
    }
    function submitForm(form) {
        var data = $('#'+form).serialize();
        $.ajax({
            type: 'post',
            url: '${createLink(controller:'queueMail',action:'modifyServiceConfig')}',
            data: data,
            success: function (response) {
                $('#configListing').html(response);
            },
            error: function(xhr,status,error){
                //$('#error').html(status);
            }
        });
    }
</script>