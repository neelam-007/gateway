package com.l7tech.server.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.BindOnlyLdapUser;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.Lifecycle;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway implementation of Simple LDAP identity provider.
 */
@LdapClassLoaderRequired
public class BindOnlyLdapIdentityProviderImpl implements BindOnlyLdapIdentityProvider, ApplicationContextAware, ConfigurableIdentityProvider, Lifecycle {
    private static final Logger logger = Logger.getLogger(BindOnlyLdapIdentityProviderImpl.class.getName());

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    }

    @Override
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        return authenticate(pc, false);
    }

    @Override
    public AuthenticationResult authenticate(LoginCredentials pc, boolean allowUserUpgrade) throws AuthenticationException {
        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            return userManager.authenticatePasswordCredentials(pc);
        }

        String msg = "Attempt to authenticate using unsupported credential type on this provider: " + pc.getFormat();
        logger.log(Level.SEVERE, msg);
        throw new AuthenticationException(msg);
    }

    @Override
    public BindOnlyLdapUser findUserByCredential(LoginCredentials lc) throws FindException {
        return null;
    }

    @Override
    public X509Certificate findCertByIssuerAndSerial(X500Principal issuer, BigInteger serial) throws FindException {
        return null;
    }

    @Override
    public X509Certificate findCertBySki(String ski) throws FindException {
        return null;
    }

    @Override
    public X509Certificate findCertByThumbprintSHA1(String thumbprintSHA1) throws FindException {
        return null;
    }

    @Override
    public void setIdentityProviderConfig(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException {
        this.config = (BindOnlyLdapIdentityProviderConfig) configuration;
        userManager.configure(this);
        urlProvider = new LdapUrlProviderImpl(config.getLdapUrl(), ldapRuntimeConfig);
    }

    @Override
    public IdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public BindOnlyLdapUserManager getUserManager() {
        return userManager;
    }

    @Override
    public BindOnlyLdapGroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        EnumSet<EntityType> typeset = EnumSet.noneOf(EntityType.class);
        typeset.addAll(Arrays.asList(types));

        EntityHeaderSet<IdentityHeader> ret = new EntityHeaderSet<IdentityHeader>();

        if (searchString != null) {
            String login = searchString.trim().replace("*", "");
            if (login.length() > 0 && typeset.contains(EntityType.USER)) {
                ret.add(new IdentityHeader(config.getOid(), login, EntityType.USER, login, "Template username \"" + login + "\"", login, null));
            }
        }

        return ret;
    }

    @Override
    public String getLastWorkingLdapUrl() {
        return urlProvider.getLastWorkingLdapUrl();
    }

    @Override
    public String markCurrentUrlFailureAndGetFirstAvailableOne(@Nullable String urlThatFailed) {
        return urlProvider.markCurrentUrlFailureAndGetFirstAvailableOne(urlThatFailed);
    }

    @Override
    public String getAuthRealm() {
        return HexUtils.REALM;
    }

    @Override
    public void test(boolean fast, String testUser, char[] testPassword) throws InvalidIdProviderCfgException {
        if (fast)
            return;

        if (testUser == null)
            throw new InvalidIdProviderCfgException("A test username must be provided in order to test a Simple LDAP provider.");

        if (testPassword == null)
            throw new InvalidIdProviderCfgException("A test password must be provided in order to test a Simple LDAP provider.");

        try {
            getUserManager().authenticatePasswordCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken(testUser, testPassword), HttpBasic.class));
            // We don't care about the AuthenticationResult, just that it succeeded

        } catch (BadCredentialsException e) {
            throw new InvalidIdProviderCfgException("Test credentials failed to authenticate.", e);
        }
    }

    @Override
    public void preSaveClientCert(BindOnlyLdapUser user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        throw new ClientCertManager.VetoSave("Feature not supported for bind-only LDAP provider");
    }

    @Override
    public void setUserManager(BindOnlyLdapUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(BindOnlyLdapGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public void validate(BindOnlyLdapUser user) throws ValidationException {
        throw new ValidationException("Validation not supported for bind-only LDAP");
    }

    @Override
    public boolean hasClientCert(String login) throws AuthenticationException {
        return false;
    }

    @Override
    public void start() throws LifecycleException {
    }

    @Override
    public void stop() throws LifecycleException {
    }

    public void setLdapRuntimeConfig(LdapRuntimeConfig ldapRuntimeConfig) {
        this.ldapRuntimeConfig = ldapRuntimeConfig;
    }

    private LdapRuntimeConfig ldapRuntimeConfig;
    private BindOnlyLdapIdentityProviderConfig config;
    private BindOnlyLdapUserManager userManager;
    private BindOnlyLdapGroupManager groupManager;

    private LdapUrlProviderImpl urlProvider;
}
