/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

import com.l7tech.common.util.CausedIOException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Encapsulates a collection of MimeHeaders, as might appear at the start of a MIME multipart body.
 */
public class MimeHeaders {
    private final Map headers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    public static final ContentTypeHeader DEFAULT_CONTENT_TYPE = new ContentTypeHeader("application",
                                                                                       "octet-stream",
                                                                                       Collections.EMPTY_MAP);

    MimeHeaders() {
    }

    /**
     * Add a MIME header to this collection.
     * TODO: make this package private
     *
     * @param header the header to add
     * @throws IOException if this header already exists in this collection of MimeHeaders
     */
    public void add(MimeHeader header) throws IOException {
        final String name = header.getName();
        if (headers.containsKey(name))
            throw new IOException("Duplicate MIME header: " + name);
        if (MimeUtil.CONTENT_TYPE.equalsIgnoreCase(name) && !(header instanceof ContentTypeHeader))
            throw new IllegalStateException("Content-Type header was not stored in ContentTypeHeader instance");
        headers.put(name, header);
    }

    /**
     * Look up a MIME header by name (ie, "Content-Transfer-Encoding").  The lookup is case-insensitive.  If you
     * want the Content-Type header specifically, use getContentType instead since it will never return null.
     *
     * @return the MimeHeader with the specified name, or null if it did not appear in the message.
     */
    public MimeHeader get(String name) {
        return (MimeHeader)headers.get(name);
    }

    /**
     * Get the Content-Type header, or a reasonable default.
     *
     * @return the Content-Type header, or a default content type if there wasn't one.
     */
    public ContentTypeHeader getContentType() {
        MimeHeader ctype = get(MimeUtil.CONTENT_TYPE);
        if (ctype instanceof ContentTypeHeader)
            return (ContentTypeHeader)ctype;
        return DEFAULT_CONTENT_TYPE;
    }

    /** @return true if there is a Content-Length header. */
    public boolean hasContentLength() {
        return headers.containsKey(MimeUtil.CONTENT_LENGTH);
    }

    /**
     * @return the Content-Length, which might be zero.  Only valid if hasContentLength() is true.
     * @throws IOException if there is no Content-Length header, or if it is not a valid long.
     */
    public long getContentLength() throws IOException {
        MimeHeader clen = get(MimeUtil.CONTENT_LENGTH);
        if (clen == null)
            throw new IllegalStateException("No Content-Length header.");
        try {
            return Long.parseLong(clen.getValue());
        } catch (NumberFormatException nfe) {
            throw new CausedIOException("Content-Length is not a valid number", nfe);
        }
    }

    /**
     * Get the value of the Content-ID header with any enclosing angle brackets removed.
     *
     * @return the Content-ID, or null if it was missing or empty.
     */
    public String getContentId() {
        MimeHeader cid = get(MimeUtil.CONTENT_ID);
        if (cid == null)
            return null;
        String value = cid.getValue().trim();
        if (value.length() < 1)
            return null;
        if (value.startsWith("<") && value.length() > 1)
            value = value.substring(1);
        if (value.endsWith(">") && value.length() > 1)
            value = value.substring(0, value.length() - 1);
        return value;
    }

    /**
     * @return the number of MIME headers present.
     */
    public int size() {
        return headers.size();
    }

    /**
     * Writes the headers to the specified output stream, with CRLF after each header,
     * followed by an additional CRLF.  Does not write any initial CRLF or any MIME boundaries.  Does not
     * flush or close the stream when it finishes.  May perform many short writes -- caller is responsible for
     * configuring output buffering if it's desired.  Always uses UTF-8 when outputting headers, which should
     * be OK since they are not permitted contain anything but US-ASCII anyway.
     *
     * @param os the OutputStream to write.
     * @throws IOException if the underlying OutputStream cannot be written to.
     */
    public void write(OutputStream os) throws IOException {
        for (Iterator i = headers.values().iterator(); i.hasNext();) {
            MimeHeader h = (MimeHeader)i.next();
            h.write(os);
        }
        os.write(MimeHeader.CRLF);
    }
}
