package com.l7tech.proxy.attachments;

import com.l7tech.common.attachments.MultipartMessageReader;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ClientMultipartMessageReader extends MultipartMessageReader {
    private final Logger logger = Logger.getLogger(getClass().getName());
    
    public ClientMultipartMessageReader(InputStream inputStream, String multipartBoundary) {
        pushbackInputStream = new PushbackInputStream(inputStream, SOAP_PART_BUFFER_SIZE);
        this.multipartBoundary = multipartBoundary;
    }

}
