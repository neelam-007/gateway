/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractLdapIdentityProviderServer implements IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        doInitialize( config );
        logger = LogManager.getInstance().getSystemLogger();
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new RuntimeException( e );
        }
    }

    protected abstract void doInitialize( IdentityProviderConfig config );
    protected abstract AbstractLdapConstants getConstants();

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public User authenticate( LoginCredentials pc ) throws AuthenticationException {
        if (!valid) {
            String msg = "invalid id provider asked to authenticate";
            logger.info(msg);
            throw new AuthenticationException(msg);
        }
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

    public boolean isReadOnly() { return true; }

    public void invalidate() {
        valid = false;
        groupManager.invalidate();
        userManager.invalidate();
    }

    /**
     * searches for users and groups whose name (cn) match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    public Collection search(EntityType[] types, String searchString) throws FindException {
        AbstractLdapConstants constants = getConstants();
        if (!valid) {
            logger.info("invalid id provider asked for search");
            throw new FindException("provider invalidated");
        }
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
        // todo: get a sorted result from ldap server instead of sorting in collection
        Collection output = new TreeSet(new EntityHeaderComparator());
        try
        {
            NamingEnumeration answer = null;
            // search string for users and or groups based on passed types wanted
            String filter = null;
            if (wantUsers && wantGroups) {
                filter = "(|" +
                             "(&" +
                                 "(|";
                String[] grpClasses = constants.groupObjectClass();
                for (int groupClassCnt = 0; groupClassCnt < grpClasses.length; groupClassCnt++) {
                    filter +=        "(objectclass=" + grpClasses[groupClassCnt] + ")";
                }
                filter +=            "(objectclass=" + constants.userObjectClass() + ")" +
                                 ")" +
                                 "(" + constants.userNameAttribute() + "=" + searchString + ")" +
                             ")" +
                             "(&" +
                                 "(objectClass=" + constants.oUObjClassName() + ")" +
                                 "(" + constants.oUObjAttrName() + "=" + searchString + ")" +
                             ")" +
                         ")";
            } else if (wantUsers) {
                filter = "(&" +
                           "(objectclass=" + constants.userObjectClass() + ")" +
                           "(" + constants.userNameAttribute() + "=" + searchString + ")" +
                         ")";
            } else if (wantGroups) {
                filter = "(|" +
                            "(&" +
                              "(|";
                String[] grpClasses = constants.groupObjectClass();
                for (int groupClassCnt = 0; groupClassCnt < grpClasses.length; groupClassCnt++) {
                    filter +=   "(objectclass=" + grpClasses[groupClassCnt] + ")";
                }
                filter +=     ")" +
                              "(" + constants.groupNameAttribute() + "=" + searchString + ")" +
                            ")" +
                            "(&" +
                              "(objectClass=" + constants.oUObjClassName() + ")" +
                              "(" + constants.oUObjAttrName() + "=" + searchString + ")" +
                            ")" +
                         ")";
            }
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            DirContext context = browseContext();
            answer = context.search(cfg.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE), filter, sc);
            while (answer.hasMore())
            {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                // set the dn (unique id)
                String dn = sr.getName() + "," + cfg.getProperty(LdapConfigSettings.LDAP_SEARCH_BASE);
                Attributes atts = sr.getAttributes();
                // is it user or group ?
                EntityType type = getSearchResultType(sr);
                EntityHeader header = null;
                // construct header accordingly
                if (type == EntityType.GROUP) {
                    String groupName = getGroupName(sr);
                    if (groupName == null) groupName = dn;
                    header = new EntityHeader(dn, EntityType.GROUP, groupName, null);
                } else if (type == EntityType.USER) {
                    Object tmp = LdapManager.extractOneAttributeValue(atts, getConstants().userLoginAttribute());
                    if (tmp != null) {
                        header = new EntityHeader(dn, EntityType.USER, tmp.toString(), null);
                    } else {
                        logger.info("A user found in the search did not have a login value: " + dn);
                    }
                } else {
                    logger.warning("objectclass not supported for dn=" + dn);
                }
                // if we successfully constructed a header, add it to result list
                if (header != null) output.add(header);
            }
            if (answer != null) answer.close();
            context.close();
        } catch (NamingException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return output;
    }

    private String getGroupName(SearchResult sr) {
        String output = null;
        Attributes atts = sr.getAttributes();
        // is it user or group ?
        Attribute objectclasses = atts.get("objectclass");
        // check for OU group
        String groupclass = getConstants().oUObjClassName();
        if (attrContainsCaseIndependent(objectclasses, groupclass)) {
            // extract ou value
            Attribute valuesWereLookingFor = atts.get(getConstants().oUObjAttrName());
            if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                try {
                    output = valuesWereLookingFor.get(0).toString();
                } catch (NamingException e) {
                    logger.warning("cannot extract cn from this group");
                }
            }
        } else {
            // extract the cn
            Attribute valuesWereLookingFor = atts.get(getConstants().userNameAttribute());
            if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                try {
                    output = valuesWereLookingFor.get(0).toString();
                } catch (NamingException e) {
                    logger.warning("cannot extract cn from this group");
                }
            }
        }
        return output;
    }

    /**
     * determines whether the SearchResult contains a User or a Group
     * @param sr
     * @return EntityType.USER, EntityType.GROUP, or EntityType.UNDEFINED
     */
    private EntityType getSearchResultType(SearchResult sr) {
        Attributes atts = sr.getAttributes();
        // is it user or group ?
        Attribute objectclasses = atts.get("objectclass");
        // check if it's a user
        String userclass = getConstants().userObjectClass();
        if (attrContainsCaseIndependent(objectclasses, userclass)) return EntityType.USER;
        // check that it's a group
        String[] groupclasses = getConstants().groupObjectClass();
        for (int i = 0; i < groupclasses.length; i++) {
            if (attrContainsCaseIndependent(objectclasses, groupclasses[i])) return EntityType.GROUP;
        }
        // check for OU group
        String groupclass = getConstants().oUObjClassName();
        if (attrContainsCaseIndependent(objectclasses, groupclass)) return EntityType.GROUP;
        return EntityType.UNDEFINED;
    }

    private boolean attrContainsCaseIndependent(Attribute attr, String valueToLookFor) {
        if (attr.contains(valueToLookFor)) return true;
        if (attr.contains(valueToLookFor.toLowerCase())) return true;
        return false;
    }

    private DirContext browseContext() throws NamingException {
        return LdapManager.getBrowseContext( cfg );
    }

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public GroupManager getGroupManager() {
        return groupManager;
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    protected IdentityProviderConfig cfg = null;
    protected AbstractLdapGroupManagerServer groupManager = null;
    protected AbstractLdapUserManagerServer userManager = null;
    protected volatile boolean valid = true;
    protected Logger logger = null;
    protected MessageDigest _md5;
}
