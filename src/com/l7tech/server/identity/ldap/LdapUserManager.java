package com.l7tech.server.identity.ldap;

import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.*;

import javax.naming.*;
import javax.naming.directory.*;
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
            context = parent.getBrowseContext();
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
            logger.finest("user " + dn + " does not exist in" + cfg.getName() + "(" + e.getMessage() + ")");
            return null;
        } catch ( NamingException ne ) {
            logger.log( Level.WARNING, ne.getMessage(), ne );
            return null;
        } finally {
            try {
                if ( context != null ) context.close();
            } catch (NamingException e) {
                logger.log( Level.WARNING, e.getMessage(), e );
                return null;
            }
        }
    }

    /**
     * find a user based on his login attribute
     * @return a LdapUser object, null if not found
     */
    public User findByLogin(String login) throws FindException {
        DirContext context = null;
        try {
            context = parent.getBrowseContext();

            StringBuffer filter = new StringBuffer("(|");
            UserMappingConfig[] userTypes = cfg.getUserMappings();

            for (int i = 0; i < userTypes.length; i ++) {
                filter.append("(");
                filter.append(userTypes[i].getLoginAttrName());
                filter.append("=");
                filter.append(login);
                filter.append(")");
            }
            filter.append(")");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer = null;
            answer = context.search(cfg.getSearchBase(), filter.toString(), sc);

            String dn = null;
            try {
                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    dn = sr.getName() + "," + cfg.getSearchBase();
                } else {
                    logger.fine(cfg.getName() + " cannot find cn=" + login);
                    return null;
                }
            } finally {
                answer.close();
            }
            return findByPrimaryKey(dn);
        } catch (NamingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        } finally {
            try {
                if ( context != null ) context.close();
            } catch ( NamingException e ) {
                logger.log(Level.WARNING, "Caught NamingException while closing LDAP Context", e);
            }
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
    public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
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

    public Class getImpClass() {
        return LdapUser.class;
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

    public Collection search(String searchString) throws FindException {
        return parent.search(new EntityType[] { EntityType.USER }, "*" );
    }

    public EntityHeader userToHeader( User user ) {
        return new EntityHeader(user.getUniqueIdentifier(), EntityType.USER, user.getLogin(), user.getName());
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

    public Integer getVersion( long oid ) throws FindException {
        return new Integer(0);
    }

    public Map findVersionMap() throws FindException {
        return Collections.EMPTY_MAP;
    }

    public Entity getCachedEntity( long o, int maxAge ) throws FindException, CacheVeto {
        throw new UnsupportedOperationException();
    }

    public boolean authenticateBasic(String dn, String passwd) {
        UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // todo refactor to use all possible urls
        env.put(Context.PROVIDER_URL, parent.getLastWorkingLdapUrl());
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, dn);
        env.put(Context.SECURITY_CREDENTIALS, passwd);
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.timeout", LdapIdentityProvider.LDAP_CONNECT_TIMEOUT );
        env.put("com.sun.jndi.ldap.connect.pool.timeout", LdapIdentityProvider.LDAP_POOL_IDLE_TIMEOUT );
        env.lock();

        DirContext userCtx = null;
        try
        {
            userCtx = new InitialDirContext(env);
            // Close the context when we're done
            userCtx.close();
        } catch (AuthenticationException e) {
            // when you get bad credentials
            logger.info( "User failed to authenticate: " + dn  + " in provider " + cfg.getName());
            return false;
        } catch (NamingException e) {
            // when you get a connection problem
            logger.log( Level.WARNING, "General naming failure for user: " + dn + " in provider " + cfg.getName(), e);
            return false;
        }
        logger.info("User: "+ dn +" authenticated successfully in provider " + cfg.getName());
        return true;
    }

    private LdapIdentityProviderConfig cfg;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private LdapIdentityProvider parent;
}
