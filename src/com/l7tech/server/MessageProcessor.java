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
        if ( _serviceManager == null ) throw new IllegalStateException( "ServiceManager is null!" );
        try {
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

    private static MessageProcessor _instance = null;

    private ServiceManager _serviceManager;
    private Logger _log = LogManager.getInstance().getSystemLogger();
}
