<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<form action="adminDingus" method="post" class="adminform adminAddArticle" accept-charset="UTF-8">
    <h:localVar key="page_dingusLabel" /><h:textarea name="postedmarkdown" length="100" height="20" label="${page_dingusLabel}" styleClass="articleText" value="${Article.postedmarkdown}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" pattern=".*" /><br/>
    <button type="submit" name="action" value="Preview"><h:local key="page_preview"/></button>
</form>
<br/>
<c:if test="${null != Article}"><div><main><article class="article${Article.articleid}">
<c:out escapeXml="false" value="${Article.postedhtml}"/></article></main>
<hr/>
<p><c:out escapeXml="false" value="${rawHtml}"/></p>
</div>
</c:if>
</main>
<%@ include file="/WEB-INF/foot.jspf" %>
