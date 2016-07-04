#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<h2><spring:message code="${parentArtifactId}.replace.this.link.name" /></h2>

<br/>
<table>
  <tr>
   <th>Patient Id</th>
   <th>Name</th>
   <th>Identifier</th>
  </tr>
  <c:forEach var="patient" items="${symbol_dollar}{thePatientList}">
      <tr>
        <td>${symbol_dollar}{patient.patientId}</td>
        <td>${symbol_dollar}{patient.personName}</td>
        <td>${symbol_dollar}{patient.patientIdentifier}</td>
      </tr>		
  </c:forEach>
</table>

<%@ include file="/WEB-INF/template/footer.jsp"%>
