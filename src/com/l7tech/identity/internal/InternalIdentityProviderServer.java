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
import java.util.Collection;
import java.util.TreeSet;
import java.security.cert.Certificate;

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
                String dbPassHash = dbUser.getPassword();
                String authPassHash;

                if (pc.getFormat() == CredentialFormat.CLIENTCERT ||  pc.getFormat() == CredentialFormat.CLIENTCERT_X509_ASN1_DER) {
                    Certificate localcert = userManager.retrieveUserCert(Long.toString(dbUser.getOid()));
                    // todo, get the cert from principal compare cert
                    return true;
                }
                else if ( pc.getFormat() == CredentialFormat.CLEARTEXT )
                    authPassHash = User.encodePasswd( login, new String( credentials, ENCODING ) );
                else
                    authPassHash = new String( credentials, ENCODING );

                if ( dbPassHash.equals( authPassHash ) ) {
                    authUser.copyFrom( dbUser );
                    return true;
                }

                LogManager.getInstance().getSystemLogger().log(Level.INFO, "Incorrect password for login " + login);
                return false;
            }
        } catch ( FindException fe ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, fe);
            return false;
        } catch ( UnsupportedEncodingException uee ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, uee);
            throw new RuntimeException( uee );
        }
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
}
