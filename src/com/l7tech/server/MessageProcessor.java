/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.util.Locator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor {
    public AssertionStatus processMessage( Request request, Response response ) throws IOException, PolicyAssertionException, MessageProcessingException {
        try {
            if ( _serviceManager == null ) throw new IllegalStateException( "ServiceManager is null!" );

            PublishedService service = _serviceManager.resolveService( request );
            AssertionStatus status;
            if ( service == null || service.isDisabled() ) {
                if ( service == null )
                    _log.log(Level.INFO, "Service not found" );
                else
                    _log.log( Level.WARNING, "Service disabled" );

                status = AssertionStatus.SERVICE_NOT_FOUND;
            } else {
                _log.log(Level.FINER, "Service resolved" );
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
                    _log.info("Requestor did not provide policy id.");
                }

                // run the policy
                ServerAssertion ass = service.rootAssertion();
                status = ass.checkRequest( request, response );

                if ( status == AssertionStatus.NONE ) {
                    service.incrementRequestCount();
                    if ( request.isRouted() ) {
                        _log.log(Level.INFO, "Request was routed with status " + " " + status.getMessage() + "(" + status.getNumeric() + ")" );
                    } else {
                        _log.log(Level.WARNING, "Request was not routed!");
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    _log.log( Level.WARNING, status.getMessage() );
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, sre.getMessage(), sre);
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

    private static MessageProcessor _instance = null;
    private static ThreadLocal _currentRequest = new ThreadLocal();

    private ServiceManager _serviceManager;
    private Logger _log = LogManager.getInstance().getSystemLogger();
}
