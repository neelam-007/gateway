package com.l7tech.gateway.config.client.beans;

import com.l7tech.gateway.config.client.ConfigurationException;

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

    private final File propertiesFile;
    
    public PropertiesConfigurationBeanProvider( File file ) {
        this.propertiesFile = file;
    }
    
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
            ConfigurationBean configBean = new ConfigurationBean();
            configBean.setConfigName( property );
            configBean.setConfigValue( properties.getProperty(property) );
            configuration.add(configBean);
        }
        
        return configuration;
    }

    public void storeConfiguration(Collection<ConfigurationBean> configuration) throws ConfigurationException {
        Properties properties = new Properties();
        
        for ( ConfigurationBean configBean : configuration ) {
            properties.put( configBean.getConfigName(), configBean.getConfigValue() );
        }

        try {
            properties.store( new FileOutputStream(propertiesFile), "Configuration Properties" );
        } catch (IOException ioe) {
            throw new ConfigurationException("Unable to load configuration from file '"+propertiesFile.getAbsolutePath()+"'.", ioe);
        }
    }

}
