/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.url;

import com.l7tech.objectmodel.imp.NamedEntityImp;

/**
 * A cached original document, to be stored in the database to avoid having to retrieve it over a network at runtime.
 *
 * The inherited {@link #_name} field is optional.
 * 
 * @author alex
 */
public class UrlCacheRow extends NamedEntityImp {
    public UrlCacheRow() {
        super();
    }

    /**
     * The document itself
     */
    public String getContent() {
        return content;
    }

    /**
     * The original URL from which the document was fetched
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * The canonical URL from which the document can officially be downloaded
     */
    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    /**
     * The size, in bytes, of a UTF-8 serialization of the content.
     */
    public int getSize() {
        return size;
    }

    /**
     * The time, in milliseconds UTC, when this version of the document was retrieved.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * The type of resource
     */
    public UrlCacheEntryType getType() {
        return type;
    }

    /**
     * The MIME type of the resource
     */
    public String getMimeType() {
        return mimeType;
    }

    /** @deprecated only used for serialization and persistence */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setCanonicalUrl(String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setContent(String content) {
        this.content = content;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setSize(int size) {
        this.size = size;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** @deprecated only used for serialization and persistence */
    protected void setType(UrlCacheEntryType type) {
        this.type = type;
    }

    private UrlCacheEntryType type;
    private String content;
    private String originalUrl;
    private String canonicalUrl;
    private String mimeType;
    private int size;
    private long timestamp;
}
