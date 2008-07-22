package com.l7tech.gateway.common.spring.factory.config;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Factory bean for system property values.
 *
 * @author Steve Jones
 */
public class SystemPropertyFactoryBean extends AbstractFactoryBean {

    //- PUBLIC

    /**
     *
     */
    public SystemPropertyFactoryBean() {
    }

    /**
     * Set the name of the system property to access.
     *
     * @param propertyName the name e.g. 'com.l7tech.server.myProperty'
     */
    public void setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Set the default value for the property.
     *
     * @param propertyDefault the default value to use
     */
    public void setPropertyDefault(String propertyDefault) {
        this.propertyDefault = propertyDefault;
    }

    /**
     * This factory creates String objects.
     *
     * @return String.class
     */
    public Class getObjectType() {
        return String.class;
    }

    //- PROTECTED

    /**
     *
     */
    protected Object createInstance() throws Exception {
        String defaultValue = propertyDefault;
        if (propertyDefault == null) {
            defaultValue = "";    
        }

        try {
            return System.getProperty(propertyName, defaultValue);
        }
        catch(SecurityException se) {
            return defaultValue;
        }
    }

    //- PRIVATE

    private String propertyName;
    private String propertyDefault;
}
