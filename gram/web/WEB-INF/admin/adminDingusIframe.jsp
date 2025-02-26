<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="h" uri="uri:libwebsitetools:htmlTools" %>
<%@ taglib prefix="g" uri="uri:gram" %><!DOCTYPE html>
<html lang="${$_LIBIMEAD_PRIMARY_LOCALE.toLanguageTag()}" class="reset"><head>
<h:stylesheet/><h:meta/>
</head><body class="reset">
<main>
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<c:if test="${null != Article}"><article class="article${Article.articleid}">
<c:out escapeXml="false" value="${Article.postedhtml}"/></article>
<hr/>
<p><c:out escapeXml="false" value="${Article.summary}"/></p>
</c:if>
</main>
<h:javascript/></body></html>
