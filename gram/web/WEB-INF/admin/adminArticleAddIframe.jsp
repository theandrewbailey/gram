<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="h" uri="uri:libwebsitetools:htmlTools" %>
<%@ taglib prefix="g" uri="uri:gram" %><!DOCTYPE html>
<html lang="${$_LIBIMEAD_PRIMARY_LOCALE.toLanguageTag()}" class="reset"><head>
<h:stylesheet/><h:meta/>
</head><body class="reset">
<main><c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<%@ include file="/WEB-INF/article.jspf" %>
<hr/><div class="indexPage"><c:out escapeXml="false" value="${Article.summary}"/></div></main>
<h:javascript/></body></html>
