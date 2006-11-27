package com.l7tech.server.partition;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.*;

/**
 * User: megery
 * Date: Nov 10, 2006
 * Time: 10:15:56 AM
 */
public class PartitionInformation{

    private static final String CONNECTOR_XPATH = "/Server/Service/Connector";

    public static final String PARTITIONS_BASE = "etc/conf/partitions/";
    public static final String TEMPLATE_PARTITION_NAME = "partitiontemplate_";
    public static final String DEFAULT_PARTITION_NAME = "default_";

    String partitionId;
    String oldPartitionId;
    boolean isNewPartition = false;

    Map<EndpointType, EndpointHolder> endpointsList;
    OSSpecificFunctions osf;
    Document originalDom;


    public enum EndpointType {
        BASIC_HTTP("Basic HTTP Endpoint"),
        SSL_HTTP("SSL Endpoint"),
        SSL_HTTP_NOCLIENTCERT("SSL Endpoint With No Client Certitifcate"),
        ;

        private String endpointName;

        EndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public PartitionInformation(String partitionName) {
        this.partitionId = partitionName;
        isNewPartition = true;
        endpointsList = new TreeMap<EndpointType, EndpointHolder>();
        EndpointHolder.populateDefaultEndpoints(endpointsList);
    }

    public PartitionInformation(String partitionId, Document doc, boolean isNew) throws XPathExpressionException {
        this.partitionId = partitionId;
        isNewPartition = isNew;
        endpointsList = new TreeMap<EndpointType, EndpointHolder>();
        EndpointHolder.populateDefaultEndpoints(endpointsList);

        //pass false to isNew since, if we have a doc then it's not a new partition.
        //now navigate the doc to get the Connector/port information
        parseConnectors(doc);
    }

    private void parseConnectors(Document doc) throws XPathExpressionException {
        setOriginalDom(doc);
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList connectors = (NodeList) xpath.evaluate(CONNECTOR_XPATH, doc, XPathConstants.NODESET);
        for (int nodeIndex = 0; nodeIndex < connectors.getLength(); nodeIndex++) {
            Element connectorNode = (Element) connectors.item(nodeIndex);

            boolean isSecure = connectorNode.hasAttribute("secure") && connectorNode.getAttribute("secure").equals("true");
            boolean wantClientCert = isSecure && connectorNode.hasAttribute("clientAuth") && !connectorNode.getAttribute("clientAuth").equals("false");

            String portNumber = connectorNode.hasAttribute("port")?connectorNode.getAttribute("port"):"";
            String ipAddress = connectorNode.hasAttribute("address")?connectorNode.getAttribute("address"):"*";

            updateEndpoint(ipAddress, portNumber, isSecure, wantClientCert);
        }
    }

    private void updateEndpoint(String ip, String portNumber, boolean isSecure, boolean isClientCertWanted) {

        EndpointHolder holder;
        if (!isSecure) holder = endpointsList.get(EndpointType.BASIC_HTTP);
        else {
            if (isClientCertWanted) holder = endpointsList.get(EndpointType.SSL_HTTP);
            else holder = endpointsList.get(EndpointType.SSL_HTTP_NOCLIENTCERT);
        }

        if (holder != null) {
            holder.ipAddress = ip;
            holder.port = portNumber;
        }
    }

    public OSSpecificFunctions getOSSpecificFunctions() {
        if (osf == null) osf = OSDetector.getOSSpecificFunctions(partitionId);
        return osf;
    }

    public void setPartitionId(String newId) {
        oldPartitionId = partitionId;
        partitionId = newId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public String getOldPartitionId() {
        return oldPartitionId;
    }

    public void setNewPartition(boolean newPartition) {
        isNewPartition = newPartition;
    }

    public boolean isNewPartition() {
        return isNewPartition;
    }

    public Map<EndpointType, EndpointHolder> getEndpoints() {
        return endpointsList;
    }

    public void setEndpointsList(Map<EndpointType, EndpointHolder> endpointsList) {
        this.endpointsList = endpointsList;
    }

    public String toString() {
        return partitionId;
    }


    public Document getOriginalDom() {
        return originalDom;
    }

    public void setOriginalDom(Document originalDom) {
        this.originalDom = originalDom;
    }

    public static class EndpointHolder {
        private static String[] headings = new String[] {
            "Endpoint Type",
            "IP Address",
            "Port",
//            "Uses SSL",
//            "Uses Client Certificate",
        };

        private EndpointType endpointType;
        public String ipAddress;
        public String port;

//        public boolean isSecure;
//        public boolean isClientCert;

//        public EndpointHolder(String ipAddress, String port, boolean secure, boolean isClientCert) {
//            this.ipAddress = ipAddress;
//            this.port = port;
//            this.isSecure = secure;
//            this.isClientCert = isClientCert;
//        }

        public EndpointHolder(EndpointType type) {
            this.endpointType = type;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndpointHolder that = (EndpointHolder) o;

            if (endpointType != that.endpointType) return false;
//            if (isClientCert != that.isClientCert) return false;
//            if (isSecure != that.isSecure) return false;
            if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
            if (port != null ? !port.equals(that.port) : that.port != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (ipAddress != null ? ipAddress.hashCode() : 0);
            result = 31 * result + (port != null ? port.hashCode() : 0);
            result = 31 * result + endpointType.getName().hashCode();
//            result = 31 * result + (isSecure ? 1 : 0);
//            result = 31 * result + (isClientCert ? 1 : 0);
            return result;
        }


        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(endpointType.getName());
            sb.append(ipAddress.equals("*")?"* (all interfaces)":ipAddress).append(", ");
            sb.append(port);
//            sb.append((isSecure?", SSL":""));
//            sb.append((isClientCert?", With Client Cert":""));
            return sb.toString();
        }

        public static String[] getHeadings() {
            return headings;
        }

        public Object getValue(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return endpointType.getName();
                case 1:
                    return ipAddress;
                case 2:
                    return port;
//                case 3:
//                    return isClientCert;
                default:
                    return null;
            }
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    ipAddress = String.valueOf(aValue);
                    break;
                case 2:
                    port = String.valueOf(aValue);
                    break;
//                case 2:
//                    isSecure = ((Boolean)aValue).booleanValue();
//                    break;
//                case 3:
//                    isClientCert = ((Boolean)aValue).booleanValue();
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return EndpointType.class;
                case 1:
                    return String.class;
                case 2:
                    return Short.class;
//                case 3:
//                    return Boolean.class;
                default:
                    return String.class;
            }
        }

        public static void populateDefaultEndpoints(Map<EndpointType, EndpointHolder> endpoints) {
            endpoints.put(PartitionInformation.EndpointType.BASIC_HTTP,
                    new PartitionInformation.EndpointHolder(PartitionInformation.EndpointType.BASIC_HTTP));

            endpoints.put(PartitionInformation.EndpointType.SSL_HTTP,
                    new PartitionInformation.EndpointHolder(PartitionInformation.EndpointType.SSL_HTTP));

            endpoints.put(PartitionInformation.EndpointType.SSL_HTTP_NOCLIENTCERT,
                    new PartitionInformation.EndpointHolder(PartitionInformation.EndpointType.SSL_HTTP_NOCLIENTCERT));
        }
    }
}
