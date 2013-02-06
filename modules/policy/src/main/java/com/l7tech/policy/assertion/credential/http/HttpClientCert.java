/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * This class isn't actually used except to fill a hole in the inheritance hierarchy due to the bizarre way that
 * ServerSslAssertion delegates to ServerHttpClientCert.  Don't ask.
 *  
 * @author alex
 */
public class HttpClientCert extends HttpCredentialSourceAssertion {
    private boolean checkCertValidity = true;

    public HttpClientCert() {
    }

    public HttpClientCert(boolean checkCertValidity) {
        this.checkCertValidity = checkCertValidity;
    }

    public boolean isCheckCertValidity() {
        return checkCertValidity;
    }

    public void setCheckCertValidity(boolean checkCertValidity) {
        this.checkCertValidity = checkCertValidity;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "HTTP client certificate");
        meta.put(AssertionMetadata.DESCRIPTION, "The requestor must use a client cert as part of the SSL handshake.");
        return meta;
    }
}
