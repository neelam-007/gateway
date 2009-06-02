package com.l7tech.xml.soap;

import javax.xml.soap.SOAPConstants;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 17-Jun-2008
 * Time: 10:52:13 PM
 * To change this template use File | Settings | File Templates.
 */
public enum SoapVersion {
    SOAP_1_1(SOAPConstants.SOAP_1_1_PROTOCOL, 1001),
    SOAP_1_2(SOAPConstants.SOAP_1_2_PROTOCOL, 1002),
    UNKNOWN(null, 0);

    private final String protocol;
    private final int rank;

    private SoapVersion(final String protocol, final int rank ) {
        this.protocol = protocol;
        this.rank = rank;
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

    public static SoapVersion namespaceToSoapVersion(String namespace) {
        if(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(namespace)) {
            return SOAP_1_2;
        } else if(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(namespace)) {
            return SOAP_1_1;
        } else {
            return UNKNOWN;
        }
    }
}
