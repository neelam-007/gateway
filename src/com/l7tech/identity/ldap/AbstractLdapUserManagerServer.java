/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractLdapUserManagerServer implements UserManager {
    public AbstractLdapUserManagerServer( IdentityProviderConfig config ) {
        _ldapManager = new LdapManager( config );
        _config = config;
        logger = LogManager.getInstance().getSystemLogger();
    }

    protected abstract AbstractLdapConstants getConstants();

    /**
     * Get a User object for the given dn
     *
     * @param dn the distinguished name to look for in the directory
     * @throws FindException
     */
    public User findByPrimaryKey(String dn) throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }

        DirContext context = null;
        try {
            context = _ldapManager.getBrowseContext();
            Attributes attributes = context.getAttributes(dn);
            Attribute classes = attributes.get( AbstractLdapConstants.OBJCLASS_ATTR );

            // Check that it's really a user
            for (int i = 0; i < classes.size(); i++) {
                String oc = classes.get(i).toString();
                if ( !constants.userObjectClass().equalsIgnoreCase( oc ) ) continue;

                LdapUser out = new LdapUser();
                out.setProviderId( _config.getOid() );
                out.setDn(dn);
                Object tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userEmailAttribute() );
                if (tmp != null) out.setEmail(tmp.toString());
                tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userFirstnameAttribute() );
                if (tmp != null) out.setFirstName(tmp.toString());
                tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userLastnameAttribute() );
                if (tmp != null) out.setLastName(tmp.toString());
                tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userLoginAttribute() );
                if (tmp != null) out.setLogin(tmp.toString());
                tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userNameAttribute());
                if (tmp != null) out.setCn(tmp.toString());
                tmp = _ldapManager.extractOneAttributeValue(attributes, constants.userPasswordAttribute() );
                if (tmp != null) {
                    byte[] tmp2 = (byte[])tmp;
                    out.setPassword(new String(tmp2));
                }

                return out;
            }
            return null;
        } catch ( NamingException ne ) {
            logger.log( Level.SEVERE, ne.getMessage(), ne );
            return null;
        } finally {
            try {
                if ( context != null ) context.close();
            } catch (NamingException e) {
                logger.log( Level.SEVERE, e.getMessage(), e );
                return null;
            }
        }
    }

    public User findByLogin(String login) throws FindException {
        if (!valid) {
            logger.log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        try {
            DirContext context = _ldapManager.getBrowseContext();
            // Search using attribute list.
            /*Attributes matchAttrs = new BasicAttributes(true); // ignore attribute name case
            matchAttrs.put(new BasicAttribute( getConstants().userLoginAttribute(), login));*/

            String filter = "(" + getConstants().userNameAttribute() + "=" + login + ")";
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = null;
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            // Close the anon context now that we're done with it
            context.close();
            String dn = null;
            if (answer.hasMore()) {
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                logger.finer("found dn:" + dn + " for login: " + login);
            } else {
                logger.info("nothing has cn= " + login);
                return null;
            }
            return findByPrimaryKey(dn);
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public void delete(String identifier) throws DeleteException {
        throw new DeleteException( UNSUPPORTED );
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void delete(User user) throws DeleteException {
        throw new DeleteException( UNSUPPORTED );
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public String save(User user) throws SaveException {
        throw new SaveException( UNSUPPORTED );
    }

    public String save(User user, Set groupHeaders ) throws SaveException {
        throw new SaveException( UNSUPPORTED );
    }

    /**
     * This is actually not supported in this UserManager since the we assume the ldap connector is only used to
     * read user information
     */
    public void update(User user) throws UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public void update(User user, Set groupHeaders ) throws UpdateException {
        throw new UpdateException( UNSUPPORTED );
    }

    public Collection findAllHeaders() throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + constants.userObjectClass() + ")";
            SearchControls sc = new SearchControls();
            // String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};
            //answer = getAnonymousContext().search(_config.getSearchBase(), null, attrToReturn);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = _ldapManager.getBrowseContext();
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                String login = null;
                String dn = null;
                SearchResult sr = (SearchResult)answer.next();
                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = _ldapManager.extractOneAttributeValue(atts, constants.userLoginAttribute() );
                if (tmp != null) login = tmp.toString();
                if (login != null && dn != null) {
                    EntityHeader header = new EntityHeader(dn, EntityType.USER, login, null);
                    output.add(header);
                }
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return output;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        if (!valid) {
            logger.log(Level.SEVERE, "invalid user manager");
            throw new FindException("invalid manager");
        }
        Collection output = new ArrayList();
        if (_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE) == null ||
            _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE).length() < 1) {
            throw new FindException("No search base provided");
        }
        try
        {
            NamingEnumeration answer = null;
            String filter = "(objectclass=" + getConstants().userObjectClass() + ")";
            SearchControls sc = new SearchControls();
            // String[] attrToReturn = {LOGIN_ATTR_NAME, NAME_ATTR_NAME};
            //answer = getAnonymousContext().search(_config.getSearchBase(), null, attrToReturn);
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = _ldapManager.getBrowseContext();
            answer = context.search(_config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
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
                dn = sr.getName() + "," + _config.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                Object tmp = _ldapManager.extractOneAttributeValue(atts, getConstants().userLoginAttribute() );
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
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return output;
    }

    public Collection findAll() throws FindException {
        if (!valid) {
            logger.log(Level.SEVERE, "invalid user manager");
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
            logger.log(Level.SEVERE, "invalid user manager");
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
        env.put(Context.PROVIDER_URL, _config.getProperty(LdapConfigSettings.LDAP_HOST_URL));
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
            logger.info( "User failed to authenticate: " + dn );
            return false;
        } catch (NamingException e) {
            logger.log( Level.WARNING, "General naming failure for user: " + dn, e);
            return false;
        }
        logger.info("User: "+ dn +" authenticated successfully.");
        return true;
    }

    public void invalidate() {
        valid = false;
    }

    /*private void buildGroupMembershipsBasedOnOUs(User user) {
        String dn = user.getName();
        // look for traces of "ou=" in the dn, see if the user belongs to any ous
        String lookfor = AbstractLdapConstants.oUObjAttrName() + "=";
        int foundat = -1;
        foundat = dn.indexOf(lookfor, 0);
        while (foundat >= 0) {
            // todo
        }

    }*/


    private volatile boolean valid = true;
    private LdapManager _ldapManager;
    private IdentityProviderConfig _config;
    protected Logger logger = null;

    private static final String UNSUPPORTED = "This operation is not supported in an LDAP-style UserManager!";
}
