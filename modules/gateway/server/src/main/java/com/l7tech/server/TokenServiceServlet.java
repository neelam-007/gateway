package com.l7tech.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.InvalidClientCertificateException;
import com.l7tech.identity.User;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.xml.processor.BadSecurityContextException;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.server.identity.AuthCache;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.transport.http.HttpTransportModule;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.SoapFaultLevel;
import org.springframework.beans.BeansException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.w3c.dom.Document;
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
 */
public class TokenServiceServlet extends HttpServlet {
    private WebApplicationContext applicationContext;
    private TokenService tokenService;
    private SoapFaultManager soapFaultManager;
    private StashManagerFactory stashManagerFactory;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        applicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        if (applicationContext == null) {
            throw new ServletException("Configuration error; could not get application context");
        }
        try {
            tokenService = applicationContext.getBean("tokenService", TokenService.class);
            soapFaultManager = applicationContext.getBean("soapFaultManager", SoapFaultManager.class);
            stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        }
        catch(BeansException be) {
            throw new ServletException("Configuration error; could not get required beans.", be);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            HttpTransportModule.requireEndpoint(req, SsgConnector.Endpoint.STS);
            super.service(req, resp);
        } catch (ListenerException e) {
            logger.log(Level.INFO, "Token service not enabled on this connector");
            resp.sendError(404, "Token service not available on this port");
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; context requests must use POST");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
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

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        try {
            AssertionStatus status;
            try {
                final StashManager stashManager = stashManagerFactory.createStashManager();
                request.initialize(stashManager, ctype, req.getInputStream(),Message.getMaxBytes());
                status = tokenService.respondToSecurityTokenRequest(context, authenticator(context), false, false);
                context.setPolicyResult(status);
            } catch (InvalidDocumentFormatException e) {
                String msg = "Request is not formatted as expected. " + e.getMessage();
                logger.log(Level.INFO, msg, e);
                sendBackNonSoapError(res, HttpServletResponse.SC_BAD_REQUEST, msg);
                return;
            } catch (TokenServiceImpl.TokenServiceException e) {
                String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
                return;
            } catch (ProcessorException e) {
                String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
                return;
            } catch (BadSecurityContextException e) {
                String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
                return;
            } catch (GeneralSecurityException e) {
                String msg = "Could not respond to RequestSecurityToken. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
                return;
            } catch (AuthenticationException e) {
                sendBackNonSoapError(res, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
                return;
            }

            // init headers (needed for faults also)
            respKnob.beginResponse();
            if ( respKnob.getStatus() == 0 ) {
                res.setStatus(HttpConstants.STATUS_OK);
            }

            // in case of failure, return soap fault
            if (status != AssertionStatus.NONE) {
                returnFault(context, res);
                return;
            }

            // get response document
            Document output;
            try {
                output = context.getResponse().getXmlKnob().getDocumentReadOnly();
            } catch (SAXException e) {
                String msg = "Cannot retrieve response document. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
                return;
            }

            // don't let this IOException fall through, this is a debugging nightmare!
            try {
                outputRequestSecurityTokenResponse(output, res);
                logger.finest("Sent back SecurityToken:" + XmlUtil.nodeToFormattedString(output));
            } catch (IOException e) {
                String msg = "Error printing result. " + e.getMessage();
                logger.log(Level.SEVERE, msg, e);
                sendExceptionFault(context, e, res);
            }
        } catch (Throwable e) {
            String msg = "UNHANDLED EXCEPTION: " + e.getMessage();
            logger.log(Level.SEVERE, msg, e);
            sendExceptionFault(context, e, res);
        } finally {
            context.close();
        }
    }

    private TokenServiceImpl.CredentialsAuthenticator authenticator(final PolicyEnforcementContext context) {
        return new TokenServiceImpl.CredentialsAuthenticator() {
            @Override
            public User authenticate(LoginCredentials creds) {
                User authenticatedUser = null;
                Collection<IdentityProvider> providers;
                boolean sawInvalidClientCertException = false;
                try {
                    // go through providers and try to authenticate the cert
                    IdentityProviderFactory ipf = (IdentityProviderFactory)applicationContext.getBean("identityProviderFactory");
                    providers = ipf.findAllIdentityProviders();
                    for (IdentityProvider provider : providers) {
                        try {
                            AuthenticationResult authResult = AuthCache.getInstance().getCachedAuthResult(creds, provider);
                            if (authResult != null) {
                                if (authenticatedUser != null) {
                                    throw new AuthenticationException("The cert used to sign this request is valid " +
                                                                      "on more than one provider. Secure conversation " +
                                                                      "contexts must be associated unambiguously to one " +
                                                                      "user.");
                                } else {
                                    authenticatedUser = authResult.getUser();
                                }
                            }
                        } catch (InvalidClientCertificateException icce) {
                            sawInvalidClientCertException = true;
                            logger.log(Level.INFO, "Invalid client certificateException trying to authenticate credentials ''{0}'', on provider ''{1}''.",
                                    new String[]{icce.getMessage(), provider.getConfig().getName()});
                        } catch (AuthenticationException e) {
                            logger.log(Level.INFO, "AuthenticationException trying to authenticate credentials ''{0}'', on provider ''{1}''.",
                                    new String[]{e.getMessage(), provider.getConfig().getName()});
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "could not get id provider from factory", e);
                    return null;
                }
                if (authenticatedUser == null) {
                    logger.fine("Credentials did not authenticate against any provider.");
                    if ( sawInvalidClientCertException ) {
                        // set response header so that the XML VPN Client is made aware of this situation
                        context.getResponse().getHeadersKnob().addHeader(SecureSpanConstants.HttpHeaders.CERT_STATUS,
                                                                              SecureSpanConstants.CERT_INVALID);
                    }
                } else {
                    logger.finer("authenticated: " + authenticatedUser);
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

    private void returnFault(PolicyEnforcementContext context, HttpServletResponse hresp) throws IOException {
        OutputStream responseStream = null;
        try {
            responseStream = hresp.getOutputStream();
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile

            SoapFaultLevel faultLevelInfo = context.getFaultlevel();
            final SoapFaultManager.FaultResponse fault = soapFaultManager.constructReturningFault(faultLevelInfo, context);
            soapFaultManager.sendExtraHeaders(fault, hresp);
            hresp.setContentType(fault.getContentType().getFullValue());
            responseStream.write(fault.getContentBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private void sendExceptionFault(PolicyEnforcementContext context, Throwable e, HttpServletResponse hresp) throws IOException {
        OutputStream responseStream = null;
        try {
            final SoapFaultManager.FaultResponse faultInfo = soapFaultManager.constructExceptionFault(e, null, context);
            responseStream = hresp.getOutputStream();
            soapFaultManager.sendExtraHeaders(faultInfo, hresp);
            hresp.setContentType(faultInfo.getContentType().getFullValue());
            hresp.setStatus(500); // soap faults "MUST" be sent with status 500 per Basic profile
            responseStream.write(faultInfo.getContentBytes());
        } finally {
            if (responseStream != null) responseStream.close();
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    public static final String DEFAULT_CONTENT_TYPE = XmlUtil.TEXT_XML + "; charset=utf-8";

}
