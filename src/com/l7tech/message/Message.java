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
    static final String PREFIX             = "com.l7tech.message.request";
    static final String PREFIX_HTTP        = PREFIX + ".http";
    static final String PREFIX_HTTP_HEADER = PREFIX_HTTP + ".header";
    public static final String PARAM_HTTP_CONTENT_TYPE      = PREFIX_HTTP_HEADER + ".content-type";
    public static final String PARAM_HTTP_CONTENT_LENGTH    = PREFIX_HTTP_HEADER + ".content-length";
    public static final String PARAM_HTTP_DATE              = PREFIX_HTTP_HEADER + ".date";

    TransportMetadata getTransportMetadata();
    Iterator getParameterNames();
    void setParameter( String name, Object value );
    Object getParameter( String name );
}
