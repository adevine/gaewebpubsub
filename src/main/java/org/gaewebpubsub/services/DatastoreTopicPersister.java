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

import com.google.appengine.api.datastore.*;

import java.util.*;

/**
 * The DatastoreTopicPersister uses the App Engine DatastoreService to persist data.
 *
 * TODO - check topicKey, userKey and userName length limits
 * TODO - check that right exceptions are thrown if necessary (like when topic doesn't exist)
 */
public class DatastoreTopicPersister implements TopicPersister {
    //Entity Kinds
    private static final String TOPIC_KIND = "Topic";
    private static final String SUBSCRIBER_KIND = "Subscriber";

    //Column names
    public static final String MESSAGE_NUM_PROP = "messageNum";
    public static final String USER_NAME_PROP = "userName";
    public static final String CHANNEL_TOKEN_PROP = "channelToken";
    public static final String SELF_NOTIFY_PROP = "selfNotify";

    public boolean createTopic(String topicKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;

        //create the topic if it doesn't already exist
        DatastoreService datastore = getDatastore();
        Transaction txn = datastore.beginTransaction();
        try {
            datastore.get(topicEntityKey(topicKey));
            return false; //topic already existed, we didn't create a new one
        } catch (EntityNotFoundException enfe) {
            //then need to create topic
            try {
                Entity topicEntity = new Entity(TOPIC_KIND, topicKey);
                topicEntity.setProperty(MESSAGE_NUM_PROP, 0);
                //TODO - do I want to persist the topiclifespan?
                datastore.put(topicEntity);
                return true;
            } catch (Exception e) {
                throw new TopicAccessException("Error saving topic " + topicKey, e);
            }
        } finally {
            commit(txn);
        }
    }

    public boolean addUserToTopic(String topicKey,
                                  String userKey,
                                  String userName,
                                  String channelToken,
                                  boolean selfNotify) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert channelToken != null && channelToken.trim().length() > 0;

        DatastoreService datastore = getDatastore();

        Transaction txn = datastore.beginTransaction();
        try {
            datastore.get(subscriberEntityKey(topicKey, userKey));
            return false; //subscriber already existed
        } catch (EntityNotFoundException enfe) {
            //then need to create subscriber
            try {
                Entity subscriberEntity = new Entity(SUBSCRIBER_KIND, userKey, topicEntityKey(topicKey));
                subscriberEntity.setProperty(USER_NAME_PROP, userName);
                subscriberEntity.setProperty(CHANNEL_TOKEN_PROP, channelToken);
                subscriberEntity.setProperty(SELF_NOTIFY_PROP, selfNotify);
                datastore.put(subscriberEntity);
                return true;
            } catch (Exception e) {
                throw new TopicAccessException("Error adding user " + userKey + " to topic " + topicKey, e);
            }
        } finally {
            commit(txn);
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

    public int persistMessage(String topicKey, String userKey, String userName, String message)
            throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert message != null;

        //TODO - for now, not persisting message, just updating the message num - make saving full message optional

        //TODO - this is definitely a bottleneck - maybe make in order message nums optional
        DatastoreService datastore = getDatastore();
        Transaction txn = datastore.beginTransaction();
        try {
            Entity topicEntity = datastore.get(topicEntityKey(topicKey));
            int currentMessageNum = ((Number) topicEntity.getProperty(MESSAGE_NUM_PROP)).intValue();
            //update the message num and save
            topicEntity.setProperty(MESSAGE_NUM_PROP, currentMessageNum + 1);
            datastore.put(topicEntity);
            return currentMessageNum + 1;
        } catch (EntityNotFoundException enfe) {
            throw new TopicAccessException("Topic " + topicKey + " not found");
        } catch (Exception e) {
            throw new TopicAccessException("Error persisting message to topic " + topicKey, e);
        } finally {
            commit(txn);
        }
    }

    public boolean unsubscribeUserFromTopic(String topicKey, String userKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        DatastoreService datastore = getDatastore();
        Transaction txn = datastore.beginTransaction();
        try {
            Entity subscriberEntity = datastore.get(subscriberEntityKey(topicKey, userKey));
            datastore.delete(subscriberEntity.getKey());
            return true;
        } catch (EntityNotFoundException enfe) {
            return false;
        } catch (Exception e) {
            throw new TopicAccessException("Error unsubscribing user " + userKey + " from topic " + topicKey, e);
        } finally {
            commit(txn);
        }

        //TODO - possibly delete Topic object from datastore if no more subscribers (or mark completed)?
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

    protected void commit(Transaction txn) throws TopicAccessException {
        try {
            if (txn != null) {
                txn.commit();
            }
        } catch (Exception e) {
            throw new TopicAccessException("Could not save data", e);
        }
    }
}