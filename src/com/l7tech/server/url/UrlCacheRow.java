/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.url;

import com.l7tech.objectmodel.imp.EntityImp;

/**
 * @author alex
 * @version $Revision$
 */
public class UrlCacheRow extends EntityImp {
    public UrlCacheRow() {
        super();
    }

    public String getContent() {
        return _content;
    }

    public void setContent(String content) {
        _content = content;
    }

    public String getUrl() {
        return _url;
    }

    public void setUrl(String url) {
        _url = url;
    }

    public int getSize() {
        return _size;
    }

    public void setSize(int size) {
        _size = size;
    }

    public long getTimestamp() {
        return _timestamp;
    }

    public void setTimestamp(long timestamp) {
        _timestamp = timestamp;
    }

    private String _content;
    private String _url;
    private int _size;
    private long _timestamp;
}
