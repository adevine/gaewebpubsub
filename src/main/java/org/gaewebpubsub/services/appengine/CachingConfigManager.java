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
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import org.gaewebpubsub.services.ConfigManager;

/**
 * This implementation of the ConfigManager stores its data in the Datastore and uses the MemcacheService for fast
 * access. Note with this implementation property values are limited to 500 chars.
 */
public class CachingConfigManager implements ConfigManager {
    /**
     * The Datastore kind that is used to save config data
     */
    private static final String CONFIG_KIND = "Config";
    /**
     * The config values are saved under a "value" property in the datastore entities.
     */
    private static final String VALUE_PROP = "value";

    public String get(String propertyName, String defaultValue) {
        if (defaultValue == null) {
            throw new IllegalArgumentException("defaultValue may not be null");
        }
        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();

        Object retVal = memcacheService.get(propertyName);
        if (retVal == null) {
            //then it's not in the cache, so try to load it from Datastore, and if not there save and return default
            retVal = getFromDatastore(propertyName, defaultValue);
            memcacheService.put(propertyName, retVal);
        }

        return (String) retVal;
    }

    public void set(String propertyName, String value) {
        if (value == null) {
            throw new IllegalArgumentException("value may not be null");
        }

        setInDatastore(propertyName, value);
        MemcacheServiceFactory.getMemcacheService().put(propertyName, value);
    }

    protected String getFromDatastore(String propertyName, String defaultValue) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastoreService.beginTransaction();
        try {
            Key key = KeyFactory.createKey(CONFIG_KIND, propertyName);
            Entity configEntity = datastoreService.get(key);
            String retVal = (String) configEntity.getProperty(VALUE_PROP);
            if (retVal == null) {
                //shouldn't happen, but could if entity is missing VALUE_PROP for some reason. If so, just put it again
                throw new EntityNotFoundException(key);
            }
            return retVal;
        } catch (EntityNotFoundException enfe) {
            Entity configEntity = new Entity(CONFIG_KIND, propertyName);
            configEntity.setProperty(VALUE_PROP, defaultValue);
            datastoreService.put(configEntity);
            txn.commit();
            return defaultValue;
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }

    protected void setInDatastore(String propertyName, String value) {
        DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
        Transaction txn = datastoreService.beginTransaction();
        try {
            Entity configEntity = new Entity(CONFIG_KIND, propertyName);
            configEntity.setProperty(VALUE_PROP, value);
            datastoreService.put(configEntity);
            txn.commit();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }
    }
}
