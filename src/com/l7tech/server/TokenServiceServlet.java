package com.l7tech.server;

import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.IdentityProviderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The servlet handling WS Trust RequestSecurityToken requests.
 * The SSA requests such a token when it desires to establish a Secure Conversation.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 5, 2004<br/>
 * $Id$<br/>
 */
public class TokenServiceServlet extends HttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; context requests must use POST");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Document payload = null;
        try {
            payload = extractXMLPayload(req);
        } catch (ParserConfigurationException e) {
            String msg = "Could not parse payload as xml. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (SAXException e) {
            String msg = "Could not parse payload as xml. " + e.getMessage();
            logger.log(Level.WARNING, msg, e);
            sendBackNonSoapError(res, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        TokenService tokenService;
        try {
            X509Certificate a = KeystoreUtils.getInstance().getSslCert();
            PrivateKey i = KeystoreUtils.getInstance().getSSLPrivateKey();
            tokenService = new TokenService(i, a);
        } catch (CertificateException e) {
            String msg = "Error getting server cert/private key: " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (KeyStoreException e) {
            String msg = "Error getting server cert/private key: " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (IOException e){
            String msg = "Error getting server cert/private key: " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        }

        Document response = null;
        try {
            response = tokenService.respondToRequestSecurityToken(payload, authenticator(), req.getRemoteAddr());
        } catch (InvalidDocumentFormatException e) {
            String msg = "Request is not formatted as expected. " + e.getMessage();
            logger.log(Level.INFO, msg, e);
            sendBackNonSoapError(res, HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        } catch (TokenService.TokenServiceException e) {
            String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (ProcessorException e) {
            String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (BadSecurityContextException e) {
            String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (GeneralSecurityException e) {
            String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        } catch (AuthenticationException e) {
            sendBackNonSoapError(res, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }
        // dont let this ioexception fall through, this is a debugging nightmare!
        try {
            outputRequestSecurityTokenResponse(response, res);
            logger.finest("Sent back SecurityToken:" + XmlUtil.nodeToFormattedString(response));
        } catch (IOException e) {
            String msg = "Error printing result. " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        }
    }

    private final TokenService.CredentialsAuthenticator authenticator() {
        return new TokenService.CredentialsAuthenticator() {
            public User authenticate(LoginCredentials creds) throws AuthenticationException {
                IdentityProviderConfigManager idpcm = (IdentityProviderConfigManager)Locator.getDefault().
                                                        lookup(IdentityProviderConfigManager.class);
                User authenticatedUser = null;
                Collection providers = null;
                try {
                    // go through providers and try to authenticate the cert
                    providers = IdentityProviderFactory.findAllIdentityProviders(idpcm);
                    for (Iterator iterator = providers.iterator(); iterator.hasNext();) {
                        IdentityProvider provider = (IdentityProvider) iterator.next();
                        try {
                            User dude = provider.authenticate(creds);
                            if (dude != null) {
                                if (authenticatedUser != null) {
                                    throw new AuthenticationException("The cert used to sign this request is valid " +
                                                                      "on more than one provider. Secure conversation " +
                                                                      "contexts must be associated unambigously to one " +
                                                                      "user.");
                                } else {
                                    authenticatedUser = dude;
                                }
                            }
                        } catch (AuthenticationException e) {
                            logger.log(Level.INFO, "exception trying to authenticate credentials against " +
                                                   provider.getConfig().getName(), e);
                        } catch (IOException e) {
                            logger.log(Level.INFO, "excetion trying to authenticate credentials against " +
                                                   provider.getConfig().getName(), e);
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "could not get id provider from factory", e);
                    return null;
                }
                if (authenticatedUser == null) {
                    logger.fine("Credentials did not authenticate against any provider.");
                }
                return authenticatedUser;
            }
        };
    }

    private void outputRequestSecurityTokenResponse(Document requestSecurityTokenResponse,
                                                    HttpServletResponse res) throws IOException {
        res.setContentType("text/xml; charset=utf-8");
        ServletOutputStream os = res.getOutputStream();
        XmlUtil.nodeToOutputStream(requestSecurityTokenResponse, os);
        os.close();
    }

    private Document extractXMLPayload(HttpServletRequest req)
            throws IOException, ParserConfigurationException, SAXException {
        BufferedReader reader = new BufferedReader(req.getReader());
        DocumentBuilder parser = getDomParser();
        return parser.parse( new InputSource(reader));

    }

    private DocumentBuilder getDomParser() throws ParserConfigurationException {
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setEntityResolver(XmlUtil.getSafeEntityResolver());
        return builder;
    }


    private void sendBackNonSoapError(HttpServletResponse resp, int status, String msg) throws IOException {
        OutputStream responseStream = null;
        try {
            responseStream = resp.getOutputStream();
            resp.setStatus(status);
            if (msg != null)
                responseStream.write(msg.getBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendBackSoapFault(HttpServletRequest req, HttpServletResponse resp, String msg, Throwable e) throws IOException {
        OutputStream responseStream = null;
        try {
            responseStream = resp.getOutputStream();
            resp.setContentType(DEFAULT_CONTENT_TYPE);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Element exceptiondetails = null;
            try {
                if (e != null && e.getMessage() != null && e.getMessage().length() > 0) {
                    exceptiondetails = SoapFaultUtils.makeFaultDetailsSubElement("exception", e.getMessage());
                }
                responseStream.write(SoapFaultUtils.generateRawSoapFault(SoapFaultUtils.FC_SERVER,
                                                                         msg,
                                                                         exceptiondetails,
                                                                         req.getRequestURL().toString()).getBytes());
            } catch (SAXException e1) {
                logger.log(Level.SEVERE, "exception sending back soapfault", e);
            }
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private DocumentBuilderFactory dbf;
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";

}
