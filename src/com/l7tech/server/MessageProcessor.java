/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.util.Locator;
import com.l7tech.common.security.xml.SessionNotFoundException;
import com.l7tech.common.security.xml.Session;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.logging.LogManager;

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
            if ( _serviceManager == null ) throw new IllegalStateException( "ServiceManager is null!" );

            PublishedService service = _serviceManager.resolveService( request );

            AssertionStatus status;
            if ( service == null ) {
                logger.info( "Service not found" );
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
                    long reqPolicyId = Long.parseLong(requestorVersion.substring(0, requestorVersion.indexOf('|')));
                    long reqPolicyVer = Long.parseLong(requestorVersion.substring(requestorVersion.indexOf('|')+1));
                    if (reqPolicyVer != service.getVersion() || reqPolicyId != service.getOid()) {
                        // this will make the servlet send back the URL of the policy
                        response.setPolicyViolated(true);
                        throw new PolicyAssertionException("Wrong policy version " + requestorVersion);
                    }
                } else {
                    logger.info("Requestor did not provide policy id.");
                }

                // If an xml-enc session id is provided, make sure it's still valid
                // (because we can't wait for the XmlResponseSecurity to fail because it happens after routing)
                if (checkForInvalidXmlSessIdRef(request)) {
                    response.setParameter( Response.PARAM_HTTP_SESSION_STATUS, "invalid" );
                    logger.info("Request refered to a session id that is not valid. Policy will not be executed.");
                    return AssertionStatus.FALSIFIED;
                }

                // run the policy
                service.attemptedRequest();
                ServerAssertion ass = service.rootAssertion();
                status = ass.checkRequest( request, response );

                if ( status == AssertionStatus.NONE ) {
                    service.authorizedRequest();
                    if ( request.isRouted() ) {
                        logger.info( "Request was routed with status " + " " + status.getMessage() + "(" + status.getNumeric() + ")" );
                        service.completedRequest();
                    } else {
                        logger.warning( "Request was not routed!");
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    logger.warning( status.getMessage() );
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            logger.log(Level.SEVERE, sre.getMessage(), sre);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null )
            _instance = new MessageProcessor();
        return _instance;
    }

    private MessageProcessor() {
        // This only uses Locator because only one instance of ServiceManager must
        // be active at once.
        _serviceManager = (ServiceManager)Locator.getDefault().lookup( ServiceManager.class );
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
            String msg = "Exception finding session with id=" + sessionIDHeaderValue;
            logger.log(Level.WARNING, msg, e);
            return true;
        } catch (NumberFormatException e) {
            String msg = "Session id is not long value : " + sessionIDHeaderValue;
            logger.log(Level.WARNING, msg, e);
            return true;
        }

        // upload this session into the request message's context
        request.setParameter(Request.PARAM_HTTP_XML_SESSID, xmlsession);

        return false;
    }

    private static MessageProcessor _instance = null;
    private static ThreadLocal _currentRequest = new ThreadLocal();
    private static ThreadLocal _currentResponse = new ThreadLocal();

    private ServiceManager _serviceManager;
    private Logger logger = LogManager.getInstance().getSystemLogger();
}
