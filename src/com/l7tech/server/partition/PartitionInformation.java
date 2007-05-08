package com.l7tech.server.partition;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PartitionActions;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 10, 2006
 * Time: 10:15:56 AM
 */
public class PartitionInformation{

    private static final Logger logger = Logger.getLogger(PartitionInformation.class.getName());
    private static final String CONNECTOR_XPATH = "/Server/Service/Connector";

    public static final String PARTITIONS_BASE = "etc/conf/partitions/";
    public static final String TEMPLATE_PARTITION_NAME = "partitiontemplate_";
    public static final String DEFAULT_PARTITION_NAME = "default_";
    public static final String ENABLED_FILE = "enabled";

    public static final String SYSTEM_PROP_HTTPPORT = "com.l7tech.server.httpPort";
    public static final String SYSTEM_PROP_SSLPORT = "com.l7tech.server.httpsPort";
    public static final String SYSTEM_PROP_PARTITIONNAME = "com.l7tech.server.partitionName";
    public static final String SYSTEM_PROP_RMIPORT = "com.l7tech.server.clusterPort";

    String partitionId;
    String oldPartitionId;
    boolean isNewPartition = false;
    boolean isEnabled = false;
    private boolean shouldDisable = false;
    
    List<HttpEndpointHolder> httpEndpointsList;
    List<OtherEndpointHolder> otherEndpointsList;
    OSSpecificFunctions osf;
    Document originalDom;
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65535;

    private static String DEFAULT_HTTP_PORT = "8080";
    private static String DEFAULT_SSL_PORT = "8443";
    private static String DEFAULT_NOAUTH_PORT = "9443";
    private static String DEFAULT_RMI_PORT = "2124";
    private static String DEFAULT_SHUTDOWN_PORT = "8005";


    public static final String ALLOWED_PARTITION_NAME_PATTERN = "[^\\p{Punct}\\s]{1,128}";

    public enum OtherEndpointType {
        RMI_ENDPOINT("Inter-Node Communication Port"),
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

        //since this is a new partitionm, try to make sure the ports don't conflict
        PartitionActions.validateAllPartitionEndpoints(this, true);
    }

    public PartitionInformation(String partitionId, Document doc, boolean isNew) throws XPathExpressionException {
        this.partitionId = partitionId;
        isNewPartition = isNew;
        httpEndpointsList = new ArrayList<HttpEndpointHolder>();
        otherEndpointsList = new ArrayList<OtherEndpointHolder>();
        makeDefaultEndpoints(httpEndpointsList, otherEndpointsList);
        //pass false to isNew since, if we have a doc then it's not a new partition.
        //now navigate the doc to get the Connector/port information
        parseDomForEndpoints(doc);
        parseOtherEndpoints();
    }

    public PartitionInformation copy() {
        PartitionInformation theCopy = new PartitionInformation(this.getPartitionId());
        theCopy.setEnabled(this.isEnabled());
        theCopy.setNewPartition(this.isNewPartition());
        theCopy.setOriginalDom(this.getOriginalDom());
        theCopy.setShouldDisable(this.shouldDisable());
        theCopy.setHttpEndpointsList(this.getHttpEndpoints());
        theCopy.setOtherEndpointsList(this.getOtherEndpoints());
        return theCopy;
    }

