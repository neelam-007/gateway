package com.l7tech.server.config;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 24, 2006
 * Time: 12:16:38 PM
 */
public class PartitionActions {
    private static final Logger logger = Logger.getLogger(PartitionActions.class.getName());
    private OSSpecificFunctions osFunctions;

    private static final String SERVICE_NAME_KEY = "SERVICE_NAME";
    private static final String SERVICE_DISPLAY_NAME_KEY = "PR_DISPLAYNAME";
    private static final String SERVICE_LOGPREFIX_KEY = "PR_LOGPREFIX";
    private static final String SERVICE_LOG_KEY = "PR_STDOUTPUT";
    private static final String SERVICE_ERRLOG_KEY = "PR_STDERROR";
    private static final String PARTITION_NAME_KEY = "PARTITIONNAMEPROPERTY";

    private static String[][] secureConnectorEndpointAttributes = new String[][] {
            {"port", "9443"},
            {"maxThreads", "150"},
            {"minSpareThreads", "25"},
            {"maxSpareThreads", "75"},
            {"enableLookups", "false"},
            {"disableUploadTimeout", "true"},
            {"acceptCount", "100"},
            {"scheme", "https"},
            {"secure", "true"},
            {"clientAuth", "false"},
            {"sslProtocol", "TLS"},
            {"keystoreFile", "/ssg/etc/keys/ssl.ks"},
            {"keystorePass", "blahblah"},
            {"keystoreType", "PKCS12"},
            {"SSLImplementation", "com.l7tech.server.tomcat.SsgSSLImplementation"},
    };

    private static String[][] basicConnectorEndpointAttributes = new String[][] {
            {"port", "8080"},
            {"maxThreads", "150"},
            {"minSpareThreads", "25"},
            {"maxSpareThreads", "75"},
            {"enableLookups", "false"},
            {"redirectPort", "8443"},
            {"acceptCount", "100"},
            {"connectionTimeout","20000"},
            {"disableUploadTimeout", "true"},
            {"socketFactory", "com.l7tech.server.tomcat.SsgServerSocketFactory"},
    };

    public PartitionActions(OSSpecificFunctions osf) {
        osFunctions = osf;
    }

    public static void changeDirName(String oldPartitionId, String newPartitionId) throws IOException {
        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions("");
        String oldDirPath = osFunctions.getPartitionBase() + oldPartitionId;
        File oldDir = new File(oldDirPath);
        if (!oldDir.exists()) throw new FileNotFoundException("Could not find the directory: " + oldDirPath);

        String newDirPath = osFunctions.getPartitionBase() + newPartitionId;
        File newDir = new File(newDirPath);
        if (newDir.exists()) throw new IOException(
                "Cannot rename \"" + oldPartitionId + "\" to \"" + newPartitionId + "\". " + newDir.getPath() + " already exists");

        if (!oldDir.renameTo(newDir))
            throw new IOException("Cannot rename \"" + oldPartitionId + "\" to \"" + newPartitionId + "\".");
    }

    public File createNewPartition(String partitionDir) throws IOException, InterruptedException {
        String fullPath = osFunctions.getPartitionBase() + partitionDir;
        File newPartitionDir = new File(fullPath);
        if (!newPartitionDir.exists()) {
            logger.info("Creating \"" + partitionDir + "\" Directory");
            newPartitionDir.mkdir();
        }
        copyTemplateFiles(newPartitionDir);
        setLinuxFilePermissions(new String[]{newPartitionDir.getAbsolutePath()}, "775", newPartitionDir, osFunctions);
        setLinuxFilePermissions(new String[]{newPartitionDir.getAbsolutePath() + "/var/attachments"}, "775", newPartitionDir, osFunctions);
        setLinuxFilePermissions(new String[]{newPartitionDir.getAbsolutePath() + "/var/modules"}, "775", newPartitionDir, osFunctions);
        return newPartitionDir;
    }

    private void copyTemplateFiles(File partitionPath) throws IOException {
        if (!partitionPath.isDirectory()) {
            throw new IllegalArgumentException("Partition destination must be a directory");
        }

        File templateDir = new File(osFunctions.getPartitionBase() + PartitionInformation.TEMPLATE_PARTITION_NAME);
        if (!templateDir.exists()) {
            throw new FileNotFoundException("Could not find \"" + templateDir.getName() + "\". Cannot copy template configuration files");
        }

        copyFilesInDirectory(templateDir, partitionPath);
    }

