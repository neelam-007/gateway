package com.l7tech.config.client.beans;

import com.l7tech.config.client.ConfigurationException;
import com.l7tech.util.ResourceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Properties configuration bean provider that uses Properties files for storage.
 *
 * @author steve
 */
public class PropertiesConfigurationBeanProvider implements ConfigurationBeanProvider {

    //- PUBLIC

    public PropertiesConfigurationBeanProvider( final File file ) {
        this( file, "", true );
    }

    public PropertiesConfigurationBeanProvider( final File file,
                                                final String prefix,
                                                final boolean preserveExtraProperties ) {
        this.propertiesFile = file;
        this.propertyPrefix = prefix;
        this.preserveExtraProperties = preserveExtraProperties;
    }

    @Override
    public boolean isValid() {
        return propertiesFile.isFile() && propertiesFile.canWrite();
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        Properties properties = loadPropertiesFromFile();
        for ( String property : Collections.list((Enumeration<String>)properties.propertyNames()) ) {
            String name = unprefix(property);
            if ( name != null ) {
                ConfigurationBean configBean = new ConfigurationBean();
                configBean.setConfigName( name );
                configBean.setConfigValue( onLoad(name, properties.getProperty(property)) );
                configuration.add(configBean);
            } else {
                    logger.log(Level.WARNING, "Ignoring unknown property: " + property);
            }
        }
        
        return configuration;
    }

    @SuppressWarnings({ "StringEquality" })
    @Override
    public void storeConfiguration( final Collection<ConfigurationBean> configuration ) throws ConfigurationException {
        final Properties properties = loadPropertiesFromFile();

        final Set<String> propertiesToKeep = new HashSet<String>();
        for ( final ConfigurationBean configBean : configuration ) {
            final String toPersist = onPersist(configBean.getConfigName(), configBean.getConfigValue(), configuration);
            if ( toPersist == SKIP ) { // check string identity
                propertiesToKeep.add( prefix(configBean.getConfigName()) );
            } else if ( toPersist != null ) {
                propertiesToKeep.add( prefix(configBean.getConfigName()) );
                properties.setProperty( prefix(configBean.getConfigName()), toPersist );
            } else {
                // will be removed
            }
        }

        if ( preserveExtraProperties ) {
            for ( final String property : properties.stringPropertyNames() ) {
                String propertyName = unprefix(property);
                if(propertyName != null) {
                    final ConfigurationBean bean = getConfigurationBean( unprefix( property ), configuration );
                    if ( bean == null ) {
                        propertiesToKeep.add( property );
                    }
                } else {
                    propertiesToKeep.add(property);
                    logger.log(Level.WARNING, "Ignoring unknown property: " + property);
                }
            }
        }

        for ( final String propertyName : properties.stringPropertyNames() ) {
            if ( !propertiesToKeep.contains( propertyName ) ) {
                properties.remove( propertyName );
            }
        }

        OutputStream out = null;
        try {
            out = new FileOutputStream(propertiesFile);
            properties.store( out, "Configuration Properties" );
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load configuration from file '"+propertiesFile.getAbsolutePath()+"'.", ioe);
        } finally {
            ResourceUtils.closeQuietly( out );
        }
    }

    //- PROTECTED

    /**
     * Special property value for use with onPersist.
     *
     * @see #onPersist(String, Object, Collection)
     */
    protected static final String SKIP = "skip-property";

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
     * <p>Return the special value @{link SKIP} to skip persistence for the item.</p>
     *
     * @param name The name of the (unprefixed) property / configuration bean
     * @param value The value from the configuration bean
     * @return The property value
     * @see #SKIP
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

    /**
     * Load configuration from properties file.
     *
     * @return The loaded properties values.
     * @throws ConfigurationException If an error occurs.
     */
    protected Properties loadPropertiesFromFile() throws ConfigurationException {
        Properties properties = new Properties();

        InputStream in = null;
        if ( propertiesFile.isFile() ) {
            try {
                in = new FileInputStream(propertiesFile);
                properties.load( in );
            } catch (IOException ioe) {
                throw new ConfigurationException("Unable to load configuration from file '"+propertiesFile.getAbsolutePath()+"'.", ioe);
            } finally {
                ResourceUtils.closeQuietly( in );
            }
        }

        return properties;
    }

    //- PRIVATE

    private final File propertiesFile;
    private final String propertyPrefix;
    private final boolean preserveExtraProperties;
    private static final Logger logger = Logger.getLogger(PropertiesConfigurationBeanProvider.class.getName());

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
