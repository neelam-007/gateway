package com.l7tech.xml.soap;

import javax.xml.soap.SOAPConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a specific or unknown SOAP version.
 */
public enum SoapVersion {
    SOAP_1_1("SOAP 1.1", "1.1", SOAPConstants.SOAP_1_1_PROTOCOL, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, 1001, "text/xml"),
    SOAP_1_2("SOAP 1.2", "1.2", SOAPConstants.SOAP_1_2_PROTOCOL, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, 1002, "application/soap+xml"),
    UNKNOWN("unspecified", "", null, null, 0, null);

    private final String protocol;
    private final int rank;
    private final String namespaceUri;
    private final String label;
    private final String versionNumber;
    private final String contentType;

    private SoapVersion(final String label, final String versionNumber, final String protocol, final String namespaceUri, final int rank, final String contentType ) {
        this.protocol = protocol;
        this.namespaceUri = namespaceUri;
        this.rank = rank;
        this.label = label;
        this.versionNumber = versionNumber;
        this.contentType = contentType;
    }

    /**
     * @return the friendly human-readable name of this SOAP version, e.g. "SOAP 1.1" or "unspecified".
     */
    public String getLabel() {
        return label;
    }

    public String getProtocol() {
        return protocol;
    }

    /**
     * Is the given SoapVersion earlier than this SoapVersion.
     *
     * @param version The SoapVersion to check.
     * @return true if earlier, false if same or more recent.
     */
    public boolean isPriorVersion( final SoapVersion version ) {
        return version.rank < rank;        
    }

    /**
     * @return the envelope namespace URI for this SOAP version, or null for UNKNOWN version.
     */
    public String getNamespaceUri() {
        return namespaceUri;
    }

    /**
     * @return all known envelope namespace URIs except for the one used by this SOAP version.
     *         Never null, but may be an empty set if the version is UNKNOWN.
     */
    public Set<String> getOtherNamespaceUris() {
        return getOtherNamespaceUris(this);
    }

    /**
     * @return the base content type required by this SOAP version (omitting any properties such as charset etc).
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return a version number string, such as "1.1", "1.2" or "".  Never null, but may be empty for {@link SoapVersion#UNKNOWN}.
     */
    public String getVersionNumber() {
        return versionNumber;
    }

    /**
     * @param namespace the namespace URI to look up.
     * @return the SoapVersion that uses this namespace, or UNKNOWN if there was no match.
     */
    public static SoapVersion namespaceToSoapVersion(String namespace) {
        if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(namespace)) {
            return SOAP_1_2;
        } else if(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(namespace)) {
            return SOAP_1_1;
        } else {
            return UNKNOWN;
        }
    }

    /**
     * Get the SoapVersion for the given version text.
     *
     * @param versionNumber The version number, e.g. "1.1"
     * @return The soap version that matches or UNKNOWN
     */
    public static SoapVersion versionNumberToSoapVersion( final String versionNumber ) {
        SoapVersion soapVersion = SoapVersion.UNKNOWN;

        for ( final SoapVersion version : values() ) {
            if ( version.getVersionNumber().equals( versionNumber )) {
                soapVersion = version;
                break;
            }
        }

        return soapVersion;
    }

    /**
     * Lookup a SoapVersion by its content type header.
     *
     * @param contentTypeBase a base content type, ie just "text/xml" or "application/soap+xml".  Required.
     * @return the corresponding SoapVersion.  Never null, but may be UNKNOWN.
     */
    public static SoapVersion contentTypeToSoapVersion(String contentTypeBase) {
        // For now, there's only two, so we'll just hardcode it
        if ("text/xml".equalsIgnoreCase(contentTypeBase)) {
            return SoapVersion.SOAP_1_1;
        } else if ("application/soap+xml".equalsIgnoreCase(contentTypeBase)) {
            return SoapVersion.SOAP_1_2;
        } else {
            return SoapVersion.UNKNOWN;
        }
    }

    static Set<String> getOtherNamespaceUris(SoapVersion soapVersion) {
        // For now, there's only two, so we'll just hardcode it
        switch (soapVersion) {
            case SOAP_1_1: return OTHER_THAN_SOAP_1_1;
            case SOAP_1_2: return OTHER_THAN_SOAP_1_2;
            default: return OTHER_THAN_UNKNOWN;
        }
    }

    static final Set<String> OTHER_THAN_SOAP_1_1 = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(SOAP_1_2.getNamespaceUri())));
    static final Set<String> OTHER_THAN_SOAP_1_2 = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(SOAP_1_1.getNamespaceUri())));
    static final Set<String> OTHER_THAN_UNKNOWN = Collections.unmodifiableSet(Collections.<String>emptySet());
}
