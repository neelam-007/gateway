package com.l7tech.identity.ldap;

import com.l7tech.identity.*;
import com.l7tech.logging.LogManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.common.util.HexUtils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Collection;
import java.util.TreeSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 13, 2003
 *
 */
public class LdapIdentityProviderServer implements IdentityProvider {
    public void initialize(IdentityProviderConfig config) {
        if (config.type() != IdentityProviderType.LDAP) {
            throw new IllegalArgumentException("Expecting Ldap config type");
        }
        cfg = config;
        groupManager = new LdapGroupManagerServer(cfg);
        userManager = new LdapUserManagerServer(cfg);
        logger = LogManager.getInstance().getSystemLogger();
        try {
            _md5 = MessageDigest.getInstance( "MD5" );
        } catch (NoSuchAlgorithmException e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new RuntimeException( e );
        }
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
                filter = "(&(|(objectclass=" + LdapGroupManagerServer.GROUP_OBJCLASS + ")" +
                         "(objectclass=" + LdapUserManagerServer.USER_OBJCLASS + "))" +
                         "(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            } else if (wantUsers) {
                filter = "(&(objectclass=" + LdapUserManagerServer.USER_OBJCLASS + ")" +
                         "(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            } else if (wantGroups) {
                filter = "(&(objectclass=" + LdapGroupManagerServer.GROUP_OBJCLASS + ")" +
                         "(" + LdapManager.NAME_ATTR_NAME + "=" + searchString + "))";
            }
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            java.util.Hashtable env = new java.util.Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, cfg.getProperty(LdapConfigSettings.LDAP_HOST_URL));
            DirContext context = new InitialDirContext(env);
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
                if (objectclasses.contains(LdapGroupManagerServer.GROUP_OBJCLASS)) {
                    header = new EntityHeader(dn, EntityType.GROUP, dn, null);
                } else if (objectclasses.contains(LdapUserManagerServer.USER_OBJCLASS)) {
                    Object tmp = LdapManager.extractOneAttributeValue(atts, LdapManager.LOGIN_ATTR_NAME);
                    if (tmp != null) {
                        header = new EntityHeader(dn, EntityType.USER, tmp.toString(), null);
                    }
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

    public String getAuthRealm() {
        return HttpDigest.REALM;
    }


    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityProviderConfig cfg = null;
    private LdapGroupManagerServer groupManager = null;
    private LdapUserManagerServer userManager = null;
    private volatile boolean valid = true;
    private Logger logger = null;
    private MessageDigest _md5;
}
