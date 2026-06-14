<%-- used for sending RSS feeds, with proper MIME type and in JSP form for potential cachability.
parameters: page attribute: String RSSOut
--%><%@ page session="false" contentType="application/rss+xml" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><c:out escapeXml="false" value="${RSSOut}"/>