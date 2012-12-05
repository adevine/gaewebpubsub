# GAE Web Pub/Sub #

GAEWebPubSub is a publish/subscribe service that is a very thin layer built
around the Channel API of Google App Engine. It provides a simple way to
create topics that multiple clients can connect to for sharing messages (think
chat rooms or an instant messenger).

The rationale for building GAEWebPubSub is that I wanted to take advantage
of the Channel API, but at the same time I was worried about being tied to
App Engine for my entire application. The benefit of GAEWebPubSub is that it
can be completely stand alone without having knowledge of any other parts of
the rest of your application.

## Quick Start ##

GAEWebPubSub is integrated into a web app by including a javascript file and
then interacting with the Topic object created by the script (e.g. making
sendMessage() calls and responding to onmessage callbacks).

1.  First, you must include the javascript file that connects the user to the
    topic:

        <script src="http://gaewebpubsub.appspot.com/connect?topicKey=TOPIC_KEY_HERE&userName=USER_NAME_HERE&userKey=USER_KEY_HERE"></script>

    You should replace the parameters to the script with the following values:
    1.  **TOPIC_KEY_HERE**: This should be a unique key that defines the
        topic. For security reasons this key should be unguessable (e.g. a
        secure hash value).
    2.  **USER_NAME_HERE**: The name of the user subscribing to the topic.
        This name should be unique among all subscribers in the same topic.
    3.  **USER_KEY_HERE**: A globally unique key that identifies the
        subscriber.

2.  To send messages to the topic you use the sendMessage() method:

        gaewps.topics[TOPIC_KEY_HERE].sendMessage("Hi! I'm a new message!");

    The sendMessage() method can also takes optional "selfNotify" and
    "returnReceiptCallback" arguments explained in the Javascript Reference
    section below.

3.  To respond to incoming messages you set an "onmessage" handler on the
    topic object:

        gaewps.topics[TOPIC_KEY_HERE].onmessage = function(messageText, messageNumber, senderName) {
            alert("Just got message #" + messageNumber + " from " + senderName + ": " + messageText);
        };

4.  In addition, you can be notified when users join or leave the topic by
    setting "onconnected" or "ondisconnected" handlers, and you can get a list
    of all current subscribers by calling the "getSubscribers(callback)"
    method.

To see an example chat program built with GAEWebPubSub go to
<http://gaewebpubsub.appspot.com/demo.jsp>.

## Hosting Your Own Instance of GAEWebPubSub ##

To use GAEWebPubSub in production you will need to host your own instance of
the project in Google App Engine. To do that you will need to:

1.  Create a new application in App Engine. See the App Engine documentation
    for information.
2.  Fork this project into your own git repository.
3.  The only file you should need to edit in the project is
    "appengine-web.xml". You will need to change the "application" entry to
    be the ID of the application you created in App Engine.
4.  This project is built with Maven and the Maven App Engine plugins, so you
    will need Maven to build and deploy the project. To build the project
    locally you will need to have unzipped the App Engine SDK on your machine.
    Be sure that the "gae.home" property in the pom.xml file points to this
    location.
5.  To build and run locally just run "mvn gae:run" from the command line, and
    to deploy run "mvn gae:deploy".
6.  You can then hit your deployed instance by pointing your GAEWebPubSub
    javascript file to http://YOUR_APP_ID_HERE.appspot.com/connect .

## Javascript Reference ##

Request Parameters to the GAEWebPubSub javascript file:

1.  **topicKey** - Required. This is the unique key that identifies a particular
    topic. In order to prevent unauthorized users from entering the topic,
    this should be an unguessable value, like a random UUID or secure hash.
2.  **userName** - Required. The name of the user subscribing to the topic. Must
    be unique among all subscribers to a particular topic.
3.  **userKey** - Required. A globally unique identifier for the user. This
    value should also be unguessable, or else an imposter could connect to the
    topic pretending to be a different user.
4.  **topicLifetime** - Optional. Specifies the time, in minutes, that the topic
    should live for. If not specified, the default topic lifetime is 120
    minutes. The maximum lifespan is one day (this is the maximum lifespan of
    the underlying App Engine Channel objects).
5.  **validation** - Optional or Required, depending on configuration options.
    This parameter can be used to restrict access to your running instance of
    GAEWebPubSub. See the "Configuration Options" and "Security and Validation"
    sections below.

The gaewps topic object (accessed using gaewps.topics[YOUR_TOPIC_KEY]):

+   *sendMessage(messageText, [selfNotify, [returnReceiptCallback]])* - This
    method is used to send messages to the topic. This method returns the
    message number of the sent message (message numbers are monotonically
    increasing, scoped to a single subscriber). Note the maximum messageText
    length is 32K, as specified by the Channel API.
    1.  **messageText** - Required. The message to send to all other subscribers
        of the topic.
    2.  **selfNotify** - Optional, defaults to false. If set to true, then the
        user calling this method will ALSO be notified of the message in the
        onmessage handler.
    3.  **returnReceiptCallback** - Optional. If specified, this must be a
        function object that takes 2 parameters, messageNumber and
        senderName. If specified, when other users receive your message, they
        will call back to the server to send a "return receipt". This
        returnReceiptCallback function will thus be called every time a
        user receives your message. The messageNumber parameter will be
        the number of the message you sent (i.e. the value returned by the
        sendMessage method) and senderName will be the user name of the
        subscriber who received your message and is sending the return
        receipt.

