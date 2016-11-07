
<g:select name="queueType" noSelection="${['':'']}" required="required"
          from="${instance.queueList}" value="${instance.queueType}"
          valueMessagePrefix="queuemail.queueType" onChange="configureFields(this.value)"/>

<g:select name="serviceClazz" noSelection="${['':'']}" value="${instance.serviceClazz?:''}" from="${instance.classes}" onChange="loadService(this.value)"/>

<script>
    function loadService(value) {
        if (value!='') {
            var data = $("#changeServiceConfigForm").serialize();
            $.ajax({
                type: 'post',
                url: '${createLink(controller:'queueMail',action:'changeServiceConfig')}',
                data: data,
                success: function (response) {
                    $('#configListing').html(response);
                },
                error: function (xhr, status, error) {
                    $('#error').html(status);
                }
            });
        } else {
            $('#configListing').html('');
        }
    }
</script>