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
public abstract class MessageAdapter implements Message {
    public void setParameter( Object name, Object value ) {
        _params = new HashMap();
        _params.put( name, value );
    }

    public Object getParameter( Object name ) {
        return _params.get( name );
    }

    public Iterator getParameterNames() {
        return _params.keySet().iterator();
    }

    protected Map _params = Collections.EMPTY_MAP;
}
