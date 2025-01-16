<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main class="adminform adminHealth">
    <form action="adminHealth" method="POST" class="adminform" accept-charset="UTF-8">
        <h:button type="submit" name="action" value="refresh"><h:local key="page_refresh_page"/></h:button>
        <h:button type="submit" name="action" value="error"><h:local key="page_error_rss"/></h:button>
        <h:button type="submit" name="action" value="reload"><h:local key="page_reload_site"/></h:button>
    </form>
    <c:forEach items="${processes.get()}" var="process"><details class="process" open="true"><summary>${process.key}</summary><pre>${process.value.get()}</pre></details>
    </c:forEach>
    <c:set scope="page" var="Subject" value="Subject"/>
    <c:forEach items="${certPaths}" var="certPath"><details class="certpath">
        <summary><h:local key="page_health_cert_path"><h:param object="${certInfo.get(certPath.getCertificates().get(certPath.getCertificates().size()-1)).get(Subject)}"/><h:param>${fn:substringBefore(((certPath.getExpiration().getTime() - $_LIBWEBSITETOOLS_REQUEST_START_TIME.toInstant().toEpochMilli()) / 86400000),".")}</h:param></h:local></summary><c:forEach items="${certPath.getCertificates()}" var="cert">
        <details class="secondmin"><summary><h:local key="page_health_cert"><h:param object="${certInfo.get(cert).get(Subject)}"/><h:param object="${certInfo.get(cert).daysUntilExpiration}"/></h:local></summary>
            <table><c:forEach items="${certInfo.get(cert)}" var="info">
                <tr class="secondmin"><td class="secondmin">${info.key}</td><td>${info.value}</td></tr></c:forEach>
        </table></details>
    </c:forEach></details></c:forEach>
    <details class="performance"><summary>Server-Timings</summary><ul>
    <c:forEach items="${performance.get()}" var="perf"><li><details class="perf"><summary>${perf.key}</summary>
        <table><c:forEach items="${perf.value}" var="perfVal">
        <tr><td>${perfVal.key}</td><td>${perfVal.value}</td></tr></c:forEach></table>
    </details></li></c:forEach></ul></details>
    <details class="cached"><summary><h:local key="page_health_cache_count"><h:param object="${cached.get().size()-1}"/></h:local></summary>
        <ul><c:forEach items="${cached.get()}" var="pageEnt"><li>${pageEnt}</li></c:forEach></ul>
    </details>
    <details class="articles"><summary><h:local key="page_health_article_count"><h:param object="${articles.get().size()}"/></h:local></summary><table>
    <c:forEach items="${articles.get()}" var="art"><tr class="secondmin" id="${art.uuid}"><td><g:articleUrl article="${art}" cssClass="nocache"/></td><td><c:if test="${art.sectionid.name != ' '}">${art.sectionid.name}</c:if></td><td><h:time datetime="${art.posted}" pattern="EEE MM/dd/yy h:mm a"/></td></tr>
    </c:forEach></table></details>
    <details class="comments"><summary><h:local key="page_health_comment_count"><h:param object="${comments.get().size()}"/></h:local></summary><table>
    <c:forEach items="${comments.get()}" var="comm"><tr class="secondmin" id="${comm.uuid}"><td><h:time datetime="${comm.posted}" pattern="EEE MM/dd/yy h:mm a"/></td><td>${comm.postedname}</td><td><g:articleUrl article="${comm.articleid}" cssClass="nocache"/></td></tr>
    </c:forEach></table></details>
    <details class="files"><summary><h:local key="page_health_file_count"><h:param object="${files.get().size()}"/></h:local></summary>
    <table><c:forEach items="${files.get()}" var="file">
    <tr class="secondmin" id="${file.uuid}"><td><a href="${file.url}" target="_blank" rel="noopener" class="nocache">${file.filename}</a></td>
    <td><h:filesize length="${file.datasize}"/></td><td>${file.mimetype}</td><td><h:time datetime="${file.atime}" pattern="yyyy-MM-dd h:mm a" /></td></tr></c:forEach></table>
    </details>
</main>
<%@ include file="/WEB-INF/adminFoot.jspf" %>
