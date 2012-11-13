if (typeof window.gaewps_TokenManager == "undefined") {
    window.gaewps_TokenManager = {};
}
if (typeof window.gaewps_TokenManager["@TOPIC_KEY@"] == "undefined") {
    window.gaewps_TokenManager["@TOPIC_KEY@"] = {};

    //sendMessage is how a client sends a message
    window.gaewps_TokenManager["@TOPIC_KEY@"].sendMessage = function(messageText) {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "@BASE_PATH@/send", true /*async*/);
        xhr.send("topicKey=" + encodeURIComponent("@TOPIC_KEY@") +
                 "userKey=" + encodeURIComponent("@USER_KEY@") +
                 "message=" + encodeURIComponent(messageText));
        //TODO - worry about response?
    };

    //disconnect should be called when the user disconnects
    //TODO - add onunload disconnect??
    window.gaewps_TokenManager["@TOPIC_KEY@"].disconnect = function() {
        var xhr = new XMLHttpRequest();
        xhr.open("POST", "@BASE_PATH@/disconnect", true /*async*/);
        xhr.send("topicKey=" + encodeURIComponent("@TOPIC_KEY@") +
                 "userKey=" + encodeURIComponent("@USER_KEY@"));
        //TODO - worry about response?
        if (typeof window.gaewps_TokenManager["@TOPIC_KEY@"].gaeChannelSocket != "undefined") {
            window.gaewps_TokenManager["@TOPIC_KEY@"].gaeChannelSocket.close();
        }
    };

    //event handlers can be set up by the client to respond to different events. We default to no-op methods
    window.gaewps_TokenManager["@TOPIC_KEY@"].onmessage = function(messageText, messageNumber, senderName) { };
    window.gaewps_TokenManager["@TOPIC_KEY@"].onconnected = function(userName) { };
    window.gaewps_TokenManager["@TOPIC_KEY@"].ondisconnected = function(userName) { };
}

function gaewps_init() {
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
            window.gaewps_TokenManager["@TOPIC_KEY@"].onmessage(envelope.message, envelope.messageNumber, envelope.sender);
        } else if ("connect" == envelope.eventType) {
            window.gaewps_TokenManager["@TOPIC_KEY@"].onconnected(envelope.sender);
        } else if ("disconnect" == envelope.eventType) {
            window.gaewps_TokenManager["@TOPIC_KEY@"].ondisconnected(envelope.sender);
        }
    };
    window.gaewps_TokenManager["@TOPIC_KEY@"].gaeChannelSocket = channelSocket;
}

//call init, dynamically loading the app engine channel dependency if needed
if (typeof goog.appengine.Channel == "undefined") {
    var scriptElement = document.createElement("script");
    scriptElement.setAttribute("type", "text/javascript");
    scriptElement.setAttribute("src", "@BASE_PATH@/_ah/channel/jsapi");
    if (scriptElement.addEventListener) {
        scriptElement.addEventListener("load", gaewps_init, false);
    } else if (scriptElement.readyState) {
        scriptElement.onreadystatechange = gaewps_init;
    }
    document.getElementsByTagName("head")[0].appendChild(scriptElement);
}