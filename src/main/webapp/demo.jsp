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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>GAE Web Pub Sub Demo</title>
</head>
<body>
<p>
  This page gives a demonstration of the GAE Web Pub Sub functionality.
</p>
<form method="post" action="demorun.jsp">
  <p>
    <label for="topicKey">Enter a topic key</label> or <button type="button" onclick="generateTopicKey()">Generate one</button>:
    <input id="topicKey" name="topicKey" type="text" size="80" maxlength="128"/>
  </p>
  <p>
    <label for="userName">Enter your name</label>:
    <input id="userName" name="userName" type="text" size="80" maxlength="128"/>
  </p>
  <button type="submit">Go!</button>
</form>

<script type="text/javascript">
  function generateUuid() {
      return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
          var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
          return v.toString(16);
      });
  }

  function generateTopicKey() {
      document.getElementById("topicKey").value = generateUuid();
  }
</script>
</body>
</html>