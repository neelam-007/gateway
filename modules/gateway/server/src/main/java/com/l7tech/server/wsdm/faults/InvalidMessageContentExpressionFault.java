package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

/**
 * An exception defined by wsn base notification
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 20, 2007<br/>
 */
public class InvalidMessageContentExpressionFault extends GenericNotificationExceptionFault {

    public InvalidMessageContentExpressionFault(String msg) {
        super(msg);
    }

    @Override
    protected String getSoapFaultDetailXML() {
        return  "      <detail>\n" +
                "        <wsntw:InvalidMessageContentExpressionFault xmlns:wsntw=\"" + Namespaces.WSNTW + "\">\n" +
                "            <wsrf-bf:Timestamp>" + now() + "</wsrf-bf:Timestamp>\n" +
                "            <wsrf-bf:Description>" + msg + "</wsrf-bf:Description>\n" +
                "        </wsntw:InvalidMessageContentExpressionFault>\n" +
                "      </detail>\n";
    }
}