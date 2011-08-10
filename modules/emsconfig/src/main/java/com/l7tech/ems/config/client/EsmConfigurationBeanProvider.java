package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.PropertiesConfigurationBeanProvider;
import com.l7tech.config.client.beans.ConfigurationBean;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

/**
 * 
 */
public class EsmConfigurationBeanProvider extends PropertiesConfigurationBeanProvider {

    //- PUBLIC

    public EsmConfigurationBeanProvider() {
        super( getConfigurationFile(), "em.", false );
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
    protected String onPersist( final String name, final Object value, final Collection<ConfigurationBean> beans ) {
        String persistValue;

        if ( CONFIG_ADMIN_PASS.equals(name) ) {
            if ( value != null ) {
                // encode the password before storing to the properties file
                persistValue = hashPassword( value.toString() );
            } else {
                persistValue = SKIP; // preserve existing hashed value
            }
        } else {
            persistValue = super.onPersist( name, value, beans );
        }

        return persistValue;
    }

    //- PACKAGE

    static String hashPassword( final String password ) {
        final Sha512CryptPasswordHasher cryptHasher = new Sha512CryptPasswordHasher();
        return cryptHasher.hashPassword( password.getBytes( Charsets.UTF8 ) );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EsmConfigurationBeanProvider.class.getName() );

    private static final String CONFIG_ADMIN_PASS = "admin.pass";
    
    private static final String PROP_CONFIG_FILE = "com.l7tech.ems.config.file";
    private static final String DEFAULT_CONFIG_FILE = "/opt/SecureSpan/EnterpriseManager/var/emconfig.properties";

    private static File getConfigurationFile() {
        File config = new File( ConfigFactory.getProperty( PROP_CONFIG_FILE, DEFAULT_CONFIG_FILE ) );
        if ( !config.isFile() ) {
            File parent = config.getParentFile();
            if ( parent.isDirectory() ) {
                try {
                    if (!config.createNewFile()) {
                        logger.log( Level.INFO, "Unable to create configuration file '"+config.getAbsolutePath()+"' (file already present)." );
                    }
                } catch ( IOException ioe ) {
                    logger.log( Level.INFO, "Error creating configuration file '"+config.getAbsolutePath()+"'.", ioe );
                }
            }
        }
        return config;
    }
}
