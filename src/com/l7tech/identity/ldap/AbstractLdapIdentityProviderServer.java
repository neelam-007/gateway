/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public void authenticate( PrincipalCredentials pc ) throws AuthenticationException {
        if (!valid) {
            String msg = "invalid id provider asked to authenticate";
            logger.info(msg);
            throw new AuthenticationException(msg);
        }
        User realUser = null;
        try {
            realUser = userManager.findByLogin(pc.getUser().getLogin());
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
            boolean res = userManager.authenticateBasic(realUser.getName(), new String(pc.getCredentials()));
            if (res) {
                pc.getUser().copyFrom(realUser);
                // success
                return;
            }
            logger.info("credentials did not authenticate for " + pc.getUser().getLogin());
            throw new BadCredentialsException("credentials did not authenticate");
        } else if (pc.getFormat() == CredentialFormat.DIGEST) {
            String dbPassHash = realUser.getPassword();
            byte[] credentials = pc.getCredentials();
            Map authParams = (Map)pc.getPayload();
            if (authParams == null) {
                String msg = "No Digest authentication parameters found in PrincipalCredentials payload!";
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

            User authUser = pc.getUser();
            if ( response.equals( expectedResponse ) ) {
                logger.info("User " + authUser.getLogin() + " authenticated successfully with digest credentials.");
                authUser.copyFrom( realUser );
                return;
            } else {
                String msg = "User " + authUser.getLogin() + " failed to match.";
                logger.warning(msg);
                throw new AuthenticationException(msg);
            }
        } else {
            logger.log(Level.SEVERE, "Attempt to authenticate using unsupported method" + pc.getFormat());
            throw new AuthenticationException("Only cleartext or digest credentials are currently supported!");
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
                                 "(|" +
                                     "(objectclass=" + constants.groupObjectClass() + ")" +
                                     "(objectclass=" + constants.userObjectClass() + ")" +
                                 ")" +
                                 "(" + constants.userNameAttribute() + "=" + searchString + ")" +
                             ")" +
                             "(&" +
                                 "(objectClass=" + AbstractLdapConstants.oUObjClassName() + ")" +
                                 "(" + AbstractLdapConstants.oUObjAttrName() + "=" + searchString + ")" +
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
                              "(objectclass=" + constants.groupObjectClass() + ")" +
                              "(" + constants.groupNameAttribute() + "=" + searchString + ")" +
                            ")" +
                            "(&" +
                              "(objectClass=" + AbstractLdapConstants.oUObjClassName() + ")" +
                              "(" + AbstractLdapConstants.oUObjAttrName() + "=" + searchString + ")" +
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
                Attribute objectclasses = atts.get("objectclass");
                EntityHeader header = null;
                // construct header accordingly
                if (objectclasses.contains( getConstants().groupObjectClass() )) {
                    header = new EntityHeader(dn, EntityType.GROUP, dn, null);
                } else if (objectclasses.contains( getConstants().userObjectClass() )) {
                    Object tmp = LdapManager.extractOneAttributeValue(atts, getConstants().userLoginAttribute() );
                    if (tmp != null) {
                        header = new EntityHeader(dn, EntityType.USER, tmp.toString(), null);
                    }
                } else if (objectclasses.contains(AbstractLdapConstants.oUObjClassName()) ||
                           objectclasses.contains(AbstractLdapConstants.oUObjClassName().toLowerCase())) {
                    header = new EntityHeader(dn, EntityType.GROUP, dn, null);
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
