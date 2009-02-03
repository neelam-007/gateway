/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

public class PermissiveHostnameVerifier implements HostnameVerifier {
    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return true;
    }
}
