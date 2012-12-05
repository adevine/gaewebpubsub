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

import java.util.*;

/**
 * Obviously, this class wouldn't work in a distributed environment. Just used as a simple startup/test class.
 */
public class InMemoryTopicPersister implements TopicPersister {
    private Map<String, List<SubscriberData>> subscribersByTopicKey = new HashMap<String, List<SubscriberData>>();

    public synchronized boolean createTopic(String topicKey, int topicLifetime) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;

        if (subscribersByTopicKey.containsKey(topicKey)) {
            return false;
        }

        subscribersByTopicKey.put(topicKey, new ArrayList<SubscriberData>());
        return true;
    }

    public synchronized boolean addUserToTopic(String topicKey, String userKey, String userName, String channelToken)
            throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert channelToken != null && channelToken.trim().length() > 0;

        //ensure the topic exists
        if (!subscribersByTopicKey.containsKey(topicKey)) {
            throw new TopicAccessException("Could not find topic " + topicKey);
        }

        List<SubscriberData> subscribers = loadTopicSubscribers(topicKey);

        boolean alreadyAdded = false;
        for (SubscriberData subscriber : subscribers) {
            if (userKey.equals(subscriber.userKey)) {
                alreadyAdded = true;
                break;
            }
        }

        if (!alreadyAdded) {
            subscribers.add(new SubscriberData(userKey, userName, channelToken, 0));
        }

        return !alreadyAdded;
    }

    public synchronized List<SubscriberData> loadTopicSubscribers(String topicKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;

        List<SubscriberData> retVal = subscribersByTopicKey.get(topicKey);
        if (retVal == null) {
            retVal = new ArrayList<SubscriberData>();
        }
        return retVal;
    }

    public synchronized void persistMessage(String topicKey,
                                            String userKey,
                                            String userName,
                                            String message,
                                            int messageNum) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;
        assert message != null;

        //we don't need to do any persistence, just check that the topic exists
        if (!subscribersByTopicKey.containsKey(topicKey)) {
            throw new TopicAccessException("Topic " + topicKey + " not found");
        }
    }

    public synchronized boolean unsubscribeUserFromTopic(String topicKey, String userKey) throws TopicAccessException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        boolean wasDeleted = false;
        List<SubscriberData> subscribers = loadTopicSubscribers(topicKey);
        for (Iterator<SubscriberData> iterator = subscribers.iterator(); iterator.hasNext(); ) {
            SubscriberData subscriberData = iterator.next();
            if (userKey.equals(subscriberData.userKey)) {
                iterator.remove();
                wasDeleted = true;
                break;
            }
        }

        if (subscribers.isEmpty()) {
            //then delete this whole topic - may be no-op if topic never existed
            subscribersByTopicKey.remove(topicKey);
        }

        return wasDeleted;
    }
}
