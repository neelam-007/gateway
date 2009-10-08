package com.l7tech.server.identity;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.FindException;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * @author steve
 */
public interface AuthenticatingIdentityProvider<UT extends User, GT extends Group, UMT extends UserManager<UT>, GMT extends GroupManager<UT, GT>> extends IdentityProvider<UT, GT, UMT , GMT> {
    /**
     * Called by {@link com.l7tech.server.policy.assertion.identity.ServerIdentityAssertion} to try to
     * identify a {@link com.l7tech.identity.User} based on a {@link com.l7tech.policy.assertion.credential.LoginCredentials} that was previously attached to the
     * message by a {@link com.l7tech.policy.assertion.Assertion} that is a credential source.
     * <p>
     * If this method returns a User, the authentication was successful, but whether that user is
     * authorized is then left up to the implementations of ServerIdentityAssertion, namely
     * {@link com.l7tech.server.policy.assertion.identity.ServerSpecificUser} and
     * {@link com.l7tech.server.policy.assertion.identity.ServerMemberOfGroup}.
     *
     * @param pc an identity and a set of credentials.
     * @return an authenticated {@link com.l7tech.identity.User}. May be null if no user matching the specified credentials can be found for this provider.
     */
    AuthenticationResult authenticate( LoginCredentials pc ) throws AuthenticationException;

    /**
     * Find a user that matches the given credential.
     * 
     * @param lc The login credentials for the user.
     * @return The user or null if no matching user is found.
     * @throws FindException If an error occurs
     */
    UT findUserByCredential( LoginCredentials lc ) throws FindException;

    /**
     * Lookup a certificate from this provider by issuer DN and serial number.
     *
     * @param issuer The certificiate issuers principal
     * @param serial The certificate serial number
     * @return the certificate or null if not found.
     */
    X509Certificate findCertByIssuerAndSerial(final X500Principal issuer, final BigInteger serial) throws FindException;

    /**
     * Lookup a certificate from this provider by Subject Key Identifier.
     *
     * @param ski The certificiate Subject Key Identifier (BASE64)
     * @return the certificate or null if not found.
     */
    X509Certificate findCertBySki(final String ski) throws FindException;

    /**
     * Lookup a certificate from this provider by SHA1 thumbprint (fingerprint).
     *
     * @param thumbprintSHA1 The certificiate thumbprint (BASE64)
     * @return the certificate or null if not found.
     */
    X509Certificate findCertByThumbprintSHA1(final String thumbprintSHA1) throws FindException;
}
