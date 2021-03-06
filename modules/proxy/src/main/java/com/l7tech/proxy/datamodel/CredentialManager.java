/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.security.token.SecurityTokenType;

import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * Get usernames and passwords from somewhere.  The caller of the manager interface will then
 * typically stuff them into an Ssg object for safe keeping.
 *
 * In the GUI environment, the default CredentialManager will just pop up a login window.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:31:45 AM
 */
public abstract class CredentialManager {
    public static enum ReasonHint {
        NONE(""),
        PRIVATE_KEY("for your client certificate "),
        TOKEN_SERVICE("for your security token service ");

        private final String r;
        private ReasonHint(String r) { this.r = r; }
        public String toString() { return r; }
    }

    /**
     * Get the credentials for this SSG.  If they aren't already loaded, this may involve reading
     * them from the keystore or prompting the user for them.  No new credentials will be obtained
     * if this SSG already has existing credentials configured.  To indicate that the existing
     * credentials are no good and that you need new ones, call getNewCredentials() instead.
     *
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg  the Ssg whose credentials you want
     * @return the credentials for this Ssg, or null if none are configured
     * @throws OperationCanceledException if credentials are needed but the user declines to supply them at this time.
     * @throws com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException if credentials are needed and an HTTP challenge back to the client is required to obtain them for this Ssg.
     */
    public abstract PasswordAuthentication getCredentials(Ssg ssg) throws OperationCanceledException, HttpChallengeRequiredException;

    /**
     * Same as getCredentialsForTrustedSsg(), but can provide a hint to the user about what the credentials are to be
     * used for.
     *
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the Ssg whose credentials you want
     * @param hint hint to the user about what we plan to do with the password when we get it.  Currently required.
     *             The same credentials must be returned regardless of which hint is provided.
     *             (Eventually we may support using a private key password that differs from the Gateway password.)
     * @param disregardExisting if true, the user will be prompted for new credentials even if cached credentials are on hand.
     * @param reportBadPassword if true, the user will be advised that the existing credentials are bad.  Implies disregardExisting.
     * @return the credentials for this Ssg, or null if none are configured.
     * @throws OperationCanceledException if credentials are needed but the user declines to supply them at this time.
     * @throws com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException if credentials are needed and an HTTP challenge back to the client is required to obtain them for this Ssg.
     */
    public abstract PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg,
                                                                        ReasonHint hint,
                                                                        boolean disregardExisting,
                                                                        boolean reportBadPassword)
            throws OperationCanceledException, HttpChallengeRequiredException;

    /**
     * Get replacement credentials for this SSG.  This method will always prompt the user for
     * new credentials to replace the current ones.
     *
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the Ssg whose credentials you want to update
     * @param displayBadPasswordMessage if true, user will be told that current credentials are no good.
     * @return the new credentials for this Ssg.  Never null.
     * @throws OperationCanceledException if credentials are needed but the user declines to supply them at this time.
     * @throws com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException if credentials are needed and an HTTP challenge back to the client is required to obtain them for this Ssg.
     */
    public abstract PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException, HttpChallengeRequiredException;

    /**
     * Get auxiliary credentials for this Ssg, to be used to obtain a security token of the specified type from
     * the specified token service hostname. 
     *
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the Ssg whose auxiliary credentials you want.  Currently required.
     * @param tokenType the type of security token we are trying to get, or null if not relevant.
     * @param stsHostname the hostname of the security token service we plan to talk to.  Currently required.
     * @param hint hint to the user about what we plan to do with the password when we get it.  Currently required.
     *             The same credentials must be returned regardless of which hint is provided.
     * @param reportBadPassword if true, the user will be advised that the existing credentials are bad.
     * @return the credentials to use.  Never null.
     * @throws com.l7tech.proxy.datamodel.exceptions.CredentialsUnavailableException if no matching credentials are configured or available 
     * @throws OperationCanceledException if we prompted the user, but he clicked cancel
     */
    public abstract PasswordAuthentication getAuxiliaryCredentials(Ssg ssg, SecurityTokenType tokenType, String stsHostname, ReasonHint hint, boolean reportBadPassword) throws OperationCanceledException;

    /**
     * Unobtrusively notify that a lengthy operation is now in progress.
     * In the GUI environment, this will put up a "Please wait..." dialog.
     * Only one "Please wait..." dialog will be active for a given Ssg.
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the affected Ssg, or null.
     * @param message the message to display.  Required.
     */
    public abstract void notifyLengthyOperationStarting(Ssg ssg, String message);

    /**
     * Unobtrusively notify that a lengthy operation has completed.
     * Tears down any "Please wait..." dialog.
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the affected Ssg, or null.
     */
    public abstract void notifyLengthyOperationFinished(Ssg ssg);

    /**
     * Notify the user that the key store for the given Ssg has been damaged.
     * This should _not_ be used in cases where the user merely mistyped the password to decrypt his
     * private key.  The user should be given the option of either canceling, which will abort the operation,
     * or continuing, which will cause the invalid keystore to be deleted (which will require the SSG admin to revoke
     * the client cert, if the only copy of it's private key is now gone).
     *
     * Whatever decision the user makes should be remembered for the rest of this session.
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the Ssg whose keystore was found to be corrupt.  Required.
     * @throws OperationCanceledException if the user does not wish to delete the invalid keystore
     */
    public abstract void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException;

    /**
     * Notify the user that a client certificate has already been issued for his account.
     * At this point there is nothing the user can do except try a different account, or contact
     * his Gateway administrator and beg to have the lost certificate revoked from the database.
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param ssg the Ssg whose client certificate was found to be already issued.  Required.
     */
    public abstract void notifyCertificateAlreadyIssued(Ssg ssg);

    /**
     * Notify the user that an operation could not be performed because the specified feature does not seem
     * to be available on the specified target Gateway.
     *
     * @param ssg      the gateway we needed service from.  Must not be null
     * @param feature  the feature we were trying to use, ie "certificate signing service".  Must not be null.
     */
    public abstract void notifyFeatureNotAvailable(Ssg ssg, String feature);

    /**
     * Notify the user that an SSL connection to the server could not be established because the hostname did not match
     * the one in the certificate presented by the server during the handshake.
     * <p>
     * Caller <em>must not</em> hold the Ssg monitor when calling this method.
     *
     * @param server  description of the SSL server we were talking to (ie, "the Gateway foo.bar.baz")
     * @param whatWeWanted  the expected hostname, equal to ssg.getSsgAddress() if the server was an Ssg
     * @param whatWeGotInstead  the hostname in the peer's certificate
     */
    public abstract void notifySslHostnameMismatch(String server, String whatWeWanted, String whatWeGotInstead);

    /**
     * Notify the user that the discovered server certificate could not be trusted automatically.
     * The user may elect to trust it anyway, or to cancel.  If the certificate should be trusted, this method
     * will return.  Otherwise, it will thorw OperationCanceledException.
     *
     * @param sslPeer the SSL peer whose certificate was not trusted.  Used by non-interactive Bridge users to
     *                decide whether to trust a given certificate.  Must not be null.
     * @param serverDesc description of the SSL server we were talking to (ie, "the Gateway foo.bar.baz").  Must not be null.
     * @param untrustedCertificate the certificate that was presented by this server and that did not pass muster.  Must not be null.
     * @throws OperationCanceledException if trustworthiness of this certificate was not confirmed.
     */
    public abstract void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException;

    /**
     * Save any changes made to the specified Ssg back to whereever it came from.
     * @param ssg the Ssg whose data has changed.  Must not be null.
     */
    public abstract void saveSsgChanges(Ssg ssg);
}
