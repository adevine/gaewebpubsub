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
