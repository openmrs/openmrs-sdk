<table>
  <tr>
   <th>User Id</th>
   <th>Username</th>
  </tr>
  <% if (users) { %>
     <% users.each { %>
      <tr>
        <td>${ ui.format(it.userId) }</td>
        <td>${ ui.format(it.systemId) }</td>
      </tr>
    <% } %>
  <% } else { %>
  <tr>
    <td colspan="2">${ ui.message("general.none") }</td>
  </tr>
  <% } %>
</table>