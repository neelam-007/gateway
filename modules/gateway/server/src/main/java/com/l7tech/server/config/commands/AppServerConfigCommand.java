package com.l7tech.server.config.commands;

import com.l7tech.util.ResourceUtils;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Apr 28, 2006
 * Time: 9:55:47 AM
 */
public class AppServerConfigCommand extends BaseConfigurationCommand {

    private static final Logger logger = Logger.getLogger(AppServerConfigCommand.class.getName());

    private static final String BACKUP_FILE_NAME = "server_xml_upgrade_backup";

    private static final String SOCKET_FACTORY_ATTR_NAME = "socketFactory";
    private static final String SOCKET_FACTORY_ATTR_VALUE = "com.l7tech.server.tomcat.SsgServerSocketFactory";
    private static final String INSECURE_CONNECTOR_XPATH = "/Server/Service/Connector[@secure=\"false\" or not(@secure)]";

    private static final String HOST_XPATH = "/Server/Service/Engine/Host";

    private static final String CONNECTION_ID_VALVE_NAME = "Connection ID Valve";
    private static final String CONNECTION_ID_VALVE_VALUE = "com.l7tech.server.tomcat.ConnectionIdValve";

    private static final String RESPONSE_KILLER_VALVE_NAME = "Response Killer Valve";
    private static final String RESPONSE_KILLER_VALVE_VALUE = "com.l7tech.server.tomcat.ResponseKillerValve";

    private static final String VALVE_NODE_NAME = "Valve";

    public AppServerConfigCommand() {
        super();
    }

    public boolean execute() {
        boolean success = false;

        File tomcatServerConfigFile = new File(getOsFunctions().getTomcatServerConfig());
        if (tomcatServerConfigFile.exists()) {
            File[] files = new File[]
            {
                tomcatServerConfigFile
            };

            backupFiles(files, BACKUP_FILE_NAME);
        }

        try {
            updateServerConfig(tomcatServerConfigFile);
            success = true;
        } catch (Exception e) {
            logger.severe("Problem updating the server.xml file with Connection ID Management support");
            logger.severe(e.getMessage());
        }
        return success;
    }

    private void updateServerConfig(File tomcatServerConfigFile) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(tomcatServerConfigFile);
            Document doc = XmlUtil.parse(fis);

            boolean somethingWasUpdated = doConnectionIdSupportConfig(doc);

            fis.close();
            fis = null;

            if (somethingWasUpdated) {
                fos = new FileOutputStream(tomcatServerConfigFile);
                XmlUtil.nodeToOutputStream(doc, fos);

                fos.close();
                fos = null;
            }
        } catch (FileNotFoundException e) {
            throw new Exception("Could not locate the file: " + tomcatServerConfigFile + ". The SecureSpan gateway cannot start without this file");
        } catch (IOException e) {
            throw new Exception("There was an error while updating the file: " + tomcatServerConfigFile + ". (" + e.getMessage() + ")");
        } catch (SAXException e) {
            throw new Exception(tomcatServerConfigFile + " contains invalid XML and cannot be parsed. (" + e.getMessage() + ")");
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private boolean doConnectionIdSupportConfig(Document doc) throws XPathExpressionException {

        XPath xpath = XPathFactory.newInstance().newXPath();

        //check for insecure connector nodes and update as necessary
        boolean changedInsecureConnector = doInsecureConnectorConfig(xpath, doc);

        //now do the secure ones
        boolean changedSecureConnector = false;

        //now do the valves
        boolean neededValveJob = doValveConfig(xpath, doc);

        return changedInsecureConnector || changedSecureConnector || neededValveJob;
    }

    private boolean doInsecureConnectorConfig(XPath xe, Document doc) throws XPathExpressionException {
        NodeList connectors = (NodeList) xe.evaluate(INSECURE_CONNECTOR_XPATH, doc, XPathConstants.NODESET);

        if (!updateConnectors(connectors, SOCKET_FACTORY_ATTR_NAME, SOCKET_FACTORY_ATTR_VALUE)) {
            logger.info("No need to update the server.xml socketFactory attribute since it already exists for all connectors");
            return false;
        }

        return true;
    }

    private boolean updateConnectors(NodeList connectors, String attributeName, String attributeValue) {
        boolean didSomething = false;
        if (connectors != null && connectors.getLength() > 0) {
            //there are some insecure connector nodes so lets see if they already have the attributes set.
        for (int x = 0; x < connectors.getLength(); ++x) {
                Element connector = (Element) connectors.item(x);
                if (!connector.hasAttribute(attributeName) || !connector.getAttribute(attributeName).equals(attributeValue)) {
                    //the attribute doesn't exist so add it.
                    connector.setAttribute(attributeName, attributeValue);
                    if (connector.hasAttribute("port")) {
                        logger.info("Updated server.xml: Added " + attributeName + " attribute to connector (port " + connector.getAttribute("port") + ")");
                    } else {
                        logger.info("Updated server.xml: Added " + attributeName + " attribute");
                    }
                    didSomething = true;
                }
            }
        }
        return didSomething;
    }

    private boolean doValveConfig(XPath xe, Document doc) throws XPathExpressionException {
        boolean neededValveJob = false;
        NodeList hosts = (NodeList) xe.evaluate(HOST_XPATH, doc, XPathConstants.NODESET);
        if (hosts != null && hosts.getLength() > 0) {
            for (   int x = 0; x < hosts.getLength(); ++x) {
                Element host = (Element) hosts.item(x);
                if (doValveJob(doc, host, CONNECTION_ID_VALVE_NAME, CONNECTION_ID_VALVE_VALUE)) {
                    neededValveJob = true;
                }
            }
            if (!neededValveJob) {
                logger.info("No need to update the server.xml with a Connection ID Valve since it already exists for all hosts");
            }

            for (int x = 0 ;x < hosts.getLength(); ++x) {
                Element host = (Element) hosts.item(x);
                if (doValveJob(doc, host, RESPONSE_KILLER_VALVE_NAME, RESPONSE_KILLER_VALVE_VALUE)) {
                    neededValveJob = true;
                }
            }
            if (!neededValveJob) {
                logger.info("No need to update the server.xml with a Response Killer Valve since it already exists for all hosts");
            }
        }
        return neededValveJob;
    }

    private boolean findSpecifiedValves(Element parent, String valveValue) throws XPathExpressionException {
        NodeList valves = parent.getElementsByTagName(VALVE_NODE_NAME);
        if (valves == null || valves.getLength() <= 0) {
            return false;
        }

        for (int i = 0; i < valves.getLength(); i++) {
            Element valve = (Element) valves.item(i);
            if (valve.getAttribute("className").equals(valveValue)) {
                return true;
            }
        }

        return false;
    }

    private boolean doValveJob(Document rootDoc, Element hostToModify, String valveName, String valveValue) throws XPathExpressionException {
        if (!findSpecifiedValves(hostToModify, valveValue)) {
            Element valve = rootDoc.createElement(VALVE_NODE_NAME);
            valve.setAttribute("className", valveValue);
            hostToModify.appendChild(valve);
            if (hostToModify.hasAttribute("name")) {
                logger.info("Updated server xml: Added new " + valveName + " to hostToModify (" + hostToModify.getAttribute("name") +")");
            } else {
                logger.info("Updated server xml: Added new " + valveName + " to hostToModify");
            }
            return true;
        }
        return false;
    }
}
