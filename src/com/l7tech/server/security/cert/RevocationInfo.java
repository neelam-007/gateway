/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.cert;

import java.io.InputStream;

/**
 * @author alex
*/
public class RevocationInfo {
    private final String crlUrl;
    private final Type type;
    private InputStream stream;
    private Status status;
    private Throwable thrown;

    protected RevocationInfo(String url, Type type) {
        this.crlUrl = url;
        this.type = type;
    }

    protected RevocationInfo(String url, Type type, Status status) {
        this.crlUrl = url;
        this.status = status;
        this.type = type;
    }

    public String getCrlUrl() {
        return crlUrl;
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getThrown() {
        return thrown;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream inputStream) {
        this.stream = inputStream;
    }

    public void setThrown(Throwable e) {
        this.thrown = e;
    }

    public static enum Type {
        CRL,
        OCSP
    }

    public static enum Status {
        NO_CRL_URL,
        INVALID_CRL_URL,
        MULTIPLE_CRL_URLS,
        UNSUPPORTED_URL,
        LDAP_URL_NO_QUERY,
        LDAP_CRL_MISSING,
        LDAP_CRL_NOVALUE,
        LDAP_CRL_MULTIVALUED,
        RETRIEVAL_FAILED,
        REVOKED,
        OK,
        CRL_EXCEPTION
    }
}
