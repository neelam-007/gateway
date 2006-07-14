package com.l7tech.server.identity.ldap;

import com.l7tech.identity.User;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.IdentityProvider;
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
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 */
public class LdapUserManager implements UserManager {

    public LdapUserManager(IdentityProvider identityProvider) {
        if (identityProvider == null) {
            throw new IllegalArgumentException("Identity Provider is required");
        }
        if (!(identityProvider instanceof LdapIdentityProvider)) {
            throw new IllegalArgumentException("LDAP Identity Provider is required");
        }

        this.identityProvider = (LdapIdentityProvider)identityProvider;
        ldapIdentityProviderConfig = (LdapIdentityProviderConfig)identityProvider.getConfig();
    }

    /**
     * fo subclasing and class proxying
     */
    protected LdapUserManager() {
    }

    /**
     * find user based on dn
     *
     * @return a LdapUser object, null if not found
     */
    public User findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        try {
            context = identityProvider.getBrowseContext();
            Attributes attributes = context.getAttributes(dn);

            if (!identityProvider.isValidEntryBasedOnUserAccountControlAttribute(attributes)) {
                // This is warning level because it could
                // be caused by a locked out user trying to
                // get in using certificate granted by ssg.
                logger.warning("User " + dn + " is locked or disabled. Returning null.");
                return null;
            } else if (identityProvider.checkExpiredMSADAccount(attributes)) {
                logger.warning("Account " + dn + " is expired. Returning null.");
                return null;
            }

            UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
            Attribute objectclasses = attributes.get("objectclass");
            for (UserMappingConfig userType : userTypes) {
                String userclass = userType.getObjClass();
                if (LdapIdentityProvider.attrContainsCaseIndependent(objectclasses, userclass)) {
                    LdapUser out = new LdapUser();
                    out.setProviderId(ldapIdentityProviderConfig.getOid());
                    out.setDn(dn);
                    out.setAttributes(attributes);
                    Object tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getEmailNameAttrName());
                    if (tmp != null) out.setEmail(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getFirstNameAttrName());
                    if (tmp != null) out.setFirstName(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getLastNameAttrName());
                    if (tmp != null) out.setLastName(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getLoginAttrName());
                    if (tmp != null) out.setLogin(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getNameAttrName());
                    if (tmp != null) out.setCn(tmp.toString());
                    tmp = LdapIdentityProvider.extractOneAttributeValue(attributes, userType.getPasswdAttrName());
                    // todo, something about the passwd type
                    if (tmp != null) {
                        byte[] tmp2 = (byte[]) tmp;
                        out.setPassword(new String(tmp2));
                    }
                    return out;
                }
            }
            return null;
        } catch (NameNotFoundException e) {
            logger.finest("user " + dn + " does not exist in" + ldapIdentityProviderConfig.getName() + "(" + e.getMessage() + ")");
            return null;
        } catch (NamingException ne) {
            logger.log(Level.WARNING, ne.getMessage(), ne);
            return null;
        } finally {
            try {
                if (context != null) context.close();
            } catch (NamingException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                return null;
            }
        }
    }

    /**
     * find a user based on his login attribute
     *
     * @return a LdapUser object, null if not found
     */
    public User findByLogin(String login) throws FindException {
        DirContext context = null;
        try {
            context = identityProvider.getBrowseContext();

            StringBuffer filter = new StringBuffer("(|");
            UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();

            for (UserMappingConfig userType : userTypes) {
                filter.append("(");
                filter.append(userType.getLoginAttrName());
                filter.append("=");
                filter.append(login);
                filter.append(")");
            }
            filter.append(")");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration answer;
            answer = context.search(ldapIdentityProviderConfig.getSearchBase(), filter.toString(), sc);

            String dn = null;
            try {
                if (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    dn = sr.getName() + "," + ldapIdentityProviderConfig.getSearchBase();
                } else {
                    logger.fine(ldapIdentityProviderConfig.getName() + " cannot find cn=" + login);
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
                if (context != null) context.close();
            } catch (NamingException e) {
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
    public Collection<IdentityHeader> findAllHeaders() throws FindException {
        return identityProvider.search(new EntityType[]{EntityType.USER}, "*");
    }

    /**
     * throws UnsupportedOperationException
     */
    public Collection<IdentityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection<IdentityHeader> search(String searchString) throws FindException {
        return identityProvider.search(new EntityType[]{EntityType.USER}, "*");
    }

    public IdentityHeader userToHeader(User user) {
        return new IdentityHeader(user.getProviderId(), user.getUniqueIdentifier(), EntityType.USER, user.getLogin(), user.getName());
    }

    public User headerToUser(IdentityHeader header) {
        LdapUser user = new LdapUser();
        user.setProviderId(this.identityProvider.getConfig().getOid());
        user.setDn(header.getStrId());
        user.setCn(header.getName());
        return user;
    }

    /**
     * like findAllHeaders but returns LdapUser objects instead of EntityHeader objects
     */
    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection<IdentityHeader> headers = findAllHeaders(offset, windowSize);
        Collection<User> output = new ArrayList<User>();
        for (IdentityHeader header : headers) {
            output.add(findByPrimaryKey(header.getStrId()));
        }
        return output;
    }

    public Integer getVersion(long oid) throws FindException {
        return new Integer(0);
    }

    public Map findVersionMap() throws FindException {
        return Collections.EMPTY_MAP;
    }

    public Entity getCachedEntity(long o, int maxAge) throws FindException, EntityManager.CacheVeto {
        throw new UnsupportedOperationException();
    }

    public boolean authenticateBasic(String dn, String passwd) {
        if (passwd == null || passwd.length() < 1) {
            logger.info("User: " + dn + " refused authentication because empty password provided.");
            return false;
        }
        String ldapurl = identityProvider.getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = identityProvider.markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
        }
        while (ldapurl != null) {
            UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapurl);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, dn);
            //todo: consider using sasl features, to avoid sending passwords in clear
            //todo: could be determined during ldap provider setup phase
            //todo: see http://java.sun.com/products/jndi/tutorial/ldap/security/sasl.html
            //todo: sasl is not supported by all directories, we could also use ssl to avoid
            //todo: sending anything in clear env.put(Context.SECURITY_PROTOCOL, "ssl");
            env.put(Context.SECURITY_CREDENTIALS, passwd);
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.timeout", LdapIdentityProvider.LDAP_CONNECT_TIMEOUT);
            env.put("com.sun.jndi.ldap.connect.pool.timeout", LdapIdentityProvider.LDAP_POOL_IDLE_TIMEOUT);
            env.lock();

            DirContext userCtx;
            try {
                userCtx = new InitialDirContext(env);
                // Close the context when we're done
                userCtx.close();
                logger.info("User: " + dn + " authenticated successfully in provider " + ldapIdentityProviderConfig.getName());
                return true;
            } catch (CommunicationException e) {
                logger.log(Level.INFO, "Could not establish context using LDAP URL " + ldapurl, e);
                ldapurl = identityProvider.markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            } catch (AuthenticationException e) {
                // when you get bad credentials
                logger.info("User failed to authenticate: " + dn + " in provider " + ldapIdentityProviderConfig.getName());
                return false;
            } catch (NamingException e) {
                logger.log(Level.WARNING, "General naming failure for user: " + dn + " in provider " + ldapIdentityProviderConfig.getName(), e);
                return false;
            }
        }
        logger.warning("Could not establish context on any of the ldap urls.");
        return false;
    }

    private LdapIdentityProviderConfig ldapIdentityProviderConfig;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private LdapIdentityProvider identityProvider;
}
