/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.server.policy.custom;

import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.ServiceRequest;
import com.l7tech.policy.assertion.ext.ServiceResponse;

import java.util.logging.Logger;

/**
 * @author emil
 * @version Feb 23, 2004
 */
public class TestServiceInvocation extends ServiceInvocation {
    private final Logger logger = Logger.getLogger(TestServiceInvocation.class.getName());

    public void onRequest( ServiceRequest request) {
        logger.info("in request");
    }

    public void onResponse( ServiceResponse response) {
        logger.info("in request");
    }
}