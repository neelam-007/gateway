package com.l7tech.server;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.common.message.HttpServletRequestKnob;
import com.l7tech.common.message.HttpServletResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.security.xml.processor.BadSecurityContextException;
import com.l7tech.common.security.xml.processor.ProcessorException;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
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
    private WebApplicationContext applicationContext;
    private TokenService tokenService;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        tokenService = (TokenService)applicationContext.getBean("tokenService");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; context requests must use POST");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            final Message response = new Message();
            final Message request = new Message();

            final String rawct = req.getContentType();
            ContentTypeHeader ctype = rawct != null && rawct.length() > 0
              ? ContentTypeHeader.parseValue(rawct)
              : ContentTypeHeader.XML_DEFAULT;

            final HttpRequestKnob reqKnob = new HttpServletRequestKnob(req);
            request.attachHttpRequestKnob(reqKnob);

            final HttpServletResponseKnob respKnob = new HttpServletResponseKnob(res);
            response.attachHttpResponseKnob(respKnob);

            final PolicyEnforcementContext context = new PolicyEnforcementContext(request, response, req, res);

            AssertionStatus status = AssertionStatus.UNDEFINED;
            try {
                final StashManager stashManager = StashManagerFactory.createStashManager();
                request.initialize(stashManager, ctype, req.getInputStream());
                status = tokenService.respondToSecurityTokenRequest(context, authenticator());
            } catch (InvalidDocumentFormatException e) {
                String msg = "Request is not formatted as expected. " + e.getMessage();
                logger.log(Level.INFO, msg, e);
                sendBackNonSoapError(res, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } catch (TokenServiceImpl.TokenServiceException e) {
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
            } catch (NoSuchPartException e) {
                String msg = "Cannot initialize request context. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendBackSoapFault(req, res, msg, e);
                return;
            }

            // in case of failure, return soap fault
            if (status != AssertionStatus.NONE) {
                sendBackSoapFault(req, res, context.getFaultDetail().getFaultString(), null);
                return;
            }

            // get response document
            Document output = null;
            try {
                output = context.getResponse().getXmlKnob().getDocumentReadOnly();
            } catch (SAXException e) {
                String msg = "Cannot retrieve response document. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendBackSoapFault(req, res, msg, e);
                return;
            }

            // dont let this ioexception fall through, this is a debugging nightmare!
            try {
                outputRequestSecurityTokenResponse(output, res);
                logger.finest("Sent back SecurityToken:" + XmlUtil.nodeToFormattedString(output));
                return;
            } catch (IOException e) {
                String msg = "Error printing result. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendBackSoapFault(req, res, msg, e);
                return;
            }
        } catch (Throwable e) {
            String msg = "UNHANDLED EXCEPTION: " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendBackSoapFault(req, res, msg, e);
            return;
        }
    }

    private final TokenServiceImpl.CredentialsAuthenticator authenticator() {
        return new TokenServiceImpl.CredentialsAuthenticator() {
            public User authenticate(LoginCredentials creds) {
                IdentityProviderConfigManager idpcm =
                  (IdentityProviderConfigManager)applicationContext.getBean("identityProviderConfigManager");
                User authenticatedUser = null;
                Collection providers = null;
                try {
                    // go through providers and try to authenticate the cert
                    IdentityProviderFactory ipf = (IdentityProviderFactory)applicationContext.getBean("identityProviderFactory");
                    providers = ipf.findAllIdentityProviders(idpcm);
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
                responseStream.write(SoapFaultUtils.generateSoapFaultXml(SoapFaultUtils.FC_SERVER,
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
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";

}
