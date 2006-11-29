package com.l7tech.server.partition;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.commons.lang.StringUtils;

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

    List<HttpEndpointHolder> httpEndpointsList;
    List<OtherEndpointHolder> otherEndpointsList;
    
    OSSpecificFunctions osf;
    Document originalDom;

    public enum OtherEndpointType {
        RMI_ENDPOINT("Inter-Node Communication"),
        TOMCAT_MANAGEMENT_ENDPOINT("Shutdown Port"),
        ;

        private String endpointName;

        OtherEndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public enum HttpEndpointType {
        BASIC_HTTP("Basic HTTP Endpoint"),
        SSL_HTTP("SSL Endpoint"),
        SSL_HTTP_NOCLIENTCERT("SSL Endpoint (No Client Certitifcate)"),
        ;

        private String endpointName;

        HttpEndpointType(String endpointName) {this.endpointName = endpointName;}
        public String getName() {return endpointName;}
        public String toString() {return endpointName;}
    }

    public PartitionInformation(String partitionName) {
        this.partitionId = partitionName;
        isNewPartition = true;
        httpEndpointsList = new ArrayList<HttpEndpointHolder>();
        otherEndpointsList = new ArrayList<OtherEndpointHolder>();
        makeDefaultEndpoints(httpEndpointsList, otherEndpointsList);
    }

    public PartitionInformation(String partitionId, Document doc, boolean isNew) throws XPathExpressionException {
        this.partitionId = partitionId;
        isNewPartition = isNew;
        httpEndpointsList = new ArrayList<HttpEndpointHolder>();
        otherEndpointsList = new ArrayList<OtherEndpointHolder>();
        makeDefaultEndpoints(httpEndpointsList, otherEndpointsList);
        //pass false to isNew since, if we have a doc then it's not a new partition.
        //now navigate the doc to get the Connector/port information
        parseConnectors(doc);
    }

    private void makeDefaultEndpoints(List<HttpEndpointHolder> httpEndpointsList, List<OtherEndpointHolder> otherEndpointsList) {
        HttpEndpointHolder.populateDefaultEndpoints(httpEndpointsList);
        OtherEndpointHolder.populateDefaultEndpoints(otherEndpointsList);
        if (partitionId.equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
            getOtherEndPointByType(OtherEndpointType.RMI_ENDPOINT).port = "2124";
            getOtherEndPointByType(OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT).port = "8005";
        }
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

        HttpEndpointHolder holder;
        if (!isSecure) holder = getHttpEndPointByType(HttpEndpointType.BASIC_HTTP);
        else {
            if (isClientCertWanted) holder = getHttpEndPointByType(HttpEndpointType.SSL_HTTP);
            else holder = getHttpEndPointByType(HttpEndpointType.SSL_HTTP_NOCLIENTCERT);
        }

        if (holder != null) {
            holder.ipAddress = ip;
            holder.port = portNumber;
        }
    }

    private OtherEndpointHolder getOtherEndPointByType(OtherEndpointType endpointType) {
        for (OtherEndpointHolder endpointHolder : otherEndpointsList) {
            if (endpointHolder.endpointType == endpointType) {
                return endpointHolder;
            }
        }
        return null;
    }

    private HttpEndpointHolder getHttpEndPointByType(HttpEndpointType endpointType) {
        for (HttpEndpointHolder endpointHolder : httpEndpointsList) {
            if (endpointHolder.endpointType == endpointType) {
                return endpointHolder;
            }
        }
        return null;
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

    public List<HttpEndpointHolder> getHttpEndpoints() {
        return httpEndpointsList;
    }

    public List<OtherEndpointHolder> getOtherEndpoints() {
        return otherEndpointsList;
    }


    public void setHttpEndpointsList(List<HttpEndpointHolder> httpEndpointsList) {
        this.httpEndpointsList = httpEndpointsList;
    }

    public void setOtherEndpointsList(List<OtherEndpointHolder> otherEndpointsList) {
        this.otherEndpointsList = otherEndpointsList;
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

    public static class OtherEndpointHolder {
        public OtherEndpointType endpointType;
        public String port; 

        public OtherEndpointHolder(OtherEndpointType endpointType) {
            this.endpointType = endpointType;
        }


        public OtherEndpointHolder(String port, OtherEndpointType endpointType) {
            this.port = port;
            this.endpointType = endpointType;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OtherEndpointHolder that = (OtherEndpointHolder) o;

            if (endpointType != that.endpointType) return false;
            if (port != null ? !port.equals(that.port) : that.port != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (endpointType != null ? endpointType.hashCode() : 0);
            result = 31 * result + (port != null ? port.hashCode() : 0);
            return result;
        }


        public String toString() {
            return endpointType.getName() + port;
        }

        public static void populateDefaultEndpoints(List<OtherEndpointHolder> endpoints) {
            endpoints.add(new OtherEndpointHolder(PartitionInformation.OtherEndpointType.RMI_ENDPOINT));
            endpoints.add(new OtherEndpointHolder(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT));
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    port = String.valueOf(aValue);
                    break;
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return OtherEndpointType.class;
                case 1:
                    return Integer.class;
                default:
                    return String.class;
            }
        }
    }

    public static class HttpEndpointHolder {
        private static String[] headings = new String[] {
            "Endpoint Type",
            "IP Address",
            "Port",
        };

        public HttpEndpointType endpointType;
        public String ipAddress;
        public String port;

        public HttpEndpointHolder(HttpEndpointType type) {
            this.endpointType = type;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HttpEndpointHolder that = (HttpEndpointHolder) o;

            if (endpointType != that.endpointType) return false;
            if (ipAddress != null ? !ipAddress.equals(that.ipAddress) : that.ipAddress != null) return false;
            if (port != null ? !port.equals(that.port) : that.port != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (ipAddress != null ? ipAddress.hashCode() : 0);
            result = 31 * result + (port != null ? port.hashCode() : 0);
            result = 31 * result + endpointType.getName().hashCode();
            return result;
        }


        public String toString() {
            if (StringUtils.isEmpty(ipAddress) || StringUtils.isEmpty(port)) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(endpointType.getName());
            sb.append(ipAddress.equals("*")?"* (all interfaces)":ipAddress).append(", ");
            sb.append(port);
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
            }
        }

        public static Class<?> getClassAt(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return HttpEndpointType.class;
                case 1:
                    return String.class;
                case 2:
                    return Integer.class;
                default:
                    return String.class;
            }
        }

        public static void populateDefaultEndpoints(List<HttpEndpointHolder> endpoints) {
            endpoints.add(new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.BASIC_HTTP));
            endpoints.add(new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP));
            endpoints.add(new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT));
        }
    }
}
