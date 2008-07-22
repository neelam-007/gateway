package com.l7tech.wsdl;

import com.l7tech.common.mime.ContentTypeHeader;

import java.io.Serializable;
import java.io.IOException;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;

/**
 * Holds policy settings to enforce on a MIME attachment including a display name, maximum attachment size,
 * and list of valid content types.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartInfo implements Cloneable, Serializable {

    //- PUBLIC

    /**
     * Create an uninitialized mime part info.
     */
    public MimePartInfo() {
        contentTypes = new String[0];
    }

    /**
     * Create a named mime part info.
     *
     * @param name the name of the part
     * @param contentType the type for the part
     */
    public MimePartInfo(String name, String contentType) {
        this.name = name;
        this.contentTypes = new String[]{contentType};
    }

    /**
     * Create a template mime part info.
     *
     * @param contentType the type for the part(s)
     */
    public MimePartInfo(String contentType) {
        this.contentTypes = new String[]{contentType};
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the maximum length in kilobytes that this attachment is permitted to be.
     *
     * <p>Note that for templated parts this is the size limit for EACH occurance.</p>
     *
     * @return the maximum size
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Set the maximum length in kilobytes that this attachment is permitted to be.
     *
     * @param maxLength the maximum size.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     *
     */
    public String[] getContentTypes() {
        return contentTypes;
    }

    /**
     *
     */
    public void setContentTypes(String[] contentTypes) {
        if (contentTypes == null)
            contentTypes = new String[0];

        this.contentTypes = (String[]) new LinkedHashSet(Arrays.asList(contentTypes)).toArray(new String[0]);
        try {
            contentTypeHeaders(); // detect invalid patterns early
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid Content-Type pattern: " + e.getMessage());
        }
    }

    /**
     *
     */
    public void addContentType(String contentType) {
        if (contentType == null) throw new NullPointerException();

        Set newContentTypes = new LinkedHashSet(Arrays.asList(contentTypes));
        newContentTypes.add(contentType);
        contentTypes = (String[]) newContentTypes.toArray(new String[0]);
        contentTypeHeaders = null;  // invalidate list of parsed patterns
    }

    /**
     *
     */
    public String retrieveAllContentTypes() {
        String[] cTypes = contentTypes;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < cTypes.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(cTypes[i]);
        }

        return sb.toString();
    }

    /**
     *
     */
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

    /**
     *
     */
    public boolean isRequireSignature() {
        return requireSignature;
    }

    /**
     *
     */
    public void setRequireSignature(final boolean requireSignature) {
        this.requireSignature = requireSignature;
    }

    /**
     *
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException("Clone error", cnse);
        }
    }

    //- PRIVATE

    private String name;
    private String[] contentTypes;
    private int maxLength;
    private boolean requireSignature = false;

    private transient ContentTypeHeader[] contentTypeHeaders;

    /**
     *
     */
    private ContentTypeHeader[] contentTypeHeaders() throws IOException {
        ContentTypeHeader[] ctHeaders = contentTypeHeaders;

        if (ctHeaders == null) {
            String[] cTypes = contentTypes;
            ctHeaders = new ContentTypeHeader[cTypes.length];
            for (int i = 0; i < cTypes.length; i++) {
                String val = (String)cTypes[i];
                ctHeaders[i] = ContentTypeHeader.parseValue(val);
            }
            contentTypeHeaders = ctHeaders;
        }

        return ctHeaders;
    }
}
