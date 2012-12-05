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
 * This struct-like class is used to keep information about a subscriber to a topic.
 */
public class SubscriberData {
    public String userKey;
    public String userName;
    public String channelToken;
    public int messageCount;

    public SubscriberData() { }

    public SubscriberData(String userKey, String userName, String channelToken, int messageCount) {
        this.userKey = userKey;
        this.userName = userName;
        this.channelToken = channelToken;
        this.messageCount = messageCount;
    }
}
