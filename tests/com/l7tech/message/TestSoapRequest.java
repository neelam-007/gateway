/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;

import com.l7tech.common.util.XmlUtil;

/**
 * Simple standalone SoapRequest for testing.
 */
public class TestSoapRequest extends SoapRequest {
    private Node requestMessage;

    public TestSoapRequest(Document requestMessage) {
        super(new TestTransportMetadata());
        this.requestMessage = requestMessage;
    }

    protected InputStream doGetRequestInputStream() throws IOException {
        return new ByteArrayInputStream((XmlUtil.nodeToString(requestMessage).getBytes()));
    }

    public boolean isReplyExpected() {
        return false;
    }
}
