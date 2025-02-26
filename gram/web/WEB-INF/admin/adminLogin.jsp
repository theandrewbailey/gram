<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<c:set var="autofocus" value="true" scope="page"/>
<main><%@ include file="/WEB-INF/admin/adminLoginForm.jspf" %></main>
<br/><footer class="downContent">
<p><h:responseTag><h:local key="page_footerFormat">
    <h:param><h:time datetime="${requestTime}"/></h:param>
    <h:param object="${renderMillis}"/>
</h:local></h:responseTag></p>
</footer><h:javascript/></body></html>
