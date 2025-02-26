<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<p><a href="adminExport" class="nocache"><h:local key="page_downloadBackup"/></a></p>
<form action="adminImport" method="POST" enctype="multipart/form-data" accept-charset="UTF-8" class="uploadBackup">
    <fieldset><legend><h:local key="page_uploadBackupLegend"/></legend>
        <h:localVar key="page_uploadBackupLabel"/><h:file name="zip" label="${page_uploadBackupLabel} " labelNextLine="false" /><br/>
        <button type="submit"><h:local key="page_upload"/></button>
    </fieldset>
</form>
<p><a href="mg" class="nocache"><h:local key="page_downloadMilligram"/></a></p>
</main>
<%@ include file="/WEB-INF/admin/adminFoot.jspf" %>
