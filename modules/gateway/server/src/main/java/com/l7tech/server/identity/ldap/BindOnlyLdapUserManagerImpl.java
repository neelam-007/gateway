package com.l7tech.server.identity.ldap;

import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * User manager for a simple bind-only LDAP provider.
 */
@LdapClassLoaderRequired
public class BindOnlyLdapUserManagerImpl implements BindOnlyLdapUserManager {
    private static final Logger logger = Logger.getLogger(BindOnlyLdapUserManagerImpl.class.getName());

    private BindOnlyLdapIdentityProvider identityProvider;
    private BindOnlyLdapIdentityProviderConfig identityProviderConfig;
    private LdapRuntimeConfig ldapRuntimeConfig;
    private Config config;

    @Override
    public BindOnlyLdapUser findByPrimaryKey(String identifier) throws FindException {
        return findByLogin(identifier);
    }

    @Override
    public BindOnlyLdapUser findByLogin(String login) throws FindException {
        assertUsernameMatchesRegexFindException(login);

        // Do not include DN as it will be expanded at runtime, when authentication is attempted.
        return makeUser(login, null);
    }

    @Override
    public void delete(BindOnlyLdapUser user) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String identifier) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteAll(Goid ipoid) throws DeleteException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(BindOnlyLdapUser user) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(BindOnlyLdapUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String save(Goid id, BindOnlyLdapUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    @Override
    public BindOnlyLdapUser reify(UserBean bean) {
        if (!bean.getProviderId().equals(identityProviderConfig.getGoid()))
            throw new IllegalArgumentException("User bean does not belong to this provider");

        BindOnlyLdapUser user = makeUser(bean.getLogin(), bean.getSubjectDn());
        user.setProviderId(bean.getProviderId());
        user.setLogin(bean.getLogin());
        return user;
    }

    @Override
    public void update(BindOnlyLdapUser user, Set<IdentityHeader> groupHeaders) throws UpdateException, FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
        return new EntityHeaderSet<IdentityHeader>();
    }

    @Override
    public IdentityHeader userToHeader(BindOnlyLdapUser user) {
        return new IdentityHeader(user.getProviderId(), user.getId(), EntityType.USER, user.getLogin(), user.getName(), user.getCn(), null);
    }

    @Override
    public BindOnlyLdapUser headerToUser(IdentityHeader header) {
        BindOnlyLdapUser user = makeUser(header.getStrId(), null);
        user.setProviderId(identityProviderConfig.getGoid());
        user.setLogin(header.getStrId());
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

    @Override
    public boolean authenticateBasic(String dn, String passwd) {
        return LdapUtils.authenticateBasic(identityProvider, identityProviderConfig, ldapRuntimeConfig, logger, dn, passwd);
    }

    @Override
    public AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc) throws BadCredentialsException, BadUsernamePatternException {
        // basic authentication
        final String login = pc.getLogin();
        final String dn = makeDn(login);
        boolean res = authenticateBasic(dn, new String(pc.getCredentials()));
        if (res) {
            // success.  Include actual expanded DN in the auth result.
            return new AuthenticationResult(makeUser(login, dn), pc.getSecurityTokens());
        }
        logger.info("credentials did not authenticate for " + login);
        throw new BadCredentialsException("credentials did not authenticate");
    }

    @Override
    public String makeDn(String login) throws BadUsernamePatternException {
        assertUsernameMatchesRegex(login);

        String prefix = identityProviderConfig.getBindPatternPrefix();
        if (prefix == null) prefix = "";

        String suffix = identityProviderConfig.getBindPatternSuffix();
        if (suffix == null) suffix = "";

        return prefix + login + suffix;
    }

    private void assertUsernameMatchesRegex(String login) throws BadUsernamePatternException {
        String regex = config.getProperty(ServerConfigParams.PARAM_BIND_ONLY_LDAP_USERNAME_PATTERN);
        Pattern pattern = Pattern.compile(regex == null || regex.trim().length() < 1 ? "^.+$" : regex);

        if (!pattern.matcher(login).matches()) {
            throw new BadUsernamePatternException();
        }
    }

    private void assertUsernameMatchesRegexFindException(String login) throws FindException {
        try {
            assertUsernameMatchesRegex(login);
        } catch (BadUsernamePatternException e) {
            throw new FindException(e.getMessage(), e);
        }
    }

    @Override
    public void configure(BindOnlyLdapIdentityProvider provider) {
        this.config = ConfigFactory.getCachedConfig();
        this.identityProvider = provider;
        this.identityProviderConfig = (BindOnlyLdapIdentityProviderConfig)provider.getConfig();
    }

    @Override
    public void setLdapRuntimeConfig(final LdapRuntimeConfig ldapRuntimeConfig) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }

    private BindOnlyLdapUser makeUser(String login, @Nullable String dn) {
        return new BindOnlyLdapUser(identityProviderConfig.getGoid(), dn, login);
    }

}
