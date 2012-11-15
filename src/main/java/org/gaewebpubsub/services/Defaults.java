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

import org.gaewebpubsub.services.appengine.CachingConfigManager;
import org.gaewebpubsub.services.appengine.ChannelApiTopicManager;
import org.gaewebpubsub.services.appengine.DatastoreTopicPersister;

/**
 * Returns the default service implementations that will be used by the app. If you wish to swap out any implementations
 * you should only need to change this class, and potentially the connectTemplate.js file if you change the topic
 * manager implementation.
 */
public class Defaults {
    public static ConfigManager newConfigManager() {
        return new CachingConfigManager();
    }

    public static TopicPersister newTopicPersister() {
        return new DatastoreTopicPersister();
    }

    public static TopicManager newTopicManager() {
        ChannelApiTopicManager retVal = new ChannelApiTopicManager();
        retVal.setTopicPersister(newTopicPersister());
        return retVal;
    }
}
