/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SessionNotFoundException;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceCache;
import com.l7tech.service.resolution.ServiceResolutionException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor {
    public AssertionStatus processMessage( Request request, Response response ) throws IOException, PolicyAssertionException {
        try {
            PublishedService service = ServiceCache.getInstance().resolve(request);

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
                        String msg = "Wrong policy version " + requestorVersion;
                        logger.fine(msg);
                        // this will make the servlet send back the URL of the policy
                        response.setPolicyViolated(true);
                        throw new PolicyAssertionException(msg);
                    }
                } else {
                    logger.fine("Requestor did not provide policy id.");
                }

                // If an xml-enc session id is provided, make sure it's still valid
                // (because we can't wait for the XmlResponseSecurity to fail because it happens after routing)
                if (checkForInvalidXmlSessIdRef(request)) {
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
                    logger.info("Request referred to an invalid session id. Policy will not be executed.");
                    return AssertionStatus.FALSIFIED;
                }

                // Get the server policy
                ServerAssertion serverPolicy = ServiceCache.getInstance().getServerPolicy(service.getOid());
                if (serverPolicy == null) {
                    throw new ServiceResolutionException("service is resolved but no corresponding policy available.");
                }

                // Run the policy
                logger.finest("Run the server policy");
                ServiceCache.getInstance().getServiceStatistics(service.getOid()).attemptedRequest();
                status = serverPolicy.checkRequest( request, response );

                if ( status == AssertionStatus.NONE ) {
                    ServiceCache.getInstance().getServiceStatistics(service.getOid()).authorizedRequest();
                    RoutingStatus rstat = request.getRoutingStatus();
                    if ( rstat == RoutingStatus.ROUTED ) {
                        logger.fine( "Request was routed with status " + " " + status.getNumeric() + " (" + status.getMessage() + ")" );
                        ServiceCache.getInstance().getServiceStatistics(service.getOid()).completedRequest();
                    } else if ( rstat == RoutingStatus.ATTEMPTED ) {
                        logger.severe( "Request routing failed with status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    logger.warning( "Policy evaluation resulted in status " + status.getNumeric() + " (" + status.getMessage() + ")" );
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            logger.log(Level.SEVERE, sre.getMessage(), sre);
            return AssertionStatus.SERVER_ERROR;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null )
            _instance = new MessageProcessor();
        return _instance;
    }

    public DocumentBuilder getDomParser() throws ParserConfigurationException {
        return _dbf.newDocumentBuilder();
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
            logger.log(Level.WARNING, "Exception finding session with id=" + sessionIDHeaderValue, e);
            return true;
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Session id is not long value : " + sessionIDHeaderValue, e);
            return true;
        }

        // upload this session into the request message's context
        request.setParameter(Request.PARAM_HTTP_XML_SESSID, xmlsession);

        return false;
    }

    private static MessageProcessor _instance = null;
    private static ThreadLocal _currentRequest = new ThreadLocal();
    private static ThreadLocal _currentResponse = new ThreadLocal();

    private Logger logger = LogManager.getInstance().getSystemLogger();

    private DocumentBuilderFactory _dbf;
    private XmlPullParserFactory _xppf;
}