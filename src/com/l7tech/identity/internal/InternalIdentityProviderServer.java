package com.l7tech.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.logging.LogManager;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFormat;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

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

                if ( pc.getFormat() == CredentialFormat.CLEARTEXT )
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

    private IdentityProviderConfig cfg;
    private InternalUserManagerServer userManager;
    private InternalGroupManagerServer groupManager;
}
