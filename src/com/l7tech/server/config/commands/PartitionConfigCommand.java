package com.l7tech.server.config.commands;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.partition.PartitionInformation;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 10:51:03 AM
 */
public class PartitionConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(PartitionConfigCommand.class.getName());
    PartitionConfigBean partitionBean;

    private interface ConnectorMatcher {
        boolean matchesCriteria(Element connector);
    }

    public PartitionConfigCommand(ConfigurationBean bean) {
        super(bean);
        partitionBean = (PartitionConfigBean) configBean;
    }

    public boolean execute() {
        boolean success = true;
        PartitionInformation pInfo = partitionBean.getPartitionInfo();
        try {
            updatePartitionEndpoints(pInfo);
            updateSystemProperties(pInfo);
            updateFirewallRules(pInfo);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    private void updateFirewallRules(PartitionInformation pInfo) {
        //TODO write an appropriate iptables fragment for this partition
        String firewallFile = pInfo.getOSSpecificFunctions().getConfigurationBase() + "firewall-rules.txt";
        System.out.println("Firewall file = " + firewallFile);
    }

    private void updateSystemProperties(PartitionInformation pInfo) throws IOException {
        File systemPropertiesFile = new File(pInfo.getOSSpecificFunctions().getSsgSystemPropertiesFile());
        if (!systemPropertiesFile.exists()) systemPropertiesFile.createNewFile();

        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(systemPropertiesFile);
            Properties prop = new Properties();
            prop.load(fis);

            List<PartitionInformation.HttpEndpointHolder> httpEndpoints = pInfo.getHttpEndpoints();
            List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();

            PartitionInformation.HttpEndpointHolder httpEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, httpEndpoints);
            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_HTTPPORT, httpEndpoint.port);

            PartitionInformation.HttpEndpointHolder sslEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP, httpEndpoints);
            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_SSLPORT, sslEndpoint.port);

            PartitionInformation.OtherEndpointHolder rmiEndpoint = getOtherEndpointByType(PartitionInformation.OtherEndpointType.RMI_ENDPOINT, otherEndpoints);
            if (StringUtils.isNotEmpty(rmiEndpoint.port))
                prop.setProperty(PartitionConfigBean.SYSTEM_PROP_RMIPORT, rmiEndpoint.port);

