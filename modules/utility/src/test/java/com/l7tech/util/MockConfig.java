package com.l7tech.util;

import com.l7tech.util.Functions.Unary;

import java.util.Map;
import java.util.Properties;

/**
 * Mock implementation of Config that is backed by properties.
 */
public class MockConfig implements Config {

    private final Unary<String,String> propertyGetter;

    public MockConfig( final Properties properties ) {
        this.propertyGetter = new Unary<String,String>(){
            @Override
            public String call( final String propertyName ) {
                return properties.getProperty( propertyName );
            }
        };
    }

    public MockConfig( final Map<String,String> properties ) {
        this.propertyGetter = new Unary<String,String>(){
            @Override
            public String call( final String propertyName ) {
                return properties.get( propertyName );
            }
        };
    }

    @Override
    public String getProperty( final String propertyName ) {
        return propertyGetter.call( propertyName );
    }

    @Override
    public String getProperty( final String propertyName, final String defaultValue) {
        final String value = propertyGetter.call( propertyName );
        return value == null ? defaultValue : value;
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
