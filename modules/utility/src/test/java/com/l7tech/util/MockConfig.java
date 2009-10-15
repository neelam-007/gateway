package com.l7tech.util;

import java.util.Properties;

/**
 * Mock implementation of Config that is backed by properties.
 */
public class MockConfig implements Config {

    private final Properties properties;

    public MockConfig( final Properties properties ) {
        this.properties = properties;        
    }

    @Override
    public String getProperty( final String propertyName, final String defaultValue) {
        return properties.getProperty(propertyName, defaultValue);
    }

    @Override
    public int getIntProperty(final String propertyName, final int defaultValue) {
        return Integer.parseInt( getProperty( propertyName, Integer.toString(defaultValue) ) );
    }

    @Override
    public long getLongProperty(final String propertyName, final long defaultValue) {
        return Long.parseLong( getProperty( propertyName, Long.toString(defaultValue) ) );
    }

    @Override
    public boolean getBooleanProperty(final String propertyName, final boolean defaultValue) {
        return Boolean.valueOf( getProperty( propertyName, Boolean.toString(defaultValue) ) );
    }

    @Override
    public long getTimeUnitProperty( final String propertyName, final long defaultValue ) {
        return TimeUnit.parse( getProperty( propertyName, Long.toString(defaultValue)), TimeUnit.MILLIS);
    }
}
