/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.util.Iterator;

/**
 * @author alex
 */
public interface Message {
    TransportMetadata getTransportMetadata();
    Iterator getParameterNames();
    void setParameter( String name, Object value );
    Object getParameter( String name );
}
