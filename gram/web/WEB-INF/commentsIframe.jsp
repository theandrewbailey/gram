<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="h" uri="uri:libwebsitetools:htmlTools" %>
<%@ taglib prefix="g" uri="uri:gram" %><!DOCTYPE html>
<html lang="${$_LIBIMEAD_PRIMARY_LOCALE.toLanguageTag()}" class="reset"><head>
<h:stylesheet/><h:meta/>
<h:localVar key="page_title"/><h:title siteTitle="${page_title}" pageTitle="${title}" siteTitleHide="${siteTitleHide}" />
</head><body class="reset">
<section id="comments"><c:forEach items="${Article.commentCollection}" var="comm" varStatus="status"><c:if test="${status.last}"><span id="last"></span></c:if>
    <article class="comment" id="${comm.uuid}">
<c:out escapeXml="false" value="${comm.postedhtml}"/>
<footer><h:local key="page_commentFooter"><h:param><h:time datetime="${comm.posted}"/></h:param><h:param object="${fn:trim(comm.postedname)}"/></h:local></footer></article></c:forEach></section>
<%@ include file="/WEB-INF/commentForm.jspf" %>
<!--<h:responseTag><h:local key="page_footerFormat"><h:param><h:time datetime="${requestTime}"/></h:param><h:param object="${renderMillis}"/></h:local></h:responseTag>-->
<h:javascript/></body></html>
