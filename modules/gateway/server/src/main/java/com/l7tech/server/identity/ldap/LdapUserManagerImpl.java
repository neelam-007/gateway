package com.l7tech.server.identity.ldap;

import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;

import javax.naming.AuthenticationException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import java.util.Collection;
import java.util.Set;
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
        identityProviderConfig = identityProvider.getConfig();
        ldapTemplate = new LdapUtils.LdapTemplate(
                identityProviderConfig.getSearchBase(),
                getReturningAttributes() ){
            @Override
            DirContext getDirContext() throws NamingException {
                return identityProvider.getBrowseContext();
            }
        };
    }

    public void setLdapRuntimeConfig( final LdapRuntimeConfig ldapRuntimeConfig ) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }

    /**
     * find user based on dn
     *
     * @return a LdapUser object, null if not found
     */
    @Override
    public LdapUser findByPrimaryKey(final String dn) throws FindException {
        final LdapUser[] userHolder = new LdapUser[1];
        try {
            ldapTemplate.attributes( dn, new LdapUtils.LdapListener(){
                @Override
                void attributes( final String dn, final Attributes attributes ) throws NamingException {
                    userHolder[0] = buildUser( dn, attributes );
                }
            } );
        } catch (NameNotFoundException e) {
            if ( logger.isLoggable(Level.FINEST )) {
                logger.finest("User " + dn + " does not exist in" + getIdentityProviderConfig().getName() + " (" + ExceptionUtils.getMessage(e) + ")");
            }
        } catch (AuthenticationException ae) {
            logger.log(Level.WARNING, "LDAP authentication error: " + ExceptionUtils.getMessage(ae), ExceptionUtils.getDebugException(ae));
        } catch (NamingException ne) {
            logger.log(Level.WARNING, "LDAP error: " + ExceptionUtils.getMessage(ne), ExceptionUtils.getDebugException(ne));
        }

        return userHolder[0];
    }

    /**
     * find a user based on his login attribute
     *
     * @return a LdapUser object, null if not found
     */
    @Override
    public LdapUser findByLogin( final String login ) throws FindException {
        final LdapUser[] userHolder = new LdapUser[1];
        try {
            LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();

            LdapSearchFilter filter = new LdapSearchFilter();
            filter.or();
            UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
            for (UserMappingConfig userType : userTypes) {
                filter.attrEquals(userType.getLoginAttrName(), login);
            }
            filter.end();

            ldapTemplate.search( filter.buildFilter(), 1, null, new LdapUtils.LdapListener(){
                @Override
                boolean searchResult( final SearchResult sr ) throws NamingException {
                    userHolder[0] =  buildUser( sr.getNameInNamespace(), sr.getAttributes());
                    return false;
                }
            } );

            if ( userHolder[0] == null ) {
                logger.fine(ldapIdentityProviderConfig.getName() + " cannot find cn=" + login);
            }
        } catch (AuthenticationException ae) {
            logger.log(Level.WARNING, "LDAP authentication error: " + ae.getMessage(), ExceptionUtils.getDebugException(ae));
        } catch (NamingException ne) {
            logger.log(Level.WARNING, "LDAP error: " + ne.getMessage(), ExceptionUtils.getDebugException(ne));
        }

        return userHolder[0];
    }

    private LdapUser buildUser( final String dn, final Attributes attributes ) throws NamingException {
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

        final LdapIdentityProviderConfig ldapIdentityProviderConfig = getIdentityProviderConfig();
        final UserMappingConfig[] userTypes = ldapIdentityProviderConfig.getUserMappings();
        final Attribute objectclasses = attributes.get(LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME);
        for (UserMappingConfig userType : userTypes) {
            String userclass = userType.getObjClass();
            if (LdapUtils.attrContainsCaseIndependent(objectclasses, userclass)) {
                LdapUser out = new LdapUser();
                out.setProviderId(ldapIdentityProviderConfig.getGoid());
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
                if (tmp instanceof byte[]) {
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
    public void deleteAll(Goid ipoid) {
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
        LdapUser user = new LdapUser();
        user.setProviderId(identityProviderConfig.getGoid());
        user.setDn(header.getStrId());
        user.setCn(header.getName());
        return user;
    }

    @Override
    public boolean authenticateBasic(String dn, String passwd) {
        return LdapUtils.authenticateBasic(getIdentityProvider(), getIdentityProviderConfig(), this.ldapRuntimeConfig, this.logger, dn, passwd);
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
    private LdapRuntimeConfig ldapRuntimeConfig;
    private LdapUtils.LdapTemplate ldapTemplate;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
