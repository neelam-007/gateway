package com.l7tech.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Context;
import javax.naming.AuthenticationException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 * This manager class lists users in a ldap directory given a LdapIdentityProviderConfig object
 * This manager does not support save, update or delete.
 *
 * This version assumes users are registered in inetOrgPerson objects. Login is "uid"
 * attribute, password is "userPassword" attribute, name is "cn", first name is "givenName",
 * last name is "sn".
 */
public class LdapUserManagerServer extends LdapManager implements UserManager {

    public LdapUserManagerServer(IdentityProviderConfig config) {
        super(config);
    }

    /**
     * Get a User object for the given dn
     *
     * @param dn the distinguished name to look for in the directory
     * @throws FindException
     */
    public User findByPrimaryKey(String dn) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        try {
            DirContext context = getAnonymousContext();
            Attributes attributes = context.getAttributes(dn);
            User out = new User();
            out.setProviderId(config.getOid());
            out.setName(dn);
            Object tmp = extractOneAttributeValue(attributes, EMAIL_ATTR_NAME);
            if (tmp != null) out.setEmail(tmp.toString());
            tmp = extractOneAttributeValue(attributes, FIRSTNAME_ATTR_NAME);
            if (tmp != null) out.setFirstName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LASTNAME_ATTR_NAME);
            if (tmp != null) out.setLastName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, LOGIN_ATTR_NAME);
            if (tmp != null) out.setLogin(tmp.toString());
            // this would override the dn
            // tmp = extractOneAttributeValue(attributes, NAME_ATTR_NAME);
            // if (tmp != null) out.setName(tmp.toString());
            tmp = extractOneAttributeValue(attributes, PASSWD_ATTR_NAME);
            if (tmp != null) out.setPassword(tmp.toString());
            Collection groupHeaders = findGroupMembershipsAsHeaders(out);
            out.setGroupHeaders(new HashSet(groupHeaders));
            context.close();
            return out;
        } catch (NamingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
    }

    public User findByLogin(String login) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        try {
            DirContext context = getAnonymousContext();
            // Search using attribute list.
            Attributes matchAttrs = new BasicAttributes(true); // ignore attribute name case
            matchAttrs.put(new BasicAttribute(LOGIN_ATTR_NAME, login));
            String[] attrToReturn = {"dn"};
            NamingEnumeration answer = null;
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), matchAttrs, attrToReturn);
            // Close the anon context now that we're done with it
            context.close();
            String dn = null;
            if (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
            } else return null;
            return findByPrimaryKey(dn);
        } catch (NamingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
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
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null || config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) throw new FindException("No search base provided");
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + USER_OBJCLASS + ")";
            SearchControls sc = new SearchControls();
            // String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};
            //answer = getAnonymousContext().search(config.getSearchBase(), null, attrToReturn);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getAnonymousContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                String login = null;
                String dn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, LOGIN_ATTR_NAME);
                if (tmp != null) login = tmp.toString();
                if (login != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        }
        catch (NamingException e)
        {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
            throw new FindException(e.getMessage(), e);
        }
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null || config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) throw new FindException("No search base provided");
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + USER_OBJCLASS + ")";
            SearchControls sc = new SearchControls();
            // String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};
            //answer = getAnonymousContext().search(config.getSearchBase(), null, attrToReturn);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getAnonymousContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            int count = 0;
            while (answer.hasMore())
            {
                if (count < offset) {
                    ++count;
                    continue;
                }
                if (count >= offset+windowSize) {
                    break;
                }
                ++count;
                String login = null;
                String dn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, LOGIN_ATTR_NAME);
                if (tmp != null) login = tmp.toString();
                if (login != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            // if nothing can be found, just trace this exception and return empty collection
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
        }
        return output;
    }

    public Collection findAll() throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection headers = findAllHeaders();
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        if (!valid) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection headers = findAllHeaders(offset, windowSize);
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public boolean authenticateBasic(String dn, String passwd) {
        if (!valid) {
            return false;
        }
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, config.getProperty(LdapConfigSettings.LDAP_HOST_URL));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, passwd);
        DirContext userCtx = null;
        try
        {
            userCtx = new InitialDirContext(env);
            // Close the context when we're done
            userCtx.close();
        } catch (AuthenticationException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "User failed to authenticate: " + dn, e);
            return false;
        } catch (NamingException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "General naming failure for user: " + dn, e);
            return false;
        }
        LogManager.getInstance().getSystemLogger().info("User: "+ dn +" authenticated successfully.");
        return true;
    }

    public void invalidate() {
        valid = false;
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private Collection findGroupMembershipsAsHeaders(User user) {
        Collection out = new ArrayList();
        try
        {
            NamingEnumeration answer = null;
            String filter = "(" + GROUPOBJ_MEMBER_ATTR + "=" + user.getLogin() + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getAnonymousContext();
            answer = context.search(config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                SearchResult sr = (SearchResult)answer.next();

                String dn = null;
                String cn = null;
                String description = null;

                Attributes atts = sr.getAttributes();
                Object tmp = extractOneAttributeValue(atts, NAME_ATTR_NAME);
                if (tmp != null) cn = tmp.toString();
                tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTR);
                if (tmp != null) description = tmp.toString();

                dn = sr.getName() + "," + config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                EntityHeader grpheader = new EntityHeader(dn, EntityType.USER, cn, description);
                out.add(grpheader);
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            // if nothing can be found, just trace this exception and return empty collection
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, e);
        }
        return out;
    }

    // mappings for attribute names
    // these may become properties of the LdapIdentityProviderConfig
    private static final String EMAIL_ATTR_NAME = "mail";
    private static final String FIRSTNAME_ATTR_NAME = "givenName";
    private static final String LASTNAME_ATTR_NAME = "sn";
    private static final String PASSWD_ATTR_NAME = "userPassword";
    private boolean valid = true;
}
