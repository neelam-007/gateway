/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import org.springframework.context.ApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerRoutingAssertion implements ServerAssertion {
    public static final String ENCODING = "UTF-8";

    protected ServerRoutingAssertion(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected ApplicationContext  applicationContext;
}
