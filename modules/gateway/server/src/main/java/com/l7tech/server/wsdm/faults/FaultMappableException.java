package com.l7tech.server.wsdm.faults;

import com.l7tech.util.ISO8601Date;
import com.l7tech.server.wsdm.Namespaces;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

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
    protected static final Map<String, String> namespaces = new HashMap<String, String>() {{
        put(Namespaces.WSA, "wsa");
        put(Namespaces.WSRF_BF, "wsrf-bf");
    }};

    public FaultMappableException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public String getSoapFaultXML() {
        return
        "<soap:Envelope " + getNamespacesXML() + ">\n" +
        "  <soap:Header>\n" +
        getHeadersXML() +
        "  </soap:Header>\n" +
        "  <soap:Body>\n" +
        "    <soap:Fault>\n" +
        "      <faultcode>" + getFaultCode() + "</faultcode>\n" +
        "      <faultstring>" + getFaultString() + "</faultstring>\n" +
        (getActor()==null ? "" : "      <faultactor>" + getActor() + "</faultactor>\n") +
        getSoapFaultDetailXML() +
        "    </soap:Fault>\n" +
        "  </soap:Body>\n" +
        "</soap:Envelope>";
    }

    /**
     * @return xmlns:prefix="namespace_uri"... String
     */
    private String getNamespacesXML() {
        StringBuilder nsXML = new StringBuilder();
        Map<String,String> namespaces = getNamespaces();
        for (String ns : namespaces.keySet()) {
            nsXML.append(" xmlns:").append(namespaces.get(ns)).append("=\"").append(ns).append("\"");
        }
        return nsXML.toString();
    }

    protected static Map<String, String> getNamespaces() {
        return namespaces;
    }

    protected String getNamespacePrefix(String namespace) {
        return namespaces.get(namespace);
    }

    protected String getHeadersXML() {
        String wsaPrefix = getNamespacePrefix(Namespaces.WSA);
        return "    <" + wsaPrefix + ":Action>\n      " + getWSAAction() + "\n" + "    </" + wsaPrefix + ":Action>\n";
    }

    /**
     * Subclasses may override to provide details.
     *
     * @return The SOAP 1.1 Body/Fault/detail element in String format.
     */
    protected String getSoapFaultDetailXML(){
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

    /**
     * @return the subsubcode, subcode, or code of the fault if present, in this order
     */
    protected String getFaultCode() {
        return "soap:Client";
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