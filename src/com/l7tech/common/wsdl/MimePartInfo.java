package com.l7tech.common.wsdl;

import com.l7tech.common.mime.ContentTypeHeader;

import java.io.Serializable;
import java.io.IOException;
import java.util.Vector;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartInfo implements Serializable {
    protected String name;
    protected Object[] contentTypes = null;
    private int maxLength;

    private transient ContentTypeHeader[] contentTypeHeaders = null;

    public MimePartInfo() {
    }

    public MimePartInfo(String name, String contentType) {
        this.name = name;
        contentTypes = new String[1];
        contentTypes[0] = contentType;
        contentTypeHeaders = null; // invalidate list of parsed patterns
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object[] getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(Object[] contentTypes) {
        this.contentTypes = contentTypes;
        try {
            contentTypeHeaders(); // detect invalid patterns early
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid Content-Type pattern: " + e.getMessage());
        }
    }

    public void addContentType(String contentType) {
        Vector newContentTypes = new Vector();
        for (int i = 0; i < contentTypes.length; i++) {
            newContentTypes.add(contentTypes[i]);
        }
        // add the new content type to the list
        newContentTypes.add(contentType);
        contentTypes = newContentTypes.toArray();
        contentTypeHeaders = null;  // invalidate list of parsed patterns
    }

    private ContentTypeHeader[] contentTypeHeaders() throws IOException {
        if (contentTypeHeaders == null) {
            contentTypeHeaders = new ContentTypeHeader[contentTypes.length];
            for (int i = 0; i < contentTypes.length; i++) {
                String val = (String)contentTypes[i];
                contentTypeHeaders[i] = ContentTypeHeader.parseValue(val);
            }
        }
        return contentTypeHeaders;
    }

    public String retrieveAllContentTypes() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < contentTypes.length; i++) {
            sb.append((String) contentTypes[i]).append(", ");
        }

        String resultString = sb.toString();

        // don't show the last 2 characters
        return resultString.substring(0, resultString.length()-2);
    }

    public boolean validateContentType(ContentTypeHeader contentType) throws IOException {
        if(contentType == null) return false;

        ContentTypeHeader[] ct = contentTypeHeaders();

        for (int i = 0; i < ct.length; i++) {
            ContentTypeHeader valid = ct[i];
            if (contentType.matches(valid.getType(), valid.getSubtype()))
                return true;
        }
        // not found
        return false;
    }

    // TODO should we consider using long here instead of int
    /** @return the maximum size in bytes that this attachment is permitted to be. */
    public int getMaxLength() {
        return maxLength;
    }

    // TODO should we consider using long here instead of int
    /** @param maxLength the maximum size in bytes for this attachment. */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
