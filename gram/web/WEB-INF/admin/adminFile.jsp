<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<form action="file" method="POST" enctype="multipart/form-data" class="adminform adminContent" accept-charset="UTF-8">
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if><h:localVar key="page_valueMissing" var="valueMissing" />
<h:textbox name="directory" label="Directory:" datalist="${directories}" maxLength="250" labelNextLine="false" value="${prop.key}" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" />
<h:file name="filedata" label="File Upload: " labelNextLine="false" required="true" valueMissing="${valueMissing}" multiple="true" />
<%-- <h:checkbox name="overwrite" label="Overwrite" /> --%>
<button type="submit"><h:local key="page_upload"/></button>
<c:if test="${null != uploadedfiles}"><p class="adminform"><h:local key="page_uploadSuccess"/><c:forEach items="${uploadedfiles}" var="uploadedfile"><br/><a href="file/${uploadedfile.filename}" target="_blank" rel="noopener" class="nocache">${uploadedfile.filename}</a></c:forEach></p></c:if>
</form>
<br/>
<form action="adminFile" method="POST" class="adminform adminContent" accept-charset="UTF-8"><p>Current Files:</p>
    <c:forEach items="${files}" var="dir">
    <details ${opened_dir == dir.key ? "open='true'" : ""} ><summary>${dir.key}</summary><table>
    <c:forEach items="${dir.value}" var="con">
    <tr class="secondmin" id="${con.uuid}"><td><a href="${con.url}" target="_blank" rel="noopener" class="nocache">${con.filename}</a></td>
    <td><h:filesize length="${con.datasize}"/></td>
    <td><a href="file/${dir.key}${con.filename}" target="_blank" rel="noopener" class="nocache"><h:time datetime="${con.atime}" pattern="yyyy-MM-dd h:mm a" /></a></td>
    <td>${con.mimetype}&nbsp;<h:button type="submit" name="action" value="delete|${con.url}"><h:local key="page_delete"/></h:button></td></tr>
    </c:forEach></table></details></c:forEach>
</form>
</main>
<%@ include file="/WEB-INF/admin/adminFoot.jspf" %>
