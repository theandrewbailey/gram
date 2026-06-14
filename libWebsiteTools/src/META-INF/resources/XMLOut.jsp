<%-- used for sending generic XML, with proper MIME type and in JSP form for potential cachability.
parameters: page attribute: String XMLOut
--%><%@ page session="false" contentType="application/xml" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:out escapeXml="false" value="${XMLOut}"/>