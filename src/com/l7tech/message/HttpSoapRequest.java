/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpSoapRequest extends SoapRequest {
    public HttpSoapRequest( HttpTransportMetadata htm ) throws IOException {
        super( htm );
    }

    protected InputStream doGetRequestInputStream() throws IOException {
           HttpTransportMetadata htm = (HttpTransportMetadata)_transportMetadata;
           return htm.getRequest().getInputStream();
       }

    public boolean isReplyExpected() {
        return true;
    }
}
