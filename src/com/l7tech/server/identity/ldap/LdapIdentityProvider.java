package com.l7tech.server.identity.ldap;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    /** LDAP connection attempts will fail after 5 seconds' wait */
    public static final String LDAP_CONNECT_TIMEOUT = new Integer(5 * 1000).toString();
    /** An unused LDAP connection will be closed after 30 seconds of inactivity */
    public static final String LDAP_POOL_IDLE_TIMEOUT = new Integer(30 * 1000).toString();

    public LdapIdentityProvider(IdentityProviderConfig config) {
        try {
            this.config = (LdapIdentityProviderConfig)config;
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        userManager = new LdapUserManager(this.config, this);
        groupManager = new LdapGroupManager(this.config, this);
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException, IOException {
        LdapUser realUser = null;
        realUser = (LdapUser)userManager.findByLogin( pc.getLogin());
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
        DirContext context = null;
        String filter = null;
        try
        {
            NamingEnumeration answer = null;
            // search string for users and or groups based on passed types wanted
            if (wantUsers && wantGroups) {
                String userFilter = userSearchFilterWithParam(searchString);
                String grpFilter = groupSearchFilterWithParam(searchString);
                // no group mapping is now allowed
                if (grpFilter == null) {
                    filter = userFilter;
                } else filter = "(|" + userFilter + grpFilter + ")";
            } else if (wantUsers) {
                filter = userSearchFilterWithParam(searchString);
            } else if (wantGroups) {
                filter = groupSearchFilterWithParam(searchString);
                // no group mapping is now allowed
                if (filter == null) {
                    return Collections.EMPTY_SET;
                }
            }
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            context = getBrowseContext(config);
            answer = context.search(config.getSearchBase(), filter, sc);
            while (answer.hasMore()) {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                // set the dn (unique id)
                String dn = sr.getName() + "," + config.getSearchBase();
                EntityHeader header = searchResultToHeader(sr, dn);
                // if we successfully constructed a header, add it to result list
                if (header != null) output.add(header);
                else logger.warning("objectclass not supported for dn=" + dn);
            }
            if (answer != null) answer.close();
        } catch (NamingException e) {
            logger.log(Level.WARNING, "error searching with filter: " + filter, e);
        } finally {
            if ( context != null ) {
                try {
                    context.close();
                } catch ( NamingException e ) {
                    logger.log( Level.WARNING, "Caught NamingException while closing LDAP Context", e );
                }
            }
        }
        return output;
    }

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    /**
     * builds a search filter for all user object classes based on the config object
     */
    public String userSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");
        StringBuffer output = new StringBuffer("(|");
        UserMappingConfig[] userTypes = config.getUserMappings();
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
     * @return the search filter or null if no group mappings are declared for this config
     */
    public String groupSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        if (groupTypes == null || groupTypes.length <= 0) return null;
        StringBuffer output = new StringBuffer("(|");
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
        UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
        env.put( "java.naming.ldap.version", "3" );
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        Object temp = config.getLdapUrl();
        if ( temp != null ) env.put(Context.PROVIDER_URL, temp );
        env.put("com.sun.jndi.ldap.connect.pool", "true");
        env.put("com.sun.jndi.ldap.connect.timeout", LDAP_CONNECT_TIMEOUT );
        env.put("com.sun.jndi.ldap.connect.pool.timeout", LDAP_POOL_IDLE_TIMEOUT );
        String dn = config.getBindDN();
        if ( dn != null && dn.length() > 0 ) {
            String pass = config.getBindPasswd();
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.SECURITY_PRINCIPAL, dn );
            env.put( Context.SECURITY_CREDENTIALS, pass );
        }
        env.lock();
        // Create the initial directory context.
        return new InitialDirContext(env);
    }

    public void test() throws InvalidIdProviderCfgException {
        // make sure we can connect
        DirContext context = null;
        try {
            context = getBrowseContext(config);
        } catch (NamingException e) {
            // note. i am not embedding the NamingException because it sometimes
            // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
            String msg = "cannot connect to this directory";
            logger.log(Level.INFO, "ldap config test failure " + msg, e);
            throw new InvalidIdProviderCfgException(msg);
        }

        NamingEnumeration answer = null;
        String filter = null;
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        // make sure the base DN is valid and contains at least one entry
        try {
            answer = context.search(config.getSearchBase(), "(objectClass=*)", sc);
        } catch (NamingException e) {
            String msg = "Cannot search using base: " + config.getSearchBase();
            logger.log(Level.INFO, "ldap config test failure " + msg, e);

            // cleanup and leave test with exception
            try {
                if (answer != null) answer.close();
            } catch (NamingException e2) {
                logger.finest("Could not close answer " + e2.getMessage());
            }
            try {
                context.close();
            } catch (NamingException e2) {
                logger.finest("Could not close context " + e2.getMessage());
            }

            throw new InvalidIdProviderCfgException(msg);
        }

        // check user mappings. make sure they work
        boolean atLeastOneUser = false;
        UserMappingConfig[] userTypes = config.getUserMappings();
        Collection offensiveUserMappings = new ArrayList();
        Collection userMappingsWithoutLoginAttribute = new ArrayList();
        for (int i = 0; i < userTypes.length; i++) {
            if (userTypes[i].getLoginAttrName() == null || userTypes[i].getLoginAttrName().length() < 1) {
                userMappingsWithoutLoginAttribute.add(userTypes[i]);
                continue;
            }
            filter = "(|" +
                         "(&" +
                            "(objectClass=" + userTypes[i].getObjClass() + ")" +
                            "(" + userTypes[i].getLoginAttrName() + "=*)" +
                         ")" +
                         "(&" +
                            "(objectClass=" + userTypes[i].getObjClass() + ")" +
                            "(" + userTypes[i].getNameAttrName() + "=*)" +
                         ")" +
                      ")";
            try {
                answer = context.search(config.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String dn = sr.getName() + "," + config.getSearchBase();
                    EntityHeader header = searchResultToHeader(sr, dn);
                    // if we successfully constructed a header, add it to result list
                    if (header != null) {
                        atLeastOneUser = true;
                        break;
                    }
                }
                answer.close();
            } catch (NamingException e) {
                offensiveUserMappings.add(userTypes[i]);
                logger.log(Level.FINE, "error testing user mapping" + userTypes[i].getObjClass(), e);
            }
        }

        // check group mappings. make sure they work
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        Collection offensiveGroupMappings = new ArrayList();
        boolean atLeastOneGroup = false;
        for (int i = 0; i < groupTypes.length; i++) {
            filter = "(&" +
                         "(objectClass=" + groupTypes[i].getObjClass() + ")" +
                         "(" + groupTypes[i].getNameAttrName() + "=*)" +
                     ")";
            try {
                answer = context.search(config.getSearchBase(), filter, sc);
                while (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    // set the dn (unique id)
                    String dn = sr.getName() + "," + config.getSearchBase();
                    EntityHeader header = searchResultToHeader(sr, dn);
                    // if we successfully constructed a header, add it to result list
                    if (header != null) {
                        atLeastOneGroup = true;
                        break;
                    }
                }
                answer.close();
            } catch (NamingException e) {
                offensiveGroupMappings.add(groupTypes[i]);
                logger.log(Level.FINE, "error testing group mapping" + groupTypes[i].getObjClass(), e);
            }
        }
        try {
            context.close();
        } catch (NamingException e) {
            logger.log(Level.INFO, "error closing context", e);
        }

        // merge all errors in a special report
        StringBuffer error = new StringBuffer();

        if (userMappingsWithoutLoginAttribute.size() > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("The following user mapping(s) do not define login attribute.");
            for (Iterator iterator = userMappingsWithoutLoginAttribute.iterator(); iterator.hasNext();) {
                UserMappingConfig userMappingConfig = (UserMappingConfig) iterator.next();
                error.append(" " + userMappingConfig.getObjClass());
            }
        }

        if (offensiveUserMappings.size() > 0 || offensiveGroupMappings.size() > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("The following mappings caused errors:");
            for (Iterator iterator = offensiveUserMappings.iterator(); iterator.hasNext();) {
                UserMappingConfig userMappingConfig = (UserMappingConfig) iterator.next();
                error.append(" User mapping " + userMappingConfig.getObjClass());
            }
            for (Iterator iterator = offensiveGroupMappings.iterator(); iterator.hasNext();) {
                GroupMappingConfig groupMappingConfig = (GroupMappingConfig) iterator.next();
                error.append(" Group mapping " + groupMappingConfig.getObjClass());

            }
        }

        if (!atLeastOneUser) {
            if (error.length() > 0) error.append('\n');
            error.append("This configuration did not yeild any users");
        }

        if (!atLeastOneGroup && groupTypes.length > 0) {
            if (error.length() > 0) error.append('\n');
            error.append("This configuration did not yeild any group");
        }


        if (error.length() > 0) {
            logger.fine("Test produced following error(s): " + error.toString());
            throw new InvalidIdProviderCfgException(error.toString());
        } else logger.finest("this ldap config was tested successfully");
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
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (int i = 0; i < userTypes.length; i ++) {
            String userclass = userTypes[i].getObjClass();
            if (attrContainsCaseIndependent(objectclasses, userclass)) {
                Object tmp = null;
                String login = null;
                try {
                    tmp = extractOneAttributeValue(atts, userTypes[i].getLoginAttrName());
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "cannot extract user login", e);
                    tmp = null;
                }
                if (tmp != null) {
                    login = tmp.toString();
                }
                // if description attribute present, use it
                String description = null;
                try {
                    tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                if (login != null) {
                    return new EntityHeader(dn, EntityType.USER, login, description);
                } else {
                    return null;
                }
            }
        }
        // check that it's a group
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
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
                // if description attribute present, use it
                Object tmp = null;
                String description = null;
                try {
                    tmp = extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                return new EntityHeader(dn, EntityType.GROUP, groupName, description);
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

    public static final String DESCRIPTION_ATTRIBUTE_NAME = "description";

    private final LdapIdentityProviderConfig config;
    private final MessageDigest _md5;
    private final LdapUserManager userManager;
    private final LdapGroupManager groupManager;

    private final Logger logger = Logger.getLogger(getClass().getName());
}