    public static void copyFilesInDirectory(File sourceDirectory, File destination) throws IOException {
        File[] templateFiles = sourceDirectory.listFiles();
        if (templateFiles != null && templateFiles.length > 0) {
            for (File currentFile : templateFiles) {
                if (currentFile.isDirectory()) {
                    File destinationDir = new File(destination, currentFile.getName());
                    if (!destinationDir.exists())
                        destinationDir.mkdir();

                    copyFilesInDirectory(currentFile, destinationDir);
                } else {
                    File newFile = new File(destination, currentFile.getName());
                    FileUtils.copyFile(currentFile, newFile);
                }
            }
        }
    }

    public static boolean renamePartition(PartitionInformation partitionToRename, String newName, PartitionActionListener listener) throws IOException {
        //0. Check if this partition exists
        if (partitionToRename == null)
            return false;

        File newPartitionDir = new File(partitionToRename.getOSSpecificFunctions().getPartitionBase() + newName);
        if (newPartitionDir.exists())
            return false;

        //if we are renaming a partition that doesn't exist on disk yet then just rename it, don't bother trying all the disk related stuff
        File existingPartitionDir = new File(partitionToRename.getOSSpecificFunctions().getPartitionBase() + partitionToRename.getPartitionId());
        if (!existingPartitionDir.exists()) {
            PartitionManager.getInstance().renamePartition(partitionToRename.getPartitionId(), newName);
            return true;
        }

        //else
        boolean renamed;
        try {
            boolean confirmationGoAhead = true;
            if (listener != null) {
                String message;
                if (partitionToRename.getOSSpecificFunctions().isLinux()) {
                    message = "Please ensure that the \"" + partitionToRename.getPartitionId() + "\" partition is stopped before proceeding.\n\n" +
                            "Is the partition stopped and ready to be renamed?";
                } else {
                    message = "This will require the \"" + partitionToRename.getPartitionId() + "\" partition to be stopped and renamed.\n\n" +
                            "Is it OK to stop and rename the partition now?";
                }

                confirmationGoAhead = listener.getConfirmation(message);
            }

            if (confirmationGoAhead) {
                //1. create the "new" directory
                newPartitionDir.mkdir();

                //2. copy the configuration to the new directory
                File oldPartitionDir = new File(partitionToRename.getOSSpecificFunctions().getPartitionBase() + partitionToRename.getPartitionId());
                copyFilesInDirectory(oldPartitionDir, newPartitionDir);

                //this will stop/remove any services associated with the partition
                removePartition(partitionToRename, null);
                PartitionManager.getInstance().addPartition(newName);
                PartitionInformation newPi = PartitionManager.getInstance().getPartition(newName);
                prepareNewpartition(newPi);

                //6. if (windows) install the new service
                installWindowsService(newPi);
                renamed = true;
            } else {
                renamed = false;
            }
        } catch (Exception e) {
            logger.warning("Could not rename the \"" + partitionToRename.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            renamed = false;
        }
        return renamed;
    }

    public static void updateSystemProperties(PartitionInformation pInfo) throws IOException {
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
            prop.setProperty(PartitionInformation.SYSTEM_PROP_HTTPPORT, httpEndpoint.getPort());

            PartitionInformation.HttpEndpointHolder sslEndpoint = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP, httpEndpoints);
            prop.setProperty(PartitionInformation.SYSTEM_PROP_SSLPORT, sslEndpoint.getPort());

            PartitionInformation.OtherEndpointHolder rmiEndpoint = PartitionActions.getOtherEndpointByType(PartitionInformation.OtherEndpointType.RMI_ENDPOINT, otherEndpoints);
            if (StringUtils.isNotEmpty(rmiEndpoint.getPort()))
                prop.setProperty(PartitionInformation.SYSTEM_PROP_RMIPORT, rmiEndpoint.getPort());

            prop.setProperty(PartitionInformation.SYSTEM_PROP_PARTITIONNAME, pInfo.getPartitionId());

            fos = new FileOutputStream(systemPropertiesFile);
            prop.store(fos, "");
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    public static void fixKeystorePaths(File partitionDir) throws FileNotFoundException {
        File serverConfig = new File(partitionDir, "server.xml");
        File keystoreProperties = new File(partitionDir, "keystore.properties");
        FileInputStream serverConfigFis = null;
        FileInputStream keystoreConfigFis = null;

        FileOutputStream serverConfigFos = null;
        FileOutputStream keystoreConfigFos = null;
        try {
            File newKeystorePath = new File(partitionDir, "keys");

            serverConfigFis = new FileInputStream(serverConfig);
            Document serverConfigDom = XmlUtil.parse(serverConfigFis);
            NodeList nodes = serverConfigDom.getElementsByTagName("Connector");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element connectorNode = (Element) nodes.item(i);
                if (connectorNode.hasAttribute("keystoreFile")) {
                    String keystorePath = connectorNode.getAttribute("keystoreFile");
                    int keystoreFileIndex = keystorePath.indexOf("ssl.ks");

                    String newKsFile = newKeystorePath.getAbsolutePath() + File.separator + keystorePath.substring(keystoreFileIndex);
                    connectorNode.setAttribute("keystoreFile", newKsFile);
                }
            }
            serverConfigFos = new FileOutputStream(serverConfig);
            XmlUtil.nodeToOutputStream(serverConfigDom, serverConfigFos);

            keystoreConfigFis = new FileInputStream(keystoreProperties);
            Properties props = new Properties();
            props.load(keystoreConfigFis);
            props.setProperty("keystoredir", newKeystorePath.getAbsolutePath());
            keystoreConfigFos = new FileOutputStream(keystoreProperties);
            props.store(keystoreConfigFos, "");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } finally {
            ResourceUtils.closeQuietly(serverConfigFis);
            ResourceUtils.closeQuietly(serverConfigFos);
            ResourceUtils.closeQuietly(keystoreConfigFis);
            ResourceUtils.closeQuietly(keystoreConfigFos);
        }
    }

