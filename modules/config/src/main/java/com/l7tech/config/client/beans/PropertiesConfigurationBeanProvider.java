package com.l7tech.config.client.beans;

import com.l7tech.config.client.ConfigurationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Properties configuration bean provider that uses Properties files for storage.
 *
 * @author steve
 */
public class PropertiesConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public PropertiesConfigurationBeanProvider( final File file ) {
        this( file, "" );
    }

    public PropertiesConfigurationBeanProvider( final File file, final String prefix ) {
        this.propertiesFile = file;
        this.propertyPrefix = prefix;
    }

    @Override
    public boolean isValid() {
        return propertiesFile.isFile() && propertiesFile.canWrite();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        Properties properties = new Properties();
        try {
            properties.load( new FileInputStream(propertiesFile) );
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load configuration from file '"+propertiesFile.getAbsolutePath()+"'.", ioe);
        }
        
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();
        
        for ( String property : Collections.list((Enumeration<String>)properties.propertyNames()) ) {
            String name = unprefix(property);
            if ( name != null ) {
                ConfigurationBean configBean = new ConfigurationBean();
                configBean.setConfigName( name );
                configBean.setConfigValue( onLoad(name, properties.getProperty(property)) );
                configuration.add(configBean);
            }
        }
        
        return configuration;
    }

    @Override
    public void storeConfiguration( final Collection<ConfigurationBean> configuration ) throws ConfigurationException {
        Properties properties = new Properties();
        
        for ( ConfigurationBean configBean : configuration ) {
            String toPersist = onPersist(configBean.getConfigName(), configBean.getConfigValue(), configuration);
            if ( toPersist != null ) {
                properties.setProperty( prefix(configBean.getConfigName()), toPersist );
            } else {
                properties.remove( prefix(configBean.getConfigName()) );
            }
        }

        try {
            properties.store( new FileOutputStream(propertiesFile), "Configuration Properties" );
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load configuration from file '"+propertiesFile.getAbsolutePath()+"'.", ioe);
        }
    }

    //- PROTECTED

    /**
     * Invoked on load of a property from property file.
     *
     * <p>The default implementation returns the given value. Override to customize loading of a property.</p>
     *
     * @param name The name of the (unprefixed) property / configuration bean
     * @param value The value from the property file
     * @return The configuration value
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected Object onLoad( final String name, final String value ) {
        return value;
    }

    /**
     * Invoked on persist of a configuration bean to the property file.
     *
     * <p>The default implementation returns the given value. Override to customize persistence of a property.</p>
     *
     * @param name The name of the (unprefixed) property / configuration bean
     * @param value The value from the configuration bean
     * @return The property value
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected String onPersist( final String name, final Object value, final Collection<ConfigurationBean> beans ) {
        return value instanceof String ? 
                (String) value :
                value==null ? null : value.toString();
    }

    /**
     * Get a configuration bean from the given collection.
     *
     * @param name The bean name
     * @param beans The collection of beans.
     * @return The bean or null if not found
     */
    protected ConfigurationBean getConfigurationBean( final String name, final Collection<ConfigurationBean> beans ) {
        ConfigurationBean bean = null;

        for ( ConfigurationBean configBean : beans ) {
            if ( name.equals(configBean.getConfigName()) ) {
                bean = configBean;
                break;
            }
        }

        return bean;
    }

    /**
     * Get a configuration bean value from the given collection.
     *
     * @param name The bean name
     * @param beans The collection of beans.
     * @return The bean configuration value or null if the bean was not found or the value was null
     */
    protected Object getConfigurationBeanValue( final String name, final Collection<ConfigurationBean> beans ) {
        Object value = null;

        ConfigurationBean bean = getConfigurationBean( name, beans );
        if ( bean != null ) {
            value = bean.getConfigValue();
        }

        return value;
    }

    //- PRIVATE

    private final File propertiesFile;
    private final String propertyPrefix;

    private String unprefix( final String name ) {
        String cleanName = null;

        if  ( name.startsWith( propertyPrefix ) ) {
            cleanName = name.substring(propertyPrefix.length());
        }

        return cleanName;
    }

    private String prefix( final String name ) {
        return propertyPrefix + name;
    }
}
