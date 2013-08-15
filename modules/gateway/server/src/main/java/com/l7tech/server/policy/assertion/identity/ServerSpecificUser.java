package com.l7tech.server.policy.assertion.identity;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.util.ServerGoidUpgradeMapper;
import org.springframework.context.ApplicationContext;

public class ServerSpecificUser extends ServerIdentityAssertion<SpecificUser> {
    public ServerSpecificUser(SpecificUser assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext);

        requiredLogin = assertion.getUserLogin();
        requiredUid = assertion.getUserUid();
        requiredProvider = assertion.getIdentityProviderOid();
    }

    /**
     * Verifies that the authenticated <code>User</code> matches the <code>User</code>
     * corresponding to this Assertion's <code>userLogin</code> property.
     *
     * @param authResult the authentication result <code>User</code> to check
     * @return <code>AssertionStatus.NONE</code> if the <code>User</code> matches.
     */
    @Override
    public AssertionStatus checkUser(AuthenticationResult authResult) {
        // The login and the uid can't both be null
        if (requiredLogin == null && requiredUid == null) {
            logAndAudit(AssertionMessages.SPECIFICUSER_NOLOGIN_NOOID);
            return AssertionStatus.SERVER_ERROR;
        }

        User requestingUser = authResult.getUser();
        Goid requestProvider = requestingUser.getProviderId();
        String requestLogin = requestingUser.getLogin();

        // provider must always match
        if (!requestProvider.equals(requiredProvider)) {
            logAndAudit(AssertionMessages.SPECIFICUSER_PROVIDER_MISMATCH,
                    Goid.toString(requestProvider), Goid.toString(requiredProvider));
            return AssertionStatus.AUTH_FAILED;
        }

        // uid only needs to match if it's set as part of the assertion
        if (requiredUid != null && !"".equals(requiredUid)) {
            if (!requestingUser.isEquivalentId(requiredUid)) {
                logAndAudit(AssertionMessages.SPECIFICUSER_USERID_MISMATCH);
                return AssertionStatus.AUTH_FAILED;
            }
        }

        // login only needs to match if it's set as part of the assertion
        if (requiredLogin != null && !"".equals(requiredLogin)) {
            if (!requiredLogin.equals(requestLogin)) {
                logAndAudit(AssertionMessages.SPECIFICUSER_LOGIN_MISMATCH);
                return AssertionStatus.AUTH_FAILED;
            }
        }

        logger.fine("Match successful");
        return AssertionStatus.NONE;
    }

    protected SpecificUser specificUser;
    private final String requiredLogin;
    private final String requiredUid;
    private final Goid requiredProvider;
}
