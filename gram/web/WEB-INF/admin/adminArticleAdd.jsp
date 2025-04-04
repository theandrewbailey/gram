<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<main>
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<form action="adminArticle" method="post" class="adminform adminArticleAdd" accept-charset="UTF-8">
    <h:localVar key="page_patternMismatch" var="patternMismatch" /><h:localVar key="page_valueMissing" var="valueMissing" />
    <div><h:localVar key="page_articleCategory"/><h:textbox name="section" label="${page_articleCategory}: " labelNextLine="false" patternMismatch="${patternMismatch}" datalist="${groups}" value="${Article.sectionid.name}"/></div>
    <div><h:localVar key="page_articleTitle"/><h:textbox name="articletitle" label="${page_articleTitle}: " maxLength="250" size="64" labelNextLine="false" value="${Article.articletitle}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></div>
    <div><h:localVar key="page_articleDescription"/><h:textbox name="description" label="${page_articleDescription}: " maxLength="250" size="64" labelNextLine="false" value="${Article.description}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></div>
    <div><h:localVar key="page_articleAuthor"/><h:textbox name="postedname" label="${page_articleAuthor}: " maxLength="250" size="43" labelNextLine="false" value="${Article.postedname}" patternMismatch="${patternMismatch}" /></div>
    <div><h:localVar key="page_articleDate"/><h:textbox name="posted" label="${page_articleDate}: " maxLength="50" size="32" labelNextLine="false" title="ala Fri, 21 Dec 2012 00:20:12 EDT" value="${isNewArticle?null:formattedDate}" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></div>
    <div><h:localVar key="page_articleSuggestion"/><h:textbox name="suggestion" label="${page_articleSuggestion}: " maxLength="250" size="43" labelNextLine="false" value="${Article.suggestion}" patternMismatch="${patternMismatch}" placeholder="${defaultSearchTerm}" /></div>
    <div><h:localVar key="page_articleCommentable"/><h:checkbox name="comments" label=" ${page_articleCommentable}" checked="${Article.comments}" /></div>
    <div><h:localVar key="page_articleText"/><h:textarea name="postedmarkdown" length="100" height="20" label="${page_articleText}: " styleClass="articleText" value="${Article.postedmarkdown}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></div>
    <button type="submit" name="action" value="Preview" formtarget="articlePreview" formaction="adminArticle?iframe"><h:local key="page_preview"/></button>
    <button type="submit" name="action" value="Add Article"><h:local key="page_articleAdd"/></button>
</form>
<iframe name="articlePreview" class="resizeable" src="${iframeSrc}"></iframe>
</main><h:javascript/></body></html>
