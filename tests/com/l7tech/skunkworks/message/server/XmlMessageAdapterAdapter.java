/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.mime.*;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.message.*;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.PublishedService;
import com.l7tech.skunkworks.message.HttpRequestKnob;
import com.l7tech.skunkworks.message.Message;
import com.l7tech.skunkworks.message.MimeKnob;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * Temporary bridge class to make the new Message instances usable by the old SSG MessageProcessor.
 */
public abstract class XmlMessageAdapterAdapter implements XmlMessage {
    protected final PolicyEnforcementContext context;
    protected final Message message;

    protected XmlMessageAdapterAdapter(PolicyEnforcementContext context, Message message) {
        if (context == null || message == null) throw new NullPointerException();
        this.context = context;
        this.message = message;
    }

    public TransportMetadata getTransportMetadata() {
        return new TransportMetadata() {
            public TransportProtocol getProtocol() {
                throw new UnsupportedOperationException();
            }

            public Object getRequestParameter(String name) {
                throw new UnsupportedOperationException();
            }

            public Object getResponseParameter(String name) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void initialize(InputStream messageBody, ContentTypeHeader outerContentType) throws IOException {
        try {
            message.attachInputStream(new ByteArrayStashManager(), outerContentType, messageBody);
        } catch (NoSuchPartException e) {
            throw new CausedIOException(e);
        }
    }

    public boolean isInitialized() {
        return message.getKnob(MimeKnob.class) != null;
    }

    public boolean isMultipart() throws IOException {
        return message.getMimeKnob().isMultipart();
    }

    public PartInfo getFirstPart() throws IOException {
        return message.getMimeKnob().getFirstPart();
    }

    public PartIterator getParts() throws IOException {
        return message.getMimeKnob().getParts();
    }

    public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
        return message.getMimeKnob().getPartByContentId(contentId);
    }

    public long getContentLength() throws IOException, SAXException {
        return message.getMimeKnob().getContentLength();
    }

    public InputStream getEntireMessageBody() throws IOException, SAXException {
        try {
            return message.getMimeKnob().getEntireMessageBodyAsInputStream();
        } catch (NoSuchPartException e) {
            throw new IOException("At least one multipart part's body has been lost");
        }
    }

    public Document getDocument() throws SAXException, IOException {
        return message.getXmlKnob().getDocument();
    }

    public void setDocument(Document doc) {
        try {
            message.getMimeKnob().getFirstPart().setContentType(ContentTypeHeader.XML_DEFAULT);
            message.getXmlKnob().setDocument(doc);
        } catch (SAXException e) {
            throw new CausedIllegalStateException(e);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    public ContentTypeHeader getOuterContentType() throws IOException {
        return message.getMimeKnob().getOuterContentType();
    }

    public Collection getDeferredAssertions() {
        return context.getDeferredAssertions();
    }

    public void addDeferredAssertion(ServerAssertion owner, ServerAssertion decoration) {
        context.addDeferredAssertion(owner, decoration);
    }

    public void removeDeferredAssertion(ServerAssertion owner) {
        context.removeDeferredAssertion(owner);
    }

    public Iterator getParameterNames() {
        throw new UnsupportedOperationException();
    }

    public void setParameter(String name, Object value) {
        if (name.equals(Request.PARAM_SERVICE)) {
            context.setService((PublishedService)value);
            return;
        } else if (name.startsWith(Request.PREFIX_HTTP_HEADER)) {
            context.getResponse().getHttpResponseKnob().setHeader(name.substring(Request.PREFIX_HTTP_HEADER.length()+1), (String)value);
            return;
        } else if (name.equals(Response.PARAM_HTTP_STATUS)) {
            context.getResponse().getHttpResponseKnob().setStatus(((Integer)value).intValue());
            return;
        }

        throw new UnsupportedOperationException("Can't setParameter(" + name + ", " + value + ")");
    }

    public void setParameterIfEmpty(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    private HttpRequestKnob getRequestKnob() {
        return context.getRequest().getHttpRequestKnob();
    }

    public Object getParameter(String name) {
        try {
            return doGetParameter(name);
        } catch (IOException e) {
            throw new RuntimeException(e); // oops
        }
    }

    private Object doGetParameter(String name) throws IOException {
        if (name.startsWith("header.")) {
            return getRequestKnob().getHeaderSingleValue(name.substring(7));
        } else if (name.equals(Request.PARAM_HTTP_REQUEST_URI)) {
            return getRequestKnob().getRequestUri();
        } else if (name.equals(Request.PARAM_REMOTE_ADDR)) {
            return context.getRequest().getTcpKnob().getRemoteAddress();
        } else if (name.equals(Request.PARAM_SERVICE)) {
            return context.getService();
        }

        throw new UnsupportedOperationException("Can't getParameter(" + name + ")");
    }

    public Object[] getParameterValues(String name) {
        if (name.startsWith("header.")) {
            return getRequestKnob().getHeaderValues(name.substring(7));
        }

        throw new UnsupportedOperationException("Can't getHeaderValues(" + name + ")");
    }

    public void runOnClose(Runnable runMe) {
        context.runOnClose(runMe);
    }

    public void close() {
        context.close();
    }
}
