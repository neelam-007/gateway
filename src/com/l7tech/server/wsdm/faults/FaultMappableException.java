package com.l7tech.server.wsdm.faults;

import com.l7tech.common.util.ISO8601Date;
import com.l7tech.server.wsdm.Namespaces;

import java.util.Date;

/**
 * Exceptions thrown containing faults to be returned to requesters. Meant to be subclassed.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 19, 2007<br/>
 */
public class FaultMappableException extends Exception {
    private String actor;
    protected String msg;

    public FaultMappableException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public String getSoapFaultXML() {
        return
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsa=\"" + Namespaces.WSA + "\" xmlns:wsrf-bf=\"" + Namespaces.WSRF_BF + "\">\n" +
        "  <soap:Header>\n" +
        "    <wsa:Action>\n" +
        "      " + getWSAAction() + "\n" +
        "    </wsa:Action>\n" +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <soap:Fault>\n" +
        "      <faultcode>soap:Client</faultcode>\n" +
        "      <faultstring>" + getFaultString() + "</faultstring>\n" +
        (getActor()==null ? "" : "      <faultactor>" + getActor() + "</faultactor>\n") +
        "      <detail>\n" +
                getDetailContent() +
        "      </detail>\n" +
        "    </soap:Fault>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }

    /**
     * Subclasses may override to provide details.
     *
     * @return The detail content for the fault.
     */
    protected String getDetailContent(){
        return "";
    }

    protected String getFaultString() {
        return getMessage();
    }

    protected String getWSAAction() {
        return "http://docs.oasis-open.org/wsrf/fault";
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    protected String now() {
        return ISO8601Date.format(new Date(System.currentTimeMillis()));
    }
}


/*
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:wsa=" http://www.w3.org/2005/08/addressing">
  <soap:Header>
    <wsa:Action>
      http://docs.oasis-open.org/wsrf/fault
    </wsa:Action>
  </soap:Header>
  <soap:Body>
    <soap:Fault>
      <faultcode>soap:Client</faultcode>
      <faultstring>No such resource exists</faultstring>
      <faultactor>http://example.org/someactor</faultactor>
      <detail>
        <wsrf-r:ResourceUnknownFault>
          <wsrf-bf:Timestamp>
            2005-05-04T20:18:44.970Z
          </wsrf-bf:Timestamp>
          <wsrf-bf:Description>
            Resource unknown
          </wsrf-bf:Description>
        </wsrf-r:ResourceUnknownFault>
      </detail>
    </soap:Fault>
  </soap:Body>
</soap:Envelope>
*/