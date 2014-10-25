/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.common.mime;

import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.CausedIOException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Encapsulates a collection of MimeHeaders, as might appear at the start of a MIME multipart body.
 */
public class MimeHeaders {
    private final Map<String, MimeHeader> headers;
    private final List<MimeHeader> headerOrder;

    private int serializedLength = -1;  // size in bytes of serialized form, or -1 if not currently known.

    MimeHeaders() {
        headers = new TreeMap<String, MimeHeader>(String.CASE_INSENSITIVE_ORDER);
        headerOrder = new ArrayList<MimeHeader>();
    }

    public MimeHeaders(Map<String, MimeHeader> headers) {
        Map<String, MimeHeader> newheaders = new TreeMap<String, MimeHeader>(String.CASE_INSENSITIVE_ORDER);
        newheaders.putAll(headers);
        this.headers = Collections.unmodifiableMap(newheaders);
        this.headerOrder = Collections.unmodifiableList(new ArrayList<MimeHeader>(headers.values()));
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
        headerOrder.add(header);
        serializedLength = -1;
    }

    /**
     * Replace a possibly-existing MIME header with the new header.
     * @param header the header to add or replace
     */
    public void replace(MimeHeader header) {
        final String name = header.getName();
        headers.put(name, header);
        boolean preexisting = false;
        for (int i = 0; i < headerOrder.size(); ++i) {
            MimeHeader h = headerOrder.get(i);
            if (h.getName().equalsIgnoreCase(name)) {
                headerOrder.set(i, header);
                preexisting = true;
            }
        }
        if (!preexisting)
            headerOrder.add(header);
        serializedLength = -1;
    }

    /**
     * Look up a MIME header by name (ie, "Content-Transfer-Encoding").  The lookup is case-insensitive.  If you
     * want the Content-Type header specifically, use getContentType instead since it will never return null.
     *
     * @return the MimeHeader with the specified name, or null if it did not appear in the message.
     */
    public MimeHeader get(String name) {
        return headers.get(name);
    }

    public MimeHeader get(int i) {
        return headerOrder.get(i);
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
        return ContentTypeHeader.OCTET_STREAM_DEFAULT;
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
            return MimeHeader.parseNumericValue(clen.getMainValue());
        } catch (NumberFormatException nfe) {
            throw new CausedIOException("Content-Length is not a valid number", nfe);
        }
    }

    /**
     * Get the value of the Content-ID header with any enclosing angle brackets removed.
     *
     * @param stripAngleBrackets true to strip angle brackets, or false to get the raw value.
     * @return the Content-ID, or null if it was missing or empty.
     */
    public String getContentId(boolean stripAngleBrackets) {
        MimeHeader cid = get(MimeUtil.CONTENT_ID);
        if (cid == null)
            return null;
        String value = cid.getMainValue().trim();
        if (value.length() < 1)
            return null;
        if (stripAngleBrackets) {
            if (value.startsWith("<") && value.length() > 1)
                value = value.substring(1);
            if (value.endsWith(">") && value.length() > 1)
                value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    /**
     * @return the number of MIME headers present.
     */
    public int size() {
        return headers.size();
    }

    /** @return the size of the current serialized form of these headers, in bytes, including final CRLF. */
    public int getSerializedLength() {
        if (serializedLength >= 0)
            return serializedLength;
        int len = 0;
        for (MimeHeader header : headerOrder) {
            len += header.getSerializedLength();
        }
        len += MimeHeader.CRLF.length;
        serializedLength = len;
        return len;
    }

    /**
     * @return the MIME headers re-encoded as a byte array.
     */
    public byte[] toByteArray() throws IOException {
        PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(128);
        try {
            write(out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    /**
     * Writes the headers to the specified output stream, with CRLF after each header,
     * followed by an additional CRLF.  Does not write any initial CRLF or any MIME boundaries.  Does not
     * flush or close the stream when it finishes.  May perform many short writes -- caller is responsible for
     * configuring output buffering if it's desired.  Always uses either Latin1 or (optionally) UTF-8 when
     * outputting headers, which should be OK since they are not permitted contain anything but US-ASCII anyway.
     *
     * @param os the OutputStream to write.
     * @throws IOException if the underlying OutputStream cannot be written to.
     */
    public void write(OutputStream os) throws IOException {
        for (MimeHeader h : headerOrder) {
            h.write(os);
        }
        os.write(MimeHeader.CRLF);
    }
}
