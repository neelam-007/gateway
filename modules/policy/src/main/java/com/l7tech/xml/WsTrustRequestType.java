package com.l7tech.xml;

import com.l7tech.util.EnumTranslator;

import java.io.Serializable;
import java.util.*;

/**
 * @author alex
 */
public final class WsTrustRequestType implements Serializable {
    private static final Map<String,WsTrustRequestType> valueMap = new HashMap<String,WsTrustRequestType>();

    public static final String NAME_ISSUE = "Issue";
    public static final String NAME_VALIDATE = "Validate";
    public static final String NAME_RENEW = "Renew";
    public static final String NAME_CANCEL = "Cancel";

    // TODO use NS factory to obtain the correct namespace URIs lazily
    public static final WsTrustRequestType ISSUE =
            new WsTrustRequestType(NAME_ISSUE, new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Issue", // TFIM GA
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue",  // WS-SX 2005-12
            });

    public static final WsTrustRequestType VALIDATE =
            new WsTrustRequestType(NAME_VALIDATE, new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Validate", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Validate", // TFIM GA
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Validate",  // WS-SX 2005-12
            });

    public static final WsTrustRequestType RENEW =
            new WsTrustRequestType(NAME_RENEW, new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Renew", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Renew", // TFIM GA
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Renew",  // WS-SX 2005-12
            });

    public static final WsTrustRequestType CANCEL =
            new WsTrustRequestType(NAME_CANCEL, new String[] {
                "http://schemas.xmlsoap.org/ws/2005/02/trust/Cancel", // TFIM beta
                "http://schemas.xmlsoap.org/ws/2005/02/security/trust/Cancel", // TFIM GA
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel",  // WS-SX 2005-12
            });

    private final String name;
    private final String[] uris;
    private final List<String> cachedUriList;

    private WsTrustRequestType(String name, String[] uris) {
        this.name = name;
        this.uris = uris;
        for ( String uri : uris ) {
            valueMap.put( uri, this );
        }
        cachedUriList = Collections.unmodifiableList(Arrays.asList(uris));
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public static WsTrustRequestType fromString(String uri) {
        return valueMap.get(uri);
    }

    public static WsTrustRequestType[] getValues() {
        return new WsTrustRequestType[] { ISSUE, VALIDATE, RENEW, CANCEL };
    }

    protected Object readResolve() {
        return fromString(uris[0]);
    }

    public String getUri() { return uris[0]; }

    public List<String> getUris() {
        return cachedUriList;
    }

    // This method is invoked reflectively by WspEnumTypeMapping
    public static EnumTranslator getEnumTranslator() {
        return new EnumTranslator() {
            @Override
            public Object stringToObject(String value) {
                return WsTrustRequestType.fromString(value);
            }

            @Override
            public String objectToString(Object target) {
                return ((WsTrustRequestType)target).getUri();
            }
        };
    }
}
