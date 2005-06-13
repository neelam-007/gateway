package com.l7tech.common.xml;

import java.io.Serializable;
import java.util.*;

/**
 * @author alex
 */
public final class WsTrustRequestType implements Serializable {
    private static final Map valueMap = new HashMap();
    // TODO use NS factory to obtain the correct namespace URIs lazily
    public static final WsTrustRequestType ISSUE =
            new WsTrustRequestType("Issue", new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Issue" // TFIM GA
            });

    public static final WsTrustRequestType VALIDATE =
            new WsTrustRequestType("Validate", new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Validate", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Validate" // TFIM GA
            });

    public static final WsTrustRequestType RENEW =
            new WsTrustRequestType("Renew", new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Renew", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Renew" // TFIM GA
            });

    public static final WsTrustRequestType CANCEL =
            new WsTrustRequestType("Cancel", new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Cancel", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Cancel" // TFIM GA
            });

    private final String name;
    private final String[] uris;
    private final List<String> cachedUriList;

    private WsTrustRequestType(String name, String[] uris) {
        this.name = name;
        this.uris = uris;
        for (int i = 0; i < uris.length; i++) {
            String uri = uris[i];
            valueMap.put(uri, this);
        }
        cachedUriList = Collections.unmodifiableList(Arrays.asList(uris));
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public static WsTrustRequestType fromString(String uri) {
        return (WsTrustRequestType)valueMap.get(uri);
    }

    public static WsTrustRequestType[] getValues() {
        return new WsTrustRequestType[] { ISSUE, VALIDATE, RENEW, CANCEL };
    }

    protected Object readResolve() {
        return fromString(uris[0]);
    }

    public String getUri() { return uris[0]; }

    public List getUris() {
        return cachedUriList;
    }
}
