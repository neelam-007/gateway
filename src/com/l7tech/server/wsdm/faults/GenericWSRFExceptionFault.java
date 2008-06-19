package com.l7tech.server.wsdm.faults;

/**
 * Base class for WSRF faults
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 20, 2007<br/>
 */
public class GenericWSRFExceptionFault extends FaultMappableException {

    public GenericWSRFExceptionFault(String msg) {
        super(msg);
    }

    protected String getDetailContent() {
        return "";
    }

    protected String getFaultString() {
        return getMessage();
    }

    protected String getWSAAction() {
        return "http://docs.oasis-open.org/wsrf/fault";
    }
}
