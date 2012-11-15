if (typeof gaewps == "undefined") {
    gaewps = {};
}
if (typeof gaewps.topicManager == "undefined") {
    gaewps.topicManager = {};
}
if (typeof gaewps.topicManager["@TOPIC_KEY@"] == "undefined") {
    gaewps.topicManager["@TOPIC_KEY@"] = {};

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
    gaewps.topicManager["@TOPIC_KEY@"].sendMessage = function(messageText) {
        gaewps.sendPost("@BASE_PATH@/send",
                        "topicKey=" + encodeURIComponent("@TOPIC_KEY@") +
                        "&userKey=" + encodeURIComponent("@USER_KEY@") +
                        "&message=" + encodeURIComponent(messageText) +
                        "@VALIDATION_PARAM@");
    };

    //disconnect can be called to disconnect the user from the topic. By default it is run on window unloading.
    gaewps.topicManager["@TOPIC_KEY@"].disconnect = function() {
        gaewps.sendPost("@BASE_PATH@/disconnect",
                        "topicKey=" + encodeURIComponent("@TOPIC_KEY@") +
                        "&userKey=" + encodeURIComponent("@USER_KEY@") +
                        "@VALIDATION_PARAM@");
        if (typeof gaewps.topicManager["@TOPIC_KEY@"].gaeChannelSocket != "undefined") {
            gaewps.topicManager["@TOPIC_KEY@"].gaeChannelSocket.close();
        }
    };
    //attach to beforeunload event
    //TODO - check for beforeunload, if not present use unload - mobile safari doesn't support beforeunload.
    if (window.addEventListener) {
        window.addEventListener("beforeunload", gaewps.topicManager["@TOPIC_KEY@"].disconnect, false);
    } else if (window.attachEvent) {
        window.attachEvent("onbeforeunload", gaewps.topicManager["@TOPIC_KEY@"].disconnect);
    }

    //event handlers can be set up by the client to respond to different events. We default to no-op methods
    gaewps.topicManager["@TOPIC_KEY@"].onmessage = function(messageText, messageNumber, senderName) { };
    gaewps.topicManager["@TOPIC_KEY@"].onconnected = function(userName) { };
    gaewps.topicManager["@TOPIC_KEY@"].ondisconnected = function(userName) { };
}

gaewps.topicManager["@TOPIC_KEY@"].init = function() {
    var parseJson = function(jsonString) {
        if (typeof JSON == "undefined") {
            return eval("(" + jsonString + ")");
        } else {
            return JSON.parse(jsonString);
        }
    };

    var channelSocket = new goog.appengine.Channel("@CHANNEL_TOKEN@").open();
    channelSocket.onmessage = function(messageObject) {
        //expect messageObject.data to be json from server
        var envelope = parseJson(messageObject.data);
        if ("message" == envelope.eventType) {
            gaewps.topicManager["@TOPIC_KEY@"].onmessage(envelope.message, envelope.messageNumber, envelope.sender);
        } else if ("connect" == envelope.eventType) {
            gaewps.topicManager["@TOPIC_KEY@"].onconnected(envelope.sender);
        } else if ("disconnect" == envelope.eventType) {
            gaewps.topicManager["@TOPIC_KEY@"].ondisconnected(envelope.sender);
        }
    };
    gaewps.topicManager["@TOPIC_KEY@"].gaeChannelSocket = channelSocket;
};

//call init, either right now or after dynamically loading the app engine channel dependency if needed
gaewps.isGoogChannelLoaded = false;
try {
    if (typeof goog.appengine.Channel != "undefined") {
        gaewps.isGoogChannelLoaded = true;
    }
} catch (e) { /*ok, channel not loaded */ }

if (gaewps.isGoogChannelLoaded) {
    gaewps.topicManager["@TOPIC_KEY@"].init();
} else {
    var scriptElement = document.createElement("script");
    scriptElement.setAttribute("type", "text/javascript");
    scriptElement.setAttribute("src", "@BASE_PATH@/_ah/channel/jsapi");
    if (scriptElement.addEventListener) {
        scriptElement.addEventListener("load", gaewps.topicManager["@TOPIC_KEY@"].init, false);
    } else if (scriptElement.readyState) {
        scriptElement.onreadystatechange = gaewps.topicManager["@TOPIC_KEY@"].init;
    }
    document.getElementsByTagName("head")[0].appendChild(scriptElement);
}