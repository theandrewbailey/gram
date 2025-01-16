<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<div>
<section id="comments"><c:forEach items="${Article.commentCollection}" var="comm" varStatus="status"><c:if test="${status.last}"><span id="last"></span></c:if>
    <article class="comment" id="${comm.uuid}">
<c:out escapeXml="false" value="${comm.postedhtml}"/>
<footer><h:local key="page_commentFooter"><h:param><h:time datetime="${comm.posted}"/></h:param><h:param object="${fn:trim(comm.postedname)}"/></h:local></footer></article></c:forEach></section>
<%@ include file="/WEB-INF/commentForm.jspf" %>
</div>
<%@ include file="/WEB-INF/foot.jspf" %>
