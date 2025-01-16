<%-- used for sending JSON, with proper MIME type and in JSP form for potential cachability.
parameters: page attribute: javax.json.Json*Builder json
--%>
<%@ page session="false" contentType="application/json" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:out escapeXml="false" value="${json.build().toString()}"/>