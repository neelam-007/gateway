package com.l7tech.identity.ldap;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.logging.LogManager;

import javax.naming.directory.*;
import javax.naming.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UserManager for ldap identity provider.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapUserManager implements UserManager {

    public LdapUserManager(LdapIdentityProviderConfig cfg, LdapIdentityProvider daddy) {
        this.cfg = cfg;
        this.parent = daddy;
    }

    /**
     * find user based on dn
     * @return a LdapUser object, null if not found
     */
    public User findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        try {
            context = LdapIdentityProvider.getBrowseContext(cfg);
            Attributes attributes = context.getAttributes(dn);

            UserMappingConfig[] userTypes = cfg.getUserMappings();
            Attribute objectclasses = attributes.get("objectclass");
            for (int i = 0; i < userTypes.length; i ++) {
                String userclass = userTypes[i].getObjClass();
                if (LdapIdentityProvider.attrContainsCaseIndependent(objectclasses, userclass)) {
                    LdapUser out = new LdapUser();
                    out.setProviderId(cfg.getOid());
                    out.setDn(dn);
                    Object tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getEmailNameAttrName());
                    if (tmp != null) out.setEmail(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getFirstNameAttrName());
                    if (tmp != null) out.setFirstName(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getLastNameAttrName());
                    if (tmp != null) out.setLastName(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getLoginAttrName());
                    if (tmp != null) out.setLogin(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getNameAttrName());
                    if (tmp != null) out.setCn(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userTypes[i].getPasswdAttrName());
                    // todo, something about the passwd type
                    if (tmp != null) {
                        byte[] tmp2 = (byte[])tmp;
                        out.setPassword(new String(tmp2));
                    }
                    return out;
                }
            }
            return null;
        } catch (NameNotFoundException e) {
            logger.finest("user " + dn + " does not exist" + e.getMessage());
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

    /**
     * find a user based on his login attribute
     * @return a LdapUser object, null if not found
     */
    public User findByLogin(String login) throws FindException {
        try {
            DirContext context = LdapIdentityProvider.getBrowseContext(cfg);

            String filter = "(|";
            UserMappingConfig[] userTypes = cfg.getUserMappings();

            for (int i = 0; i < userTypes.length; i ++) {
                filter += "(" + userTypes[i].getLoginAttrName() + "=" + login + ")";
            }
            filter += ")";

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = null;
            answer = context.search(cfg.getSearchBase(), filter, sc);
            // Close the anon context now that we're done with it
            context.close();
            String dn = null;
            try {
                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    dn = sr.getName() + "," + cfg.getSearchBase();
                    logger.finer(cfg.getName() + " found dn:" + dn + " for login: " + login);
                } else {
                    logger.info(cfg.getName() + " cannot find cn=" + login);
                    return null;
                }
            } finally {
                answer.close();
            }
            return findByPrimaryKey(dn);
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    /**
     * throws UnsupportedOperationException
     */
    public void delete(User user) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    public String save(User user) throws SaveException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    public void update(User user) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    public String save(User user, Set groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    public void update(User user, Set groupHeaders) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    /**
     * practical equivalent to LdapIdentityProvider.search(new EntityType[] {EntityType.USER}, "*");
     */
    public Collection findAllHeaders() throws FindException {
        return parent.search(new EntityType[] {EntityType.USER}, "*");
    }

    /**
     * throws UnsupportedOperationException
     */
    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
    }

    /**
     * like findAllHeaders but returns LdapUser objects instead of EntityHeader objects
     */
    public Collection findAll() throws FindException {
        Collection headers = findAllHeaders();
        Collection output = new ArrayList();
        Iterator i = headers.iterator();
        while (i.hasNext()) {
            EntityHeader header = (EntityHeader)i.next();
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    /**
     * like findAllHeaders but returns LdapUser objects instead of EntityHeader objects
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
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
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, cfg.getLdapUrl());
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

    private LdapIdentityProviderConfig cfg;
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private LdapIdentityProvider parent;
}
