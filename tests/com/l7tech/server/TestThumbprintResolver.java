/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.security.xml.ThumbprintResolver;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public class TestThumbprintResolver implements ThumbprintResolver {
    public X509Certificate lookup(String thumbprint) {
        return null;
    }
}
