/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

/**
 * Simple standalone SoapResponse for testing.
 */
public class TestSoapResponse extends SoapResponse {
    public TestSoapResponse() {
        super(new TestTransportMetadata());
    }
}
