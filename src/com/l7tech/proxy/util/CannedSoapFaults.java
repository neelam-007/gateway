/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

/**
 *
 * User: mike
 * Date: Jul 2, 2003
 * Time: 5:42:43 PM
 */
public class CannedSoapFaults {

    public static final String RESPONSE_NOT_XML =
            "<soapenv:Envelope" +
            " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
            " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Body>\n" +
            "  <soapenv:Fault>\n" +
            "   <faultcode>soapenv:Server</faultcode>\n" +
            "   <faultstring>Response wasn't XML</faultstring>\n" +
            "   <faultactor></faultactor>\n" +
            "   <detail>The response from the Gateway was not text/xml.</detail>\n" +
            "  </soapenv:Fault>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>\n";

    public static final String UNAUTHORIZED =
            "<soapenv:Envelope" +
            " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
            " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Body>\n" +
            "  <soapenv:Fault>\n" +
            "   <faultcode>soapenv:Client</faultcode>\n" +
            "   <faultstring>The user was not authorized to connect to this service.</faultstring>\n" +
            "   <faultactor></faultactor>\n" +
            "   <detail>We were unable to negotiate a set of credentials acceptable to the service provider.</detail>\n" +
            "  </soapenv:Fault>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>\n";

    public static final String TRY_AGAIN =
            "<soapenv:Envelope" +
            " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
            " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            " <soapenv:Body>\n" +
            "  <soapenv:Fault>\n" +
            "   <faultcode>soapenv:Server</faultcode>\n" +
            "   <faultstring>Service temporarily unavailable.</faultstring>\n" +
            "   <faultactor></faultactor>\n" +
            "   <detail>The server is temporarily unable to process your request.\n" +
            "           Please wait a short period of time and try again.</detail>\n" +
            "  </soapenv:Fault>\n" +
            " </soapenv:Body>\n" +
            "</soapenv:Envelope>\n";
}
