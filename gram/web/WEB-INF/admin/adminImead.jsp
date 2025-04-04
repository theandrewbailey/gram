<%@ page trimDirectiveWhitespaces="true" %>
<%@ include file="/WEB-INF/head.jspf" %>
<main>
<c:if test="${ERROR_MESSAGE != null}"><p class="error">${ERROR_MESSAGE}</p></c:if>
<form action="adminImead" method="post" class="adminform adminImead" accept-charset="UTF-8">
    <h:button type="submit" name="action" value="save"><h:local key="page_save"/></h:button>
    <h:localVar key="page_patternMismatch" var="patternMismatch" /><h:localVar key="page_valueMissing" var="valueMissing" />
    <h:datalist id="localeList" options="${locales}"/>
    <c:set scope="page" var="propCount" value="${0}"/>

    <details open="true"><summary>Security:</summary><table class="secondmin"><thead><tr><th><h:local key="page_setupKey"/></th><th><h:local key="page_setupValue"/></th></tr></thead>
    <c:forEach items="${security}" var="prop">
        <tr <c:if test="${ERRORS.contains(prop.localizationPK)}">class="error"</c:if>><td>
            <label for="${prop.localizationPK.key}">${prop.localizationPK.key}</label>
            <h:hidden name="key${propCount}" value="${prop.localizationPK.key}" /><h:hidden name="locale${propCount}" value="" />
        </td><td>
            <h:textarea id="${prop.localizationPK.key}" name="value${propCount}" length="80" labelNextLine="false" value="${prop.value}" required="true" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" />
        </td></tr><c:set scope="page" var="propCount" value="${1+propCount}"/>
    </c:forEach></table>
    <h:button type="submit" name="action" value="save"><h:local key="page_save"/></h:button>
    </details>

    <c:forEach items="${imeadProperties}" var="imeadProp">
    <details open="true"><summary>${imeadProp.key}</summary>
    <table class="secondmin"><thead><tr><th><h:local key="page_setupKey"/></th><th><h:local key="page_setupValue"/></th><c:if test="${FIRST_TIME_SETUP != 'FIRST_TIME_SETUP'}"><th></th></c:if></tr></thead>
    <c:forEach items="${imeadProp.value}" var="prop">
    <tr><td>
        <label for="${imeadProp.key}|${prop.localizationPK.key}">${prop.localizationPK.key}</label>
        </td><td>
            <h:hidden name="key${propCount}" value="${prop.localizationPK.key}" />
            <h:hidden name="locale${propCount}" value="${imeadProp.key}" />
            <h:textarea id="${imeadProp.key}|${prop.localizationPK.key}" name="value${propCount}" length="80" labelNextLine="false" value="${prop.value}" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" />
        </td>
    <c:if test="${FIRST_TIME_SETUP != 'FIRST_TIME_SETUP'}"><td><h:button type="submit" name="action" value="delete|${imeadProp.key}|${prop.localizationPK.key}"><h:local key="page_delete"/></h:button><h:hidden name="locale${propCount}" value="${imeadProp.key}" /></td></c:if>
    </tr><c:set scope="page" var="propCount" value="${1+propCount}"/>
    </c:forEach></table>
    <h:button type="submit" name="action" value="save"><h:local key="page_save"/></h:button>
    </details></c:forEach>

    <table class="secondmin">
        <thead><tr><th>Locale:</th><th>Key:</th><th>Value:</th></tr></thead>
        <c:forEach begin="${propCount}" end="${propCount+4}" var="newCount"><tr>
            <td><h:textbox name="locale${newCount}" value="${imeadProp.key}" datalist="localeList" maxLength="16" labelNextLine="false" /></td>
            <td><h:textbox name="key${newCount}" maxLength="250" labelNextLine="false" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></td>
            <td><h:textarea name="value${newCount}" length="70" labelNextLine="false" valueMissing="${valueMissing}" patternMismatch="${patternMismatch}" /></td>
        </tr></c:forEach>
    </table>
    <h:button type="submit" name="action" value="save"><h:local key="page_setupAdd"/></h:button>
</form>
<c:if test="${FIRST_TIME_SETUP == 'FIRST_TIME_SETUP'}">
    <form action="adminImport" method="POST" enctype="multipart/form-data" accept-charset="UTF-8">
        <fieldset><legend><h:local key="page_uploadBackupLegend"/></legend>
            <h:localVar key="page_uploadBackupLabel"/><h:file name="zip" label="${page_uploadBackupLabel}" labelNextLine="false" />
            <h:button type="submit" name="action" value="Upload"><h:local key="page_upload"/></h:button>
        </fieldset>
    </form>
</c:if>
</main>
<%@ include file="/WEB-INF/admin/adminFoot.jspf" %>
