package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Implements common functionality shared between UserManager and GroupManager.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 18, 2003
 */
public class LdapManager {
    protected LdapManager(IdentityProviderConfig config) {
        this.config = config;
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                return valuesWereLookingFor.get(0);
        }
        return null;
    }

    protected DirContext getBrowseContext() throws NamingException {
        return getBrowseContext( this.config );
    }

    protected static DirContext getBrowseContext( IdentityProviderConfig config ) throws NamingException {
        Hashtable env = new Hashtable();
        env.put( "java.naming.ldap.version", "3" );
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        Object temp = config.getProperty( LdapConfigSettings.LDAP_HOST_URL );
        if ( temp != null ) env.put(Context.PROVIDER_URL, temp );
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.timeout", "30000" );

        String dn = config.getProperty( LdapConfigSettings.LDAP_BIND_DN );
        if ( dn != null && dn.length() > 0 ) {
            String pass = config.getProperty( LdapConfigSettings.LDAP_BIND_PASS );
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, dn );
            env.put( Context.SECURITY_CREDENTIALS, pass );
        }

        // Create the initial directory context.
        return new InitialDirContext(env);
    }

    protected IdentityProviderConfig config;
}
