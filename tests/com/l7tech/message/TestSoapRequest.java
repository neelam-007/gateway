/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple standalone SoapRequest for testing.
 */
public class TestSoapRequest extends SoapRequest {
    public Object doGetParameter( String name ) {
        return null;
    }

    private Node requestMessage;

    public TestSoapRequest(Document requestMessage) {
        super(new TestTransportMetadata());
        this.requestMessage = requestMessage;
        try {
            initialize(getInputStream(), ContentTypeHeader.XML_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream((XmlUtil.nodeToString(requestMessage).getBytes()));
    }

    public boolean isReplyExpected() {
        return false;
    }
}
