package com.l7tech.server.wstrust;

import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 12-Aug-2004
 */
public class CredentialsFinder implements Handler {
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Message chain handler for message invocation.
     *
     * @param i The invocation.
     */
    public void invoke(MessageInvocation i) throws Throwable {
        final HttpServletRequest req = (HttpServletRequest)i.getMessageContext().getProperty("javax.servlet.http.HttpServletRequest");
        if (req != null) {
            LoginCredentials creds = findCredentialsBasic(req);
            logger.fine("http basic credentials found");
            i.setCredentials(creds);
        } else {

        }
        i.invokeNext();
    }

    /**
     * Find the credentials using http basic method.
     *
     * @param req the servlet request
     * @return the <code>LoginCredentials</code> or null if not found
     */
    private LoginCredentials findCredentialsBasic(HttpServletRequest req) {
        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.length() < 1) {
            logger.fine("No BASIC auth authorization header found.");
            return null;
        }

        ServerHttpBasic httpBasic = new ServerHttpBasic(new HttpBasic());
        LoginCredentials creds = null;
        try {
            creds = httpBasic.findCredentials(authorizationHeader);
            if (creds == null) {
                logger.warning("No credentials found.");
            }
            return creds;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception looking for exception.", e);
        }
        return null;
    }

}