    public static void updatePartitionEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        updateHttpEndpoints(pInfo);
        updateOtherEndpoints(pInfo);
    }

    private static void updateOtherEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
        List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();
        PartitionInformation.OtherEndpointHolder shutdownEndpoint = PartitionActions.getOtherEndpointByType(PartitionInformation.OtherEndpointType.TOMCAT_MANAGEMENT_ENDPOINT, otherEndpoints);

        Document serverConfigDom = pInfo.getOriginalDom();
        if (serverConfigDom == null) {
            serverConfigDom = getDomFromServerConfig(pInfo);
        }
        NodeList serverNodes = serverConfigDom.getElementsByTagName("Server");
        for (int i = 0; i < serverNodes.getLength(); i++) {
            Element serverNode = (Element) serverNodes.item(i);
            serverNode.setAttribute("port", shutdownEndpoint.getPort());
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

    private static void updateHttpEndpoints(PartitionInformation pInfo) throws IOException, SAXException {
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

    private static void doEndpointTypeAwareUpdates(PartitionInformation pInfo, List<PartitionInformation.HttpEndpointHolder> endpoints, Document serverConfig) {

        NodeList connectors = serverConfig.getElementsByTagName("Connector");
        pruneConnectors(serverConfig, connectors, endpoints);

        Map<PartitionInformation.HttpEndpointType,Element> existingConnectors = PartitionActions.getHttpConnectorsByType(connectors);
        String redirectPort = "";
        String seenKeystorePass = null;
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            PartitionInformation.HttpEndpointType type = endpoint.endpointType;
            if (type == PartitionInformation.HttpEndpointType.SSL_HTTP) {
                redirectPort = endpoint.getPort();
                Element secureConnector = existingConnectors.get(PartitionInformation.HttpEndpointType.SSL_HTTP);
                if (secureConnector != null) {
                    if (secureConnector.hasAttribute("keystorePass")) {
                            seenKeystorePass = secureConnector.getAttribute("keystorePass");
                    }
                }
            }

            Element connector;
            if (!existingConnectors.containsKey(type)) {
                connector = PartitionActions.addNewConnector(pInfo, serverConfig, endpoint);
                existingConnectors.put(endpoint.endpointType, connector);
            } else {
                connector = existingConnectors.get(type);
            }
            connector.setAttribute("address", endpoint.getIpAddress());
            connector.setAttribute("port", endpoint.getPort());
        }
        existingConnectors.get(PartitionInformation.HttpEndpointType.BASIC_HTTP).setAttribute("redirectPort", redirectPort);
        if (StringUtils.isNotEmpty(seenKeystorePass))
            existingConnectors.get(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT).setAttribute("keystorePass", seenKeystorePass);
    }


    private static void pruneConnectors(Document dom, NodeList connectors, List<PartitionInformation.HttpEndpointHolder> newEndpoints) {
        for (int index = 0; index < connectors.getLength(); index++) {
            Element connectorNode = (Element) connectors.item(index);
            if (!existsInNewEndpoints(connectorNode, newEndpoints))
                dom.removeChild(connectorNode);
        }
    }

    private static boolean existsInNewEndpoints(Element connector, List<PartitionInformation.HttpEndpointHolder> endpoints) {
        boolean isSecure = StringUtils.equals(connector.getAttribute("secure"), "true");
        boolean needsClientCert = StringUtils.equals(connector.getAttribute("clientAuth"),"want");

        PartitionInformation.HttpEndpointHolder holder;
        if (isSecure) {
            if (needsClientCert) holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP,endpoints);
            else holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, endpoints);
        } else {
            holder = PartitionActions.getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, endpoints);
        }
        return StringUtils.isNotEmpty(holder.getIpAddress()) && StringUtils.isNotEmpty(holder.getPort());
    }

    private static Document getDomFromServerConfig(PartitionInformation pInfo) throws IOException, SAXException {
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

    public static void prepareNewpartition(PartitionInformation newPartition) throws IOException, SAXException {
        if (newPartition == null) {
            return;
        }

        //edit the system.properties file so the name is correct
        try {
            updatePartitionEndpoints(newPartition);
            fixKeystorePaths(new File(newPartition.getOSSpecificFunctions().getPartitionBase() + newPartition.getPartitionId()));
            updateSystemProperties(newPartition);
        } catch (FileNotFoundException e) {
            logger.warning("Error while preparing the \"" + newPartition.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            throw e;
        } catch (IOException e) {
            logger.warning("Error while preparing the \"" + newPartition.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            throw e;
        } catch (SAXException e) {
            logger.warning("Error while preparing the \"" + newPartition.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            throw e;
        }

        if (newPartition.getOSSpecificFunctions().isWindows())
            prepareWindowsServiceConfig(newPartition);
    }

    private static void writeWindowsServiceConfigFile(PartitionInformation pinfo) throws IOException {
        File configFile = new File(pinfo.getOSSpecificFunctions().getPartitionBase() + pinfo.getPartitionId(),"partition_config.cmd");
        PrintStream os = null;
        logger.info("Modifying the windows service configuration for " + pinfo.getPartitionId());
        try {
            //write out a config file that will set some variables needed by the service installer.
            os = new PrintStream(new FileOutputStream(configFile));
            os.println("set " + PARTITION_NAME_KEY + "=" + pinfo.getPartitionId());
            os.println("set " + SERVICE_NAME_KEY + "=" + pinfo.getPartitionId().replaceAll("_", "") + "SSG");
            os.println("set " + SERVICE_DISPLAY_NAME_KEY + "=" + "SecureSpan Gateway - " + pinfo.getPartitionId() + " Partition");
            os.println("set " + SERVICE_LOGPREFIX_KEY + "=" + pinfo.getPartitionId() + "_ssg_service.log");
            os.println("set " + SERVICE_LOG_KEY + "=" + "%TOMCAT_HOME%\\logs\\catalina.out." + pinfo.getPartitionId());
            os.println("set " + SERVICE_ERRLOG_KEY + "=" + "%TOMCAT_HOME%\\logs\\catalina.err." + pinfo.getPartitionId());
            os.flush();
        } catch (FileNotFoundException e) {
            logger.warning("Error while modifying the windows service configuration for the \"" + pinfo.getPartitionId() + "\" partition. [" + e.getMessage() +"]");
            throw e;
        } catch (IOException e) {
            logger.warning("Error while modifying the windows service configuration for the \"" + pinfo.getPartitionId() + "\" partition. [" + e.getMessage() +"]");
            throw e;
        } finally {
            ResourceUtils.closeQuietly(os);
        }
    }

    public static void prepareWindowsServiceConfig(PartitionInformation pInfo) throws IOException {
        if (!pInfo.getOSSpecificFunctions().isWindows())
            return;

        writeWindowsServiceConfigFile(pInfo);
    }

    public static boolean removePartition(PartitionInformation partitionToRemove, PartitionActionListener listener) {
        boolean wasDeleted = false;

        OSSpecificFunctions osFunctions = partitionToRemove.getOSSpecificFunctions();
        String message;
        boolean okToProceed;
        if (osFunctions.isWindows()) {
            try {
                if (listener != null) {
                    message = "Removing the \"" + partitionToRemove.getPartitionId() + "\" partition will stop the service and remove all the associated configuration.\n\n" +
                            "This cannot be undone.\n" +
                            "Do you wish to proceed?";
                    okToProceed = listener.getConfirmation(message);
                } else {
                    okToProceed = true;
                }

                if (okToProceed) {
                    //this will stop the service if it's running and uninstall it too
                    uninstallService(partitionToRemove, osFunctions);
                    wasDeleted = true;
                }
            } catch (Exception e) {
                logger.warning("Could not uninstall the SSG service for the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
                wasDeleted = false;
            }
        } else {
            try {
                if (listener != null) {
                    message = "Please ensure that the \"" + partitionToRemove.getPartitionId() + "\" partition is stopped before proceeding.\n\n" +
                            "Is the partition stopped?";
                    okToProceed = listener.getConfirmation(message);
                } else {
                    okToProceed = true;
                }

                if (okToProceed) {
                    wasDeleted = true;
                }
            } catch (Exception e) {
                logger.warning("Could not delete the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
                wasDeleted = false;
            }
                    //call the partitionControl.sh stop partitionToRemove.getPartitionId on linux
//            String controlScript = partitionToRemove.getOSSpecificFunctions().getOriginalPartitionControlScriptName();
//            File parentDir = new File(controlScript).getParentFile();
//TODO currently it's not possible to run the script unless you are root. We need to change this or stopping/starting won't work for the partition in linux
//            try {
//                int retcode = executeCommand(new String[] {
//                    controlScript,
//                    "stop",
//                    partitionToRemove.getPartitionId()
//                }, parentDir);
//                wasDeleted = true;
//                if (retcode == 0) {
//                    logger.info("Partition stopped successfully");
//                    wasDeleted = true;
//                } else {
//                    logger.warning("Could not stop the partition");
//                    wasDeleted = false;
//                }
//            } catch (IOException e) {
//                logger.warning("Could not stop the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
//                wasDeleted = false;
//            } catch (InterruptedException e) {
//                logger.warning("Could not stop the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
//                wasDeleted = false;
//            }
        }

        if (wasDeleted) {
            doRemoveAssociatedDatabasePrompts(partitionToRemove, listener);
            File deleteMe = new File(osFunctions.getPartitionBase() + partitionToRemove.getPartitionId());
            if (deleteMe.exists()) {
                wasDeleted = FileUtils.deleteDir(deleteMe);
            }

            if (wasDeleted) {
                PartitionManager.getInstance().removePartition(partitionToRemove.getPartitionId());
            }
        }

        return wasDeleted;
    }

    private static void doRemoveAssociatedDatabasePrompts(PartitionInformation partitionToRemove, PartitionActionListener listener) {
        if (listener != null) {
            try {
                OSSpecificFunctions osf = partitionToRemove.getOSSpecificFunctions();

                SsgDatabaseConfigBean dbBean = new SsgDatabaseConfigBean(osf.getDatabaseConfig());
                DBActions dba = new DBActions(osf);
                boolean removeDb = listener.getConfirmation(
                        "This wizard can remove the database used by this partition.\n" +
                        "This will remove all data in the database and cannot be undone.\n\n" +
                        "Are you sure you want to delete the database named \"" + dbBean.getDbName() + "\" ?");

                if (removeDb)
                        dba.dropDatabase(dbBean.getDbName(), dbBean.getDbHostname(), dbBean.getDbUsername(), dbBean.getDbPassword(), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static int executeCommand(String[] cmdArray, File parentDir) throws IOException, InterruptedException {
        int retCode = 0;
        if (cmdArray != null && cmdArray.length != 0) {
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(cmdArray, null, parentDir);
                InputStream is = new BufferedInputStream(p.getInputStream());
                byte[] buff = HexUtils.slurpStreamLocalBuffer(is);
                retCode = p.waitFor();
            } finally {
                if (p != null)
                    p.destroy();
            }
        }
        return retCode;
    }

    public static void installWindowsService(PartitionInformation pInfo) throws InterruptedException, IOException {
        if (!pInfo.getOSSpecificFunctions().isWindows())
            return;

        String serviceCommandFile = pInfo.getOSSpecificFunctions().getSpecificPartitionControlScriptName();
        String[] cmdArray = new String[] {
                serviceCommandFile,
                "install",
        };

        //install the service
        try {
            Process p = null;
            try {
                logger.info("Attempting to install windows service for \"" + pInfo.getPartitionId() + "\" partition.");
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

    public static void uninstallService(PartitionInformation partitionToRemove, OSSpecificFunctions osSpecificFunctions) throws IOException, InterruptedException {
        if (!osSpecificFunctions.isWindows())
            return;

        String serviceCleanupFile = osSpecificFunctions.getSsgInstallRoot() + "bin/remove_service.cmd";
        String partitionBase = osSpecificFunctions.getPartitionBase();
        String partitionName = partitionToRemove.getPartitionId();

        File partitionServiceConfigFile = new File(partitionBase + partitionName, "partition_config.cmd");
        if (new File(serviceCleanupFile).exists() && partitionServiceConfigFile.exists()) {
            String[] cmdArray = new String[] {
                serviceCleanupFile,
                partitionBase,
                partitionName,
            };

            //call the cleanup script for this partition to stop and remove the service.
            Process p = null;
            try {
                logger.info("Stopping and uninstalling windows service for \"" + partitionName + "\" partition.");
                File parentDir = new File(partitionBase + partitionName);
                p = Runtime.getRuntime().exec(cmdArray, null, parentDir);
                InputStream is = new BufferedInputStream(p.getInputStream());
                byte[] buff = HexUtils.slurpStreamLocalBuffer(is);
//                System.out.println(new String(buff));
                p.waitFor();
            } finally {
                if (p != null)
                    p.destroy();
            }
        }
    }

    public static Vector<String> getAvailableIpAddresses() {
        String localHostName;
        Set<String> allIpAddresses = new HashSet<String>();
        allIpAddresses.add("*");
        try {
            localHostName = InetAddress.getLocalHost().getCanonicalHostName();
            InetAddress[] localAddresses = InetAddress.getAllByName(localHostName);
            for (InetAddress localAddress : localAddresses) {
                allIpAddresses.add(localAddress.getHostAddress());
            }

            NetworkingConfigurationBean netBean = new NetworkingConfigurationBean("","");
            List<NetworkingConfigurationBean.NetworkConfig> networkConfigs = netBean.getAllNetworkInterfaces();
            if (networkConfigs != null) {
                for (NetworkingConfigurationBean.NetworkConfig networkConfig : networkConfigs) {
                    String bootProto = networkConfig.getBootProto();

                    if (!StringUtils.equals(bootProto, NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO))
                        allIpAddresses.add(networkConfig.getIpAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not determine the network interfaces for this gateway. Please run the system configuration wizard");
        }
        return new Vector<String>(allIpAddresses);
    }

    public void setLinuxFilePermissions(String[] files, String permissions, File workingDir, OSSpecificFunctions osf) throws IOException, InterruptedException {
        if (!osf.isLinux())
            return;

        logger.fine("Setting Permissions on var directory to 775");
        List<String> commandLine = new ArrayList<String>();
        commandLine.add("chmod");
        commandLine.add(permissions);
        for (String file : files) {
            commandLine.add(file);
        }

        logger.fine("Command Line = " + commandLine);

        Process changer = null;
        try {
            String[] commandsArray = commandLine.toArray(new String[0]);
            changer = Runtime.getRuntime().exec(commandsArray, null, workingDir);
            int x = changer.waitFor();
            BufferedInputStream is = new BufferedInputStream(changer.getInputStream());
            //make sure the command executes if it's waiting for someone to read it's output
            byte[] bytes = HexUtils.slurpStreamLocalBuffer(is);
            logger.fine("Return Code = " + String.valueOf(x));
            logger.fine("Output of command = " + new String(bytes));

        } finally {
            if (changer != null)
                changer.destroy();
        }
    }

    public static boolean validateSinglePartitionEndpoints(PartitionInformation pinfo) {
        boolean hadErrors = false;

        List<PartitionInformation.IpPortPair> seenPairs = new ArrayList<PartitionInformation.IpPortPair>();

        List<PartitionInformation.EndpointHolder> allHolders = new ArrayList<PartitionInformation.EndpointHolder>();
        allHolders.addAll(pinfo.getHttpEndpoints());
        allHolders.addAll(pinfo.getOtherEndpoints());

        for (PartitionInformation.EndpointHolder holder : allHolders) {
            int intPort;
            try {
                intPort = Integer.parseInt(holder.getPort());
            } catch (NumberFormatException e) {
                intPort = 0;
                holder.setPort("");
            }

            if ( intPort < 1024) {
                holder.setValidationMessaqe("The SecureSpan Gateway cannot use ports less than 1024");
                hadErrors = true;
            } else if (intPort > 65535) {
                holder.setValidationMessaqe("The maximum port allowed is 65535");
                hadErrors = true;
            } else {
                PartitionInformation.IpPortPair pair = new PartitionInformation.IpPortPair(holder);
                boolean foundMatch = false;
                for (PartitionInformation.IpPortPair seenPair : seenPairs) {
                    if (seenPair.equals(pair)) {
                        foundMatch = true;
                        break;
                    }
                }

                if (!foundMatch) {
                    holder.setValidationMessaqe("");
                    seenPairs.add(pair);
                } else {
                    holder.setValidationMessaqe(pair.toString() + " is already in use in this partition.");
                    hadErrors = true;
                }
            }
        }

        return !hadErrors;
    }

    public static boolean validateAllPartitionEndpoints(PartitionInformation currentPartition, boolean incrementEndpoints) {
        boolean isOK = validateSinglePartitionEndpoints(currentPartition);

        if (isOK) {
            List<PartitionInformation.EndpointHolder> currentEndpoints = new ArrayList<PartitionInformation.EndpointHolder>();
            currentEndpoints.addAll(currentPartition.getHttpEndpoints());
            currentEndpoints.addAll(currentPartition.getOtherEndpoints());

            Map<String, List<PartitionInformation.IpPortPair>> portMap = PartitionManager.getInstance().getAllPartitionPorts();
            //don't compare against the current partition
            portMap.remove(currentPartition.getPartitionId());

            for (PartitionInformation.EndpointHolder currentEndpoint : currentEndpoints) {
                List<String> matches = findMatchingEndpoints(currentEndpoint, portMap);
                if (!matches.isEmpty()) {
                    if (incrementEndpoints) {
                        incrementPartitionEndpoint(currentEndpoint, portMap);
                        isOK = true;
                    } else {
                        String message = new PartitionInformation.IpPortPair(currentEndpoint).toString() + " is used by partitions: ";
                        boolean first = true;
                        for (String match : matches) {
                            message += (first?"":", ") + match;
                            first = false;
                        }
                        currentEndpoint.setValidationMessaqe(message);
                        isOK = false;
                    }
                }
            }
        }

        return isOK;
    }

    private static void incrementPartitionEndpoint(PartitionInformation.EndpointHolder currentEndpoint, Map<String, List<PartitionInformation.IpPortPair>> portMap) {
        List<PartitionInformation.IpPortPair> allPairs = new ArrayList<PartitionInformation.IpPortPair>();
        for (String partitionName : portMap.keySet()) {
            List<PartitionInformation.IpPortPair> onePartitionsPairs = portMap.get(partitionName);
            if (onePartitionsPairs != null) {
                allPairs.addAll(onePartitionsPairs);
            }
        }

        PartitionInformation.IpPortPair currentPair = new PartitionInformation.IpPortPair(currentEndpoint);
        int x = Integer.parseInt(currentPair.getPort());
        do {
            currentPair.setPort(String.valueOf(++x));
        } while(x <= PartitionInformation.MAX_PORT && allPairs.contains(currentPair));
    }

    private static List<String> findMatchingEndpoints(PartitionInformation.EndpointHolder currentEndpoint,
                                                 Map<String, List<PartitionInformation.IpPortPair>> portMap) {
        List<String> matches = new ArrayList<String>();
        for (Map.Entry<String, List<PartitionInformation.IpPortPair>> partitionEntry : portMap.entrySet()) {
            List<PartitionInformation.IpPortPair> pairs = partitionEntry.getValue();
            PartitionInformation.IpPortPair currentPair = new PartitionInformation.IpPortPair(currentEndpoint);
            if (pairs.contains(currentPair)) {
                matches.add(partitionEntry.getKey());
            }
        }

        return matches;
    }

    public static void doFirewallConfig(PartitionInformation pInfo) {
        List<PartitionInformation.HttpEndpointHolder> httpEndpoints = pInfo.getHttpEndpoints();
        List<PartitionInformation.OtherEndpointHolder> otherEndpoints = pInfo.getOtherEndpoints();

        PartitionInformation.HttpEndpointHolder basicEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.BASIC_HTTP, httpEndpoints);
        PartitionInformation.HttpEndpointHolder sslEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP, httpEndpoints);
        PartitionInformation.HttpEndpointHolder noAuthSslEndpoint = getHttpEndpointByType(PartitionInformation.HttpEndpointType.SSL_HTTP_NOCLIENTCERT, httpEndpoints);
        PartitionInformation.OtherEndpointHolder rmiEndpoint = getOtherEndpointByType(PartitionInformation.OtherEndpointType.RMI_ENDPOINT, otherEndpoints);

        String rules = PartitionInformation.firewallRules;
        if (basicEndpoint.getIpAddress().equals("*"))
            rules = rules.replaceAll("-d " + PartitionInformation.BASIC_IP_MARKER, "");
        else
            rules = rules.replaceAll(PartitionInformation.BASIC_IP_MARKER, basicEndpoint.getIpAddress());
        rules = rules.replaceAll(PartitionInformation.BASIC_PORT_MARKER, basicEndpoint.getPort());

        if (sslEndpoint.getIpAddress().equals("*"))
            rules = rules.replaceAll("-d " + PartitionInformation.SSL_IP_MARKER, "");
        else
            rules = rules.replaceAll(PartitionInformation.SSL_IP_MARKER, sslEndpoint.getIpAddress());
        rules = rules.replaceAll(PartitionInformation.SSL_PORT_MARKER, sslEndpoint.getPort());

        if (noAuthSslEndpoint.getIpAddress().equals("*"))
            rules = rules.replaceAll("-d " + PartitionInformation.NOAUTH_SSL_IP_MARKER, "");
        else
            rules = rules.replaceAll(PartitionInformation.NOAUTH_SSL_IP_MARKER, noAuthSslEndpoint.getIpAddress());

        rules = rules.replaceAll(PartitionInformation.NOAUTH_SSL_PORT_MARKER, noAuthSslEndpoint.getPort());

        rules = rules.replaceAll(PartitionInformation.RMI_PORT_MARKER, rmiEndpoint.getPort());

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
            ResourceUtils.closeQuietly(fos);
        }
    }

    public static PartitionInformation.HttpEndpointHolder getHttpEndpointByType(PartitionInformation.HttpEndpointType type,
                                                                    List<PartitionInformation.HttpEndpointHolder> endpoints) {
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            if (endpoint.endpointType == type) return endpoint;
        }
        return null;
    }

    public static PartitionInformation.OtherEndpointHolder getOtherEndpointByType(PartitionInformation.OtherEndpointType type,
                                                                    List<PartitionInformation.OtherEndpointHolder> endpoints) {
        for (PartitionInformation.OtherEndpointHolder endpoint : endpoints) {
            if (endpoint.endpointType == type) return endpoint;
        }
        return null;
    }

    public static  Map<PartitionInformation.HttpEndpointType, Element> getHttpConnectorsByType(NodeList connectors) {
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


    public static void enablePartitionForStartup(PartitionInformation pInfo){
        OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
        if (osf.isLinux()) {
            File enableStartupFile = new File(osf.getPartitionBase() + pInfo.getPartitionId(), PartitionInformation.ENABLED_FILE);
            if (pInfo.shouldDisable()) {
                logger.warning("Disabling the \"" + pInfo.getPartitionId() + "\" partition.");
                enableStartupFile.delete();
            } else {
                try {
                    logger.info("Enabling the \"" + pInfo.getPartitionId() + "\" partition.");
                    enableStartupFile.createNewFile();
                } catch (IOException e) {
                    logger.warning("Error while enabling the \"" + pInfo.getPartitionId() + "\" partition. [" + e.getMessage());
                }
            }
        }
    }

    public static Element addNewConnector(PartitionInformation pInfo, Document serverConfig, PartitionInformation.HttpEndpointHolder newEndpoint) {
        PartitionInformation.HttpEndpointType httpType = newEndpoint.endpointType;
        Element newNode = serverConfig.createElement("Connector");
        switch(httpType) {
            case BASIC_HTTP:
                for (String[] basicConnectorEndpointAttribute : basicConnectorEndpointAttributes) {
                    newNode.setAttribute(basicConnectorEndpointAttribute[0], basicConnectorEndpointAttribute[1]);
                }
                break;
            case SSL_HTTP:
                for (String[] secureConnectorEndpointAttribute : secureConnectorEndpointAttributes) {
                    newNode.setAttribute(secureConnectorEndpointAttribute[0], secureConnectorEndpointAttribute[1]);
                }
                newNode.setAttribute("secure", "true");
                newNode.setAttribute("clientAuth", "want");
                newNode.setAttribute("keystoreFile", pInfo.getOSSpecificFunctions().getKeystoreDir()+ "ssl.ks");
                break;
            case SSL_HTTP_NOCLIENTCERT:
                for (String[] secureConnectorEndpointAttribute : secureConnectorEndpointAttributes) {
                    newNode.setAttribute(secureConnectorEndpointAttribute[0], secureConnectorEndpointAttribute[1]);
                }
                newNode.setAttribute("secure", "true");
                newNode.setAttribute("clientAuth", "false");
                newNode.setAttribute("keystoreFile", pInfo.getOSSpecificFunctions().getKeystoreDir()+ "ssl.ks");
                break;
        }
        Element serviceElement = XmlUtil.findFirstChildElementByName(serverConfig.getDocumentElement(), (String) null, "Service");
        Element engineElement = XmlUtil.findFirstChildElementByName(serviceElement, (String) null, "Engine");

        serviceElement.insertBefore(newNode, engineElement);
        return newNode;
    }
}