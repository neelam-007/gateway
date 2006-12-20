package com.l7tech.server.config.commands;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PartitionActions;
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

    private static final String SERVICE_NAME_KEY = "SERVICE_NAME";
    private static final String SERVICE_DISPLAY_NAME_KEY = "PR_DISPLAYNAME";
    private static final String SERVICE_LOGPREFIX_KEY = "PR_LOGPREFIX";
    private static final String SERVICE_LOG_KEY = "PR_STDOUTPUT";
    private static final String SERVICE_ERRLOG_KEY = "PR_STDERROR";
    private static final String PARTITION_NAME_KEY = "PARTITIONNAMEPROPERTY";

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
            updateStartupScripts(pInfo);
            updateFirewallRules(pInfo);
            enablePartitionForStartup(pInfo);
        } catch (Exception e) {
            success = false;
        }
        return success;
    }

    private void enablePartitionForStartup(PartitionInformation pInfo) {
        PartitionActions.enablePartitionForStartup(pInfo);
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
            os.println("set " + SERVICE_NAME_KEY + "=" + partitionName.replaceAll("_", " ") + "SSG");
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
            ResourceUtils.closeQuietly(os);
        }
    }

    private void updateFirewallRules(PartitionInformation pInfo) {
        if (pInfo.getOSSpecificFunctions().isLinux())
            PartitionActions.doFirewallConfig(pInfo);
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

            PartitionInformation.HttpEndpointHolder httpEndpoint = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, httpEndpoints);
            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_HTTPPORT, httpEndpoint.port);

            PartitionInformation.HttpEndpointHolder sslEndpoint = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP, httpEndpoints);
            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_SSLPORT, sslEndpoint.port);

            PartitionInformation.OtherEndpointHolder rmiEndpoint = PartitionActions.getOtherEndpointByType(PartitionInformation.OtherEndpointType.RMI_ENDPOINT, otherEndpoints);
            if (StringUtils.isNotEmpty(rmiEndpoint.port))
                prop.setProperty(PartitionConfigBean.SYSTEM_PROP_RMIPORT, rmiEndpoint.port);

            prop.setProperty(PartitionConfigBean.SYSTEM_PROP_PARTITIONNAME, pInfo.getPartitionId());

            fos = new FileOutputStream(systemPropertiesFile);
            prop.store(fos, "");
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private void updatePartitionEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        updateHttpEndpoints(pInfo);
        updateOtherEndpoints(pInfo);
    }

    private void updateOtherEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();
        PartitionInformation.OtherEndpointHolder shutdownEndpoint = PartitionActions.getOtherEndpointByType(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT, otherEndpoints);

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
            ResourceUtils.closeQuietly(fos);
        }
    }

    private void updateHttpEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        List<PartitionInformation.HttpEndpointHolder> newHttpEndpoints = pInfo.getHttpEndpoints();
        Document serverConfigDom = pInfo.getOriginalDom();
        if (serverConfigDom == null) {
            serverConfigDom = getDomFromServerConfig(pInfo);
        }
        doEndpointTypeAwareUpdates(pInfo, newHttpEndpoints, serverConfigDom);

        FileOutputStream fos = null;
        try {
            OSSpecificFunctions foo = pInfo.getOSSpecificFunctions();
            fos = new FileOutputStream(foo.getTomcatServerConfig());
            XmlUtil.format(serverConfigDom, true);
            XmlUtil.nodeToOutputStream(serverConfigDom, fos);
        }finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    private void doEndpointTypeAwareUpdates(PartitionInformation pInfo, List<PartitionInformation.HttpEndpointHolder> endpoints, Document serverConfig) {

        NodeList connectors = serverConfig.getElementsByTagName("Connector");
        pruneConnectors(serverConfig, connectors, endpoints);

        Map<PartitionInformation.HttpEndpointType,Element> existingConnectors = PartitionActions.getHttpConnectorsByType(connectors);
        String redirectPort = "";
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            PartitionInformation.HttpEndpointType type = endpoint.endpointType;
            if (type == PartitionInformation.HttpEndpointType.SSL_HTTP)
                redirectPort = endpoint.port;

            Element connector;
            if (!existingConnectors.containsKey(type)) {
                connector = PartitionActions.addNewConnector(pInfo, serverConfig, endpoint);
            } else {
                connector = existingConnectors.get(type);
            }
            connector.setAttribute("address", endpoint.ipAddress);
            connector.setAttribute("port", endpoint.port);
        }
        existingConnectors.get(PartitionInformation.HttpEndpointType.BASIC_HTTP).setAttribute("redirectPort", redirectPort);
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

        PartitionInformation.HttpEndpointHolder holder;
        if (isSecure) {
            if (needsClientCert) holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP,endpoints);
            else holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, endpoints);
        } else {
            holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, endpoints);
        }
        return StringUtils.isNotEmpty(holder.ipAddress) && StringUtils.isNotEmpty(holder.port);
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
            ResourceUtils.closeQuietly(fis);
        }
        return doc;
    }

}