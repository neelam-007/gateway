package com.l7tech.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProvider {
    /**
     * Called by {@link com.l7tech.server.policy.assertion.identity.ServerIdentityAssertion} to try to
     * identify a {@link User} based on a {@link LoginCredentials} that was previously attached to the
     * message by a {@link com.l7tech.policy.assertion.credential.CredentialSourceAssertion}.
     * <p>
     * If this method returns a User, the authentication was successful, but whether that user is
     * authorized is then left up to the implementations of ServerIdentityAssertion, namely
     * {@link com.l7tech.server.policy.assertion.identity.ServerSpecificUser} and
     * {@link com.l7tech.server.policy.assertion.identity.ServerMemberOfGroup}.
     *
     * @param pc an identity and a set of credentials.
     * @return a {@link User}. Must never be null.
     */
    User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException, IOException;

    /**
     * @return the {@link IdentityProviderConfig} for this IdentityProvider.
     */
    IdentityProviderConfig getConfig();

    /**
     * @return the {@link UserManager} for this IdentityProvider. Currently should never be null, but might be in future.
     */
    UserManager getUserManager();

    /**
     * @return the {@link GroupManager} for this IdentityProvider. Currently should never be null, but might be in future.
     */
    GroupManager getGroupManager();

    /**
     * searches for users and groups whose name match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    Collection search(EntityType[] types, String searchString) throws FindException;

    String getAuthRealm();

    void test() throws InvalidIdProviderCfgException;

    void preSaveClientCert(User user, X509Certificate cert) throws ClientCertManager.VetoSave;
}
