/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import org.apache.axis.AxisFault;
import org.apache.axis.attachments.MultiPartRelatedInputStream;

import java.io.InputStream;

/**
 * @author alex
 * @version $Revision$
 */
public class MultipartInputStream extends MultiPartRelatedInputStream {
    /**
     * Multipart stream.
     *
     * @param contentType the string that holds the contentType
     * @param stream is  the true input stream from where the source.
     */
    public MultipartInputStream( String contentType, InputStream stream ) throws AxisFault {
        super( contentType, stream );
    }

    public byte[] getBoundary() {
        return boundary;
    }
}