//            PartitionInformation.OtherEndpointHolder shutdownEndpoint = getOtherEndpointByType(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT, otherEndpoints);
//            if (StringUtils.isNotEmpty(shutdownEndpoint.port))
//                prop.setProperty(PartitionConfigBean.SYSTEM_PROP_TOMCATSHUTDOWNPORT, shutdownEndpoint.port);

            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_PARTITIONNAME, pInfo.getPartitionId());

            fos = new FileOutputStream(systemPropertiesFile);
            prop.store(fos, "");
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {}
            if (fos != null) fis.close();
        }
    }

    private void updatePartitionEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        updateHttpEndpoints(pInfo);
        updateOtherEndpoints(pInfo);
    }

    private void updateOtherEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();
        PartitionInformation.OtherEndpointHolder shutdownEndpoint = getOtherEndpointByType(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT, otherEndpoints);

        Document serverConfigDom = pInfo.getOriginalDom();
        if (serverConfigDom == null) {
            serverConfigDom = getDomFromServerConfig(pInfo);
        }
        NodeList serverNodes = serverConfigDom.getElementsByTagName("Server");
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Element serverNode = (Element) serverNodes.item(i);
            serverNode.setAttribute("port", shutdownEndpoint.port);
        }
        FileOutputStream fos = null;
        try {
            OSSpecificFunctions foo = pInfo.getOSSpecificFunctions();
            fos = new FileOutputStream(foo.getTomcatServerConfig());
            XmlUtil.nodeToOutputStream(serverConfigDom, fos);
        }finally {
            if (fos != null) fos.close();
        }
    }

    private PartitionInformation.OtherEndpointHolder getOtherEndpointByType(PartitionInformation.OtherEndpointType type,
                                                                    List<PartitionInformation.OtherEndpointHolder> endpoints) {
        for (PartitionInformation.OtherEndpointHolder endpoint : endpoints) {
            if (endpoint.endpointType == type) return endpoint;
        }
        return null;
    }

    private void updateHttpEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        List<PartitionInformation.HttpEndpointHolder> newHttpEndpoints = pInfo.getHttpEndpoints();
        Document serverConfigDom = pInfo.getOriginalDom();
        if (serverConfigDom == null) {
            serverConfigDom = getDomFromServerConfig(pInfo);
        }
        doEndpointTypeAwareUpdates(newHttpEndpoints, serverConfigDom);

        FileOutputStream fos = null;
        try {
            OSSpecificFunctions foo = pInfo.getOSSpecificFunctions();
            fos = new FileOutputStream(foo.getTomcatServerConfig());
            XmlUtil.nodeToOutputStream(serverConfigDom, fos);
        }finally {
            if (fos != null) fos.close();
        }
    }

    private void doEndpointTypeAwareUpdates(List<PartitionInformation.HttpEndpointHolder> endpoints, Document serverConfig) {
        
        NodeList connectors = serverConfig.getElementsByTagName("Connector");
        pruneConnectors(serverConfig, connectors, endpoints);

        Map<PartitionInformation.HttpEndpointType,Element> connectorMap = getHttpConnectorsByType(connectors);
        String redirectPort = "";
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            PartitionInformation.HttpEndpointType type = endpoint.endpointType;
            if (type == PartitionInformation.HttpEndpointType.SSL_HTTP) redirectPort = endpoint.port;
            if (!connectorMap.containsKey(type)) {
                addNewConnector(serverConfig, endpoint);
            } else {
                Element connector = connectorMap.get(type);
                connector.setAttribute("address", endpoint.ipAddress);
                connector.setAttribute("port", endpoint.port);
            }
        }
        connectorMap.get(PartitionInformation.HttpEndpointType.BASIC_HTTP).setAttribute("redirectPort", redirectPort);
    }

    private Map<PartitionInformation.HttpEndpointType,Element> getHttpConnectorsByType(NodeList connectors) {
        ConnectorMatcher matcher = null;
        Map<PartitionInformation.HttpEndpointType,Element> elementMap = new HashMap<PartitionInformation.HttpEndpointType, Element>();
        for (int i = 0; i < connectors.getLength(); i++) {
            Element connector = (Element) connectors.item(i);
            if (!StringUtils.equals(connector.getAttribute("secure"), "true")) {
                elementMap.put(PartitionInformation.HttpEndpointType.BASIC_HTTP, connector);        
            } else if (StringUtils.equals(connector.getAttribute("secure"), "true") &&
                       StringUtils.equals(connector.getAttribute("clientAuth"), "want")) {
                elementMap.put(PartitionInformation.HttpEndpointType.SSL_HTTP, connector);                   
            } else if (StringUtils.equals(connector.getAttribute("secure"), "true") &&
                       !StringUtils.equals(connector.getAttribute("clientAuth"), "want")) {
                elementMap.put(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, connector);   
            }
        }

        return elementMap;
    }

    private void addNewConnector(Document serverConfig, PartitionInformation.HttpEndpointHolder newEndpoint) {
        PartitionInformation.HttpEndpointType httpType = newEndpoint.endpointType;
        Element newNode = serverConfig.createElement("Connector");
        switch(httpType) {
            case BASIC_HTTP:
                newNode.setAttribute("secure", "true");
                break;
            case SSL_HTTP:
                newNode.setAttribute("secure", "true");
                newNode.setAttribute("clientAuth", "want");
                break;
            case SSL_HTTP_NOCLIENTCERT:
                newNode.setAttribute("secure", "true");
                newNode.setAttribute("clientAuth", "false");
                break;
        }
    }

    private void pruneConnectors(Document dom, NodeList connectors, List<PartitionInformation.HttpEndpointHolder> newEndpoints) {
        for (int index = 0; index < connectors.getLength(); index++) {
            Element connectorNode = (Element) connectors.item(index);
            if (!existsInNewEndpoints(connectorNode, newEndpoints))
                dom.removeChild(connectorNode);
        }
    }

    private boolean existsInNewEndpoints(Element connector, List<PartitionInformation.HttpEndpointHolder> endpoints) {
        boolean isSecure = StringUtils.equals(connector.getAttribute("secure"), "true");
        boolean needsClientCert = StringUtils.equals(connector.getAttribute("clientAuth"),"want");

        PartitionInformation.HttpEndpointHolder holder= null;
        if (isSecure) {
            if (needsClientCert) holder = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP,endpoints);
            else holder = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, endpoints);
        } else {
            holder = getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, endpoints);
        }
        return StringUtils.isNotEmpty(holder.ipAddress) && StringUtils.isNotEmpty(holder.port);
    }

    private PartitionInformation.HttpEndpointHolder getHttpEndpointByType(PartitionInformation.HttpEndpointType type,
                                                                    List<PartitionInformation.HttpEndpointHolder> endpoints) {
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            if (endpoint.endpointType == type) return endpoint;
        }
        return null;
    }

    private Document getDomFromServerConfig(PartitionInformation pInfo) throws IOException, SAXException {
        Document doc = null;
        FileInputStream fis = null;
        String errorMessage = "Could not read the server.xml for partition \"" + pInfo + "\": ";
        try {
            OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
            String serverConfigPath = osf.getTomcatServerConfig();
            fis = new FileInputStream(serverConfigPath);
            doc = XmlUtil.parse(fis);

        } catch (FileNotFoundException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } catch (SAXException e) {
            logger.severe(errorMessage + e.getMessage());
            throw e;
        } finally {
            if (fis != null)
            try {
                fis.close();
            } catch (IOException e) {
            }
        }
        return doc;
    }

}
