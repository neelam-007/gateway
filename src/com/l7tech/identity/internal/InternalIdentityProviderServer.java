package com.l7tech.identity.internal;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.common.util.KeystoreUtils;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class InternalIdentityProviderServer implements IdentityProvider {
    public static final String ENCODING = "UTF-8";

    public InternalIdentityProviderServer() {
        userManager = new InternalUserManagerServer();
        groupManager = new InternalGroupManagerServer();
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            _log.log( Level.SEVERE, e.getMessage(), e );
            throw new RuntimeException( e );
        }
    }

    public void initialize( IdentityProviderConfig config ) {
        cfg = config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public void authenticate( PrincipalCredentials pc ) throws AuthenticationException {
        User authUser = pc.getUser();
        byte[] credentials = pc.getCredentials();

        String login = authUser.getLogin();
        try {
            User dbUser = null;
            try {
                dbUser = userManager.findByLogin( login );
            } catch (FindException e) {
                _log.log(Level.SEVERE, "exception looking for user", e);
                dbUser = null;
            }
            if ( dbUser == null ) {
                String err = "Couldn't find user with login " + login;
                LogManager.getInstance().getSystemLogger().log(Level.INFO, err );
                throw new AuthenticationException( err );
            } else {
                CredentialFormat format = pc.getFormat();

                // TODO: This is really ugly.  Move to CredentialFormat subclasses?
                if ( format == CredentialFormat.CLIENTCERT ) {
                    Certificate dbCert = null;
                    X509Certificate dbCertX509 = null;

                    // get the cert from the credentials
                    Certificate maybeCert = (Certificate)pc.getPayload();
                    if ( maybeCert == null ) {
                        String err = "Request was supposed to contain a certificate, but does not";
                        _log.log( Level.SEVERE, err );
                        throw new MissingCredentialsException( err );
                    }

                    // Check whether the client cert is valid (according to our root cert)
                    // (get the root cert)
                    _log.log(Level.INFO, "Verifying client cert against current root cert...");
                    Certificate rootcacert = null;
                    try {
                        String rootCertLoc = KeystoreUtils.getInstance().getRootCertPath();
                        InputStream certStream = new FileInputStream(rootCertLoc);
                        byte[] rootcacertbytes = HexUtils.slurpStream(certStream, 16384);
                        certStream.close();
                        ByteArrayInputStream bais = new ByteArrayInputStream(rootcacertbytes);
                        rootcacert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
                    } catch (IOException e) {
                        String err = "Exception retrieving root cert " + e.getMessage();
                        LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err, e);
                        throw new AuthenticationException( err, e );
                    } catch (CertificateException e) {
                        String err = "Exception retrieving root cert " + e.getMessage();
                        LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err, e);
                        throw new AuthenticationException( err, e );
                    }
                    // (we have the root cert, verify client cert with it)
                    try {
                        maybeCert.verify(rootcacert.getPublicKey());
                    } catch (SignatureException e) {
                        String err = "client cert does not verify against current root ca cert. maybe our root cert changed since this cert was created.";
                        LogManager.getInstance().getSystemLogger().log(Level.WARNING, err, e);
                        throw new BadCredentialsException( err, e );
                    } catch (GeneralSecurityException e) {
                        String err = "Exception verifying client cert " + e.getMessage();
                        LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err, e);
                        throw new BadCredentialsException( err, e );
                    }
                    _log.log(Level.INFO, "Verification OK - client cert is valid.");
                    // End of Check

                    try {
                        dbCert = userManager.retrieveUserCert(Long.toString(dbUser.getOid()));
                    } catch (FindException e) {
                        _log.log(Level.SEVERE, "FindException exception looking for user cert", e);
                        dbCert = null;
                    }
                    if ( dbCert == null ) {
                        String err = "No certificate found for user " + login;
                        _log.log( Level.WARNING, err );
                        throw new InvalidClientCertificateException( err );
                    } else if ( dbCert instanceof X509Certificate ) {
                        dbCertX509 = (X509Certificate)dbCert;
                        _log.log( Level.FINE, "Stored cert serial# is " + dbCertX509.getSerialNumber().toString() );
                    } else {
                        String err = "Stored cert is not an X509Certificate!";
                        _log.log( Level.SEVERE, err );
                        throw new AuthenticationException( err );
                    }


                    if ( maybeCert instanceof X509Certificate ) {
                        X509Certificate pcCert = (X509Certificate)maybeCert;
                        _log.log( Level.FINE, "Request cert serial# is " + pcCert.getSerialNumber().toString() );
                        if ( pcCert.equals( dbCertX509 ) ) {
                            _log.log( Level.INFO, "Authenticated user " + login + " using a client certificate" );
                            pc.getUser().copyFrom( dbUser );
                            // remember that this cert was used at least once successfully
                            userManager.setCertWasUsed(Long.toString(dbUser.getOid()));
                            return;
                        } else {
                            String err = "Failed to authenticate user " + login + " using an SSL client certificate (request certificate doesn't match database)";
                            _log.log( Level.WARNING, err );
                            throw new InvalidClientCertificateException( err );
                        }
                    } else {
                        String err = "Certificate for " + login + " is not an X509Certificate";
                        _log.log( Level.WARNING, err );
                        throw new InvalidClientCertificateException( err );
                    }
                } else {
                    String dbPassHash = dbUser.getPassword();
                    String authPassHash = null;

                    if ( format == CredentialFormat.CLEARTEXT ) {
                        authPassHash = User.encodePasswd( login, new String( credentials, ENCODING ), HttpDigest.REALM );
                    } else if ( format == CredentialFormat.DIGEST ) {
                        Map authParams = (Map)pc.getPayload();
                        if ( authParams == null ) {
                            String err = "No Digest authentication parameters found in PrincipalCredentials payload!";
                            _log.log( Level.SEVERE, err );
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
                            authUser.copyFrom( dbUser );
                            return;
                        } else {
                            throw new BadCredentialsException();
                        }
                    } else {
                        throwUnsupportedCredentialFormat(format);
                    }

                    if ( dbPassHash.equals( authPassHash ) ) {
                        authUser.copyFrom( dbUser );
                        return;
                    }

                    LogManager.getInstance().getSystemLogger().log(Level.INFO, "Incorrect password for login " + login);

                    throw new BadCredentialsException();
                }
            }
        } catch (UpdateException e ) {
            _log.log(Level.SEVERE, null, e);
            throw new AuthenticationException( e.getMessage(), e );
        } catch ( UnsupportedEncodingException uee ) {
            _log.log(Level.SEVERE, null, uee);
            throw new AuthenticationException( uee.getMessage(), uee );
        }
    }

    private void throwUnsupportedCredentialFormat(CredentialFormat format) {
        IllegalArgumentException iae = new IllegalArgumentException( "Unsupported credential format: " + format.toString() );
        _log.log( Level.WARNING, iae.toString(), iae );
        throw iae;
    }

    public IdentityProviderConfig getConfig() {
        return cfg;
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

    private IdentityProviderConfig cfg;
    private InternalUserManagerServer userManager;
    private InternalGroupManagerServer groupManager;

    private Logger _log = LogManager.getInstance().getSystemLogger();
    private MessageDigest _md5;
}
