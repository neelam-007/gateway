/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.test;

import com.l7tech.console.security.ClientCredentialManager;
import com.l7tech.common.VersionException;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceManager;
import com.l7tech.common.util.Locator;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.PasswordAuthentication;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author alex
 * @version $Revision$
 */
public class SsgAdminClient {
    protected IdentityProvider _internalIdentityProvider, _ldapIdentityProvider;
    protected UserManager _internalUserManager, _ldapUserManager;
    protected GroupManager _internalGroupManager, _ldapGroupManager;
    protected ServiceManager _serviceManager;

    protected Properties _properties;

    static final String PROP_LDAP_PROVIDER_OID  = "com.l7tech.test.SsgAdminClient.LDAPProviderOid";
    static final String PROP_SSGADMIN_LOGIN     = "com.l7tech.test.SsgAdminClient.SSGAdminLogin";
    static final String PROP_SSGADMIN_PASS      = "com.l7tech.test.SsgAdminClient.SSGAdminPass";
    static final String PROP_LOCATOR_PROPERTIES = "com.l7tech.common.locator.properties";

    static final String DEFAULT_LDAP_PROVIDER_OID = "4915203";
    static final String DEFAULT_SSGADMIN_LOGIN    = "ssgadmin";
    static final String DEFAULT_SSGADMIN_PASS     = "ssgadminpasswd";

    static final String LOCATOR_PROPERTIES      = "/com/l7tech/console/resources/services.properties";

    public SsgAdminClient() throws LoginException, IOException, FindException {
        this( null );
    }

    public SsgAdminClient( String[] args ) throws LoginException, IOException, FindException {
        String propsPath = null;
        String arg;
        for ( int i = 0; i < args.length; i++ ) {
            arg = args[i];
            if ( "-p".equals(arg) ) {
                if ( i < args.length-1 )
                    propsPath = args[++i];
                else
                    throw new IllegalArgumentException( "The -p switch requires a properties path!" );
            }
        }

        InputStream propertiesStream = propsPath == null ? null : new FileInputStream( propsPath );

        String locatorProperties = System.getProperty( PROP_LOCATOR_PROPERTIES );
        if ( locatorProperties == null ) {
            System.err.println( "Loading default Locator properties from " + LOCATOR_PROPERTIES );
            System.setProperty( PROP_LOCATOR_PROPERTIES, LOCATOR_PROPERTIES );
        }

        Locator locator = Locator.getDefault();

        Properties defaultProps = new Properties();
        defaultProps.put( PROP_LDAP_PROVIDER_OID, DEFAULT_LDAP_PROVIDER_OID );
        defaultProps.put( PROP_SSGADMIN_LOGIN, DEFAULT_SSGADMIN_LOGIN );
        defaultProps.put( PROP_SSGADMIN_PASS, DEFAULT_SSGADMIN_PASS );

        if ( propertiesStream == null ) {
            _properties = defaultProps;
            System.err.println( "Using default SsgAdminClient properties" );
        } else {
            _properties = new Properties( defaultProps );
            System.err.println( "Loading properties" );
            _properties.load( propertiesStream );
        }

        long ldapProviderOid;
        try {
            String sldapProviderOid = (String)_properties.get( PROP_LDAP_PROVIDER_OID );
            ldapProviderOid = new Long( sldapProviderOid ).longValue();
        } catch ( NumberFormatException nfe ) {
            ldapProviderOid = -1;
        }

        String login = _properties.getProperty( PROP_SSGADMIN_LOGIN );
        String pass = _properties.getProperty( PROP_SSGADMIN_PASS );

        ClientCredentialManager clientCredentialManager = (ClientCredentialManager)locator.lookup( ClientCredentialManager.class );
        PasswordAuthentication auth = new PasswordAuthentication( login, pass.toCharArray() );
        try {
            clientCredentialManager.login( auth );
        } catch (VersionException e) {
            e.printStackTrace();
            return;
        }

        IdentityProviderConfigManager providerConfigManager = new IdProvConfManagerClient();
        Iterator providers = providerConfigManager.findAllIdentityProviders().iterator();

        IdentityProvider provider;
        IdentityProviderConfig config;
        IdentityProviderType type;

        while ( providers.hasNext() ) {
            provider = (IdentityProvider)providers.next();
            config = provider.getConfig();
            type = config.type();

            if ( type == IdentityProviderType.INTERNAL ) {
                _internalIdentityProvider = provider;
                _internalUserManager = provider.getUserManager();
                _internalGroupManager = provider.getGroupManager();
            } else if ( type == IdentityProviderType.LDAP && config.getOid() == ldapProviderOid ) {
                _ldapIdentityProvider = provider;
                _ldapUserManager = provider.getUserManager();
                _ldapGroupManager = provider.getGroupManager();
            }
        }

        _serviceManager = (ServiceManager)locator.lookup( ServiceManager.class );
    }

    /**
     * Override this method to do something useful.
     * @param args the command-line arguments.  args[0] could be a properties filename!
     * @throws Exception
     */
    protected void run( String[] args ) throws Exception {
    }

    public static void main( String[] args ) throws Exception {
        SsgAdminClient client = new SsgAdminClient( args );
        System.err.println( "Found Internal IdentityProvider " + client._internalIdentityProvider );
        System.err.println( "Found Internal UserManager      " + client._internalUserManager );
        System.err.println( "Found Internal GroupManager     " + client._internalGroupManager );
        System.err.println( "Found LDAP IdentityProvider     " + client._ldapIdentityProvider );
        System.err.println( "Found LDAP UserManager          " + client._ldapUserManager );
        System.err.println( "Found LDAP GroupManager         " + client._ldapGroupManager );
        System.err.println( "Found ServiceManager            " + client._serviceManager );
        client.run( args );
        System.exit(0);
    }

}
