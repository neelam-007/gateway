package com.l7tech.server.config.commands;

import com.l7tech.common.util.FileUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 10:51:03 AM
 */
public class PartitionConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(PartitionConfigCommand.class.getName());
    PartitionConfigBean partitionBean;

    //TODO add some useful patterns here
    private static final String SERVICE_NAME_KEY = "SERVICE_NAME";
    private static final String SERVICE_DISPLAY_NAME_KEY = "PR_DISPLAYNAME";
    private static final String SERVICE_LOGPREFIX_KEY = "PR_LOGPREFIX";
    private static final String SERVICE_LOG_KEY = "PR_STDOUTPUT";
    private static final String SERVICE_ERRLOG_KEY = "PR_STDERROR";
    private static final String PARTITION_NAME_KEY = "PARTITIONNAMEPROPERTY";

    private static final String BASIC_IP_MARKER = "<HTTP_BASIC_IP>";
    private static final String BASIC_PORT_MARKER = "<HTTP_BASIC_PORT>";
    private static final String SSL_IP_MARKER = "<SSL_IP>";
    private static final String SSL_PORT_MARKER = "<SSL_PORT>";
    private static final String NOAUTH_SSL_IP_MARKER = "<SSL_NOAUTH_IP>";
    private static final String NOAUTH_SSL_PORT_MARKER = "<SSL_NOAUTH_PORT>";
    private static final String RMI_PORT_MARKER = "<RMI_PORT>";

    private static String firewallRules = new String(
        "[0:0] -I INPUT $Rule_Insert_Point -d "+ BASIC_IP_MARKER +" -p tcp -m tcp --dport " + BASIC_PORT_MARKER +" -j ACCEPT\n" +
        "[0:0] -I INPUT $Rule_Insert_Point -d " + SSL_IP_MARKER +" -p tcp -m tcp --dport " + SSL_PORT_MARKER + " -j ACCEPT\n" +
        "[0:0] -I INPUT $Rule_Insert_Point -d " + NOAUTH_SSL_IP_MARKER + " -p tcp -m tcp --dport " + NOAUTH_SSL_PORT_MARKER + " -j ACCEPT\n" +
        "[0:0] -I INPUT $Rule_Insert_Point -p tcp -m tcp --dport " + RMI_PORT_MARKER + " -j ACCEPT\n"
    );

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
            if (pInfo.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
                copyDefaultServerConfig(pInfo);
            }
            updateStartupScripts(pInfo);
            updateFirewallRules(pInfo);
            enablePartitionForStartup(pInfo);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    private void copyDefaultServerConfig(PartitionInformation pInfo) throws IOException {
        if (pInfo.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
            File source = new File(pInfo.getOSSpecificFunctions().getTomcatServerConfig());
            File destination = new File(pInfo.getOSSpecificFunctions().getSsgInstallRoot() + "/tomcat/conf/server.xml");
            FileUtils.copyFile(source, destination);
        }
    }

    private void updateStartupScripts(PartitionInformation pInfo) throws IOException, InterruptedException {
        if (pInfo.getOSSpecificFunctions().isWindows()) {
            updateStartupScriptWindows(pInfo);
        }
    }

    private void updateStartupScriptWindows(PartitionInformation pInfo) throws IOException, InterruptedException {
        String serviceCommandFile = pInfo.getOSSpecificFunctions().getSpecificPartitionControlScriptName();
        writeWindowsServiceConfigFile(pInfo.getPartitionId(), serviceCommandFile);
        installWindowsService(serviceCommandFile, pInfo);
    }

    private void installWindowsService(String serviceCommandFile, PartitionInformation pInfo) throws InterruptedException, IOException {
        if (pInfo.isNewPartition()) {
            String[] cmdArray = new String[] {
                    serviceCommandFile,
                    "install",
            };

            //install the service
            try {
                Process p = null;
                try {
                    logger.info("Installing windows service for \"" + pInfo.getPartitionId() + "\" partition.");
                    File parentDir = new File(serviceCommandFile).getParentFile();
                    p = Runtime.getRuntime().exec(cmdArray, null, parentDir);
                    p.waitFor();
                } finally {
                    if (p != null)
                        p.destroy();
                }
            } catch (IOException e) {
                logger.warning("Could not install the SSG service for the \"" + pInfo.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
                throw e;
            } catch (InterruptedException e) {

                throw e;
            }
        }
    }

    private void writeWindowsServiceConfigFile(String partitionName, String serviceCommandFile) throws IOException {
        File f = new File(serviceCommandFile);
        File configFile = new File(f.getParentFile(),"partition_config.cmd");
        PrintStream os = null;
        logger.info("Modifying the windows service configuration for " + partitionName);
        try {
            //write out a config file that will set some variables needed by the service installer.
            os = new PrintStream(new FileOutputStream(configFile));
            os.println("set " + PARTITION_NAME_KEY + "=" + partitionName);
            os.println("set " + SERVICE_NAME_KEY + "=" + "SSG"+partitionName);
            os.println("set " + SERVICE_DISPLAY_NAME_KEY + "=" + "SecureSpan Gateway - " + partitionName + " Partition");
            os.println("set " + SERVICE_LOGPREFIX_KEY + "=" + partitionName + "_ssg_service.log");
            os.println("set " + SERVICE_LOG_KEY + "=" + "%TOMCAT_HOME%\\logs\\catalina.out." + partitionName);
            os.println("set " + SERVICE_ERRLOG_KEY + "=" + "%TOMCAT_HOME%\\logs\\catalina.err." + partitionName);
            os.flush();
        } catch (FileNotFoundException e) {
            logger.warning("Error while modifying the windows service configuration for the \"" + partitionName + "\" partition. [" + e.getMessage() +"]");
            throw e;
        } catch (IOException e) {
            logger.warning("Error while modifying the windows service configuration for the \"" + partitionName + "\" partition. [" + e.getMessage() +"]");
            throw e;
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void updateFirewallRules(PartitionInformation pInfo) {
        if (pInfo.getOSSpecificFunctions().isLinux()) {
            List<PartitionInformation.HttpEndpointHolder> httpEndpoints = pInfo.getHttpEndpoints();
            List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();

            PartitionInformation.HttpEndpointHolder basicEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, httpEndpoints);
            PartitionInformation.HttpEndpointHolder sslEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP, httpEndpoints);
            PartitionInformation.HttpEndpointHolder noAuthSslEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, httpEndpoints);
            PartitionInformation.OtherEndpointHolder rmiEndpoint = getOtherEndpointByType(PartitionInformation.OtherEndpointType.RMI_ENDPOINT, otherEndpoints);

            String rules = firewallRules;
            if (basicEndpoint.ipAddress.equals("*"))
                rules = rules.replaceAll("-d " +BASIC_IP_MARKER, "");
            else
                rules = rules.replaceAll(BASIC_IP_MARKER, basicEndpoint.ipAddress);
            rules = rules.replaceAll(BASIC_PORT_MARKER, basicEndpoint.port);

            if (sslEndpoint.ipAddress.equals("*"))
                rules = rules.replaceAll("-d " +SSL_IP_MARKER, "");
            else
                rules = rules.replaceAll(SSL_IP_MARKER, sslEndpoint.ipAddress);
            rules = rules.replaceAll(SSL_PORT_MARKER, sslEndpoint.port);

            if (noAuthSslEndpoint.ipAddress.equals("*"))
                rules = rules.replaceAll("-d " +NOAUTH_SSL_IP_MARKER, "");
            else
                rules = rules.replaceAll(NOAUTH_SSL_IP_MARKER, noAuthSslEndpoint.ipAddress);
            rules = rules.replaceAll(NOAUTH_SSL_PORT_MARKER, noAuthSslEndpoint.port);

            rules = rules.replaceAll(RMI_PORT_MARKER, rmiEndpoint.port);

            FileOutputStream fos = null;
            String fileName = pInfo.getOSSpecificFunctions().getPartitionBase() + pInfo.getPartitionId() + "/" + "firewall_rules";
            try {
                fos = new FileOutputStream(fileName);
                fos.write(rules.getBytes());
            } catch (FileNotFoundException e) {
                logger.severe("Could not create the firewall rules for the \"" + pInfo.getPartitionId() + "\" partition. [" + e.getMessage());
                logger.severe("The partition will be disabled");
                pInfo.setShouldDisable(true);
                enablePartitionForStartup(pInfo);
            } catch (IOException e) {
                logger.severe("Could not create the firewall rules for the \"" + pInfo.getPartitionId() + "\" partition. [" + e.getMessage());
                logger.severe("The partition will be disabled");
                pInfo.setShouldDisable(true);
                enablePartitionForStartup(pInfo);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {}
                }
            }

        }
    }

    private void enablePartitionForStartup(PartitionInformation pInfo){
        OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
        if (osf.isLinux()) {
            File enableStartupFile = new File(osf.getPartitionBase() + pInfo.getPartitionId(), PartitionInformation.ENABLED_FILE);
            if (pInfo.shouldDisable()) {
                logger.warning("Disabling the \"" + pInfo.getPartitionId() + "\" partition.");
                enableStartupFile.delete();
            } else {
                try {
                    logger.warning("Enabling the \"" + pInfo.getPartitionId() + "\" partition.");
                    enableStartupFile.createNewFile();
                } catch (IOException e) {
                    logger.warning("Error while enabling the \"" + pInfo.getPartitionId() + "\" partition. [" + e.getMessage());
                }
            }
        }
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
