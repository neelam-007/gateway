package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

/**
 * A soap fault defined by the wsn base notification spec. If thrown in the context of a call to
 * GetMultipleResourceProperties, the default wsa:action must be changed to
 * http://docs.oasis-open.org/wsrf/rpw-2/GetMultipleResourceProperties/ResourceUnknownFault
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 20, 2007<br/>
 */
public class ResourceUnknownFault extends GenericNotificationExceptionFault {

    public ResourceUnknownFault(String msg) {
        super(msg);
    }

    protected String getDetailContent() {
        return  "        <wsrf-rw:ResourceUnknownFault xmlns:wsrf-rw=\"" + Namespaces.WSRF_RW + "\" >\n" +
                "            <wsrf-bf:Timestamp>" + now() + "</wsrf-bf:Timestamp>\n" +
                "            <wsrf-bf:Description>" + msg + "</wsrf-bf:Description>\n" +
                "        </wsrf-rw:ResourceUnknownFault>\n";
    }
}
