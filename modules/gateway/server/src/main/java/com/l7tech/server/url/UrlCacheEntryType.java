/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.url;

/**
 * @author alex
 */
public enum UrlCacheEntryType {
    WSDL("http://schemas.xmlsoap.org/wsdl/"),
    SCHEMA("http://www.w3.org/2001/XMLSchema"),
    XSLT("http://www.w3.org/1999/XSL/Transform"),
    DTD(null),
    XML(null),
    PLAIN_TEXT(null),
    BINARY(null),
    OTHER(null),
    ;

    private UrlCacheEntryType(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    /**
     * @return the usual namespace URI of this type of document, or <code>null</code> if no meaningful generalization 
     * can be made.
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    private final String namespaceUri;
}
