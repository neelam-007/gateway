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

    protected Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                return valuesWereLookingFor.get(0);
        }
        return null;
    }

    protected DirContext getAnonymousContext() throws NamingException {
        //if (anonymousContext == null) createAnonymousContext();
        //return anonymousContext;

        java.util.Hashtable env = new java.util.Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getLdapHostURL());

        // Create the initial directory context. So anonymous bind for search
        return new InitialDirContext(env);
    }

    protected synchronized void createAnonymousContext() throws NamingException {

        // Create the initial directory context. So anonymous bind for search
        // Identify service provider to use
        //java.util.Hashtable env = new java.util.Hashtable();
        //env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        //env.put(Context.PROVIDER_URL, config.getLdapHostURL());

        // Create the initial directory context. So anonymous bind for search
        //anonymousContext = new InitialDirContext(env);
    }

    //protected DirContext anonymousContext = null;
    protected LdapIdentityProviderConfig config;

    protected static final String DESCRIPTION_ATTR = "description";
    protected static final String NAME_ATTR_NAME = "cn";
    protected static final String GROUPOBJ_MEMBER_ATTR = "memberUid";
    protected static final String USER_OBJCLASS = "inetOrgPerson";
    protected static final String LOGIN_ATTR_NAME = "uid";
}
