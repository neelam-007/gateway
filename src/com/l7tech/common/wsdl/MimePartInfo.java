package com.l7tech.common.wsdl;

import java.io.Serializable;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartInfo implements Serializable {
    protected String name;
    protected String contentType;
    private int maxLength;

    public MimePartInfo(String name, String contentType) {
        this.name = name;
        this.contentType = contentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MimePartInfo)) return false;

        final MimePartInfo mimePartInfo = (MimePartInfo) o;

        if (maxLength != mimePartInfo.maxLength) return false;
        if (contentType != null ? !contentType.equals(mimePartInfo.contentType) : mimePartInfo.contentType != null) return false;
        if (name != null ? !name.equals(mimePartInfo.name) : mimePartInfo.name != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 29 * result + maxLength;
        return result;
    }
}
