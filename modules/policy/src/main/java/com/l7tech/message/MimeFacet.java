/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.common.mime.*;
import com.l7tech.util.CausedIOException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a Message that contains an outer content type and at least one part's InputStream.
 */
class MimeFacet extends MessageFacet {
    private final MimeBody mimeBody;

    protected MimeFacet(Message message) {
        super(message, null); // Null because this will normally be the last aspect
        mimeBody = null;
    }

    public MimeFacet(Message message, StashManager stash, ContentTypeHeader ctype, InputStream bodyStream, long firstPartMaxBytes)
            throws IOException
    {
        super(message, null); // Null because this will normally be the last aspect
        this.mimeBody = new MimeBody(stash, ctype, bodyStream, firstPartMaxBytes);
    }

    public MessageKnob getKnob(Class c) {
        if (c == MimeKnob.class)
            return new MimeMessageKnob();
        return super.getKnob(c);
    }

    private final class MimeMessageKnob implements MimeKnob {
        public boolean isMultipart() {
            return getMimeBody().isMultipart();
        }

        public PartIterator getParts() {
            return getMimeBody().iterator();
        }

        public PartIterator iterator() {
            return getParts();
        }

        public PartInfo getPart(int num) throws IOException, NoSuchPartException {
            return getMimeBody().getPart(num);
        }

        private MimeBody getMimeBody() {
            return mimeBody;
        }

        public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
            return getMimeBody().getPartByContentId(contentId);
        }

        public void setContentLengthLimit(long sizeLimit) throws IOException {
            getMimeBody().setBodyLengthLimit(sizeLimit);
        }
        public long getContentLengthLimit()  {
            return getMimeBody().getBodyLengthLimit();
        }

        public ContentTypeHeader getOuterContentType() {
            return getMimeBody().getOuterContentType();
        }

        @Override
        public void setOuterContentType(ContentTypeHeader contentType) {
            // TODO should some or all of the message and its knobs be reinitialized by a change to outer content type?
            getMimeBody().setOuterContentType(contentType);
        }

        public long getContentLength() throws IOException {
            try {
                return getMimeBody().getEntireMessageBodyLength();
            } catch (NoSuchPartException e) {
                throw new CausedIOException(e);
            }
        }

        public void setStreamValidatedPartsOnly() {
            getMimeBody().setEntireMessageBodyAsInputStreamIsValidatedOnly();
        }

        public InputStream getEntireMessageBodyAsInputStream() throws IOException, NoSuchPartException {
            return getMimeBody().getEntireMessageBodyAsInputStream(false);
        }

        @Override
        public InputStream getEntireMessageBodyAsInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
            return getMimeBody().getEntireMessageBodyAsInputStream(destroyAsRead);
        }

        public PartInfo getFirstPart() {
            return getMimeBody().getFirstPart();
        }
    }

    public void close() {
        try {
            mimeBody.close();
        } finally {
            super.close();
        }
    }
}
