package com.l7tech.server.wsdm.faults;

/**
 * A generic fault for methods described in wsn base notification
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 20, 2007<br/>
 */
public class GenericNotificationExceptionFault extends FaultMappableException {

    public GenericNotificationExceptionFault(String msg) {
        super(msg);
    }

    protected String getDetailContent() {
        return "";
    }

    protected String getWSAAction() {
        return "http://docs.oasis-open.org/wsn/fault";
    }
}

