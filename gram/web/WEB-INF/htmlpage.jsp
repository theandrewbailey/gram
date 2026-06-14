<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<div class="${IMEAD_ID}"><main class="leftContent ${IMEAD_ID}">
<%@ include file="/WEB-INF/searchSuggestion.jspf" %>
<c:out escapeXml="false" value="${htmlpagetext}"/>
<c:if test="${null != seeAlso}"><!-- ${seeAlsoTerm} --><nav class="seeAlso"><h1><h:local key="page_seeAlso"/></h1><ul>
<c:forEach items="${seeAlso}" var="seeArticle"><c:choose><c:when test="${null == seeArticle.imageurl}"><li class="noFigure"><article class="article${seeArticle.articleid}"><a href="<g:articleUrl article="${seeArticle}" link="${false}"/>"><h1>${seeArticle.articletitle}</h1></a></article></li>
</c:when><c:otherwise><li class="withFigure"><c:out escapeXml="false" value="${seeArticle.summary}"/></li>
</c:otherwise></c:choose></c:forEach></ul></nav></c:if>
</main>
<%@ include file="/WEB-INF/side.jspf" %>
</div>
<%@ include file="/WEB-INF/foot.jspf" %>
