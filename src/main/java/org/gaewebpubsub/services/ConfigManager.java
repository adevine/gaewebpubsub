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
 * ConfigManager is used to save application config data. This allow config data to be changed dynamically at runtime
 * (e.g. through an admin page).
 *
 * Note that config data is considered to be application-wide, so different instances should update the same underlying
 * data store.
 */
public interface ConfigManager {

    /**
     * Gets a config property
     * @param propertyName The name of the property to get. May NOT be null.
     * @param defaultValue This value will be returned if this property hasn't been previously set. May NOT be null.
     * @return The property value, or defaultValue if the property wasn't previously set.
     */
    String get(String propertyName, String defaultValue);

    /**
     * Sets the property to the specified value.
     * @param propertyName The name of the property to set. May NOT be null.
     * @param value The value to set the property to. May NOT be null.
     */
    void set(String propertyName, String value);
}
