/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.util.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class TransportMetadata {
    public void setParameter( String name, Object value ) {
        if ( _params == Collections.EMPTY_MAP ) _params = new HashMap();
        _params.put( name, value );
    }

    public Object getParameter( String name ) {
        Object param = doGetParameter( name );
        if ( param == null )
            return _params.get(name);
        else
            return param;
    }

    protected abstract Object doGetParameter( String name );

    protected Map _params = Collections.EMPTY_MAP;
}
