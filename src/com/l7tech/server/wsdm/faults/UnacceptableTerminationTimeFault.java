package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

/**
 * A soap fault defined by the wsn base notification spec
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 20, 2007<br/>
 */
public class UnacceptableTerminationTimeFault extends GenericNotificationExceptionFault {

    public UnacceptableTerminationTimeFault(String msg) {
        super(msg);
    }

    protected String getDetailContent() {
        return  "        <wsntw:UnacceptableTerminationTimeFault xmlns:wsntw=\"" + Namespaces.WSNTW + "\" xmlns:wsnt=\"" + Namespaces.WSNT + "\">\n" +
                "            <wsrf-bf:Timestamp>" + now() + "</wsrf-bf:Timestamp>\n" +
                "            <wsrf-bf:Description>" + msg + "</wsrf-bf:Description>\n" +
                "            <wsnt:MinimumTime>" + now() + "</wsnt:MinimumTime>\n" +
                "        </wsntw:UnacceptableTerminationTimeFault>\n";
    }
}
