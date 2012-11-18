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
package org.gaewebpubsub.services.appengine;

import com.google.appengine.api.datastore.*;
import org.gaewebpubsub.services.Defaults;
import org.gaewebpubsub.services.TopicAccessException;
import org.gaewebpubsub.services.TopicPersister;

import java.util.*;

/**
 * The DatastoreTopicPersister uses the App Engine DatastoreService to persist data.
 *
 * TODO - check that right exceptions are thrown if necessary (like when topic doesn't exist)
 */
public class DatastoreTopicPersister implements TopicPersister {
    //Entity Kinds
    private static final String TOPIC_KIND = "Topic";
    private static final String SUBSCRIBER_KIND = "Subscriber";
    private static final String MESSAGE_KIND = "Message";

    //Column names
    private static final String MESSAGE_NUM_PROP = "messageNum";
    private static final String EXPIRATION_TIME_PROP = "expirationTime";

    private static final String USER_NAME_PROP = "userName";
    private static final String CHANNEL_TOKEN_PROP = "channelToken";
    private static final String SELF_NOTIFY_PROP = "selfNotify";

    private static final String TOPIC_KEY_PROP = "topicKey";
    private static final String USER_KEY_PROP = "userKey";
    private static final String MESSAGE_TEXT_PROP = "messageText";

    protected boolean shouldSaveMessages = false;

    public boolean createTopic(final String topicKey, final int topicLifetime) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;

        checkSaveMessagesFlag();

        try {
            return new TransactionRunner<Boolean>(getDatastore()) {
                protected Boolean txnBlock() {
                    //create the topic if it doesn't already exist
                    try {
                        datastore.get(topicEntityKey(topicKey));
                        return false; //topic already existed, we didn't create a new one
                    } catch (EntityNotFoundException enfe) {
                        //then need to create topic
                        Entity topicEntity = new Entity(TOPIC_KIND, topicKey);
                        topicEntity.setProperty(MESSAGE_NUM_PROP, 0);
                        topicEntity.setProperty(EXPIRATION_TIME_PROP, System.currentTimeMillis() + (topicLifetime * 60000L));
                        datastore.put(topicEntity);
                        return true;
                    }
                }
            }.run();
        } catch (Exception e) {
            throw new TopicAccessException("Error saving topic " + topicKey, e);
        }
    }

    public boolean addUserToTopic(final String topicKey,
                                  final String userKey,
                                  final String userName,
                                  final String channelToken,
                                  final boolean selfNotify) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert channelToken != null && channelToken.trim().length() > 0;

        try {
            return new TransactionRunner<Boolean>(getDatastore()) {
                protected Boolean txnBlock() {
                    try {
                        datastore.get(subscriberEntityKey(topicKey, userKey));
                        return false; //subscriber already existed
                    } catch (EntityNotFoundException enfe) {
                        //then need to create subscriber
                        Entity subscriberEntity = new Entity(SUBSCRIBER_KIND, userKey, topicEntityKey(topicKey));
                        subscriberEntity.setProperty(USER_NAME_PROP, userName);
                        subscriberEntity.setProperty(CHANNEL_TOKEN_PROP, channelToken);
                        subscriberEntity.setProperty(SELF_NOTIFY_PROP, selfNotify);
                        datastore.put(subscriberEntity);
                        return true;
                    }
                }
            }.run();
        } catch (Exception e) {
            throw new TopicAccessException("Error adding user " + userKey + " to topic " + topicKey, e);
        }
    }

    public List<TopicPersister.SubscriberData> loadTopicSubscribers(String topicKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;

        List<SubscriberData> retVal = new ArrayList<SubscriberData>();

        //query for subscriber objects by topicKey
        DatastoreService datastore = getDatastore();
        try {
            PreparedQuery preparedQuery = datastore.prepare(new Query(SUBSCRIBER_KIND, topicEntityKey(topicKey)));
            for (Entity subscriberResult : preparedQuery.asIterable()) {
                retVal.add(new SubscriberData(subscriberResult.getKey().getName(),
                                              (String) subscriberResult.getProperty(USER_NAME_PROP),
                                              (String) subscriberResult.getProperty(CHANNEL_TOKEN_PROP),
                                              (Boolean) subscriberResult.getProperty(SELF_NOTIFY_PROP)));
            }
            return retVal;
        } catch (Exception e) {
            throw new TopicAccessException("Error loading subscribers for topic " + topicKey, e);
        }
    }

    public int persistMessage(final String topicKey, final String userKey, final String userName, final String message)
            throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert message != null;

        //TODO - this is definitely a bottleneck - maybe make monotonically increasing message nums optional
        try {
            return new TransactionRunner<Integer>(getDatastore()) {
                protected Integer txnBlock(Transaction txn) {
                    try {
                        Key topicEntityKey = topicEntityKey(topicKey);
                        Entity topicEntity = datastore.get(topicEntityKey);
                        int currentMessageNum = ((Number) topicEntity.getProperty(MESSAGE_NUM_PROP)).intValue();
                        //update the message num and save
                        currentMessageNum++;
                        topicEntity.setProperty(MESSAGE_NUM_PROP, currentMessageNum);
                        datastore.put(topicEntity);
                        txn.commit();

                        if (shouldSaveMessages) {
                            Entity messageEntity = new Entity(MESSAGE_KIND);
                            messageEntity.setProperty(TOPIC_KEY_PROP, topicEntityKey);
                            messageEntity.setProperty(USER_KEY_PROP, userKey);
                            messageEntity.setProperty(USER_NAME_PROP, userName);
                            messageEntity.setProperty(MESSAGE_TEXT_PROP, new Text(message));
                            messageEntity.setProperty(MESSAGE_NUM_PROP, currentMessageNum);
                            datastore.put(messageEntity);
                        }

                        return currentMessageNum;
                    } catch (EntityNotFoundException enfe) {
                        throw new TopicAccessException("Topic " + topicKey + " not found");
                    }
                }
            }.run();
        } catch (Exception e) {
            throw new TopicAccessException("Error persisting message to topic " + topicKey, e);
        }
    }

    public boolean unsubscribeUserFromTopic(final String topicKey, final String userKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        try {
            return new TransactionRunner<Boolean>(getDatastore()) {
                protected Boolean txnBlock() {
                    try {
                        Entity subscriberEntity = datastore.get(subscriberEntityKey(topicKey, userKey));
                        datastore.delete(subscriberEntity.getKey());
                        return true;
                    } catch (EntityNotFoundException enfe) {
                        return false;
                    }
                }
            }.run();
        } catch (Exception e) {
            throw new TopicAccessException("Error unsubscribing user " + userKey + " from topic " + topicKey, e);
        }
    }

    protected DatastoreService getDatastore() {
        return DatastoreServiceFactory.getDatastoreService();
    }

    protected Key topicEntityKey(String topicKey) {
        return KeyFactory.createKey(TOPIC_KIND, topicKey);
    }

    protected Key subscriberEntityKey(String topicKey, String userKey) {
        return KeyFactory.createKey(topicEntityKey(topicKey), SUBSCRIBER_KIND, userKey);
    }

    /**
     * We only check the save messages config flag when a new topic is created.
     */
    protected void checkSaveMessagesFlag() {
        this.shouldSaveMessages = Boolean.parseBoolean(Defaults.newConfigManager().get("saveMessages", "false"));
    }
}