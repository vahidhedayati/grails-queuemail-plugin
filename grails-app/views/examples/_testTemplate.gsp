<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html>
<head>
    <style>
    .site-icon img {
        width: 200px;
        margin-top:-40px;
    }
    .site-logo img {
        min-width: 15em;
        max-width: 55em;
    }
    .menu {
        background: #000;
    }
    a {
        text-decoration: none;
    }
    .menu a {
        color : #fff;
        text-decoration: none;
        font-weight:bold;
    }
    .menu a:hover {
        color : #ff0000;
        background: #ccc;
    }
    .panel-white {
        background: #fff;
        color: #000;
    }
    .panel-white a {
        text-decoration: none;
        color : #0000ff;
    }
    .block {
        display:block;
        clear:both;
    }
    .image {

    }
    .user {
        font-weight:bold;
        font-size: 1.4em;
        display:block;
    }
    </style>
</head>
<body style=" background-color: rgba(125, 159, 203, 0.67); font-size: 1.1em;">
<table  width="100%" border="0" cellpadding="0" cellspacing="0" bgcolor="#FFFFFF">
    <tr style="background: rgba(79, 117, 203, 0.67);"><td colspan="3" class="site-logo">
        <img src="cid:inlineImage"/>
        <h1 style="display:inline; margin-top:-2em;"><g:message code="domain.label"/></h1>
    </td>
    </tr>
    <tr class="menu" style="background: #000;">
        <td><a style="color:#fff;" href="http://www.example.com/"><g:message code="default.home.label"/></a></td>
        <td><a style="color:#fff;" href="http://www.example.com/start/status"><g:message code="site.status"/></a></td>
        <td><a style="color:#fff;" href="http://www.example.com/start/contact"><g:message code="contact.us"/></a></td>
    </tr>
    <tr>
        <td colspan="3" style="background: #fff;">
            <b><br/><br/>
               Variables were: ${var1} 
               ${var2}
            </b>
            <br/><br/>
        </td>
    </tr>

</table>
</body>
</html>