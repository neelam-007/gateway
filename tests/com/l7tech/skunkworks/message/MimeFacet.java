/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import com.l7tech.common.mime.*;
import com.l7tech.common.util.CausedIOException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a Message that contains an outer content type and at least one part's InputStream.
 */
class MimeFacet extends MessageFacet {
    private final MimeBody mimeBody;

    protected MimeFacet(Message message) {
        super(message, null); // Null because this will normally be the root aspect
        mimeBody = null;
    }

    public MimeFacet(Message message, StashManager stash, ContentTypeHeader ctype, InputStream bodyStream)
            throws NoSuchPartException, IOException
    {
        super(message, null); // Null because this will normally be the root aspect
        this.mimeBody = new MimeBody(stash, ctype, bodyStream);
    }

    public Knob getKnob(Class c) {
        if (c == MimeKnob.class)
            return new MimeMessageKnob();
        return super.getKnob(c);
    }

    private final class MimeMessageKnob implements MimeKnob {
        public boolean isMultipart() {
            return mimeBody.isMultipart();
        }

        public PartIterator getParts() {
            return getMimeBody().iterator();
        }

        private MimeBody getMimeBody() {
            return mimeBody;
        }

        public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
            return getMimeBody().getPartByContentId(contentId);
        }

        public ContentTypeHeader getOuterContentType() {
            return getMimeBody().getOuterContentType();
        }

        public long getContentLength() throws IOException {
            try {
                return getMimeBody().getEntireMessageBodyLength();
            } catch (NoSuchPartException e) {
                throw new CausedIOException(e);
            }
        }

        public InputStream getEntireMessageBodyAsInputStream() throws IOException, NoSuchPartException {
            return getMimeBody().getEntireMessageBodyAsInputStream(false);
        }

        public PartInfo getFirstPart() {
            return mimeBody.getFirstPart();
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
