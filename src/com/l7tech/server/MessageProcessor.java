/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.service.ServiceManager;
import com.l7tech.service.ServiceManagerImp;
import com.l7tech.objectmodel.ObjectModelException;
import org.apache.log4j.Category;

import javax.naming.NamingException;

/**
 * @author alex
 * @version $Revision$
 */
public class MessageProcessor {
    public void processMessage( Request request, Response response ) throws PolicyAssertionException, MessageProcessingException {
        // TODO: Resolve service
        // TODO: Implement :)
    }

    public static MessageProcessor getInstance() {
        if ( _instance == null ) _instance = new MessageProcessor();
        return _instance;
    }

    private MessageProcessor() {
        try {
            _serviceManager = new ServiceManagerImp();
            _initialized = true;
        } catch ( ObjectModelException ome ) {
            _log.error( ome );
        }
    }

    private Category _log = Category.getInstance( getClass() );

    private ServiceManager _serviceManager;
    private boolean _initialized = false;

    private static MessageProcessor _instance = null;
}
