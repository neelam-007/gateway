package com.l7tech.identity.ldap;

import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;
import javax.naming.Context;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 18, 2003
 *
 * Implements common functionality shared between UserManager and GroupManager
 *
 */
public class LdapManager {

    protected LdapManager(LdapIdentityProviderConfig config) {
        this.config = config;
    }

    protected Object extractOneAttributeValue(Attributes attributes, String attrName) {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
            try {
                return valuesWereLookingFor.get(0);
            } catch (NamingException e) {
                e.printStackTrace(System.err);
            }
        }
        return null;
    }

    protected DirContext getAnonymousContext() throws NamingException {
        if (anonymousContext == null) createAnonymousContext();
        return anonymousContext;
    }

    protected synchronized void createAnonymousContext() throws NamingException {

        // Create the initial directory context. So anonymous bind for search
        // Identify service provider to use
        java.util.Hashtable env = new java.util.Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getLdapHostURL());

        // Create the initial directory context. So anonymous bind for search
        anonymousContext = new InitialDirContext(env);
    }

    protected DirContext anonymousContext = null;
    protected LdapIdentityProviderConfig config;
}
