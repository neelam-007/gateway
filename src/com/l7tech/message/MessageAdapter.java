/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.server.policy.assertion.ServerAssertion;

import java.util.*;

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

    public void setParameterIfEmpty( String name, Object value ) {
        Object temp = getParameter( name );
        if ( temp == null ) setParameter( name, value );
    }

    public Object getParameter( String name ) {
        Object value = doGetParameter(name);
        if ( value == null ) value = _params.get( name );
        if ( value instanceof Object[] )
            return ((Object[])value)[0];
        else
            return value;
    }

    public abstract Object doGetParameter(String name);

    public Object[] getParameterValues( String name ) {
        Object value = doGetParameter(name);
        if ( value == null ) value = _params.get(name);
        if ( value instanceof Object[] ) {
            return (Object[])value;
        } else if ( value == null ) {
            return null;
        } else {
            return new Object[] { value };
        }
    }

    public Iterator getParameterNames() {
        return _params.keySet().iterator();
    }

    public TransportMetadata getTransportMetadata() {
        return _transportMetadata;
    }

    public Collection getDeferredAssertions() {
        return _deferredAssertions.values();
    }

    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        _deferredAssertions.put(owner, decoration);
    }

    public void removeDeferredAssertion(ServerAssertion owner) {
        _deferredAssertions.remove(owner);
    }

    protected TransportMetadata _transportMetadata;
    protected Map _params = new HashMap();
    protected Map _deferredAssertions = new LinkedHashMap();
}