    private void parseOtherEndpoints() {
        File sysProps = new File(OSDetector.getOSSpecificFunctions(partitionId).getSsgSystemPropertiesFile());
        FileInputStream fis = null;
        try {
            Properties props = new Properties();
            fis = new FileInputStream(sysProps);
            props.load(fis);
            String rmiPort = props.getProperty(PartitionInformation.SYSTEM_PROP_RMIPORT);
            getOtherEndPointByType(OtherEndpointType.RMI_ENDPOINT).setPort(StringUtils.isNotEmpty(rmiPort)?rmiPort:"2124");
        } catch (FileNotFoundException e) {
            logger.warning("no system properties file found for partition: " + partitionId);
        } catch (IOException e) {
            logger.warning("Error while reading the system properties file for partition: " + partitionId);
            logger.warning(e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(fis);
        }

    }

    private void makeDefaultEndpoints(List<HttpEndpointHolder> httpEndpointsList, List<OtherEndpointHolder> otherEndpointsList) {
        HttpEndpointHolder.populateDefaultEndpoints(httpEndpointsList);
        OtherEndpointHolder.populateDefaultEndpoints(otherEndpointsList);
        if (partitionId.equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
            getOtherEndPointByType(OtherEndpointType.RMI_ENDPOINT).setPort("2124");
            getOtherEndPointByType(OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT).setPort("8005");
        }
    }

    private void parseDomForEndpoints(Document doc) throws XPathExpressionException {
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

        NodeList nodes = doc.getElementsByTagName("Server");

        if ((nodes != null && nodes.getLength() == 1)) {
            Element serverElement = (Element) nodes.item(0);
            if (serverElement != null && serverElement.hasAttribute("port"))
                getOtherEndPointByType(OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT).setPort(serverElement.getAttribute("port"));
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
            holder.setIpAddress(ip);
            holder.setPort(portNumber);
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
        getOSSpecificFunctions().setPartitionName(partitionId);
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


    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public boolean shouldDisable() {
        return shouldDisable;
    }

    public void setShouldDisable(boolean shouldDisable) {
        this.shouldDisable = shouldDisable;
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
        if (getOSSpecificFunctions().isUnix())
            return partitionId + (isEnabled?"":" (Disabled)");

        return partitionId;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PartitionInformation that = (PartitionInformation) o;

        if (partitionId != null ? !partitionId.equals(that.partitionId) : that.partitionId != null) return false;

        return true;
    }

    public int hashCode() {
        return (partitionId != null ? partitionId.hashCode() : 0);
    }

    public Document getOriginalDom() {
        return originalDom;
    }

    public void setOriginalDom(Document originalDom) {
        this.originalDom = originalDom;
    }

    public static abstract class EndpointHolder {
        private String ipAddress = "";
        private String port;
        private String validationMessaqe;

        public String toString() {
            return describe();
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getValidationMessaqe() {
            return validationMessaqe;
        }

        public void setValidationMessaqe(String validationMessaqe) {
            this.validationMessaqe = validationMessaqe;
        }

        public boolean equals(Object o) {
            return isEquals(o);
        }

        abstract boolean isEquals(Object o);

        public int hashCode() {
            return getHashCode();
        }

        abstract int getHashCode();

        public abstract String describe();
        public abstract void setValueAt(int columnIndex, Object aValue);
    }

    public static class OtherEndpointHolder extends EndpointHolder{
        public OtherEndpointType endpointType;

        public OtherEndpointHolder(OtherEndpointType endpointType) {
            this.endpointType = endpointType;
        }

        public OtherEndpointHolder(String port, OtherEndpointType endpointType) {
            this.setPort(port);
            this.endpointType = endpointType;
        }

        public boolean isEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OtherEndpointHolder that = (OtherEndpointHolder) o;

            if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort()!= null) return false;

            return true;
        }

        public int getHashCode() {
            int result;
            result = 31 * (getPort() != null ? getPort().hashCode() : 0);
            return result;
        }

        public String describe() {
            return endpointType.getName() + " = " + getPort();
        }

        public static void populateDefaultEndpoints(List<OtherEndpointHolder> endpoints) {
            OtherEndpointHolder holder = new OtherEndpointHolder(OtherEndpointType.RMI_ENDPOINT);
            holder.setPort(DEFAULT_RMI_PORT);
            endpoints.add(holder);

            holder = new OtherEndpointHolder(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT);
            holder.setPort(DEFAULT_SHUTDOWN_PORT);
            endpoints.add(holder);
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    this.setPort(String.valueOf(aValue));
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

    public static class HttpEndpointHolder extends EndpointHolder {
        private static String[] headings = new String[] {
            "Endpoint Type",
            "IP Address",
            "Port",
        };

        public HttpEndpointType endpointType;

        public HttpEndpointHolder(HttpEndpointType type) {
            this.endpointType = type;
            setIpAddress("*");
        }

        public boolean isEquals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HttpEndpointHolder that = (HttpEndpointHolder) o;

            if (getIpAddress() != null ? !getIpAddress().equals(that.getIpAddress()) : that.getIpAddress() != null) return false;
            if (getPort() != null ? !getPort().equals(that.getPort()) : that.getPort() != null) return false;

            return true;
        }

        public int getHashCode() {
            int result;
            result = (getIpAddress() != null ? getIpAddress().hashCode() : 0);
            result = 31 * result + (getPort() != null ? getPort().hashCode() : 0);
            return result;
        }


        public String describe() {
            if (StringUtils.isEmpty(getIpAddress()) || StringUtils.isEmpty(getPort())) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(endpointType.getName()).append(" = ");
            sb.append(getIpAddress().equals("*")?"* (all interfaces)":getIpAddress()).append(", ");
            sb.append(getPort());
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
                    return getIpAddress();
                case 2:
                    return getPort();
                default:
                    return null;
            }
        }

        public void setValueAt(int columnIndex, Object aValue) {
            switch(columnIndex) {
                case 0:
                    break;
                case 1:
                    this.setIpAddress(String.valueOf(aValue));
                    break;
                case 2:
                    this.setPort(String.valueOf(aValue));
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
            HttpEndpointHolder holder = new HttpEndpointHolder(HttpEndpointType.BASIC_HTTP);
            holder.setPort(DEFAULT_HTTP_PORT);
            endpoints.add(holder);

            holder = new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP);
            holder.setPort(DEFAULT_SSL_PORT);
            endpoints.add(holder);

            holder = new PartitionInformation.HttpEndpointHolder(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT);
            holder.setPort(DEFAULT_NOAUTH_PORT);
            endpoints.add(holder);
        }
    }

    public static class IpPortPair {
        private EndpointHolder endpointHolder;

        public IpPortPair(PartitionInformation.EndpointHolder holder) {
            this.endpointHolder = holder;
        }

        public String getIpAddress() {
            return StringUtils.isEmpty(endpointHolder.ipAddress)?"":endpointHolder.ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            endpointHolder.ipAddress = ipAddress;
        }

        public String getPort() {
            return StringUtils.isEmpty(endpointHolder.port)?"":endpointHolder.port;
        }

        public void setPort(String port) {
            endpointHolder.port = port;
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IpPortPair that = (IpPortPair) o;

            if (endpointHolder != null) {
                if (StringUtils.equals(getIpAddress(), "*")) {
                    if (getPort().equals(that.getPort())) {
                        return true;
                    }
                } else if (StringUtils.equals(that.getIpAddress(), "*")) {
                    if (StringUtils.equals(getPort(), that.getPort())) {
                        return true;
                    }
                }
            }

            if (endpointHolder != null ? !endpointHolder.equals(that.endpointHolder) : that.endpointHolder != null)
                return false;

            return true;
        }

        public int hashCode() {
            return (endpointHolder != null ? endpointHolder.hashCode() : 0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(getIpAddress())) {
                sb.append(getIpAddress()).append(":");
            }
            sb.append(getPort());
            return sb.toString();
        }
    }
}