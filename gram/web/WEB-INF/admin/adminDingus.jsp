<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<form action="adminDingus" method="post" class="adminform adminArticleAdd" accept-charset="UTF-8" target="articlePreview">
    <h:localVar key="page_dingusLabel" /><h:textarea name="postedmarkdown" length="100" height="20" label="${page_dingusLabel}" styleClass="articleText" value="${Article.postedmarkdown}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" pattern=".*" /><br/>
    <button type="submit" name="action" value="Preview" ><h:local key="page_preview"/></button>
</form>
<article><p>&nbsp;</p></article>
<iframe name="articlePreview" class="resizeable"></iframe>
</main>
<%@ include file="/WEB-INF/foot.jspf" %>
