package com.l7tech.server.identity.internal;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
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
public class InternalIdentityProviderServer implements IdentityProvider {
    public static final String ENCODING = "UTF-8";

    public InternalIdentityProviderServer(IdentityProviderConfig config) {
        this.config = config;
        this.userManager = new InternalUserManagerServer( this );
        this.groupManager = new InternalGroupManagerServer( this );

        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new RuntimeException( e );
        }
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException, IOException {
        String login = pc.getLogin();
        byte[] credentials = pc.getCredentials();

        try {
            InternalUser dbUser = null;
            dbUser = (InternalUser)userManager.findByLogin( login );
            if ( dbUser == null ) {
                String err = "Couldn't find user with login " + login;
                logger.info(err);
                throw new AuthenticationException( err );
            } else {
                CredentialFormat format = pc.getFormat();

                // TODO: This is really ugly.  Move to CredentialFormat subclasses?
                if ( format.isClientCert() ) {
                    Certificate dbCert = null;
                    X509Certificate dbCertX509 = null;

                    // get the cert from the credentials
                    Certificate maybeCert = (Certificate)pc.getPayload();
                    if ( maybeCert == null ) {
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
                        String rootCertLoc = KeystoreUtils.getInstance().getRootCertPath();
                        InputStream certStream = new FileInputStream(rootCertLoc);
                        byte[] rootcacertbytes = HexUtils.slurpStream(certStream, 16384);
                        certStream.close();
                        ByteArrayInputStream bais = new ByteArrayInputStream(rootcacertbytes);
                        rootcacert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
                    } catch (CertificateException e) {
                        String err = "Exception retrieving root cert " + e.getMessage();
                        logger.log(Level.SEVERE, err, e);
                        throw new AuthenticationException( err, e );
                    }
                    // (we have the root cert, verify client cert with it)
                    try {
                        maybeCert.verify(rootcacert.getPublicKey());
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

                    ClientCertManager man = (ClientCertManager)Locator.getDefault().lookup(ClientCertManager.class);
                    try {
                        dbCert = man.getUserCert(dbUser);
                    } catch (FindException e) {
                        logger.log(Level.SEVERE, "FindException exception looking for user cert", e);
                        dbCert = null;
                    }

                    if ( dbCert == null ) {
                        String err = "No certificate found for user " + login;
                        logger.warning(err);
                        throw new InvalidClientCertificateException( err );
                    } else if ( dbCert instanceof X509Certificate ) {
                        dbCertX509 = (X509Certificate)dbCert;
                        logger.fine("Stored cert serial# is " + dbCertX509.getSerialNumber().toString());
                    } else {
                        String err = "Stored cert is not an X509Certificate!";
                        logger.severe(err);
                        throw new AuthenticationException( err );
                    }

                    if ( maybeCert instanceof X509Certificate ) {
                        X509Certificate pcCert = (X509Certificate)maybeCert;
                        logger.fine("Request cert serial# is " + pcCert.getSerialNumber().toString());
                        if ( pcCert.equals( dbCertX509 ) ) {
                            logger.finest("Authenticated user " + login + " using a client certificate" );
                            // remember that this cert was used at least once successfully
                            try {
                                PersistenceContext.getCurrent().beginTransaction();
                                man.forbidCertReset(dbUser);
                                PersistenceContext.getCurrent().commitTransaction();
                                // dont close context here. the message processor will do it
                                return dbUser;
                            } catch (SQLException e) {
                                logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                            } catch (TransactionException e) {
                                logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
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
                        String err = "Certificate for " + login + " is not an X509Certificate";
                        logger.warning(err);
                        throw new InvalidClientCertificateException( err );
                    }
                } else {
                    String dbPassHash = dbUser.getPassword();
                    String authPassHash = null;

                    if ( format == CredentialFormat.CLEARTEXT ) {
                        authPassHash = UserBean.encodePasswd( login, new String( credentials, ENCODING ), HttpDigest.REALM );
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

                        String ha2 = HexUtils.encodeMd5Digest( _md5.digest( a2.getBytes() ) );

                        String serverDigestValue;
                        if (!HttpDigest.QOP_AUTH.equals(qop))
                            serverDigestValue = dbPassHash + ":" + nonce + ":" + ha2;
                        else {
                            String nc = (String)authParams.get( HttpDigest.PARAM_NC );
                            String cnonce = (String)authParams.get( HttpDigest.PARAM_CNONCE );

                            serverDigestValue = dbPassHash + ":" + nonce + ":" + nc + ":"
                                    + cnonce + ":" + qop + ":" + ha2;
                        }

                        String expectedResponse = HexUtils.encodeMd5Digest( _md5.digest( serverDigestValue.getBytes() ) );
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

    public boolean isReadOnly() { return false; }

    /**
     * searches for users and groups whose name (cn) match the pattern described in searchString
     * pattern may include wildcard such as * character. result is sorted by name property
     *
     * todo: (once we dont use hibernate?) replace this by one union sql query and have the results sorted
     * instead of sorting in collection.
     */
    public Collection search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) throw new IllegalArgumentException("must pass at least one type");
        boolean wantUsers = false;
        boolean wantGroups = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == EntityType.USER) wantUsers = true;
            else if (types[i] == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) throw new IllegalArgumentException("types must contain users and or groups");
        Collection searchResults = new TreeSet(new EntityHeaderComparator());
        if (wantUsers) searchResults.addAll(userManager.search(searchString));
        if (wantGroups) searchResults.addAll(groupManager.search(searchString));
        return searchResults;
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

    private final IdentityProviderConfig config;
    private final InternalUserManagerServer userManager;
    private final InternalGroupManagerServer groupManager;

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final MessageDigest _md5;
}
