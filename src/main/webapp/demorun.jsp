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
<%@ page contentType="text/html;charset=UTF-8" language="java" import="org.gaewebpubsub.util.SecureHash,org.gaewebpubsub.web.BaseServlet,org.gaewebpubsub.util.Escapes" %>
<%
  String topicKey = request.getParameter("topicKey");
  String userName = request.getParameter("userName");
  if (topicKey == null || topicKey.trim().length() == 0 || userName == null || userName.trim().length() == 0) {
      throw new IllegalArgumentException("topicKey and userName parameters are required");
  }
  String userKey = SecureHash.hash(userName);
%>
<html>
<head>
  <title>GAE Web Pub Sub Chat Demo</title>
</head>
<body>
<p>
  Welcome, <%=Escapes.escapeHtml(userName)%>
</p>
<p>
  To invite a friend, tell them to go to this URL: <span style="font-weight: bold; color: blue"><%=BaseServlet.getBaseUrl(request)%>/demo.jsp?topicKey=<%=Escapes.escapeUrlParam(topicKey)%></span>
</p>
<p>
  <label for="messageText">Send a message</label>:
  <input id="messageText" name="messageText" type="text" size="80" maxlength="128" onkeydown="if (event.keyCode == 13) sendMessageToTopic();"/>
  <button type="button" onclick="sendMessageToTopic()">Send</button>
</p>
<div id="chatDiv" style="border: 1px solid black; background-color: #d3d3d3; width: 90%; height: 500px; overflow: scroll">

</div>

<script type="text/javascript" src="connect?topicKey=<%=Escapes.escapeUrlParam(topicKey)%>&userName=<%=Escapes.escapeUrlParam(userName)%>&userKey=<%=Escapes.escapeUrlParam(userKey)%>"></script>
<script type="text/javascript">
  function escapeHtml(text) {
      var div = document.createElement('div');
      div.appendChild(document.createTextNode(text));
      return div.innerHTML;
  }

  function sendMessageToTopic() {
      var messageTextInput = document.getElementById("messageText");
      var messageNum = gaewps.topics["<%=Escapes.escapeJavaScriptString(topicKey)%>"].sendMessage(
          messageTextInput.value,
          false /*we DON'T selfNotify here*/,
          function returnReceiptCallback(messageNum, senderName) {
              //another user got our text, so we mark our text as "received" by appending a check mark
              var msgSpan = document.getElementById("message_" + messageNum + "_You");
              msgSpan.innerHTML = msgSpan.innerHTML + " " + String.fromCharCode(0x2713); //2713 is check mark
          }
      );
      addMessageToChatDiv(messageNum, "You", messageTextInput.value, "font-weight: bold; color: green");
      messageTextInput.value = "";
  }

  gaewps.topics["<%=Escapes.escapeJavaScriptString(topicKey)%>"].onmessage = function(messageText, messageNumber, senderName) {
      addMessageToChatDiv(messageNumber, senderName, messageText, "font-weight: bold; color: blue");
  };

  function addMessageToChatDiv(messageNumber, senderName, messageText, spanStyle) {
      var chatDiv = document.getElementById("chatDiv");
      var safeSenderName = escapeHtml(senderName);
      var safeMessage = escapeHtml(messageText);
      var spanId = "message_" + messageNumber + "_" + safeSenderName;
      chatDiv.innerHTML = chatDiv.innerHTML + "<span id='" + spanId + "' style='" + spanStyle + "'>" + safeSenderName + " wrote: " + safeMessage + "</span><br/>";
  }

  gaewps.topics["<%=Escapes.escapeJavaScriptString(topicKey)%>"].onconnected = function(userName) {
      var chatDiv = document.getElementById("chatDiv");
      chatDiv.innerHTML = chatDiv.innerHTML + "<b>" + escapeHtml(userName) + " just connected!" + "</b><br/>";
  };

  gaewps.topics["<%=Escapes.escapeJavaScriptString(topicKey)%>"].ondisconnected = function(userName) {
      var chatDiv = document.getElementById("chatDiv");
      chatDiv.innerHTML = chatDiv.innerHTML + "<b>" + escapeHtml(userName) + " just disconnected!"  + "</b><br/>";
  };

  //show the connected subscribers now
  gaewps.topics["<%=Escapes.escapeJavaScriptString(topicKey)%>"].getSubscribers(function(subscribers) {
      var chatDiv = document.getElementById("chatDiv");
      var message = "Users who are in this chat right now: ";
      for (var i = 0; i < subscribers.length; i++) {
          if (i > 0) {
              message += ", ";
          }

          var safeSubscriberName = escapeHtml(subscribers[i]);
          var textColor = "blue";
          if (subscribers[i] == "<%=Escapes.escapeJavaScriptString(userName)%>") {
              safeSubscriberName = "You";
              textColor = "green;"
          }
          message += "<span style='font-weight: bold; color: " + textColor + "'>" + safeSubscriberName + "</span>";
      }
      chatDiv.innerHTML = chatDiv.innerHTML + message + "<br/>";
  });
</script>
</body>
</html>