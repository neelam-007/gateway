package com.l7tech.server.wsdm.faults;

import com.l7tech.server.wsdm.Namespaces;

import java.util.*;

/**
 * @author jbufu
 */
public class WsAddressingFault extends FaultMappableException {

    public WsAddressingFault(String msg) {
        super(msg);
    }

    public WsAddressingFault(WsaFaultDetail detail) {
        super(detail.content);
        wsaFaultDetails.add(detail);
    }

    @Override
    protected String getWSAAction() {
        return "http://www.w3.org/2005/08/addressing/fault";
    }

    @Override
    protected String getHeadersXML() {
        List<WsaFaultDetail> details = getWsaFaultDetails();
        if (details != null && ! details.isEmpty()) {
            StringBuilder headers = new StringBuilder(super.getHeadersXML());
            String wsaPrefix = getNamespacePrefix(Namespaces.WSA);
            headers.append("    <").append(wsaPrefix).append(":FaultDetail>\n    ");
            for (WsaFaultDetail faultDetail : details) {
                headers.append("        <").append(wsaPrefix).append(":").append(faultDetail.type.toString()).append(faultDetail.getAttributes()).append(">")
                       .append(faultDetail.content).append("</").append(wsaPrefix).append(":").append(faultDetail.type.toString()).append(">\n");
            }
            headers.append("    </").append(wsaPrefix).append(":FaultDetail>\n");
            return headers.toString();

        } else {
            return super.getHeadersXML();
        }
    }

    protected List<WsaFaultDetail> getWsaFaultDetails() {
        return wsaFaultDetails;
    }

    public void addWsaFaultDetail(WsaFaultDetailType detailType, String detailValue) {
        addWsaFaultDetail(detailType, null, detailValue);
    }

    public void addWsaFaultDetail(WsaFaultDetailType detailType, Map<String,String> detailAttrs, String detailValue) {
        wsaFaultDetails.add(new WsaFaultDetail(detailType, detailAttrs, detailValue));
    }

    @Override
    protected final String getSoapFaultDetailXML() {
        //  The SOAP 1.1 fault detail is only for use with faults related to the body of a message
        //  and is therefore not used for SOAP 1.1 faults related to processing of addressing headers
        return "";
    }

    private List<WsaFaultDetail> wsaFaultDetails = new ArrayList<WsaFaultDetail>();

    public static class WsaFaultDetail {
        private WsaFaultDetailType type;
        private Map<String,String> attributes = new HashMap<String, String>();
        private String content;

        public WsaFaultDetail(WsaFaultDetailType type, Map<String, String> attributes, String content) {
            this.type = type;
            this.attributes.putAll(attributes);
            this.content = content;
        }

        public String getAttributes() {
            if (attributes == null || attributes.isEmpty()) {
                return "";
            } else {
                StringBuilder result = new StringBuilder();
                for (String name : attributes.keySet())
                    result.append(" ").append(name).append("=\"").append(attributes.get(name)).append("\"");
                return result.toString();
            }
        }
    }

    /**
     * Predefined WSA fault detail types
     */
    public static enum WsaFaultDetailType {
        PROBLEM_HEADER("ProblemHeader"), // not _defined_ in the ws-addr-soap spec, but shows up in ws-addr.xsd
        PROBLEM_HEADER_QNAME("ProblemHeaderQName"),
        PROBLEM_IRI("ProblemIRI"),
        PROBLEM_ACTION("ProblemAction"),
        RETRY_AFTER("RetryAfter");

        private final String detailElementName;

        WsaFaultDetailType(String detailElementName) {
            this.detailElementName = detailElementName;
        }

        @Override
        public String toString() {
            return detailElementName;
        }
    }
}
