package com.l7tech.identity.internal;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import org.apache.log4j.Category;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

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

    public boolean authenticate(Principal principal, byte[] credentials) {
        if ( !(principal instanceof User) ) return false;
        User authUser = (User)principal;
        String login = authUser.getLogin();
        try {
            User dbUser = userManager.findByLogin( login );
            if ( dbUser == null ) {
                _log.info( "Couldn't find user with login " + login );
                return false;
            } else {
                String dbPassHash = dbUser.getPassword();
                String authPassHash = User.encodePasswd( login, new String( credentials, ENCODING ) );
                if ( dbPassHash.equals( authPassHash ) ) return true;
                _log.info( "Incorrect password for login " + login );
                return false;
            }
        } catch ( FindException fe ) {
            _log.error( fe );
            return false;
        } catch ( UnsupportedEncodingException uee ) {
            _log.error( uee );
            throw new RuntimeException( uee );
        }
    }

    public IdentityProviderConfig getConfig() {
        return cfg;
    }

    public boolean isReadOnly() { return false; }

    private Category _log = Category.getInstance( getClass() );
    private IdentityProviderConfig cfg;
    private InternalUserManagerServer userManager;
    private InternalGroupManagerServer groupManager;
}
