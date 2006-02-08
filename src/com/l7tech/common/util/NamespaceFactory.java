/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.util;

import org.w3c.dom.Element;

/**
 * Produces certain needed namespaces (and desired prefixes) on demand.
 * Special methods are provided to look up the most common namespaces.
 */
public class NamespaceFactory {
    protected String wsseNs = SoapUtil.SECURITY_NAMESPACE;
    protected String wsuNs = SoapUtil.WSU_NAMESPACE;

    public NamespaceFactory() {
    }

    public NamespaceFactory(String wsseNs, String wsuNs) {
        this.wsseNs = wsseNs;
        this.wsuNs = wsuNs;
    }

    public String getWsseNs() {
        return wsseNs;
    }

    public String getWsuNs() {
        return wsuNs;
    }

    /**
     * Translate a ValueType URI into a URI understandable to the intended beneficiary of this NamespaceFactory.
     * As this might require converting the URI into a qname, be warned that the parent element may be modified
     * to insert a new namespace declaration if one is not already available.
     * <p/>
     * This method will return any unrecognized value types unmodified.
     *
     * @param in      the valuetype to translate.  Should be the full URI defined by the final standard; see {@link com.l7tech.common.util.SoapUtil} for definitions.
     * @param parent  the parent of the element in which this value type attribute is to be used.  Must not be null.  Warning: might be modified to add a namespace decl.
     * @return possibly-translated ValueType URI.  May be null or empty if the input value was null or empty.
     */
    public String getValueType(String in, Element parent) {
        if (!SoapUtil.SECURITY_NAMESPACE.equals(wsseNs)) {
            // Current wsse namespace is not the final WS-S 1.0 URI; switch to using qname attributes for toolkit compat
            if (SoapUtil.VALUETYPE_ENCRYPTED_KEY.equals(in))
                return getWssePrefix(parent) + ":EncryptedKey";

            if (SoapUtil.VALUETYPE_X509.equals(in))
                return getWssePrefix(parent) + ":X509v3";

            if (SoapUtil.VALUETYPE_SKI.equals(in))
                return getWssePrefix(parent) + ":X509SubjectKeyIdentifier";
        }
        return in;
    }

    /**
     * Translate an EncodingType URI into a URI understandable to the intended beneficiary of this NamespaceFactory.
     * As this might require converting the URI into a qname, be warned that the parent element may be modifed
     * to insert a new namespace declaration if one is not already available.
     * <p/>
     * This method will return any unrecognized encoding types unmodified.
     *
     * @param in     the EncodingType to translate.  Should be the full URI defined by the final standard; see {@link com.l7tech.common.util.SoapUtil} for definitions.
     * @param parent the parent of the elment in which this encoding type attribute is to be used.  Must not be null.  Warning: might be modified to add a namespace decl.
     * @return possibly-translated EncodingType URI.  May be null or empty if the input value was null or empty.
     */
    public String getEncodingType(String in, Element parent) {
        if (!SoapUtil.SECURITY_NAMESPACE.equals(wsseNs)) {
            // Current wsse namespace is not the final WS-S 1.0 URI; switch to using qname attributes for toolkit compat
            if (SoapUtil.ENCODINGTYPE_BASE64BINARY.equals(in))
                return getWssePrefix(parent) + ":Base64Binary";
        }
        return in;
    }

    /**
     * Change the wsse namespace used by future calls to this NamespaceFactory.  Note that this may also change
     * the URIs returned by getValueType and getEncodingType.
     *
     * @param wsseNs  the new WSSE namespace URI.  Must not be null or empty.
     */
    public void setWsseNs(String wsseNs) {
        if (wsseNs == null || wsseNs.length() < 1) throw new IllegalArgumentException("wsseNs must be provided");
        this.wsseNs = wsseNs;
    }

    /**
     * Change the wsu namespace used by future calls to this NamespaceFactory.
     *
     * @param wsuNs  thew new WSU namespace URI.  Must not be null or empty.
     */
    public void setWsuNs(String wsuNs) {
        if (wsuNs == null || wsuNs.length() < 1) throw new IllegalArgumentException("wsuNs must be provided");
        this.wsuNs = wsuNs;
    }

    /**
     * Get the "wsse" prefix to use in scope for the specified element.  Note that this might cause a new namespace
     * declaration to be added to the element.
     *
     * @return the wsse prefix to use for element that yields the current wsse namespace URI.  Never null or empty.
     */
    private String getWssePrefix(Element parent) {
        return XmlUtil.getOrCreatePrefixForNamespace(parent, getWsseNs(), "wsse");
    }
}
