package com.l7tech.common.xml;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 */
public final class WsTrustRequestType implements Serializable {
    private static final Map valueMap = new HashMap();
    // TODO use NS factory to obtain the correct namespace URIs lazily
    public static final WsTrustRequestType ISSUE = new WsTrustRequestType("Issue", "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue");
    public static final WsTrustRequestType VALIDATE = new WsTrustRequestType("Validate", "http://schemas.xmlsoap.org/ws/2005/02/trust/Validate");

    private final String name;
    private final String uri;

    private WsTrustRequestType(String name, String uri) {
        this.name = name;
        this.uri = uri;
        valueMap.put(uri, this);
    }

    public String toString() {
        return name;
    }

    public static WsTrustRequestType fromString(String uri) {
        return (WsTrustRequestType)valueMap.get(uri);
    }

    protected Object readResolve() {
        return fromString(uri);
    }

    public String getUri() { return uri; }
}
