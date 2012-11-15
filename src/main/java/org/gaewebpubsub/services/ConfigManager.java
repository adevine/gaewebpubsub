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
