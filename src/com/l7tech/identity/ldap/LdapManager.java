package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;

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

    protected DirContext getAnonymousContext() throws NamingException {
        //if (anonymousContext == null) createAnonymousContext();
        //return anonymousContext;

        java.util.Hashtable env = new java.util.Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getProperty(LdapConfigSettings.LDAP_HOST_URL));

        // Create the initial directory context. So anonymous bind for search
        return new InitialDirContext(env);
    }

    //protected DirContext anonymousContext = null;
    protected IdentityProviderConfig config;

    static final String DESCRIPTION_ATTR = "description";
    static final String NAME_ATTR_NAME = "cn";
    static final String GROUPOBJ_MEMBER_ATTR = "memberUid";
    static final String USER_OBJCLASS = "inetOrgPerson";
    static final String LOGIN_ATTR_NAME = "uid";
}
