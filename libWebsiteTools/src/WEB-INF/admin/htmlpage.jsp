<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="h" uri="uri:libwebsitetools:htmlTools" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html lang="${$_LIBIMEAD_PRIMARY_LOCALE.toLanguageTag()}"><head>
<h:stylesheet/><h:meta/>
</head><body>
<noscript><h:local key="page_noscript"/></noscript>
<main class="leftContent ${IMEAD_ID}">
<c:out escapeXml="false" value="${htmlpagetext}"/></main>
<h:javascript/></body></html>
