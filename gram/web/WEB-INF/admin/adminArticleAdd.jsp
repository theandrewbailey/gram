<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<main>
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<form action="adminArticle" method="post" class="adminform adminArticleAdd" accept-charset="UTF-8">
    <h:localVar key="page_patternMismatch" var="patternMismatch" /><h:localVar key="page_valueMissing" var="valueMissing" />
    <h:textbox name="section" label="Category: " labelNextLine="false" patternMismatch="${patternMismatch}" datalist="${groups}" value="${Article.sectionid.name}"/><br/>
    <h:textbox name="articletitle" label="Title: " maxLength="250" size="64" labelNextLine="false" value="${Article.articletitle}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /><br/>
    <h:textbox name="description" label="Description: " maxLength="250" size="64" labelNextLine="false" value="${Article.description}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /><br/>
    <h:textbox name="postedname" label="By: " maxLength="250" size="43" labelNextLine="false" value="${Article.postedname}" patternMismatch="${patternMismatch}" /><br/>
    <h:textbox name="posted" label="Posted Date: " maxLength="50" size="32" labelNextLine="false" title="ala Fri, 21 Dec 2012 00:20:12 EDT" value="${formattedDate}" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /><br/>
    <h:textbox name="suggestion" label="Suggestion search term: " maxLength="250" size="43" labelNextLine="false" value="${Article.suggestion}" patternMismatch="${patternMismatch}" placeholder="${defaultSearchTerm}" /><br/>
    <h:checkbox name="comments" label="Commentable" checked="${Article.comments}" /><br/>
    <h:textarea name="postedmarkdown" length="100" height="20" label="Text (>64000):" styleClass="articleText" value="${Article.postedmarkdown}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /><br/>
    <button type="submit" name="action" value="Preview" formtarget="articlePreview"><h:local key="page_preview"/></button>
    <button type="submit" name="action" value="Add Article"><h:local key="page_articleAdd"/></button>
</form>
<article><p>&nbsp;</p></article>
<iframe name="articlePreview" class="resizeable" ${iframeSrc}></iframe>
</main>
<%@ include file="/WEB-INF/admin/adminFoot.jspf" %>
