package com.l7tech.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeaderComparator;
import com.l7tech.logging.LogManager;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.TreeSet;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

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

    public boolean authenticate( PrincipalCredentials pc ) {
        User authUser = pc.getUser();
        byte[] credentials = pc.getCredentials();

        String login = authUser.getLogin();
        try {
            User dbUser = userManager.findByLogin( login );
            if ( dbUser == null ) {
                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Couldn't find user with login " + login);
                return false;
            } else {
                CredentialFormat format = pc.getFormat();

                if ( format == CredentialFormat.CLIENTCERT ) {
                    Certificate dbCert = null;
                    Object maybeCert = pc.getPayload();

                    if ( maybeCert == null ) {
                        _log.log( Level.SEVERE, "Request was supposed to contain a certificate, but does not" );
                    } else {
                        dbCert = userManager.retrieveUserCert( login );
                        if ( dbCert == null ) {
                            _log.log( Level.WARNING, "No certificate found for user " + login );
                            return false;
                        }
                    }

                    if ( maybeCert instanceof X509Certificate ) {
                        X509Certificate pcCert = (X509Certificate)maybeCert;
                        _log.log( Level.INFO, "Authenticated user " + login + " using an SSL client certificate" );

                        return ( pcCert.equals( dbCert ) );
                    } else {
                        _log.log( Level.WARNING, "Certificate for " + login + " is not an X509Certificate" );
                        return false;
                    }
                } else {
                    String dbPassHash = dbUser.getPassword();
                    String authPassHash = null;

                    if ( format == CredentialFormat.CLEARTEXT ) {
                        authPassHash = User.encodePasswd( login, new String( credentials, ENCODING ) );
                    } else if ( format == CredentialFormat.DIGEST ) {
                        authPassHash = new String( credentials, ENCODING );
                    } else {
                        throwUnsupportedCredentialFormat(format);
                    }

                    if ( dbPassHash.equals( authPassHash ) ) {
                        authUser.copyFrom( dbUser );
                        return true;
                    }

                    LogManager.getInstance().getSystemLogger().log(Level.INFO, "Incorrect password for login " + login);

                    return false;
                }
            }
        } catch ( FindException fe ) {
            _log.log(Level.SEVERE, null, fe);
            return false;
        } catch ( UnsupportedEncodingException uee ) {
            _log.log(Level.SEVERE, null, uee);
            throw new RuntimeException( uee );
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

    private IdentityProviderConfig cfg;
    private InternalUserManagerServer userManager;
    private InternalGroupManagerServer groupManager;

    private Logger _log = LogManager.getInstance().getSystemLogger();
}
