package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 18, 2003
 *
 * Implements common functionality shared between UserManager and GroupManager
 *
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
        java.util.Hashtable env = new java.util.Hashtable();
        env.put( "java.naming.ldap.version", "3" );
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        Object temp = config.getProperty( LdapConfigSettings.LDAP_HOST_URL );
        if ( temp != null ) env.put(Context.PROVIDER_URL, temp );

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

    //protected DirContext anonymousContext = null;
    protected IdentityProviderConfig config;
}
