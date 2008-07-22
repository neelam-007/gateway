/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import javax.net.ssl.SSLContext;

/**
 * @author mike
 */
public interface SslContextHaver {
    SSLContext getSslContext();
}
