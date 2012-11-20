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

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import org.gaewebpubsub.services.SubscriberNotificationException;
import org.gaewebpubsub.services.TopicAccessException;
import org.gaewebpubsub.services.TopicManager;
import org.gaewebpubsub.services.TopicPersister;
import org.gaewebpubsub.util.Escapes;
import org.gaewebpubsub.util.SecureHash;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ChannelApiTopicManager implements TopicManager {
    protected TopicPersister topicPersister;

    public void setTopicPersister(TopicPersister topicPersister) { this.topicPersister = topicPersister; }

    public String connectUserToTopic(String topicKey,
                                     String userKey,
                                     String userName,
                                     int topicLifetime,
                                     boolean selfNotify) throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;

        topicPersister.createTopic(topicKey, topicLifetime);
        List<TopicPersister.SubscriberData> currentSubscribers = topicPersister.loadTopicSubscribers(topicKey);

        //TODO - the way this work means a user will NOT get a new token if they connect again. Is this wrong?
        TopicPersister.SubscriberData thisUsersData = getUserFromSubscriberList(userKey, currentSubscribers);
        if (thisUsersData == null) {
            String channelToken = getChannelService().createChannel(getClientId(topicKey, userKey), topicLifetime);
            topicPersister.addUserToTopic(topicKey, userKey, userName, channelToken, selfNotify);
            thisUsersData = new TopicPersister.SubscriberData(userKey, userName, channelToken, selfNotify);
        }

        //notify OTHER users that this user was added
        notifySubscribers(topicKey, userKey, currentSubscribers, false /* never self notify on connect */,
                          toJson("eventType", "connect", "sender", userName));

        return thisUsersData.channelToken;
    }

    public void sendMessage(String topicKey, String userKey, String message)
            throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert message != null;

        List<TopicPersister.SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        TopicPersister.SubscriberData sendersData = getUserFromSubscriberList(userKey, subscribers);
        if (sendersData != null) {
            int messageNum = topicPersister.persistMessage(topicKey, userKey, sendersData.userName, message);
            notifySubscribers(topicKey, userKey, subscribers, sendersData.selfNotify,
                              toJson("eventType", "message",
                                     "sender", sendersData.userName,
                                     "messageNumber", Integer.toString(messageNum),
                                     "message", message));
        }
    }

    public List<String> getCurrentSubscribers(String topicKey) throws TopicAccessException {
        List<TopicPersister.SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        List<String> retVal = new ArrayList<String>(subscribers.size());
        for (TopicPersister.SubscriberData subscriberData : subscribers) {
            retVal.add(subscriberData.userName);
        }
        return retVal;
    }

    public void disconnectUser(String topicKey, String userKey) throws SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        List<TopicPersister.SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        TopicPersister.SubscriberData sendersData = getUserFromSubscriberList(userKey, subscribers);
        if (sendersData != null) {
            notifySubscribers(topicKey, userKey, subscribers, false, /* no need to self notify on disconnect */
                              toJson("eventType", "disconnect", "sender", sendersData.userName));
            topicPersister.unsubscribeUserFromTopic(topicKey, userKey);
        }
    }

    protected ChannelService getChannelService() {
        return ChannelServiceFactory.getChannelService();
    }

    protected TopicPersister.SubscriberData getUserFromSubscriberList(
            String userKey, List<TopicPersister.SubscriberData> currentSubscribers) {
        for (TopicPersister.SubscriberData currentSubscriber : currentSubscribers) {
            if (userKey.equals(currentSubscriber.userKey)) {
                return currentSubscriber;
            }
        }
        return null;
    }

    protected void notifySubscribers(String topicKey,
                                     String userKey,
                                     List<TopicPersister.SubscriberData> subscribers,
                                     boolean selfNotify,
                                     String jsonMessage) throws SubscriberNotificationException {
        for (TopicPersister.SubscriberData subscriber : subscribers) {
            if (selfNotify || !userKey.equals(subscriber.userKey)) {
                String subscribersClientId = getClientId(topicKey, subscriber.userKey);
                try {
                    getChannelService().sendMessage(new ChannelMessage(subscribersClientId, jsonMessage));
                } catch (Exception e) {
                    //TODO - handle
                }
            }
        }
    }

    protected String getClientId(String topicKey, String userKey) {
        return SecureHash.hash(topicKey + "~~" + userKey);
    }

    protected String toJson(String... keysAndValues) {
        StringBuilder retVal = new StringBuilder();
        retVal.append("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];

            retVal.append('"').append(Escapes.escapeJavaScriptString(key)).append('"').append(":")
                    .append('"').append(Escapes.escapeJavaScriptString(value)).append('"');
            if (i < keysAndValues.length - 2) {
                retVal.append(",");
            }
        }
        retVal.append("}");
        return retVal.toString();
    }
}
