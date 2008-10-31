package com.l7tech.server.util;

import org.springframework.beans.factory.FactoryBean;

import java.util.Properties;

/**
 * Factory that extracts a named value from a Properties object.
 */
public class PropertiesValueFactoryBean implements FactoryBean {

    private final Properties properties;
    private final String key;

    public PropertiesValueFactoryBean( final Properties properties,
                                       final String key ) {
        this.properties = properties;
        this.key = key;
    }

    public Object getObject() throws Exception {
        return properties.getProperty( key, "" );
    }

    public Class getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }
}
