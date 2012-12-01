/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.common.mime.*;
import com.l7tech.util.CausedIOException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a Message that contains an outer content type and at least one part's InputStream.
 */
class MimeFacet extends MessageFacet {
    private final MimeBody mimeBody;
    private boolean bufferingDisallowed = false;

    protected MimeFacet(Message message) {
        super(message, null); // Null because this will normally be the last aspect
        mimeBody = null;
    }

    /**
     * Create a new MimeFacet for the specified message.
     *
     * @param message the Message that will own this facet.  Required.
     * @param stash a stash manager to use to store message parts for later retrieval.  Required.
     * @param ctype content type of main body.  Required.
     * @param bodyStream InputStream from which to read MIME body.  Required.
     * @param firstPartMaxBytes Maximum number of bytes to read from input stream for first MIME part, or 0 for unlimited.
     * @throws IOException if the mainInputStream cannot be read or a multipart message is not in valid MIME format
     */
    public MimeFacet(@NotNull Message message, @NotNull StashManager stash, @NotNull ContentTypeHeader ctype, @NotNull InputStream bodyStream, long firstPartMaxBytes)
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
            return getMimeBody().getEntireMessageBodyAsInputStream(bufferingDisallowed);
        }

        @Override
        public InputStream getEntireMessageBodyAsInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
            return getMimeBody().getEntireMessageBodyAsInputStream(destroyAsRead || bufferingDisallowed);
        }

        public PartInfo getFirstPart() {
            return getMimeBody().getFirstPart();
        }

        @Override
        public void setBufferingDisallowed(boolean bufferingDisallowed) {
            MimeFacet.this.bufferingDisallowed = bufferingDisallowed;
        }

        @Override
        public boolean isBufferingDisallowed() {
            return bufferingDisallowed;
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
