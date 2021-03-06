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

import java.util.List;

/**
 * TopicPersister is responsible for interacting with persistent storage (e.g. the Datastore) to save information about
 * an ongoing topic.
 */
public interface TopicPersister {
    /**
     * Creates a topic if one doesn't already exist with the given topicKey.
     *
     * @param topicKey      The unique key of the topic. May not be null or empty.
     * @param topicLifetime The time, in minutes, that the topic should live.
     * @return true if the topic was newly created, false if it already exists.
     * @throws TopicAccessException Thrown if the topic could not be created.
     */
    boolean createTopic(String topicKey, int topicLifetime) throws TopicAccessException;

    /**
     * Adds the specified user as a subscriber to the given topic if they are not already a subscriber.
     *
     * @param topicKey     The unique key of the topic. May not be null or empty.
     * @param userKey      The unique identifier of the user. May not be null or empty.
     * @param userName     The display name of the user. May not be null or empty.
     * @param channelToken The channel token that the user will use to subscribe to the topic on the client side
     *                     (e.g. the Channel APIs channel token). May not be null or empty.
     * @return true if the user was newly added, false if they were already subscribed.
     * @throws TopicAccessException Thrown if the topic doesn't exist or couldn't be loaded.
     */
    boolean addUserToTopic(String topicKey, String userKey, String userName, String channelToken)
            throws TopicAccessException;

    /**
     * Loads all of the subscribers for a given topic.
     *
     * @param topicKey The unique key of the topic. May not be null or empty.
     * @return The list of subscribers, or an empty list of the topic doesn't exist
     * @throws TopicAccessException Thrown if there was an error attempting to load the topic.
     */
    List<SubscriberData> loadTopicSubscribers(String topicKey) throws TopicAccessException;

    /**
     * "Persists" the message to disk. Note that an implementing class may decide not to persist the full message.
     *
     * @param topicKey   The unique key of the topic that the user already is connected to. May not be null or empty.
     * @param userKey    The unique identifier of the user. May not be null or empty.
     * @param userName   The unique name of the user. May not be null or empty.
     * @param message    The message, max of 32K. May not be null.
     * @param messageNum The user-specific number for this message (clients should send monotonically increasing
     *                   message numbers for each user's message)
     * @throws TopicAccessException Thrown if the topic does not exist.
     */
    void persistMessage(String topicKey, String userKey, String userName, String message, int messageNum)
            throws TopicAccessException;

    /**
     * Unsubscribes a user from the specified topic. Once a topic has no more subscribers it may be deleted.
     *
     * @param topicKey The unique key of the topic. May not be null or empty.
     * @param userKey  The unique identifier of the user. May not be null or empty.
     * @return true if the user was unsubscribed, false if the topic couldn't be found or if the user wasn't a
     *         subscriber of the topic.
     * @throws TopicAccessException Thrown if there was an error loading the topic.
     */
    boolean unsubscribeUserFromTopic(String topicKey, String userKey) throws TopicAccessException;
}
