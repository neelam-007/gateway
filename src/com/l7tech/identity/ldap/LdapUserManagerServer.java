package com.l7tech.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapUserManagerServer implements UserManager {

    public LdapUserManagerServer(LdapIdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * Get a User object for the given dn
     *
     * @param dn the distinguished name to look for in the directory
     * @throws FindException
     */
    public User findByPrimaryKey(String dn) throws FindException {
        try {
            Attributes attributes = getAnonymousContext().getAttributes(dn);
            LdapUser out = new LdapUser();
            out.setDN(dn);
            Object tmp = extractOneAttributeValue(attributes, EMAIL_ATTR_NAME);
            if (tmp != null) out.setEmail(tmp.toString());
            tmp = extractOneAttributeValue(attributes, FIRSTNAME_ATTR_NAME);
            if (tmp != null) out.setFirstName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LASTNAME_ATTR_NAME);
            if (tmp != null) out.setLastName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LOGIN_ATTR_NAME);
            if (tmp != null) out.setLogin(tmp.toString());
            tmp = extractOneAttributeValue(attributes, NAME_ATTR_NAME);
            if (tmp != null) out.setName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, PASSWD_ATTR_NAME);
            if (tmp != null) out.setPassword(tmp.toString());
            return out;
        } catch (NamingException e) {
            e.printStackTrace(System.err);
            throw new FindException(e.getMessage(), e);
        }
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void delete(User user) throws DeleteException {
        throw new DeleteException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public long save(User user) throws SaveException {
        throw new SaveException("Not supported in LdapUserManagerServer");
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void update(User user) throws UpdateException {
        throw new UpdateException("Not supported in LdapUserManagerServer");
    }

    public Collection findAllHeaders() throws FindException {
        return null;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        return null;
    }

    public Collection findAll() throws FindException {
        return null;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        return null;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    public static void main(String[] args) throws Exception {
        LdapIdentityProviderConfig config = new LdapIdentityProviderConfig();
        config.setLdapHostURL("ldap://localhost:3899/");
        LdapUserManagerServer me = new LdapUserManagerServer(config);
        User usr = me.findByPrimaryKey("cn=flascelles,dc=layer7-tech,dc=com");
        System.out.println(usr);

        usr = me.findByPrimaryKey("cn=Alex Cruise,dc=layer7-tech,dc=com");
        System.out.println(usr);

        usr = me.findByPrimaryKey("cn=dsirota,dc=layer7-tech,dc=com");
        System.out.println(usr);

        usr = me.findByPrimaryKey("cn=stranger,dc=layer7-tech,dc=com");
        System.out.println(usr);
    }

    private Object extractOneAttributeValue(Attributes attributes, String attrName) {
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

    private DirContext getAnonymousContext() throws NamingException {
        if (anonymousContext == null) createAnonymousContext();
        return anonymousContext;
    }

    private synchronized void createAnonymousContext() throws NamingException {

        // Create the initial directory context. So anonymous bind for search
        // Identify service provider to use
        java.util.Hashtable env = new java.util.Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        if (config.getSearchBase() != null && config.getSearchBase().length() > 0) {
            env.put(Context.PROVIDER_URL, config.getLdapHostURL() + config.getSearchBase());
        }
        else env.put(Context.PROVIDER_URL, config.getLdapHostURL());

        // Create the initial directory context. So anonymous bind for search
        anonymousContext = new InitialDirContext(env);
    }

    private DirContext anonymousContext = null;
    private LdapIdentityProviderConfig config;

    // mappings for attribute names
    // these may become properties of the LdapIdentityProviderConfig
    private static final String LOGIN_ATTR_NAME = "uid";
    private static final String EMAIL_ATTR_NAME = "mail";
    private static final String FIRSTNAME_ATTR_NAME = "givenName";
    private static final String LASTNAME_ATTR_NAME = "sn";
    private static final String NAME_ATTR_NAME = "cn";
    private static final String PASSWD_ATTR_NAME = "userPassword";
}
