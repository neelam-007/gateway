package com.l7tech.common.xml;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * @author alex
 */
public final class WsTrustRequestType implements Serializable {
    private static final Map valueMap = new HashMap();
    public static final WsTrustRequestType ISSUE = new WsTrustRequestType("Issue", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue");
    public static final WsTrustRequestType VALIDATE = new WsTrustRequestType("Validate", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Validate");

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
