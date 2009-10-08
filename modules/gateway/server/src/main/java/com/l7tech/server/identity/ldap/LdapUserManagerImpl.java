package com.l7tech.server.identity.ldap;

import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Set;
import java.util.Collection;
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
@LdapClassLoaderRequired
public class LdapUserManagerImpl implements LdapUserManager {
    
    public LdapUserManagerImpl() {
    }

    @Override
    public synchronized void configure(LdapIdentityProvider provider) {
        identityProvider = provider;
        identityProviderConfig = (LdapIdentityProviderConfig)identityProvider.getConfig();
    }

    /**
     * find user based on dn
     *
     * @return a LdapUser object, null if not found
     */
    @Override
    public LdapUser findByPrimaryKey(String dn) throws FindException {
        DirContext context = null;
        LdapIdentityProvider identityProvider = getIdentityProvider();
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();

        try {
            context = identityProvider.getBrowseContext();
            Attributes attributes = context.getAttributes(dn);

            if (!identityProvider.isValidEntryBasedOnUserAccountControlAttribute(dn, attributes)) {
                // This is warning level because it could
                // be caused by a locked out user trying to
                // get in using certificate granted by ssg.
                logger.warning("User " + dn + " is locked or disabled. Returning null.");
                return null;
            } else if (identityProvider.checkExpiredMSADAccount(dn, attributes)) {
                logger.warning("Account " + dn + " is expired. Returning null.");
                return null;
            }

            UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
            Attribute objectclasses = attributes.get(LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME);
            for (UserMappingConfig userType : userTypes) {
                String userclass = userType.getObjClass();
                if (LdapUtils.attrContainsCaseIndependent(objectclasses, userclass)) {
                    LdapUser out = new LdapUser();
                    out.setProviderId(ldapIdentityProviderConfig.getOid());
                    out.setDn(dn);
                    out.setAttributes(attributes);
                    Object tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getEmailNameAttrName());
                    if (tmp != null) out.setEmail(tmp.toString());
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getFirstNameAttrName());
                    if (tmp != null) out.setFirstName(tmp.toString());
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getLastNameAttrName());
                    if (tmp != null) out.setLastName(tmp.toString());
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getLoginAttrName());
                    if (tmp != null) out.setLogin(tmp.toString());
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getNameAttrName());
                    if (tmp != null) out.setCn(tmp.toString());
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userType.getPasswdAttrName());
                    // todo, something about the passwd type
                    if (tmp != null) {
                        byte[] tmp2 = (byte[]) tmp;
                        out.setPassword(new String(tmp2));
                    }

                    // check for presence of userCertificate
                    String userCertAttrName = userType.getUserCertAttrName();
                    if (userCertAttrName == null) {
                        userCertAttrName = LdapUtils.LDAP_ATTR_USER_CERTIFICATE;
                    }
                    tmp = LdapUtils.extractOneAttributeValue(attributes, userCertAttrName);
                    if (tmp != null) {
                        if (tmp instanceof byte[]) {
                            out.setLdapCertBytes((byte[])tmp);
                        } else {
                            logger.warning("User certificate Ldap property populated with " +
                                           "data in an unexpected format " + tmp.getClass());
                        }
                    }

                    return out;
                }
            }
            return null;
        } catch (NameNotFoundException e) {
            logger.finest("user " + dn + " does not exist in" + ldapIdentityProviderConfig.getName() + "(" + e.getMessage() + ")");
            return null;
        } catch (AuthenticationException ae) {
            logger.log(Level.WARNING, "LDAP authentication error: " + ae.getMessage(), ExceptionUtils.getDebugException(ae));
            return null;
        } catch (NamingException ne) {
            logger.log(Level.WARNING, "LDAP error: " + ne.getMessage(), ExceptionUtils.getDebugException(ne));
            return null;
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    /**
     * find a user based on his login attribute
     *
     * @return a LdapUser object, null if not found
     */
    @Override
    public LdapUser findByLogin(String login) throws FindException {
        DirContext context = null;
        try {
            LdapIdentityProvider identityProvider = getIdentityProvider();
            LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
            context = identityProvider.getBrowseContext();

            StringBuffer filter = new StringBuffer("(|");
            UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();

            for (UserMappingConfig userType : userTypes) {
                filter.append("(");
                filter.append(userType.getLoginAttrName());
                filter.append("={0})");
            }
            filter.append(")");

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(getReturningAttributes());
            String dn = null;

            NamingEnumeration<SearchResult> answer = null;
            try {
                answer = context.search(ldapIdentityProviderConfig.getSearchBase(),
                                        filter.toString(),
                                        new String[]{login},
                                        sc);
                if (answer.hasMore()) {
                    dn = answer.next().getNameInNamespace();
                } else {
                    logger.fine(ldapIdentityProviderConfig.getName() + " cannot find cn=" + login);
                    return null;
                }
            } finally {
                ResourceUtils.closeQuietly( answer );
            }

            // close context here so it can be re-used in find method (if pooled)
            context.close();
            context = null;
            
            return findByPrimaryKey(dn);
        } catch (AuthenticationException ae) {
            logger.log(Level.WARNING, "LDAP authentication error: " + ae.getMessage(), ExceptionUtils.getDebugException(ae));
        } catch (NamingException ne) {
            logger.log(Level.WARNING, "LDAP error: " + ne.getMessage(), ExceptionUtils.getDebugException(ne));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
        return null;
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public void delete(LdapUser user) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public void deleteAll(long ipoid) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public void delete(String identifier) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public void update(LdapUser user) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public String save(LdapUser user, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LdapUser reify(UserBean bean) {
        throw new UnsupportedOperationException();
    }

    /**
     * throws UnsupportedOperationException
     */
    @Override
    public void update(LdapUser user, Set groupHeaders) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<LdapUser> getImpClass() {
        return LdapUser.class;
    }

    /**
     * practical equivalent to LdapIdentityProvider.search(new EntityType[] {EntityType.USER}, "*");
     */
    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        return identityProvider.search(new EntityType[]{EntityType.USER}, "*");
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        return identityProvider.search(new EntityType[]{EntityType.USER}, "*");
    }

    @Override
    public IdentityHeader userToHeader(LdapUser user) {
        return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getLogin(), user.getName(), user.getCn(), null);
    }

    @Override
    public LdapUser headerToUser(IdentityHeader header) {
        LdapIdentityProvider identityProvider = getIdentityProvider();
        LdapUser user = new LdapUser();
        user.setProviderId(identityProvider.getConfig().getOid());
        user.setDn(header.getStrId());
        user.setCn(header.getName());
        return user;
    }

    @Override
    public boolean authenticateBasic(String dn, String passwd) {
        if (passwd == null || passwd.length() < 1) {
            logger.info("User: " + dn + " refused authentication because empty password provided.");
            return false;
        }
        LdapIdentityProvider identityProvider = getIdentityProvider();
        LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        String ldapurl = identityProvider.getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = identityProvider.markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
        }
        while (ldapurl != null) {
            DirContext userCtx = null;
            try {
                boolean clientAuth = ldapIdentityProviderConfig.isClientAuthEnabled();
                Long keystoreId = ldapIdentityProviderConfig.getKeystoreId();
                String keyAlias = ldapIdentityProviderConfig.getKeyAlias();
                userCtx = LdapUtils.getLdapContext(ldapurl, clientAuth, keystoreId, keyAlias, dn, passwd, identityProvider.getLdapConnectionTimeout(), identityProvider.getLdapReadTimeout(), false );
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
            } finally {
                ResourceUtils.closeQuietly( userCtx );
            }
        }
        logger.warning("Could not establish context on any of the ldap urls.");
        return false;
    }

    private String[] getReturningAttributes() {
        String[] returningAttributes = null;

        Collection<String> attributes = identityProvider.getReturningAttributes();
        if ( attributes != null ) {
            returningAttributes = attributes.toArray(new String[attributes.size()]);
        }

        return returningAttributes;
    }

    private LdapIdentityProvider getIdentityProvider() {
        LdapIdentityProvider provider =  identityProvider;
        if ( provider == null ) {
            throw new IllegalStateException("Not configured!");
        }
        return provider;
    }

    private LdapIdentityProviderConfig getIdentityProviderConfig() {
        LdapIdentityProviderConfig config = identityProviderConfig;
        if ( config == null ) {
            throw new IllegalStateException("Not configured!");
        }
        return config;
    }

    private LdapIdentityProvider identityProvider;
    private LdapIdentityProviderConfig identityProviderConfig;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
