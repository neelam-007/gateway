/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.util.*;

/**
 * @author alex
 */
public abstract class Message {
    public Header getHeader() {
        return _header;
    }

    public void setHeader(Header header) {
        _header = header;
    }

    public Body getBody() {
        return _body;
    }

    public void setBody(Body body) {
        _body = body;
    }

    public void setParameter( Object name, Object value ) {
        if ( Collections.EMPTY_MAP.equals( _params ) ) _params = new HashMap();
        _params.put( name, value );
    }

    public Object getParameter( Object name ) {
        return _params.get( name );
    }

    protected Header _header;
    protected Body _body;
    protected Map _params = Collections.EMPTY_MAP;
}
