/*
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
 */
package org.gaewebpubsub.services;

/**
 * The topic manager handles creation and message passing of the topic.
 */
public interface TopicManager {

    /**
     * Connects a user to a topic. This method either creates a new topic if that topic doesn't yet exist, or if it
     * DOES already exist the topic is loaded. If the user is not in the topic, the user is added to the topic and
     * "connect" notifications are sent to the other subscribers of the topic.
     *
     * @param topicKey      The unique key of the topic - this will be generated and managed by the caller.
     *                      May not be null or empty string.
     * @param userKey       The unique identifier of the user - this can be a userID or name, for example.
     *                      May not be null or empty string.
     * @param userName      The name of the user, which will be used when notifying other subscribers.
     *                      May not be null or empty string.
     * @param topicLifetime The time, in minutes, that the topic will exist for. Max of 24 * 60 minutes. If 0 or
     *                      negative, the default lifetime of 2 hours will be used.
     * @return A private channel token for the user in this topic. This will need to be send down to the javascript
     *         on the client.
     * @throws TopicAccessException Thrown if the topic couldn't be loaded or created.
     * @throws SubscriberNotificationException
     *                              Thrown if any existing subscribers couldn't be notified of the new user.
     */
    String connectUserToTopic(String topicKey, String userKey, String userName, int topicLifetime)
            throws TopicAccessException, SubscriberNotificationException;

    /**
     * Sends a message to all other subscribers of this topic.
     *
     * @param topicKey The unique key of the topic that the user already is connected to. May not be null or empty.
     * @param userKey  The unique identifier of the user. May not be null or empty.
     * @param message  The message, max of 32K. May not be null.
     * @throws TopicAccessException Thrown if the topic couldn't be loaded or the user isn't part of this topic.
     * @throws SubscriberNotificationException
     *                              Thrown if the message couldn't be sent to all subscribers.
     */
    void sendMessage(String topicKey, String userKey, String message)
            throws TopicAccessException, SubscriberNotificationException;

    /**
     * Disconnects a user from a topic. After disconnection the user may no longer send messages to the topic.
     *
     * @param topicKey The unique key of the topic. May not be null or empty.
     * @param userKey  The unique identifier of the user. May not be null or empty.
     */
    void disconnectUser(String topicKey, String userKey);
}
