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
import org.gaewebpubsub.services.*;
import org.gaewebpubsub.util.Escapes;
import org.gaewebpubsub.util.SecureHash;

import java.util.*;

/**
 */
public class ChannelApiTopicManager implements TopicManager {
    protected TopicPersister topicPersister;

    public void setTopicPersister(TopicPersister topicPersister) { this.topicPersister = topicPersister; }

    public SubscriberData connectUserToTopic(String topicKey, String userKey, String userName, int topicLifetime)
            throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;

        topicPersister.createTopic(topicKey, topicLifetime);
        List<SubscriberData> currentSubscribers = topicPersister.loadTopicSubscribers(topicKey);

        SubscriberData thisUsersData = getUserFromSubscriberList(userKey, currentSubscribers);
        if (thisUsersData == null) {
            String channelToken = getChannelService().createChannel(getClientId(topicKey, userKey), topicLifetime);
            topicPersister.addUserToTopic(topicKey, userKey, userName, channelToken);
            thisUsersData = new SubscriberData(userKey, userName, channelToken, 0);
        }

        //notify OTHER users that this user was added
        notifySubscribers(topicKey, userKey, currentSubscribers, false /* never self notify on connect */,
                          toJson("eventType", "connect", "sender", userName));

        return thisUsersData;
    }

    public void sendMessage(String topicKey,
                            String userKey,
                            String message,
                            int messageNumber,
                            boolean selfNotify,
                            boolean needsReceipt) throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert message != null;

        List<SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        SubscriberData sendersData = getUserFromSubscriberList(userKey, subscribers);
        if (sendersData != null) {
            topicPersister.persistMessage(topicKey, userKey, sendersData.userName, message, messageNumber);
            notifySubscribers(topicKey, userKey, subscribers, selfNotify,
                              toJson("eventType", "message",
                                     "sender", sendersData.userName,
                                     "messageNumber", Integer.toString(messageNumber),
                                     "message", message,
                                     "needsReceipt", Boolean.toString(needsReceipt)));
        }
    }

    public void sendReturnReceipt(String topicKey, String userKey, String originalSenderName, int messageNumber)
            throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert originalSenderName != null && originalSenderName.trim().length() > 0;

        List<SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        SubscriberData subscriberSendingReturnReceipt = getUserFromSubscriberList(userKey, subscribers);
        SubscriberData originalSendersData = getUserFromSubscriberListByName(originalSenderName, subscribers);
        if (subscriberSendingReturnReceipt != null && originalSendersData != null) {
            //only notify the original sender of the message
            notifySubscribers(topicKey,
                              userKey,
                              Collections.singletonList(originalSendersData),
                              false /*never self notify on return receipt*/,
                              toJson("eventType", "receipt",
                                     "sender", subscriberSendingReturnReceipt.userName,
                                     "messageNumber", Integer.toString(messageNumber)));
        }
    }

    public List<String> getCurrentSubscribers(String topicKey) throws TopicAccessException {
        List<SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        List<String> retVal = new ArrayList<String>(subscribers.size());
        for (SubscriberData subscriberData : subscribers) {
            retVal.add(subscriberData.userName);
        }
        return retVal;
    }

    public void disconnectUser(String topicKey, String userKey) throws SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        List<SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        SubscriberData sendersData = getUserFromSubscriberList(userKey, subscribers);
        if (sendersData != null) {
            topicPersister.unsubscribeUserFromTopic(topicKey, userKey);
            notifySubscribers(topicKey, userKey, subscribers, false, /* no need to self notify on disconnect */
                              toJson("eventType", "disconnect", "sender", sendersData.userName));
        }
    }

    protected ChannelService getChannelService() {
        return ChannelServiceFactory.getChannelService();
    }

    protected SubscriberData getUserFromSubscriberList(String userKey, List<SubscriberData> currentSubscribers) {
        for (SubscriberData currentSubscriber : currentSubscribers) {
            if (userKey.equals(currentSubscriber.userKey)) {
                return currentSubscriber;
            }
        }
        return null;
    }

    protected SubscriberData getUserFromSubscriberListByName(String userName, List<SubscriberData> currentSubscribers) {
        for (SubscriberData currentSubscriber : currentSubscribers) {
            if (userName.equals(currentSubscriber.userName)) {
                return currentSubscriber;
            }
        }
        return null;
    }

    protected void notifySubscribers(String topicKey,
                                     String userKey,
                                     List<SubscriberData> subscribers,
                                     boolean selfNotify,
                                     String jsonMessage) throws SubscriberNotificationException {
        Map<String, Exception> sendMessageErrorsBySubscriberName = new HashMap<String, Exception>();

        for (SubscriberData subscriber : subscribers) {
            if (selfNotify || !userKey.equals(subscriber.userKey)) {
                String subscribersClientId = getClientId(topicKey, subscriber.userKey);
                try {
                    getChannelService().sendMessage(new ChannelMessage(subscribersClientId, jsonMessage));
                } catch (Exception e) {
                    sendMessageErrorsBySubscriberName.put(subscriber.userName, e);
                }
            }
        }

        if (!sendMessageErrorsBySubscriberName.isEmpty()) {
            //then not all messages were sent successfully
            StringBuilder msg = new StringBuilder("Error notifying subscribers: ");
            for (Map.Entry<String, Exception> userNameAndException : sendMessageErrorsBySubscriberName.entrySet()) {
                msg.append("\n\t")
                        .append(userNameAndException.getKey()).append(": ")
                        .append(userNameAndException.getValue());
            }
            throw new SubscriberNotificationException(msg.toString());
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
