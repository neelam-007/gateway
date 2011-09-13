package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.CertUtils;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;

import java.util.Set;
import java.util.logging.Logger;

/**
 * User manager for a simple bind-only LDAP provider.
 */
public class BindOnlyLdapUserManager implements UserManager<LdapUser> {
    private static final Logger logger = Logger.getLogger(BindOnlyLdapUserManager.class.getName());

    private BindOnlyLdapIdentityProvider identityProvider;
    private BindOnlyLdapIdentityProviderConfig identityProviderConfig;
    private LdapRuntimeConfig ldapRuntimeConfig;

    @Override
    public LdapUser findByPrimaryKey(String identifier) throws FindException {
        return makeUser(identifier, CertUtils.extractFirstCommonNameFromDN(identifier));
    }

    @Override
    public LdapUser findByLogin(String login) throws FindException {
        return makeUser(makeDn(login), login);
    }

    @Override
    public void delete(LdapUser user) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String identifier) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(long ipoid) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(LdapUser user) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(LdapUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LdapUser reify(UserBean bean) {
        if (bean.getProviderId() != identityProviderConfig.getOid())
            throw new IllegalArgumentException("User bean does not belong to this provider");

        LdapUser user = new LdapUser();
        user.setProviderId(bean.getProviderId());
        user.setDn(bean.getSubjectDn());
        user.setCn(bean.getName());
        return user;
    }

    @Override
    public void update(LdapUser user, Set<IdentityHeader> groupHeaders) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public IdentityHeader userToHeader(LdapUser user) {
        return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getLogin(), user.getName(), user.getCn(), null);
    }

    @Override
    public LdapUser headerToUser(IdentityHeader header) {
        LdapUser user = new LdapUser();
        user.setProviderId(identityProviderConfig.getOid());
        user.setDn(header.getStrId());
        user.setCn(header.getName());
        return user;
    }

    @Override
    public Class<? extends User> getImpClass() {
        return LdapUser.class;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    public boolean authenticateBasic(String dn, String passwd) {
        return LdapUtils.authenticateBasic(identityProvider, identityProviderConfig, ldapRuntimeConfig, logger, dn, passwd);
    }

    public AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc) throws BadCredentialsException {
        // basic authentication
        final String login = pc.getLogin();
        String dn = makeDn(login);
        boolean res = authenticateBasic(dn, new String(pc.getCredentials()));
        if (res) {
            // success
            return new AuthenticationResult(makeUser(dn, login), pc.getSecurityTokens());
        }
        logger.info("credentials did not authenticate for " + login);
        throw new BadCredentialsException("credentials did not authenticate");
    }

    public String makeDn(String login) {
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        // TODO validation, checking for invalid pattern, checking for LDAP injection, etc
        return identityProviderConfig.getBindPatternPrefix() + login + identityProviderConfig.getBindPatternSuffix();
    }

    private LdapUser makeUser(String dn, String login) {
        return new LdapUser(identityProviderConfig.getOid(), dn, login);
    }

    public void configure(BindOnlyLdapIdentityProvider provider) {
        this.identityProvider = provider;
        this.identityProviderConfig = (BindOnlyLdapIdentityProviderConfig)provider.getConfig();
    }

    public void setLdapRuntimeConfig( final LdapRuntimeConfig ldapRuntimeConfig ) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }
}
