/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class TransportMetadata {
    public abstract TransportProtocol getProtocol();
    abstract Object getParameter( String name );
}
