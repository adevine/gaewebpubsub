<%--@elvariable id="basePath" type="java.lang.String"--%>
<%--@elvariable id="validationParam" type="java.lang.String"--%>
<%--@elvariable id="topicKey" type="java.lang.String"--%>
<%--@elvariable id="userKey" type="java.lang.String"--%>
<%--@elvariable id="channelToken" type="java.lang.String"--%>
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
<%@ page contentType="text/javascript;charset=UTF-8" language="java" %>
if (typeof gaewps == "undefined") {
    gaewps = {};
}
if (typeof gaewps.topicManager == "undefined") {
    gaewps.topicManager = {};
}
if (typeof gaewps.topicManager["${topicKey}"] == "undefined") {
    gaewps.topicManager["${topicKey}"] = {};

    //helper method
    gaewps.sendPost = function(url, params) {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", url, true /*async*/);
        xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        xhr.setRequestHeader("Content-length", params.length);
        xhr.setRequestHeader("Connection", "close");
        xhr.send(params);
    };

    /*
     * All of the public topicManager methods and callbacks are defined here
     */

    //sendMessage is how a client sends the specified messageText to the topic and all subscribers
    gaewps.topicManager["${topicKey}"].sendMessage = function(messageText) {
        gaewps.sendPost("${basePath}/send",
                        "topicKey=" + encodeURIComponent("${topicKey}") +
                        "&userKey=" + encodeURIComponent("${userKey}") +
                        "&message=" + encodeURIComponent(messageText) +
                        "${validationParam}");
    };

    //disconnect can be called to disconnect the user from the topic. By default it is run on window unloading.
    gaewps.topicManager["${topicKey}"].disconnect = function() {
        gaewps.sendPost("${basePath}/disconnect",
                        "topicKey=" + encodeURIComponent("${topicKey}") +
                        "&userKey=" + encodeURIComponent("${userKey}") +
                        "${validationParam}");
        if (typeof gaewps.topicManager["${topicKey}"].gaeChannelSocket != "undefined") {
            gaewps.topicManager["${topicKey}"].gaeChannelSocket.close();
        }
    };
    //attach to beforeunload event
    //TODO - check for beforeunload, if not present use unload - mobile safari doesn't support beforeunload.
    if (window.addEventListener) {
        window.addEventListener("beforeunload", gaewps.topicManager["${topicKey}"].disconnect, false);
    } else if (window.attachEvent) {
        window.attachEvent("onbeforeunload", gaewps.topicManager["${topicKey}"].disconnect);
    }

    //event handlers can be set up by the client to respond to different events. We default to no-op methods
    gaewps.topicManager["${topicKey}"].onmessage = function(messageText, messageNumber, senderName) { };
    gaewps.topicManager["${topicKey}"].onconnected = function(userName) { };
    gaewps.topicManager["${topicKey}"].ondisconnected = function(userName) { };
}

gaewps.topicManager["${topicKey}"].init = function() {
    var parseJson = function(jsonString) {
        if (typeof JSON == "undefined") {
            return eval("(" + jsonString + ")");
        } else {
            return JSON.parse(jsonString);
        }
    };

    var channelSocket = new goog.appengine.Channel("${channelToken}").open();
    channelSocket.onmessage = function(messageObject) {
        //expect messageObject.data to be json from server
        var envelope = parseJson(messageObject.data);
        if ("message" == envelope.eventType) {
            gaewps.topicManager["${topicKey}"].onmessage(envelope.message, envelope.messageNumber, envelope.sender);
        } else if ("connect" == envelope.eventType) {
            gaewps.topicManager["${topicKey}"].onconnected(envelope.sender);
        } else if ("disconnect" == envelope.eventType) {
            gaewps.topicManager["${topicKey}"].ondisconnected(envelope.sender);
        }
    };
    gaewps.topicManager["${topicKey}"].gaeChannelSocket = channelSocket;
};

//call init, either right now or after dynamically loading the app engine channel dependency if needed
gaewps.isGoogChannelLoaded = false;
try {
    if (typeof goog.appengine.Channel != "undefined") {
        gaewps.isGoogChannelLoaded = true;
    }
} catch (e) { /*ok, channel not loaded */ }

if (gaewps.isGoogChannelLoaded) {
    gaewps.topicManager["${topicKey}"].init();
} else {
    var scriptElement = document.createElement("script");
    scriptElement.setAttribute("type", "text/javascript");
    scriptElement.setAttribute("src", "${basePath}/_ah/channel/jsapi");
    if (scriptElement.addEventListener) {
        scriptElement.addEventListener("load", gaewps.topicManager["${topicKey}"].init, false);
    } else if (scriptElement.readyState) {
        scriptElement.onreadystatechange = gaewps.topicManager["${topicKey}"].init;
    }
    document.getElementsByTagName("head")[0].appendChild(scriptElement);
}
