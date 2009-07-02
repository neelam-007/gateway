package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

/**
 * wsrf-rpw:InvalidResourcePropertyQNameFault
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 21, 2007<br/>
 */
public class InvalidResourcePropertyQNameFault extends GenericWSRFExceptionFault {
    public InvalidResourcePropertyQNameFault(String msg) {
        super(msg);
    }

    @Override
    protected String getWSAAction() {
        return "http://docs.oasis-open.org/wsrf/rpw-2/GetMultipleResourceProperties/InvalidResourcePropertyQNameFault";
    }

    @Override
    protected String getSoapFaultDetailXML() {
        return  "      <detail>\n" +
                "        <wsrf-rpw:InvalidResourcePropertyQNameFault xmlns:wsrf-rpw=\"" + Namespaces.WSRF_RPW + "\">\n" +
                "            <wsrf-bf:Timestamp>" + now() + "</wsrf-bf:Timestamp>\n" +
                "            <wsrf-bf:Description>" + msg + "</wsrf-bf:Description>\n" +
                "        </wsrf-rpw:InvalidResourcePropertyQNameFault>\n" +
                "      </detail>\n";
    }
}
