<%@ page session="false" trimDirectiveWhitespaces="true" %><%@ include file="/WEB-INF/head.jspf" %>
<div><main class="indexPage leftContent">
<%@ include file="/WEB-INF/searchSuggestion.jspf" %>
<c:forEach items="${articles}" var="art" ><c:out escapeXml="false" value="${art.summary}"/><!---->
</c:forEach><%@ include file="/WEB-INF/pagenation.jspf" %>
</main><%@ include file="/WEB-INF/side.jspf" %>
</div>
<%@ include file="/WEB-INF/foot.jspf" %>