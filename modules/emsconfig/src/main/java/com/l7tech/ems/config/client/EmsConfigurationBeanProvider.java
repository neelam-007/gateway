package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.PropertiesConfigurationBeanProvider;
import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * 
 */
public class EmsConfigurationBeanProvider extends PropertiesConfigurationBeanProvider {

    public EmsConfigurationBeanProvider() {
        super(getConfigurationFile());
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EmsConfigurationBeanProvider.class.getName() );

    private static final String PROP_CONFIG_FILE = "com.l7tech.ems.config.file";
    private static final String DEFAULT_CONFIG_FILE = "/opt/SecureSpan/EnterpriseManager/var/init.properties";

    private static File getConfigurationFile() {
        File config = new File( SyspropUtil.getString( PROP_CONFIG_FILE, DEFAULT_CONFIG_FILE ) );
        if ( !config.isFile() ) {
            File parent = config.getParentFile();
            if ( parent.isDirectory() ) {
                try {
                    config.createNewFile();
                } catch ( IOException ioe ) {
                    logger.log( Level.INFO, "Error creating configuration file '"+config.getAbsolutePath()+"'.", ioe );
                }
            }
        }
        return config;
    }
}
