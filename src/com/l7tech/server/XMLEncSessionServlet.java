package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.Session;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic;
import org.apache.axis.encoding.Base64;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet allows a client to get a set of 2 symmetric keys for use in the XML Enc assertion(s).
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 27, 2003<br/>
 * $Id$
 */
public class XMLEncSessionServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
    }

    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        // ALL REQUESTS HERE MUST COME THROUGH SSL
        if (!httpServletRequest.isSecure()) {
            // send error back axing to come back through ssl
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Request must come through SSL.");
            logger.warning("Someone tried to get a xml enc session through insecure port.");
            return;
        }

        // ALL REQUESTS MUST BE AUTHENTICATED
        User user = null;
        user = authenticateRequest(httpServletRequest);
        if (user == null) {
            // send error back with a hint that credentials should be provided
            logger.warning("Invalid or no credentials provided.");
            httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Must provide valid credentials.");
            return;
        }

        // GENERATE A SESSION
        Session newSession = SessionManager.getInstance().createNewSession();
        if (newSession == null) {
            logger.warning("Unable to create a session.");
            httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create session.");
            return;
        }

        // SEND BACK SESSION
        outputSession(newSession, httpServletResponse);
        logger.info("XML-ENc session created for user " + user.getLogin() + ". session id = " + newSession.getId());
    }

    private void outputSession(Session sessionToOutput, HttpServletResponse response) throws IOException {
        response.setHeader(SecureSpanConstants.HttpHeaders.XML_SESSID_HEADER_NAME, Long.toString(sessionToOutput.getId()));
        String b64edkey = Base64.encode(sessionToOutput.getKeyReq());
        response.setHeader(SecureSpanConstants.HttpHeaders.HEADER_KEYREQ, b64edkey);
        b64edkey = Base64.encode(sessionToOutput.getKeyRes());
        response.setHeader(SecureSpanConstants.HttpHeaders.HEADER_KEYRES, b64edkey);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getOutputStream().println("ok");
        response.getOutputStream().close();
    }

    /**
     * look for basic creds in the request and authenticate them
     */
    private User authenticateRequest(HttpServletRequest req) throws IOException {
        // get the credentials
        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader == null || authorizationHeader.length() < 1) {
            logger.warning("No authorization header found.");
            return null;
        }

        ServerHttpBasic httpBasic = new ServerHttpBasic( new HttpBasic() );
        LoginCredentials creds = null;
        try {
            creds = httpBasic.findCredentials(authorizationHeader);
        } catch (CredentialFinderException e) {
            logger.log(Level.SEVERE, "Exception looking for exception.", e);
            return null;
        }
        if (creds == null) {
            logger.warning("No credentials found.");
            return null;
        }
        // we have credentials, attempt to authenticate them
        IdentityProviderConfigManager configManager = new IdProvConfManagerServer();
        Collection providers = null;
        try {
            providers = configManager.findAllIdentityProviders();
            for (Iterator i = providers.iterator(); i.hasNext();) {
                IdentityProvider provider = (IdentityProvider) i.next();
                try {
                    User u = provider.authenticate(creds);
                    logger.fine("Authentication successful for user " + creds.getLogin() + " on identity provider: " + provider.getConfig().getName());
                    return u;
                } catch (AuthenticationException e) {
                    logger.finer("Authentication failed for user " + creds.getLogin() + " on identity provider: " + provider.getConfig().getName());
                    continue;
                }
            }
            logger.warning("user " + creds.getLogin() + " did not provide valid credentials.");
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Exception getting id providers.", e);
            return null;
        } finally {
            try {
                endTransaction();
            } catch ( SQLException se ) {
                logger.log(Level.WARNING, null, se);
            } catch ( TransactionException te ) {
                logger.log(Level.WARNING, null, te);
            }
        }
        logger.warning("Creds do not authenticate against any registered id provider.");
        return null;
    }

    private void endTransaction() throws java.sql.SQLException, TransactionException {
        PersistenceContext context = PersistenceContext.getCurrent();
        context.commitTransaction();
        context.close();
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
}
