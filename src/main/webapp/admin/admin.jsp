<!DOCTYPE html>
<%--
   Copyright 2012 Alexander Devine

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java"
         import="org.gaewebpubsub.util.Escapes,org.gaewebpubsub.web.ValidationFilter,org.gaewebpubsub.services.Defaults,org.gaewebpubsub.services.ConfigManager" %>
<%
  ConfigManager configManager = Defaults.newConfigManager();

  boolean saved = false;
  if (request.getMethod().equalsIgnoreCase("post")) {
    //then save
    configManager.set(ValidationFilter.VALIDATION_KEY_CONFIG_PROP, request.getParameter("validationKey"));
    configManager.set("saveMessages", request.getParameter("saveMessages"));
    saved = true;
  }
%>
<%-- note this page is restricted to admin users only in web.xml --%>
<html>
<head>
  <title>GAE Web Pub Sub Configuration Options</title>
</head>
<body style="font-family: sans-serif">
<h1>GAE Web Pub Sub Configuration Options</h1>

<%= saved ? "<h2 style='color: red'>Config Options Saved Successfully</h2>" : "" %>

<form method="post">
  <table border="1">
    <thead>
    <tr>
      <th>Option</th>
      <th>Value</th>
      <th>Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
      <td style="width: 30%; vertical-align: top"><label for="validationKey">Validation Key</label></td>
      <td style="width: 30%; vertical-align: top; text-align: center">
        <input type="text"
               style="width: 90%"
               maxlength="400"
               id="validationKey"
               name="validationKey"
               value="<%=Escapes.escapeHtml(configManager.get(ValidationFilter.VALIDATION_KEY_CONFIG_PROP, ""))%>"/>
      </td>
      <td style="width:40%; vertical-align: top">
        Setting a validation key means that all incoming requests will be validated. This means that only requests from
        servers that also know this private validation key will be allowed. This validation key should be secure
        (e.g. a randomly generated UUID). See the ValidationFilter class for an explanation of the validation process,
        and specifically the ValidationUtils class for an explanation of the algorithm.
        You may also use a custom validation algorithm by using a different ValidationFilter implementation.
        <br/><br/>
        If you wish to disable validation, save the validation key as a blank value.
      </td>
    </tr>
    <tr>
      <td style="vertical-align: top"><label for="saveMessages">Save Messages?</label></td>
      <td style="vertical-align: top; text-align: center">
        <select id="saveMessages" name="saveMessages">
          <option value="false">no</option>
          <option value="true" <%= Boolean.parseBoolean(configManager.get("saveMessages", "false")) ? "selected" : "" %>>yes</option>
        </select>
      </td>
      <td style="width:40%; vertical-align: top">
        By default, messages will not be persisted to the datastore. If you set this value to yes, then all messages
        sent to new topics will be persisted.
      </td>
    </tr>
    </tbody>
  </table>
  <button type="submit">Save</button>
</form>

</body>
</html>