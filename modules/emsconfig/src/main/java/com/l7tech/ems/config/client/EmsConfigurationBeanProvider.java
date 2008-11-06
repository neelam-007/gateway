package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.PropertiesConfigurationBeanProvider;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.HexUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

/**
 * 
 */
public class EmsConfigurationBeanProvider extends PropertiesConfigurationBeanProvider {

    //- PUBLIC

    public EmsConfigurationBeanProvider() {
        super( getConfigurationFile(), "em." );
    }

    //- PROTECTED

    @Override
    @SuppressWarnings({"UnusedDeclaration"})
    protected Object onLoad( final String name, final String value) {
        Object persistValue;

        if ( CONFIG_ADMIN_PASS.equals(name) ) {
            persistValue = null;
        } else {
            persistValue = super.onLoad(name, value);
        }

        return persistValue;
    }

    @Override
    protected Object onPersist( final String name, final Object value, final Collection<ConfigurationBean> beans ) {
        Object persistValue;

        if ( CONFIG_ADMIN_PASS.equals(name) && value != null ) {
            // encode the password before storing to the properties file
            Object userObj = this.getConfigurationBeanValue( CONFIG_ADMIN_USER, beans );
            persistValue = HexUtils.encodePasswd( userObj==null ? "" : userObj.toString(), value.toString(), "L7SSGDigestRealm" );
        } else {
            persistValue = super.onPersist( name, value, beans );
        }

        return persistValue;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EmsConfigurationBeanProvider.class.getName() );

    private static final String CONFIG_ADMIN_USER = "admin.user";
    private static final String CONFIG_ADMIN_PASS = "admin.pass";
    
    private static final String PROP_CONFIG_FILE = "com.l7tech.ems.config.file";
    private static final String DEFAULT_CONFIG_FILE = "/opt/SecureSpan/EnterpriseManager/var/emconfig.properties";

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
