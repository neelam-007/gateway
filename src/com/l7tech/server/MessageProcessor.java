/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.*;
import com.l7tech.service.*;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.objectmodel.ObjectModelException;
import org.apache.log4j.Category;

import java.io.IOException;


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
                        _log.info( status );
                    } else {
                        _log.warn( "Request was not routed, executing default RoutingAssertion" );
                        status = _defaultRoutingAssertion.checkRequest( request, response );
                    }
                } else {
                    _log.warn( status );
                }
            }

            return status;
        } catch ( ServiceResolutionException sre ) {
            _log.warn( sre );
            return AssertionStatus.SERVER_ERROR;
        }
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null ) _instance = new MessageProcessor();
        return _instance;
    }

    private MessageProcessor() {
        try {
            _serviceManager = new ServiceManagerImp();
        } catch ( ObjectModelException ome ) {
            _log.error( ome );
        }
    }

    private Category _log = Category.getInstance( getClass() );

    private ServiceManager _serviceManager;

    private static MessageProcessor _instance = null;

    private transient RoutingAssertion _defaultRoutingAssertion = new RoutingAssertion();
}
