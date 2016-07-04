#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
<spring:htmlEscape defaultHtmlEscape="true" />
<ul id="menu">
	<li class="first"><a
		href="${symbol_dollar}{pageContext.request.contextPath}/admin"><spring:message
				code="admin.title.short" /></a></li>

	<li
		<c:if test='<%= request.getRequestURI().contains("/manage") %>'>class="active"</c:if>>
		<a
		href="${symbol_dollar}{pageContext.request.contextPath}/module/${parentArtifactId}/manage.form"><spring:message
				code="${parentArtifactId}.manage" /></a>
	</li>
	
	<!-- Add further links here -->
</ul>
<h2>
	<spring:message code="${parentArtifactId}.title" />
</h2>
