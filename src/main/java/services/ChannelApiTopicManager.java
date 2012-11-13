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
package services;

import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;

import java.util.List;
import java.util.logging.Logger;

/**
 */
public class ChannelApiTopicManager implements TopicManager {
    private static final Logger log = Logger.getLogger(ChannelApiTopicManager.class.getName());

    protected ChannelService channelService;

    protected TopicPersister topicPersister;

    public void setChannelService(ChannelService channelService) { this.channelService = channelService; }

    public void setTopicPersister(TopicPersister topicPersister) { this.topicPersister = topicPersister; }

    public String connectUserToTopic(String topicKey, String userKey, String userName, int topicLifetime)
            throws TopicAccessException, SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;
        assert userName != null && userName.trim().length() > 0;

        topicPersister.createTopic(topicKey);
        List<TopicPersister.SubscriberData> currentSubscribers = topicPersister.loadTopicSubscribers(topicKey);

        TopicPersister.SubscriberData thisUsersData = getUserFromSubscriberList(userKey, currentSubscribers);
        if (thisUsersData == null) {
            String channelToken = channelService.createChannel(getClientId(topicKey, userKey), topicLifetime);
            topicPersister.addUserToTopic(topicKey, userKey, userName, channelToken);
            thisUsersData = new TopicPersister.SubscriberData(userKey, userName, channelToken);
        }

        //notify OTHER users that this user was added
        notifyOtherSubscribers(topicKey, userKey, currentSubscribers,
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
            notifyOtherSubscribers(topicKey, userKey, subscribers,
                                   toJson("eventType", "message",
                                          "sender", sendersData.userName,
                                          "messageNumber", Integer.toString(messageNum),
                                          "message", message));
        }
    }

    public void disconnectUser(String topicKey, String userKey) throws SubscriberNotificationException {
        assert topicKey != null && topicKey.trim().length() > 0;
        assert userKey != null && userKey.trim().length() > 0;

        List<TopicPersister.SubscriberData> subscribers = topicPersister.loadTopicSubscribers(topicKey);
        TopicPersister.SubscriberData sendersData = getUserFromSubscriberList(userKey, subscribers);
        if (sendersData != null) {
            notifyOtherSubscribers(topicKey, userKey, subscribers,
                                   toJson("eventType", "disconnect", "sender", sendersData.userName));
            topicPersister.unsubscribeUserFromTopic(topicKey, userKey);
        }
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

    protected void notifyOtherSubscribers(String topicKey,
                                          String userKey,
                                          List<TopicPersister.SubscriberData> subscribers,
                                          String jsonMessage) throws SubscriberNotificationException {
        for (TopicPersister.SubscriberData subscriber : subscribers) {
            if (!userKey.equals(subscriber.userKey)) {
                String subscribersClientId = getClientId(topicKey, subscriber.userKey);
                try {
                    channelService.sendMessage(new ChannelMessage(subscribersClientId, jsonMessage));
                } catch (Exception e) {
                    //TODO - handle
                }
            }
        }
    }

    protected String getClientId(String topicKey, String userKey) {
        return topicKey + "~~" + userKey;
    }

    protected String toJson(String... keysAndValues) {
        StringBuilder retVal = new StringBuilder();
        retVal.append("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];

            retVal.append(escapeJsonString(key)).append(":").append(escapeJsonString(value));
            if (i < keysAndValues.length) {
                retVal.append(",");
            }
        }
        retVal.append("}");
        return retVal.toString();
    }

    protected String escapeJsonString(String value) {
        //todo - double check this is right
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }
}