+   *getSubscribers(callback)* - This asynchronous method can be used to get
    the user names of subscribers who have already connected to this topic.
    callback must be a function that takes one parameter, the array of user
    names of current subscribers.

+   *disconnect()* - You can manually call the disconnect method to disconnect
    the user from the topic. By default this method is called in the
    "onbeforeunload" event on the window.

+   *onmessage* - This event handler should be set to a callback function that
    takes three parameters: messageText, messageNumber, and senderName. This
    function will then be called every time a message is sent to the topic.

+   *onconnected* - This event handler can be set to a callback function that
    takes one parameter, the user name of a new subscriber to the topic. This
    function will then be called whenever a new subscriber joins this topic.

+   *ondisconnected* - This event handler callback function takes the same
    format as the onconnected callback. It is then called whenever a user
    leaves this topic.

## Configuration Options ##

If you are running GAEWebPubSub in your own App Engine instance, you can
access admin configuration options through the standard App Engine admin page.
There is a "Web Pub Sub Configuration Options" link added under the "Custom"
header in the left nav bar. You can also access the config page at
http://YOUR_APP_ID.appspot.com/admin/admin.jsp.

There are two configuration options that you can set:

+   **Validation Key** - If this option is set to a non-blank value then you
    must pass the "validation" parameter in with the call to load the
    GAEWebPubSub javascript file. This key is used to verify the validation
    parameter to ensure that only authorized clients can create and connect to
    topics. See the "Security and Validation" section below for an explanation
    of how this works and how to use it.
+   **Save Messages** - If set to true then all messages sent to any topic
    will be persisted to the App Engine Datastore using a "Message" entity
    type.

## Security and Validation ##

There are a number of security considerations to consider when deploying
GAEWebPubSub:

1.  **Ensuring unauthorized clients do not create new topics:** By default,
    GAEWebPubSub does not perform validation on incoming requests to ensure
    that only verified clients can create new topics. A design goal of
    GAEWebPubSub was that other applications should not have to be tightly
    integrated with it to use it, and thus GAEWebPubSub does not need any
    access to other apps' authentication systems. Instead, you can use the
    Validation Key config option and the validation request parameter passed
    in on the GAEWebPubSub javascript file to enable validation. Here's how
    it works:

    1.  Set the "Validation Key" configuration option to a random, unguessable
        value (such as a random UUID).
    2.  Any apps that load the GAEWebPubSub javascript file to connect to a
        topic must add a "validation" parameter, e.g

            &validation=1354685386958|76A9540817FA8A01C377557AEC9DF89E0E0CF9D26A0A656329DC96C16FDC9224

        The format of the validation parameter value is as follows:

        1.  Current timestamp (i.e. System.currentTimeMillis());
        2.  Pipe character
        3.  A SHA256 hash of timestamp + ValidationKey.

        Thus, only web applications that know your ValidationKey will be able
        to generate a correct validation parameter. You can use the code in the
        org.gaewebpubsub.util.ValidationUtils class to generate the parameter
        value.
    3.  All incoming requests are then checked using the
        org.gaewebpubsub.web.ValidationFilter servlet filter. If the
        validation parameter value does not match with the configured
        Validation Key option, then the request is rejected.

    Note you may wish to modify the ValidationFilter class to use a different
    validation algorithm. For example, you could set up a scheme where each
    application that accesses your instance uses a different private validation
    key.

2.  **Ensuring unauthorized users do not connect to topics they shouldn't be able to access:**
    It is important to ensure that the topicKeys you use are random and
    difficult to guess, like a secure hash value. Any other user who knows the
    topic key would be able to connect to the topic. Thus, you should be sure
    not to use something like an increasing integer as a topic key.
3.  **Ensuring users can not impersonate other users:** It is also important
    that your userKeys are large random values. userKeys are never passed to
    other subscribers of the topic - only user names are. Note, though, that
    user names are not validated, thus it would potentially be possible for
    a client to modify their user name to make it LOOK like they were a
    different user, at least to other subscribers. To prevent this, you could
    use a custom validation scheme similar to the Validation Key: you could
    require the userKey to be a secure hash of the userName and a private key.
    You would then need to modify the ValidationFilter to check for this
    requirement.
4.  **Ensuring unauthorized users can not snoop on messages sent to topics:**
    You will need to use SSL on your GAEWebPubSub instance if you want to
    encrypt your conversations. See <https://developers.google.com/appengine/docs/ssl>
    for information on enabling SSL in App Engine.
