/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.transport.jms;

/**
 * Used for indicating JMS test failures.  Created to avoid sending real JMSExceptions to the Manager.
 * @author alex
 * @version $Revision$
 */
public class JmsTestException extends Exception {
    public JmsTestException() {
        super();
    }

    public JmsTestException( String message ) {
        super( message );
    }
}
