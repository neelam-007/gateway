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
    SOAP_1_1(SOAPConstants.SOAP_1_1_PROTOCOL),
    SOAP_1_2(SOAPConstants.SOAP_1_2_PROTOCOL),
    UNKNOWN(null);

    private String protocol;

    private SoapVersion(String protocol) {
        this.protocol = protocol;
    }

    public String getProtocol() {
        return protocol;
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
