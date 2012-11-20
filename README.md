# GAE Web Pub/Sub #

GAEWebPubSub is a publish/subscribe is a very thin layer built around the
Channel API of Google App Engine. It provides a simple way to create
topics that multiple clients can connect to for sharing messages (think
chat rooms or an instant messenger).

The rationale for building GAEWebPubSub is that I wanted to take advantage
of the Channel API, but at the same time I was worried about being tied to
App Engine for my entire application. The benefit of GAEWebPubSub is that it
can be completely stand alone without having knowledge of any other parts of
the rest of your application.

## Quick Start ##

To start using GAEWebPubSub in your apps, you will need to:

1. Create a private fork and deploy to your own application in App Engine.
2. In your web page that needs to make publish/subscribe calls you will need
   to include a dynamic javascript file.
3. Once you've included the javascript file you will be able to publish to
   your topic with sendMessage() javascript calls, and you will be notified
   of incoming messages by specifying and onmessage() callback function.