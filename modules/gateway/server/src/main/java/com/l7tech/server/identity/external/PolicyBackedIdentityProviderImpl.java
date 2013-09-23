package com.l7tech.server.identity.external;

import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.identity.external.VirtualPolicyUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.util.ExceptionUtils;
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
import java.util.logging.Logger;

/**
 *
 */
public class PolicyBackedIdentityProviderImpl implements PolicyBackedIdentityProvider, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(PolicyBackedIdentityProviderImpl.class.getName());

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
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

        String msg = "Attempt to authenticate using unsupported credential type on policy-backed provider: " + pc.getFormat();
        auditor.logAndAudit(SystemMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
        throw new AuthenticationException(msg);
    }

    @Override
    public VirtualPolicyUser findUserByCredential(LoginCredentials pc) throws FindException {
        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            final String login = pc.getLogin();
            if (login != null && login.length() > 0) {
                return new VirtualPolicyUser(config.getGoid(), login);
            }
        }
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
    public X509Certificate findCertBySubjectDn(X500Principal subjectDn) throws FindException {
        return null;
    }

    @Override
    public void setIdentityProviderConfig(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException {
        this.config = (PolicyBackedIdentityProviderConfig) configuration;
        userManager.configure(this);
    }

    @Override
    public IdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public PolicyBackedUserManager getUserManager() {
        return userManager;
    }

    @Override
    public PolicyBackedGroupManager getGroupManager() {
        return groupManager;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        EnumSet<EntityType> typeset = EnumSet.noneOf(EntityType.class);
        typeset.addAll(Arrays.asList(types));

        EntityHeaderSet<IdentityHeader> ret = new EntityHeaderSet<>();

        if (searchString != null) {
            String login = searchString.trim().replace("*", "");
            if (login.length() > 0 && typeset.contains(EntityType.USER)) {
                ret.add(new IdentityHeader(config.getGoid(), login, EntityType.USER, login, "Template username \"" + login + "\"", login, null));
            }
        }

        return ret;
    }

    @Override
    public String getAuthRealm() {
        return HexUtils.REALM;
    }

    @Override
    public void test(boolean fast, @Nullable String testUser, @Nullable char[] testPassword) throws InvalidIdProviderCfgException {
        if (fast)
            return;

        if (testUser == null)
            throw new InvalidIdProviderCfgException("A test username must be provided in order to test a policy-backed provider.");

        if (testPassword == null)
            throw new InvalidIdProviderCfgException("A test password must be provided in order to test a policy-backed provider.");

        try {
            getUserManager().authenticatePasswordCredentials(LoginCredentials.makeLoginCredentials(new HttpBasicToken(testUser, testPassword), HttpBasic.class));
            // We don't care about the AuthenticationResult, just that it succeeded

        } catch (BadCredentialsException e) {
            throw new InvalidIdProviderCfgException("Test credentials failed to authenticate: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void preSaveClientCert(VirtualPolicyUser user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        throw new ClientCertManager.VetoSave("Feature not supported for policy-backed ID provider");
    }

    @Override
    public void setUserManager(PolicyBackedUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(PolicyBackedGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public void validate(VirtualPolicyUser user) throws ValidationException {
        // Validation always succeeds for virtual users.
        // Backing policy is responsible for implementing any needed account expiry/etc
    }

    @Override
    public boolean hasClientCert(String login) throws AuthenticationException {
        return false;
    }

    @Override
    public Goid getDefaultRoleId() {
        return config.getDefaultRoleId();
    }

    private Auditor auditor;
    private PolicyBackedIdentityProviderConfig config;
    private PolicyBackedUserManager userManager;
    private PolicyBackedGroupManager groupManager;
}
