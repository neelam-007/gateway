/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message.server;

import com.l7tech.common.mime.*;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.message.TransportMetadata;
import com.l7tech.message.TransportProtocol;
import com.l7tech.message.XmlMessage;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.skunkworks.message.Message;
import com.l7tech.skunkworks.message.MimeKnob;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author mike
 */
public abstract class XmlMessageAdapterAdapter implements XmlMessage {
    protected final MessageContext context;
    protected final Message message;

    protected XmlMessageAdapterAdapter(MessageContext context, Message message) {
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
        throw new UnsupportedOperationException();
    }

    public void setParameterIfEmpty(String name, Object value) {
        throw new UnsupportedOperationException();
    }

    public Object getParameter(String name) {
        throw new UnsupportedOperationException();
    }

    public Object[] getParameterValues(String name) {
        throw new UnsupportedOperationException();
    }


    public void runOnClose(Runnable runMe) {
        context.runOnClose(runMe);
    }

    public void close() {
        context.close();
    }
}
