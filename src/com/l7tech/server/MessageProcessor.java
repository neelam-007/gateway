/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.util.Locator;
import com.l7tech.logging.LogManager;

import java.io.IOException;
import java.util.logging.Level;


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
            if ( service == null ) {
                status = AssertionStatus.NOT_FOUND;
            } else {
                request.setParameter( Request.PARAM_SERVICE, service );
                Assertion ass = service.rootAssertion();

                status = ass.checkRequest( request, response );

                if ( status == AssertionStatus.NONE ) {
                    if ( request.isRouted() ) {
                        LogManager.getInstance().getSystemLogger().log(Level.INFO, "Request was routed with status " + status.getMessage() );
                    } else {
                        LogManager.getInstance().getSystemLogger().log(Level.WARNING, "Request was not routed!");
                        status = AssertionStatus.FALSIFIED;
                    }
                } else {
                    LogManager.getInstance().getSystemLogger().log(Level.SEVERE, status.getMessage());
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, sre);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null ) _instance = new MessageProcessor();
        return _instance;
    }

    private MessageProcessor() {
        _serviceManager = (ServiceManager)Locator.getDefault().lookup( ServiceManager.class );
    }

    private ServiceManager _serviceManager;
    private static MessageProcessor _instance = null;
}
