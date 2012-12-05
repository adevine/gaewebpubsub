<%--@elvariable id="basePath" type="java.lang.String"--%>
<%--@elvariable id="validationParam" type="java.lang.String"--%>
<%--@elvariable id="topicKey" type="java.lang.String"--%>
<%--@elvariable id="userKey" type="java.lang.String"--%>
<%--@elvariable id="userName" type="java.lang.String"--%>
<%--@elvariable id="channelToken" type="java.lang.String"--%>
<%--@elvariable id="startingMessageNum" type="java.lang.Integer"--%>
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
//create namespaces
window.gaewps = window.gaewps || {};
window.gaewps.topics = window.gaewps.topics || {};
window.gaewps.topics["${topicKey}"] = window.gaewps.topics["${topicKey}"] || {};

//define helper methods at gaewps level
(function(gaewps) {
    gaewps.sendPost = function(url, params) {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", url, true /*async*/);
        xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        xhr.send(params);
    };

    gaewps.parseJson = function(jsonString) {
        if (typeof JSON == "undefined") {
            return eval("(" + jsonString + ")");
        } else {
            return JSON.parse(jsonString);
        }
    };

    gaewps.maxNumReturnReceiptCallbacks = 5;
})(window.gaewps);

(function(topic) {
    //sendMessage is how a client sends the specified messageText to the topic and all subscribers
    topic.sendMessage = function(messageText, selfNotify, returnReceiptCallback) {
        var needsReceipt = typeof returnReceiptCallback == "function";
        this.messageCounter = this.messageCounter + 1;
        if (needsReceipt) {
            this.returnReceiptCallbacks[this.messageCounter.toString()] = returnReceiptCallback;
            this.returnReceiptCallbackIds.push(this.messageCounter.toString());
            //clear out old return receipt callbacks
            while (this.returnReceiptCallbackIds.length > gaewps.maxNumReturnReceiptCallbacks) {
                var callbackIdToRemove = this.returnReceiptCallbackIds.shift();
                delete this.returnReceiptCallbacks[callbackIdToRemove];
            }
        }

        gaewps.sendPost("${basePath}/send",
                        "topicKey=" + encodeURIComponent("${topicKey}") +
                        "&userKey=" + encodeURIComponent("${userKey}") +
                        "&selfNotify=" + encodeURIComponent(selfNotify) +
                        "&needsReceipt=" + needsReceipt +
                        "&messageNumber=" + this.messageCounter +
                        "&message=" + encodeURIComponent(messageText) +
                        "${validationParam}");

        sessionStorage["msgNum_${topicKey}_${userKey}"] = this.messageCounter; //save the counter in session storage
        return this.messageCounter;
    };

    //disconnect can be called to disconnect the user from the topic. By default it is run on window unloading.
    topic.disconnect = function() {
        gaewps.sendPost("${basePath}/disconnect",
                        "topicKey=" + encodeURIComponent("${topicKey}") +
                        "&userKey=" + encodeURIComponent("${userKey}") +
                        "${validationParam}");
        if (typeof this.gaeChannelSocket != "undefined") {
            this.gaeChannelSocket.close();
        }
    };
    //disconnect if the user leaves the window
    if (window.addEventListener) {
        //bind to beforeunload if it's there, unload if not (mobile safari doesn't support beforeunload)
        window.addEventListener(("onbeforeunload" in window) ? "beforeunload" : "unload",
                                function() { topic.disconnect(); }, false);
    } else if (window.attachEvent) {
        window.attachEvent("onbeforeunload", function() { topic.disconnect(); });
    }

    //getSubscribers can be called to get the current list of subscribers to a topic
    //callback is a callback function that takes one parameter, which is the array of user names of the current subs.
    topic.getSubscribers = function(callback) {
        var xhr = new XMLHttpRequest();
        xhr.open("GET", "${basePath}/subscribers?topicKey=" + encodeURIComponent("${topicKey}"), true/*async*/);
        xhr.onreadystatechange = function() {
            if (xhr.readyState != 4)  { return; }
            //call the callback with the subscribers array
            callback(gaewps.parseJson(xhr.responseText));
        };
        xhr.send(null);
    };

    //event handlers can be set up by the client to respond to different events. We default to no-op methods
    topic.onmessage = function(messageText, messageNumber, senderName) { };
    topic.onconnected = function(userName) { };
    topic.ondisconnected = function(userName) { };

    topic.init = function() {
        var me = this;
        var channelSocket = new goog.appengine.Channel("${channelToken}").open();
        channelSocket.onmessage = function(messageObject) {
            //expect messageObject.data to be json from server
            var envelope = gaewps.parseJson(messageObject.data);
            if ("message" == envelope.eventType) {
                //if the message needs a receipt and it's NOT a message I sent (possible if selfNotify was on)
                if (envelope.needsReceipt && "${userName}" != envelope.sender) {
                    gaewps.sendPost("${basePath}/received",
                                    "topicKey=" + encodeURIComponent("${topicKey}") +
                                    "&userKey=" + encodeURIComponent("${userKey}") +
                                    "&originalSender=" + encodeURIComponent(envelope.sender) +
                                    "&messageNumber=" + envelope.messageNumber +
                                    "${validationParam}");
                }
                me.onmessage(envelope.message, envelope.messageNumber, envelope.sender);
            } else if ("connect" == envelope.eventType) {
                me.onconnected(envelope.sender);
            } else if ("disconnect" == envelope.eventType) {
                me.ondisconnected(envelope.sender);
            } else if ("receipt" == envelope.eventType) {
                //check for callback and execute if it's there
                var receiptCallback = me.returnReceiptCallbacks[envelope.messageNumber.toString()];
                if (receiptCallback) {
                    receiptCallback(envelope.messageNumber, envelope.sender);
                }
            }
        };
        this.gaeChannelSocket = channelSocket;

        //initialize the message counter for this topic/user
        this.messageCounter = ${startingMessageNum};

        //returnReceiptCallbacks is a map of messageCounter to the return receipt callback to run for that message
        //returnReceiptCallbackIds stores the messageCounters that are mapped to return receipts so we can remove
        //them when we have too many
        this.returnReceiptCallbacks = {};
        this.returnReceiptCallbackIds = [];
    };

    //call init, either right now or after dynamically loading the app engine channel dependency if needed
    if (window.goog && window.goog.appengine && window.goog.appengine.Channel) {
        topic.init();
    } else {
        var scriptElement = document.createElement("script");
        scriptElement.setAttribute("type", "text/javascript");
        scriptElement.setAttribute("src", "${basePath}/_ah/channel/jsapi");
        if (scriptElement.addEventListener) {
            scriptElement.addEventListener("load", function() { topic.init(); }, false);
        } else if (scriptElement.readyState) {
            scriptElement.onreadystatechange = function() { topic.init(); };
        }
        document.getElementsByTagName("head")[0].appendChild(scriptElement);
    }

})(window.gaewps.topics["${topicKey}"]);