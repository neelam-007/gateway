/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SessionNotFoundException;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceStatistics;
import com.l7tech.service.resolution.ServiceResolutionException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor {
    public AssertionStatus processMessage( Request request, Response response )
            throws IOException, PolicyAssertionException, PolicyVersionException {

        // WSS-Processing Step
        if (request instanceof SoapRequest) {
            SoapRequest req = (SoapRequest)request;
            WssProcessor trogdor = (WssProcessor)Locator.getDefault().lookup(WssProcessor.class);
            X509Certificate serverSSLcert = null;
            PrivateKey sslPrivateKey = null;
            try {
                serverSSLcert = getServerCert();
                sslPrivateKey = getServerKey();
            } catch (CertificateException e) {
                logger.log(Level.SEVERE, "Error getting server cert/private key", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (KeyStoreException e) {
                logger.log(Level.SEVERE, "Error getting server cert/private key", e);
                return AssertionStatus.SERVER_ERROR;
            }
            WssProcessor.ProcessorResult wssOutput = null;
            try {
                wssOutput = trogdor.undecorateMessage(req.getDocument(), serverSSLcert, sslPrivateKey);
            } catch (WssProcessor.ProcessorException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (InvalidDocumentFormatException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (GeneralSecurityException e) {
                logger.log(Level.SEVERE, "Error in WSS processing of request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (SAXException e) {
                logger.log(Level.SEVERE, "Error getting xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            }
            // todo, refactor SoapRequest so that it keeps a hold on the original message
            if (wssOutput.getUndecoratedMessage() != null) {
                req.setDocument(wssOutput.getUndecoratedMessage());
            }
            logger.finest("WSS processing of request complete.");
        }
        // Policy Verification Step
        try {
            ServiceManager manager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
            PublishedService service = manager.resolve(request);

            AssertionStatus status;
            if ( service == null ) {
                logger.warning( "Service not found" );
                status = AssertionStatus.SERVICE_NOT_FOUND;
            } else if ( service.isDisabled() ) {
                logger.warning( "Service disabled" );
                status = AssertionStatus.SERVICE_DISABLED;
            } else {
                logger.finer( "Resolved service #" + service.getOid() );
                request.setParameter( Request.PARAM_SERVICE, service );

                // check if requestor provided a version number for published service
                String requestorVersion = (String)request.getParameter( Request.PARAM_HTTP_POLICY_VERSION );
                if (requestorVersion != null && requestorVersion.length() > 0) {
                    // format is policyId|policyVersion (seperated with char '|')
                    boolean wrongPolicyVersion = false;
                    int indexofbar = requestorVersion.indexOf('|');
                    if (indexofbar < 0) {
                        logger.finest("policy version passed has incorrect format");
                        wrongPolicyVersion = true;
                    } else {
                        try {
                            long reqPolicyId = Long.parseLong(requestorVersion.substring(0, indexofbar));
                            long reqPolicyVer = Long.parseLong(requestorVersion.substring(indexofbar+1));
                            if (reqPolicyVer != service.getVersion() || reqPolicyId != service.getOid()) {
                                logger.finest("policy version passed is invalid " + requestorVersion + " instead of "
                                                + service.getOid() + "|" + service.getVersion());
                                wrongPolicyVersion = true;
                            }
                        } catch (NumberFormatException e) {
                            wrongPolicyVersion = true;
                            logger.log(Level.FINE, "wrong format for policy version", e);
                        }
                    }
                    if (wrongPolicyVersion) {
                        response.setPolicyViolated(true);
                        throw new PolicyVersionException();
                    }
                } else {
                    logger.fine("Requestor did not provide policy id.");
                }

                // If an xml-enc session id is provided, make sure it's still valid
                // (because we can't wait for the XmlResponseSecurity to fail because it happens after routing)
                if (checkForInvalidXmlSessIdRef(request)) {
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, SecureSpanConstants.INVALID );
                    logger.info("Request referred to an invalid session id. Policy will not be executed.");
                    return AssertionStatus.FALSIFIED;
                }

                // Get the server policy
                ServerAssertion serverPolicy = null;
                try {
                    serverPolicy = manager.getServerPolicy(service.getOid());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get policy", e);
                    serverPolicy = null;
                }
                if (serverPolicy == null) {
                    throw new ServiceResolutionException("service is resolved but no corresponding policy available.");
                }

                // Run the policy
                logger.finest("Run the server policy");
                ServiceStatistics stats = null;
                try {
                    stats = manager.getServiceStatistics(service.getOid());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get a stats object", e);
                }
                if (stats != null) stats.attemptedRequest();
                status = serverPolicy.checkRequest( request, response );

                RoutingStatus rstat = request.getRoutingStatus();

                boolean authorized = false;
                if ( rstat == RoutingStatus.ATTEMPTED ) {
                    /* If policy execution got as far as the routing assertion,
                       we consider the request to have been authorized, whether
                       or not the routing itself was successful. */
                    authorized = true;
                }

                if ( status == AssertionStatus.NONE ) {
                    // Policy execution concluded successfully
                    authorized = true;
                    if ( rstat == RoutingStatus.ROUTED || rstat == RoutingStatus.NONE ) {
                        /* We include NONE because it's valid (albeit silly)
                        for a policy to contain no RoutingAssertion */
                        logger.fine("Request was completed with status " + " " + status.getNumeric() + " (" + status.getMessage() + ")");
                        if (stats != null) stats.completedRequest();
                    } else {
                        // This can only happen when a post-routing assertion fails
                        logger.severe( "Policy status was NONE but routing was attempted anyway!" );
                        status = AssertionStatus.SERVER_ERROR;
                    }
                } else {
                    // Policy execution concluded unsuccessfully
                    if (rstat == RoutingStatus.ATTEMPTED) {
                        // Most likely the failure was in the routing assertion
                        logger.warning("Request routing failed with status " + status.getNumeric() + " (" + status.getMessage() + ")");
                        status = AssertionStatus.FAILED;
                    } else {
                        // Most likely the failure was in some other assertion
                        logger.warning( "Policy evaluation resulted in status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                    }
                }

                if ( authorized && stats != null ) stats.authorizedRequest();
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            logger.log(Level.SEVERE, sre.getMessage(), sre);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private synchronized PrivateKey getServerKey() throws KeyStoreException {
        if (privateServerKey == null) {
            privateServerKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        }
        return privateServerKey;
    }

    private synchronized X509Certificate getServerCert() throws IOException, CertificateException {
        if (serverCert == null) {
            byte[] buf = KeystoreUtils.getInstance().readSSLCert();
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            serverCert = (X509Certificate)(CertificateFactory.getInstance("X.509").generateCertificate(bais));
        }
        return serverCert;
    }

    public static MessageProcessor getInstance() {
        /*if ( _instance == null )
            _instance = new MessageProcessor();
        return _instance;*/
        return SingletonHolder.singleton;
    }

    public DocumentBuilder getDomParser() throws ParserConfigurationException {
        DocumentBuilder builder = _dbf.newDocumentBuilder();
        builder.setEntityResolver(XmlUtil.getSafeEntityResolver());
        return builder;
    }

    public XmlPullParser getPullParser() throws XmlPullParserException {
        return _xppf.newPullParser();
    }

    private MessageProcessor() {
        _dbf = DocumentBuilderFactory.newInstance();
        _dbf.setNamespaceAware(true);
        _dbf.setValidating(false);

        try {
            _xppf = XmlPullParserFactory.newInstance();
        } catch (XmlPullParserException e) {
            throw new RuntimeException( e );
        }
        _xppf.setNamespaceAware( true );
        _xppf.setValidating( false );
    }

    /** Returns the thread-local current request. Could be null! */
    public static Request getCurrentRequest() {
        return (Request)_currentRequest.get();
    }

    public static void setCurrentRequest( Request request ) {
        _currentRequest.set( request );
    }

    public static Response getCurrentResponse() {
        return (Response)_currentResponse.get();
    }

    public static void setCurrentResponse( Response response ) {
        _currentResponse.set( response );
    }

    /**
     * This looks a http header containing a reference to an xml session id which has become invalid.
     * This prevents ServerXmlResponseSecurity to fail after the routing assertion succeeds.
     * @return If there is indeed a reference to an invalid session id, then this returns true
     */
    private boolean checkForInvalidXmlSessIdRef(Request request) {
        // RETREIVE SESSION ID
        // get the header containing the xml session id
        String sessionIDHeaderValue = (String)request.getParameter( Request.PARAM_HTTP_XML_SESSID );
        if (sessionIDHeaderValue == null || sessionIDHeaderValue.length() < 1) {
            // no trace of this, no worries then
            return false;
        }
        // retrieve the session
        Session xmlsession = null;
        try {
            xmlsession = SessionManager.getInstance().getSession(Long.parseLong(sessionIDHeaderValue));
        } catch (SessionNotFoundException e) {
            logger.warning( "Exception finding session with id=" + sessionIDHeaderValue );
            return true;
        } catch (NumberFormatException e) {
            logger.warning( "Session id is not long value : " + sessionIDHeaderValue );
            return true;
        }

        // upload this session into the request message's context
        request.setParameter(Request.PARAM_HTTP_XML_SESSID, xmlsession);

        return false;
    }


    private static class SingletonHolder {
        private static MessageProcessor singleton = new MessageProcessor();
    }

    //private static MessageProcessor _instance = null;
    private static ThreadLocal _currentRequest = new ThreadLocal();
    private static ThreadLocal _currentResponse = new ThreadLocal();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private DocumentBuilderFactory _dbf;
    private XmlPullParserFactory _xppf;

    private PrivateKey privateServerKey = null;
    private X509Certificate serverCert = null;
}