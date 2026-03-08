<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<form action="adminDingus" method="post" class="adminform adminArticleAdd" accept-charset="UTF-8">
    <details class="recent"><summary><h:local key="page_articleRecentImages"/></summary><ol>
        <c:forEach items="${recentPictures}" var="pic"><li>${pic}</li>
    </c:forEach></ol></details>
    <div><h:localVar key="page_dingusLabel" />
    <h:textarea name="postedmarkdown" length="100" height="20" label="${page_dingusLabel}: " styleClass="articleText" value="${Article.postedmarkdown}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" pattern=".*" /></div>
    <button type="submit" name="action" value="Preview" formtarget="articlePreview" formaction="adminDingus?iframe"><h:local key="page_preview"/></button>
</form>
<iframe name="articlePreview" class="resizeable" src="adminDingus?iframe"></iframe>
</main><h:javascript/></body></html>
