package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.objectmodel.*;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.directory.*;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.SignatureException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * Server-side implementation of the LDAP provider.
 *
 * This handles any type of directory thgough a LdapIdentityProviderConfig
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 * $Id$<br/>
 *
 */
public class LdapIdentityProvider implements IdentityProvider {
    public LdapIdentityProvider() {
    }

    public void initialize(IdentityProviderConfig config) {
        try {
            this.cfg = new LdapIdentityProviderConfig(config);
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        userManager = new LdapUserManager(cfg, this);
        groupManager = new LdapGroupManager(cfg, this);
    }

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException {
        LdapUser realUser = null;
        try {
            realUser = (LdapUser)userManager.findByLogin( pc.getLogin());
        } catch (FindException e) {
            logger.log(Level.INFO, "invalid user", e);
            throw new BadCredentialsException("invalid user");
        }
        if (realUser == null) {
            logger.info("invalid user");
            throw new BadCredentialsException("invalid user");
        }

        if (pc.getFormat() == CredentialFormat.CLEARTEXT) {
            // basic authentication
            boolean res = userManager.authenticateBasic( realUser.getDn(), new String(pc.getCredentials()) );
            if (res) {
                // success
                return realUser;
            }
            logger.info("credentials did not authenticate for " + pc.getLogin());
            throw new BadCredentialsException("credentials did not authenticate");
        } else if (pc.getFormat() == CredentialFormat.DIGEST) {
            String dbPassHash = realUser.getPassword();
            byte[] credentials = pc.getCredentials();
            Map authParams = (Map)pc.getPayload();
            if (authParams == null) {
                String msg = "No Digest authentication parameters found in LoginCredentials payload!";
                logger.log(Level.SEVERE, msg);
                throw new AuthenticationException(msg);
            }

            String qop = (String)authParams.get(HttpDigest.PARAM_QOP);
            String nonce = (String)authParams.get(HttpDigest.PARAM_NONCE);

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

            String login = pc.getLogin();
            if ( response.equals( expectedResponse ) ) {
                logger.info("User " + login + " authenticated successfully with digest credentials.");
                return realUser;
            } else {
                String msg = "User " + login + " failed to match.";
                logger.warning(msg);
                throw new AuthenticationException(msg);
            }
        } else if (pc.getFormat() == CredentialFormat.CLIENTCERT) {
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
                String rootCertLoc = KeystoreUtils.getInstance().getRootCertPath();
                InputStream certStream = new FileInputStream(rootCertLoc);
                byte[] rootcacertbytes = HexUtils.slurpStream(certStream, 16384);
                certStream.close();
                ByteArrayInputStream bais = new ByteArrayInputStream(rootcacertbytes);
                rootcacert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
            } catch (IOException e) {
                String err = "Exception retrieving root cert " + e.getMessage();
                logger.log(Level.SEVERE, err, e);
                throw new AuthenticationException( err, e );
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
                dbCert = man.getUserCert(realUser);
            } catch (FindException e) {
                logger.log(Level.SEVERE, "FindException exception looking for user cert", e);
                dbCert = null;
            }
            if ( dbCert == null ) {
                String err = "No certificate found for user " + realUser.getDn();
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
                    logger.finest("Authenticated user " + realUser.getDn() + " using a client certificate" );
                    // remember that this cert was used at least once successfully
                    try {
                        PersistenceContext.getCurrent().beginTransaction();
                        man.forbidCertReset(realUser);
                        PersistenceContext.getCurrent().commitTransaction();
                        // dont close context here. the message processor will do it
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                    } catch (TransactionException e) {
                        logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                    } catch (ObjectModelException e) {
                        logger.log(Level.WARNING, "transaction error around forbidCertReset", e);
                    }
                    return realUser;
                } else {
                    String err = "Failed to authenticate user " + realUser.getDn() + " using a client certificate " +
                                 "(request certificate doesn't match database's)";
                    logger.warning(err);
                    throw new InvalidClientCertificateException( err );
                }
            } else {
                String err = "Certificate for " + realUser.getDn() + " is not an X509Certificate";
                logger.warning(err);
                throw new InvalidClientCertificateException( err );
            }
        } else {

            String msg = "Attempt to authenticate using unsupported method on this provider: " + pc.getFormat();
            logger.log(Level.SEVERE, msg);
            throw new AuthenticationException(msg);
        }
    }

    public boolean isReadOnly() {
        return true;
    }

    /**
     * searches the ldap provider for identities
     *
     * @param types any combination of EntityType.USER and or EntityType.GROUP
     * @param searchString the search string for the users and group names, use "*" for all
     * @return a collection containing EntityHeader objects
     */
    public Collection search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) {
            throw new IllegalArgumentException("must pass at least one type");
        }
        boolean wantUsers = false;
        boolean wantGroups = false;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == EntityType.USER) wantUsers = true;
            else if (types[i] == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) {
            throw new IllegalArgumentException("types must contain users and or groups");
        }
        Collection output = new TreeSet(new EntityHeaderComparator());
        String filter = null;
        try
        {
            NamingEnumeration answer = null;
            // search string for users and or groups based on passed types wanted
            if (wantUsers && wantGroups) {
                filter = "(|" + userSearchFilterWithParam(searchString) + groupSearchFilterWithParam(searchString) + ")";
            } else if (wantUsers) {
                filter = userSearchFilterWithParam(searchString);
            } else if (wantGroups) {
                filter = groupSearchFilterWithParam(searchString);
            }
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = getBrowseContext(cfg);
            answer = context.search(cfg.getSearchBase(), filter, sc);
            while (answer.hasMore()) {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                // set the dn (unique id)
                String dn = sr.getName() + "," + cfg.getSearchBase();
                EntityHeader header = searchResultToHeader(sr, dn);
                // if we successfully constructed a header, add it to result list
                if (header != null) output.add(header);
                else logger.warning("objectclass not supported for dn=" + dn);
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            logger.log(Level.SEVERE, "error searching with filter: " + filter, e);
        }
        return output;
    }

    public String getAuthRealm() {
        return null;
    }

    /**
     * builds a search filter for all user object classes based on the config object
     */
    public String userSearchFilterWithParam(String param) {
        if (cfg == null) throw new IllegalStateException("this provider needs a config!");
        StringBuffer output = new StringBuffer("(|");
        UserMappingConfig[] userTypes = cfg.getUserMappings();
        for (int i = 0; i < userTypes.length; i++) {
            output.append("(&" +
                            "(objectClass=" + userTypes[i].getObjClass() + ")" +
                            "(" + userTypes[i].getLoginAttrName() + "=" + param + ")" +
                          ")");
            output.append("(&" +
                            "(objectClass=" + userTypes[i].getObjClass() + ")" +
                            "(" + userTypes[i].getNameAttrName() + "=" + param + ")" +
                          ")");
        }
        output.append(")");
        return output.toString();
    }

    /**
     * builds a search filter for all group object classes based on the config object
     */
    public String groupSearchFilterWithParam(String param) {
        if (cfg == null) throw new IllegalStateException("this provider needs a config!");
        StringBuffer output = new StringBuffer("(|");
        GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
        for (int i = 0; i < groupTypes.length; i++) {
            output.append("(&" +
                            "(objectClass=" + groupTypes[i].getObjClass() + ")" +
                            "(" + groupTypes[i].getNameAttrName() + "=" + param + ")" +
                          ")");
        }
        output.append(")");
        return output.toString();
    }

    public static DirContext getBrowseContext(LdapIdentityProviderConfig config) throws NamingException {
        Hashtable env = new Hashtable();
        env.put( "java.naming.ldap.version", "3" );
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        Object temp = config.getLdapUrl();
        if ( temp != null ) env.put(Context.PROVIDER_URL, temp );
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.timeout", "30000" );
        String dn = config.getBindDN();
        if ( dn != null && dn.length() > 0 ) {
            String pass = config.getBindPasswd();
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, dn );
            env.put( Context.SECURITY_CREDENTIALS, pass );
        }
        // Create the initial directory context.
        return new InitialDirContext(env);
    }

    /**
     * determines whether the SearchResult contains a User or a Group. this is a utility method used throughout
     * this package
     * @param sr
     * @return EntityType.USER, EntityType.GROUP, or EntityType.UNDEFINED
     */
    EntityHeader searchResultToHeader(SearchResult sr, String dn) {
        Attributes atts = sr.getAttributes();
        // is it user or group ?
        Attribute objectclasses = atts.get("objectclass");
        // check if it's a user
        UserMappingConfig[] userTypes = cfg.getUserMappings();
        for (int i = 0; i < userTypes.length; i ++) {
            String userclass = userTypes[i].getObjClass();
            if (attrContainsCaseIndependent(objectclasses, userclass)) {
                Object tmp = null;
                try {
                    tmp = extractOneAttributeValue(atts, userTypes[i].getLoginAttrName());
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "cannot extract user login", e);
                    tmp = null;
                }
                if (tmp != null) {
                    return new EntityHeader(dn, EntityType.USER, tmp.toString(), null);
                } else {
                    return null;
                }
            }
        }
        // check that it's a group
        GroupMappingConfig[] groupTypes = cfg.getGroupMappings();
        for (int i = 0; i < groupTypes.length; i ++) {
            if (attrContainsCaseIndependent(objectclasses, groupTypes[i].getObjClass())) {
                String groupName = null;
                Attribute valuesWereLookingFor = atts.get(groupTypes[i].getNameAttrName());
                if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                    try {
                        groupName = valuesWereLookingFor.get(0).toString();
                    } catch (NamingException e) {
                        logger.warning("cannot extract name from this group");
                    }
                }
                if (groupName == null) groupName = dn;
                return new EntityHeader(dn, EntityType.GROUP, groupName, null);
            }
        }
        return null;
    }

    static boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        if (attr.contains(valueToLookFor)) return true;
        if (attr.contains(valueToLookFor.toLowerCase())) return true;
        return false;
    }

    static Object extractOneAttributeValue(Attributes attributes, String attrName) throws NamingException {
        Attribute valuesWereLookingFor = attributes.get(attrName);
        if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                return valuesWereLookingFor.get(0);
        }
        return null;
    }

    private LdapIdentityProviderConfig cfg;
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private MessageDigest _md5;
    private LdapUserManager userManager;
    private LdapGroupManager groupManager;
}
