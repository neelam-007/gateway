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
    Iterator getParameterNames();
    void setParameter( Object name, Object value );
    Object getParameter( Object name );
}
