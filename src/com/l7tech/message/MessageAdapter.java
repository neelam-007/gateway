/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class MessageAdapter implements Message {
    public MessageAdapter( TransportMetadata tm ) {
        _transportMetadata = tm;
    }

    public void setParameter( String name, Object value ) {
        if ( _params == Collections.EMPTY_MAP ) _params = new HashMap();
        _params.put( name, value );
    }

    public Object getParameter( String name ) {
        Object value = _transportMetadata.getParameter(name);
        if ( value == null ) value = _params.get( name );
        return value;
    }

    public Iterator getParameterNames() {
        return _params.keySet().iterator();
    }

    public TransportMetadata getTransportMetadata() {
        return _transportMetadata;
    }

    protected TransportMetadata _transportMetadata;
    protected Map _params = new HashMap();
}
