package com.l7tech.server;

import com.l7tech.common.Messages;

import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssertionMessages extends Messages {
    public static final M SSL_CONTEXT_INIT_FAILED   = m(4000, Level.SEVERE, "Couldn't initialize SSL Context");
    public static final M HTTP_ROUTING_ASSERTION    = m(4001, Level.INFO, "Processing HTTP routing assertion");    

}
