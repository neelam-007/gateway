package com.l7tech.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;

import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProvider {
    /**
     * Called by {@link com.l7tech.server.policy.assertion.identity.ServerIdentityAssertion} to try to
     * identify a {@link User} based on a {@link LoginCredentials} that was previously attached to the
     * message by a {@link com.l7tech.policy.assertion.Assertion} that is a credential source.
     * <p>
     * If this method returns a User, the authentication was successful, but whether that user is
     * authorized is then left up to the implementations of ServerIdentityAssertion, namely
     * {@link com.l7tech.server.policy.assertion.identity.ServerSpecificUser} and
     * {@link com.l7tech.server.policy.assertion.identity.ServerMemberOfGroup}.
     *
     * @param pc an identity and a set of credentials.
     * @return an authenticated {@link User}. May be null if no user matching the specified credentials can be found for this provider.
     */
    AuthenticationResult authenticate( LoginCredentials pc ) throws AuthenticationException;

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
    Collection<IdentityHeader> search(EntityType[] types, String searchString) throws FindException;

    Collection<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException;

    String getAuthRealm();

    void test() throws InvalidIdProviderCfgException;

    /**
     * Allows an IdentityProvider to veto the saving of a client cert. Currently only used by
     * {@link com.l7tech.server.identity.fed.FederatedIdentityProvider}.
     * @param user the user for whom the cert is to be saved
     * @param certChain the client certificate chain
     * @throws ClientCertManager.VetoSave if the provider wants to prevent the cert from being saved
     */
    void preSaveClientCert(User user, X509Certificate[] certChain) throws ClientCertManager.VetoSave;
}
