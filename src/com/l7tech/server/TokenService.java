package com.l7tech.server;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;

/**
 * Handles WS Trust RequestSecurityToken requests as well as SAML token requests.
 * The request is originally received by the TokenServiceServlet.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 6, 2004<br/>
 * $Id$<br/>
 */
public class TokenService {
    public interface CredentialsAuthenticator {
        User authenticate(LoginCredentials creds);
    }

    /**
     * Handles the request for a security token (either secure conversation context or saml thing).
     * @param request the request for the secure conversation context
     * @param authenticator resolved credentials such as an X509Certificate to an actual user to associate the context with
     * @return
     */
    public Document respondToRequestSecurityToken(Document request, CredentialsAuthenticator authenticator) {
        // todo
        return null;
    }
}
