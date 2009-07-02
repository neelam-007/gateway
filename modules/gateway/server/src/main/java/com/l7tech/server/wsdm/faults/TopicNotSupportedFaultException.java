package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

/**
 * To represent the fact that a topic is not supported.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 3, 2008<br/>
 */
public class TopicNotSupportedFaultException extends FaultMappableException {
    public TopicNotSupportedFaultException(String msg) {
        super(msg);
    }

    @Override
    protected String getSoapFaultDetailXML() {
        return  "      <detail>\n" +
                "        <wsntw:TopicNotSupportedFault xmlns:wsntw=\"" + Namespaces.WSNTW + "\" >\n" +
                "            <wsrf-bf:Timestamp>" + now() + "</wsrf-bf:Timestamp>\n" +
                "            <wsrf-bf:Description>" + msg + "</wsrf-bf:Description>\n" +
                "        </wsntw:TopicNotSupportedFault>\n" +
                "      </detail>\n";
    }
}
