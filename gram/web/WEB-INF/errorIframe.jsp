<%@ page session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="h" uri="uri:libwebsitetools:htmlTools" %>
<%@ taglib prefix="g" uri="uri:gram" %><!DOCTYPE html>
<html lang="${$_LIBIMEAD_PRIMARY_LOCALE}" class="reset"><head>
    <h:meta/>
    <h:localVar key="page_title"/><h:title siteTitle="${site_title}" pageTitle="${title}" siteTitleHide="${siteTitleHide}" />
</head><body class="reset">
<main class="errorPage error">${ERROR_MESSAGE}</main>
<!--<h:responseTag><h:local key="page_footerFormat">
    <h:param><h:time datetime="${requestTime}"/></h:param>
    <h:param object="${renderMillis}"/>
</h:local></h:responseTag>-->
<h:javascript/></body></html>
