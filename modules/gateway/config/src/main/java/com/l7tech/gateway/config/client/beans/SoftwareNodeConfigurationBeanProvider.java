package com.l7tech.gateway.config.client.beans;

import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.config.client.beans.PropertiesConfigurationBeanProvider;
import com.l7tech.config.client.ConfigurationException;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays;
import java.io.File;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;

/**
 *
 */
public class SoftwareNodeConfigurationBeanProvider extends NodeConfigurationBeanProvider {

    //- PUBLIC

    public SoftwareNodeConfigurationBeanProvider( final File propertiesFile ) {
        super(new NodeManagementApiFactory());
        propertiesConfig = new PropertiesConfigurationBeanProvider( propertiesFile, "node." );
    }

    @Override
    public Object getInitialValue( final String configName ) {
        Object value;

        if ( "java.path".equals(configName) ) {
            value = System.getProperty("java.home");
        } else {
            value =  super.getInitialValue(configName);
        }

        return value;
    }

    @Override
    public void storeConfiguration( final Collection<ConfigurationBean> configuration ) throws ConfigurationException {
        // validate
        validateConfig( configuration );

        // store common beans
        super.storeConfiguration(configuration);

        // store software specific
        propertiesConfig.storeConfiguration(propertiesSubset(configuration));
    }

    @Override
    public Collection<ConfigurationBean> loadConfiguration() throws ConfigurationException {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        // load common beans
        configuration.addAll(super.loadConfiguration());

        // load software specific
        configuration.addAll(propertiesSubset(propertiesConfig.loadConfiguration()));

        return configuration;
    }

    //- PRIVATE

    private PropertiesConfigurationBeanProvider propertiesConfig;
    private static final Set<String> propertiesConfigNames = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("java.path", "java.heap", "initial.admin.listenaddr", "initial.admin.listenport")));

    private Collection<ConfigurationBean> propertiesSubset( final Collection<ConfigurationBean> configurationBeans ) {
        List<ConfigurationBean> configuration = new ArrayList<ConfigurationBean>();

        for ( ConfigurationBean configBean : configurationBeans ) {
            if ( propertiesConfigNames.contains( configBean.getConfigName() ) ) {
                configuration.add( configBean );
            }
        }

        return configuration;
    }

    private void validateConfig( final Collection<ConfigurationBean> configurationBeans ) throws ConfigurationException {
        for ( ConfigurationBean configBean : configurationBeans ) {
            if ( configBean.getConfigName().equals("java.path") ) {
                validateJdkPath((String)configBean.getConfigValue());
            } else if ( configBean.getConfigName().equals("initial.admin.listenaddr") ) {
                validateIpAddress((String)configBean.getConfigValue());
            }
        }
    }

    private void validateJdkPath( final String path ) throws ConfigurationException {
        if ( path != null ) {
            File javaPath = new File(new File(path), "bin/java");
            if ( !javaPath.isFile() && javaPath.canExecute() ) {
                throw new ConfigurationException("Invalid JDK path '"+path+"'.");
            }
        } else {
            throw new ConfigurationException("JDK path is required.");
        }
    }

    private void validateIpAddress( final String ipAddress ) throws ConfigurationException {
        if ( ipAddress != null ) {
            boolean isOk = false;
            try {
                isOk = "*".equals(ipAddress) || NetworkInterface.getByInetAddress( InetAddress.getByName(ipAddress) ) != null;
            } catch (UnknownHostException uhe) {
                // not ok
            } catch (SocketException e) {
                // not ok
            }

            if ( !isOk ) {
                throw new ConfigurationException("Invalid Listener IP addresss '"+ipAddress+"'.");
            }
        }
    }
}
