package com.l7tech.server.identity.internal;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.xml.saml.SamlAssertion;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.server.identity.PersistentIdentityProvider;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * IdentityProvider implementation for the internal identity provider.
 * 
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 24, 2003
 */
public class InternalIdentityProvider extends PersistentIdentityProvider {
    public static final String ENCODING = "UTF-8";

    public InternalIdentityProvider(IdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * constructor for subclassing
     */
    protected InternalIdentityProvider() {
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException, IOException {
        String login = pc.getLogin();
        char[] credentials = pc.getCredentials();

        try {
            InternalUser dbUser = null;
            dbUser = (InternalUser)userManager.findByLogin( login );
            if ( dbUser == null ) {
                String err = "Couldn't find user with login " + login;
                logger.info(err);
                throw new AuthenticationException( err );
            } else {
                if (dbUser.getExpiration() > -1 && dbUser.getExpiration() < System.currentTimeMillis()) {
                    String err = "Credentials' login matches an internal user " + login + " but that " +
                                 "account is now expired.";
                    logger.info(err);
                    throw new AuthenticationException( err );
                }
                CredentialFormat format = pc.getFormat();
                X509Certificate dbCert = null;
                if (format.isClientCert() || format == CredentialFormat.SAML) {
                    X509Certificate requestCert = null;
                    Object payload = pc.getPayload();

                    // TODO: This is really ugly.  Move to CredentialFormat subclasses?
                    if ( format.isClientCert() ) {
                        // get the cert from the credentials
                        requestCert = (X509Certificate)payload;
                    } else if (format == CredentialFormat.SAML) {
                        if (payload instanceof SamlAssertion) {
                            SamlAssertion assertion = (SamlAssertion)payload;
                            requestCert = assertion.getSubjectCertificate();
                        } else {
                            throw new BadCredentialsException("Unsupported SAML Assertion type: " +
                                                              payload.getClass().getName());
                        }
                    }

                    if ( requestCert == null ) {
                        String err = "Request was supposed to contain a certificate, but does not";
                        logger.severe(err);
                        throw new MissingCredentialsException( err );
                    }

                    // Check whether the client cert is valid (according to our root cert)
                    // (get the root cert)
                    logger.finest("Verifying client cert against current root cert...");
                    Certificate rootcacert = null;
                    try {
                        //todo: coonsider moving reading the rootCacert in ctor, or in lazy init; it may save few
                        // cycles - em 20040520
                        String rootCertLoc = keystore.getRootCertPath();
                        InputStream certStream = new FileInputStream(rootCertLoc);
                        byte[] rootcacertbytes = HexUtils.slurpStream(certStream, 16384);
                        certStream.close();
                        rootcacert = CertUtils.decodeCert(rootcacertbytes);
                    } catch (CertificateException e) {
                        String err = "Exception retrieving root cert " + e.getMessage();
                        logger.log(Level.SEVERE, err, e);
                        throw new AuthenticationException( err, e );
                    }
                    // (we have the root cert, verify client cert with it)
                    try {
                        requestCert.verify(rootcacert.getPublicKey());
                    } catch (SignatureException e) {
                        String err = "client cert does not verify against current root ca cert. maybe our root cert changed since this cert was created.";
                        logger.log(Level.WARNING, err, e);
                        throw new BadCredentialsException( err, e );
                    } catch (GeneralSecurityException e) {
                        String err = "Exception verifying client cert " + e.getMessage();
                        logger.log(Level.SEVERE, err, e);
                        throw new BadCredentialsException( err, e );
                    }
                    logger.finest("Verification OK - client cert is valid.");
                    // End of Check

                    try {
                        dbCert = (X509Certificate)clientCertManager.getUserCert(dbUser);
                    } catch (FindException e) {
                        logger.log(Level.SEVERE, "FindException exception looking for user cert", e);
                        dbCert = null;
                    }

                    if ( dbCert == null ) {
                        String err = "No certificate found for user " + login;
                        logger.warning(err);
                        throw new InvalidClientCertificateException( err );
                    }

                    logger.fine("Request cert serial# is " + requestCert.getSerialNumber().toString());
                    if ( CertUtils.certsAreEqual(requestCert, dbCert ) ) {
                        logger.finest("Authenticated user " + login + " using a client certificate" );
                        // remember that this cert was used at least once successfully
                        try {
                            clientCertManager.forbidCertReset(dbUser);
                            return dbUser;
                        } catch (ObjectModelException e) {
                            logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                        }
                    } else {
                        String err = "Failed to authenticate user " + login + " using a client certificate " +
                                     "(request certificate doesn't match database's)";
                        logger.warning(err);
                        throw new InvalidClientCertificateException( err );
                    }
                } else {
                    String dbPassHash = dbUser.getPassword();
                    String authPassHash = null;

                    if ( format == CredentialFormat.CLEARTEXT ) {
                        authPassHash = UserBean.encodePasswd( login, new String(credentials), HttpDigest.REALM );
                    } else if ( format == CredentialFormat.DIGEST ) {
                        Map authParams = (Map)pc.getPayload();
                        if ( authParams == null ) {
                            String err = "No Digest authentication parameters found in LoginCredentials payload!";
                            logger.severe(err);
                            throw new MissingCredentialsException( err );
                        }

                        String qop = (String)authParams.get( HttpDigest.PARAM_QOP );
                        String nonce = (String)authParams.get( HttpDigest.PARAM_NONCE );

                        String a2 = (String)authParams.get( HttpDigest.PARAM_METHOD ) + ":" +
                                    (String)authParams.get( HttpDigest.PARAM_URI );

                        String ha2 = HexUtils.encodeMd5Digest( HexUtils.getMd5().digest( a2.getBytes() ) );

                        String serverDigestValue;
                        if (!HttpDigest.QOP_AUTH.equals(qop))
                            serverDigestValue = dbPassHash + ":" + nonce + ":" + ha2;
                        else {
                            String nc = (String)authParams.get( HttpDigest.PARAM_NC );
                            String cnonce = (String)authParams.get( HttpDigest.PARAM_CNONCE );

                            serverDigestValue = dbPassHash + ":" + nonce + ":" + nc + ":"
                                                + cnonce + ":" + qop + ":" + ha2;
                        }

                        String expectedResponse = HexUtils.encodeMd5Digest( HexUtils.getMd5().digest( serverDigestValue.getBytes() ) );
                        String response = new String( credentials );

                        if ( response.equals( expectedResponse ) ) {
                            return dbUser;
                        } else {
                            throw new BadCredentialsException();
                        }
                    } else {
                        throwUnsupportedCredentialFormat(format);
                    }

                    if ( dbPassHash.equals( authPassHash ) ) {
                        return dbUser;
                    }

                    logger.info("Incorrect password for login " + login);

                    throw new BadCredentialsException();
                }
                return null;
            }
        } catch ( UnsupportedEncodingException uee ) {
            logger.log(Level.SEVERE, null, uee);
            throw new AuthenticationException( uee.getMessage(), uee );
        }
    }


    private void throwUnsupportedCredentialFormat(CredentialFormat format) {
        IllegalArgumentException iae = new IllegalArgumentException( "Unsupported credential format: " + format.toString() );
        logger.log( Level.WARNING, iae.toString(), iae );
        throw iae;
    }

    public IdentityProviderConfig getConfig() {
        return config;

    }

    // TODO: Make this customizable
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public void test() throws InvalidIdProviderCfgException {
        if (config.getOid() != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID) {
            logger.warning("Testing an internal id provider with no good oid. Throwing InvalidIdProviderCfgException");
            throw new InvalidIdProviderCfgException("This internal ID provider config is not valid.");
        }
    }

    public void preSaveClientCert( User user, X509Certificate[] certChain ) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public void setGroupManager(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    /**
     * Subclasses can override this for custom initialization behavior.
     * Gets called after population of this instance's bean properties.
     *
     * @throws Exception if initialization fails
     */
    protected void initDao() throws Exception {
        super.initDao();
    }

    private IdentityProviderConfig config;
    private UserManager userManager;
    private GroupManager groupManager;

    private final Logger logger = Logger.getLogger(getClass().getName());
}
