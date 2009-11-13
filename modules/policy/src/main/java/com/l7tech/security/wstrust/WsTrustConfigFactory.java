/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.wstrust;

import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.util.SyspropUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mike
 */
public class WsTrustConfigFactory {

    public static final String WST_NAMESPACE  = "http://schemas.xmlsoap.org/ws/2004/04/trust"; // pre-3.1 SSG/SSB
    public static final String WST_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/trust"; // FIM
    public static final String WST_NAMESPACE3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512"; // WS-SX

    public static final String WSP_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/12/policy";
    public static final String WSP_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/09/policy"; // FIM
    public static final String WSP_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2004/09/policy"; // WS-SX

    public static final String WSA_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
    public static final String WSA_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/08/addressing"; // FIM
    public static final String WSA_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2004/08/addressing"; // WS-SX

    // Backward compat with old Bridge operating environments: detect overridden WS-Trust URI system prop and DTRT
    public static final String PROP_WST_NS = "com.l7tech.common.security.wstrust.ns.wst";
    public static final String PROP_WST_REQUESTTYPEINDEX = "com.l7tech.common.security.wstrust.requestTypeIndex";
    private static final String tscWstNs = SyspropUtil.getString(PROP_WST_NS, WST_NAMESPACE2);
    private static final int DEFAULT_WST_REQUESTTYPEINDEX = 0;
    private static int tscWstRequestTypeIndex = SyspropUtil.getInteger(PROP_WST_REQUESTTYPEINDEX, DEFAULT_WST_REQUESTTYPEINDEX);


    private static final Map<String,WsTrustConfig> configs = new HashMap<String,WsTrustConfig>();
    static {
        configs.put(WST_NAMESPACE, new OldWsTrustConfig(WST_NAMESPACE, WSP_NAMESPACE, WSA_NAMESPACE));
        configs.put(WST_NAMESPACE2, new OldWsTrustConfig(WST_NAMESPACE2, WSP_NAMESPACE2, WSA_NAMESPACE2));
        configs.put(WST_NAMESPACE3, new WsSxWsTrustConfig(WST_NAMESPACE3, WSP_NAMESPACE3, WSA_NAMESPACE3));
    }


    /**
     * Get the WS-Trust configuration to use with the specified WS-Trust namespace URI.
     *
     * @param wstNamespaceUri  the WS-Trust namespace URI.  Must not be null.
     * @return the WsTrustConfig for this URI.  Never null.
     * @throws WsTrustConfigException if this URI is not recognized.
     */
    public static WsTrustConfig getWsTrustConfigForNamespaceUri(String wstNamespaceUri) throws WsTrustConfigException {
        WsTrustConfig got = configs.get(wstNamespaceUri);
        if (got == null)
            throw new WsTrustConfigException("Unrecognized WS-Trust namespace URI: " + wstNamespaceUri);
        return got;
    }


    /**
     * Get the current default WS-Trust configuration for the current environment.
     *
     * @return  a WsTrustConfig instance.  Never null.
     */
    public static WsTrustConfig getDefaultWsTrustConfig() {
        try {
            return getWsTrustConfigForNamespaceUri(tscWstNs);
        } catch (WsTrustConfigException e) {
            return new OldWsTrustConfig(WST_NAMESPACE2, WSP_NAMESPACE2, WSA_NAMESPACE2);
        }
    }

    private static class OldWsTrustConfig extends WsTrustConfig {
        private final Map<String,String> requestTypeMap;

        public OldWsTrustConfig(String wstNs, String wspNs, String wsaNs) {
            super(wstNs, wspNs, wsaNs);
            requestTypeMap = new HashMap<String,String>();
            int idx = tscWstRequestTypeIndex;
            if (idx < 0 || idx >= WsTrustRequestType.ISSUE.getUris().size()) idx = DEFAULT_WST_REQUESTTYPEINDEX;
            requestTypeMap.put(WsTrustRequestType.NAME_ISSUE, WsTrustRequestType.ISSUE.getUris().get(idx));
            requestTypeMap.put(WsTrustRequestType.NAME_RENEW, WsTrustRequestType.RENEW.getUris().get(idx));
            requestTypeMap.put(WsTrustRequestType.NAME_VALIDATE, WsTrustRequestType.VALIDATE.getUris().get(idx));
            requestTypeMap.put(WsTrustRequestType.NAME_CANCEL, WsTrustRequestType.CANCEL.getUris().get(idx));
        }

        @Override
        protected String getRequestTypeUri(WsTrustRequestType requestType) {
            String ret = requestTypeMap.get(requestType.getName());
            if (ret == null) throw new IllegalArgumentException("Unknown WsTrustRequestType: " + requestType.getName());
            return ret;
        }
    }

    private static class WsSxWsTrustConfig extends WsTrustConfig {
        private final Map<String,String> requestTypeMap;

        public WsSxWsTrustConfig(String wstNs, String wspNs, String wsaNs) {
            super(wstNs, wspNs, wsaNs);
            requestTypeMap = new HashMap<String,String>();
            requestTypeMap.put(WsTrustRequestType.NAME_ISSUE, "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue");
            requestTypeMap.put(WsTrustRequestType.NAME_RENEW, "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Renew");
            requestTypeMap.put(WsTrustRequestType.NAME_VALIDATE, "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Validate");
            requestTypeMap.put(WsTrustRequestType.NAME_CANCEL, "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel");
        }

        @Override
        protected String getRequestTypeUri(WsTrustRequestType requestType) {
            String ret = requestTypeMap.get(requestType.getName());
            if (ret == null) throw new IllegalArgumentException("Unknown WsTrustRequestType: " + requestType.getName());
            return ret;
        }

    }
}
