package com.l7tech.server.partition;

import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PartitionActions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Manage the partition configurations on an SSG.
 * Partitions are physically separate instances of the gateway running from one codebase (the WAR) with different configurations.
 *
 * Partition configurations are located in $SSG_HOME/etc/conf/partitions/partitionName where partitionName is a normal String.
 * All configuration for an SSG is under this directory
 *
 * Date: Nov 10, 2006
 * Time: 10:00:01 AM
 */
public class PartitionManager {

    private static final Logger logger = Logger.getLogger(PartitionManager.class.getName());

    //only these files will be copied. Anything else left in SSG_ROOT/etc/conf is likley custom, like a custom assertion
    private static String[] configFileWhitelist = new String[] {
        "hibernate.properties.rpmsave",
        "hibernate.properties",
        "keystore.properties.rpmsave",
        "keystore.properties",
        "uddi.properties.rpmsave",
        "uddi.properties",
        "ssglog.properties.rpmsave",
        "ssglog.properties",
        "system.properties.rpmsave",
        "system.properties",
        "krb5.conf.rpmsave",
        "krb5.conf",
        "login.config.rpmsave",
        "login.config",
        "cluster_hostname.rpmsave",
        "cluster_hostname",
        "cluster_hostname-dist.rpmsave",
        "cluster_hostname-dist",
    };

    private static PartitionManager instance;

    private Map<String, PartitionInformation> partitions;

    private PartitionInformation activePartition;

    public static PartitionManager getInstance() {
        if (instance == null)
            instance = new PartitionManager();
        return instance;
    }

    //private constructor - must access this class through getInstance()
    private PartitionManager() {
        partitions = new HashMap<String, PartitionInformation>();
        enumeratePartitions();
    }

    private void enumeratePartitions() {
        partitions.clear();
        String partitionRoot = OSDetector.getOSSpecificFunctions().getPartitionBase();

        File partitionBaseDir = new File(partitionRoot);

        if (partitionBaseDir.exists() && partitionBaseDir.isDirectory()) {
            File[] partitionDirectories = partitionBaseDir.listFiles();
            if (partitionDirectories != null) {
                for (File partitionDir : partitionDirectories) {
                    if (partitionDir.isDirectory()) {
                        String partitionName = partitionDir.getName();
                        if (!partitionName.equals(PartitionInformation.TEMPLATE_PARTITION_NAME))
                            addPartition(partitionName);
                    }
                }
            }
        }
    }

    public void addPartition(PartitionInformation pi) {
        if (pi.isNewPartition()) {
            partitions.put(pi.getPartitionId(), pi);
        } else {
            addPartition(pi.getPartitionId());
        }
    }

    public void addPartition(String partitionId) {
        //inside partitionDir is a server.xml that we need to parse
        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions(partitionId);
        String serverXmlPath = osf.getTomcatServerConfig();
        InputStream is = null;
        try {
            Document dom = null;
            try {
                is = new FileInputStream(serverXmlPath);
                dom = XmlUtil.parse(is);
            } catch (FileNotFoundException e) {
                logger.warning("Could not find a server.xml for partition \"" + partitionId + "\". This partition " +
                        "will not be able to start until it is configured with the Configuration Wizard.");
            }

            PartitionInformation pi;

            if (dom == null) {
                pi = new PartitionInformation(partitionId);
                pi.setNewPartition(false);
                pi.setEnabled(false);
                pi.shouldDisable(); //make sure that unless something changes, this stays disabled.
                new File(osf.getPartitionBase() + partitionId, PartitionInformation.ENABLED_FILE).delete();
            } else {
                pi = new PartitionInformation(partitionId, dom, false);
                pi.setEnabled(new File(osf.getPartitionBase() + partitionId, PartitionInformation.ENABLED_FILE).exists());
            }
            partitions.put(pi.getPartitionId(), pi);
        } catch (XPathExpressionException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionId + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } catch (SAXException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionId + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } catch (IOException e) {
            logger.warning("There was an error while reading the configuration of partition \"" + partitionId + "\". This partition will not be enumerated");
            logger.warning(e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    public boolean isPartitioned() {
        return !partitions.isEmpty();
    }

    public Set<String> getPartitionNames() {
            return partitions.keySet();
        }

    public PartitionInformation getPartition(String partitionId) {
        return partitions.get(partitionId);
    }

    public PartitionInformation getActivePartition() {
        if (activePartition == null) {
            enumeratePartitions();
            //if there's no active partition then return the default one.
            activePartition = getPartition(PartitionInformation.DEFAULT_PARTITION_NAME);
        }
        return activePartition;
    }

    public void setActivePartition(PartitionInformation newActivePartition) {
        activePartition = getPartition(newActivePartition.getPartitionId());
        if (activePartition == null) {
            partitions.put(newActivePartition.getPartitionId(), newActivePartition);
            activePartition = getPartition(newActivePartition.getPartitionId());
        }
    }

    public void removePartition(String partitionName) {
        partitions.remove(partitionName);
    }

    public static void doMigration() {
        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions("");
        File oldSsgConfigDirectory = new File(osf.getSsgInstallRoot() + "etc/conf");
        File oldKeystoreDirectory = new File(osf.getSsgInstallRoot() + "etc/keys");
        File tomcatServerConfig = new File(osf.getSsgInstallRoot() + "tomcat/conf/server.xml");
        File oldTomcatServerConfig = new File(osf.getSsgInstallRoot() + "tomcat/conf/server.xml.rpmsave");
        final File partitionsBaseDir = new File(osf.getPartitionBase());
        final File defaultPartitionDir = new File(partitionsBaseDir, PartitionInformation.DEFAULT_PARTITION_NAME);
        final File templatePartitionDir = new File(partitionsBaseDir, PartitionInformation.TEMPLATE_PARTITION_NAME);

        final Set<String> whitelist = new HashSet<String>(Arrays.asList(configFileWhitelist));

        try {
            List<File> originalFiles = new ArrayList<File>();

            File[] filesInOldConfigDirectory = oldSsgConfigDirectory.listFiles(
                    new FileFilter() {
                        public boolean accept(File pathname) {
                            return whitelist.contains(pathname.getName());
                        }
                    });
            if (filesInOldConfigDirectory == null)
 	 	        filesInOldConfigDirectory = new File[0];

            List<File> deletableOriginalFiles = new ArrayList<File>(Arrays.asList(filesInOldConfigDirectory));

            deletableOriginalFiles.add(oldKeystoreDirectory);

            originalFiles.addAll(deletableOriginalFiles);
            originalFiles.add(tomcatServerConfig);
            originalFiles.add(oldTomcatServerConfig);
            if (osf.isWindows())
                originalFiles.add(new File(osf.getOriginalPartitionControlScriptName()));

            if (!partitionsBaseDir.exists()) {
                System.out.println("Creating Partition Root Directory");
                if (!partitionsBaseDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE PARTITION ROOT DIRECTORY - Please check file permissions and re-run this tool.");
                }
            }

            if (!templatePartitionDir.exists()) {
                System.out.println("Creating Template Partition Directory");
                if (!templatePartitionDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE THE PARTITION TEMPLATE DIRECTORY. New partitions cannot be created without the partition template. Please check file permissions and re-run this tool.");
                }
            }

            if (!defaultPartitionDir.exists()) {
                System.out.println("Creating Default Partition Directory");
                if (!defaultPartitionDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE THE DEFAULT PARTITION. The SSG will not run without a default partition. Please check file permissions and re-run this tool.");
                }
            }

            PartitionActions pa = new PartitionActions(osf);
            List<String> fileNames = new ArrayList<String>();
            if (templatePartitionDir.listFiles().length == 0) {
                System.out.println("Copying original configuration files to the template partition.");
                try {
                    copyConfigurations(originalFiles, templatePartitionDir);
                    for (File originalFile : originalFiles) {
                        fileNames.add(originalFile.getName());
                    }
                    pa.setLinuxFilePermissions(fileNames.toArray(new String[0]), "775", templatePartitionDir, osf);

                } catch (IOException e) {
                    System.out.println("Error while creating the template partition: " + e.getMessage());
                    System.exit(1);
                } catch (InterruptedException e) {
                    System.out.println("Error while setting the file permissions for the template partition: " + e.getMessage());
                }
            }

            if (defaultPartitionDir.listFiles().length == 0) {
                System.out.println("Copying configuration files to the default partition.");
                try {
                    if (filesInOldConfigDirectory.length <= 1) {//no originals, this is a new install, copy the ones from the partition template
                        List<File> templateFiles = new ArrayList<File>();
                        templateFiles.add(oldKeystoreDirectory);
                        templateFiles.add(oldTomcatServerConfig);
                        templateFiles.addAll(Arrays.asList(templatePartitionDir.listFiles()));

                        copyConfigurations(templateFiles, defaultPartitionDir);
                        fileNames.clear();
                        for (File templateFile : templateFiles) {
                            fileNames.add(templateFile.getName());
                        }
                        pa.setLinuxFilePermissions(fileNames.toArray(new String[0]), "775", defaultPartitionDir, osf);
                    } else {
                        copyConfigurations(originalFiles, defaultPartitionDir);
                        fileNames.clear();
                        for (File originalFile : originalFiles) {
                            fileNames.add(originalFile.getName());
                        }
                        pa.setLinuxFilePermissions(fileNames.toArray(new String[0]), "755", defaultPartitionDir, osf);
                        renameUpgradeFiles(defaultPartitionDir, ".rpmsave");
                        if (osf.isLinux()) {
                            File f = new File(osf.getPartitionBase() + "default_/" + "enabled");
                            if (!f.exists())
                                f.createNewFile();
                        }
                    }
                    fixKeystorePaths(defaultPartitionDir);
                    PartitionActions.doFirewallConfig(new PartitionInformation(PartitionInformation.DEFAULT_PARTITION_NAME));
                } catch (IOException e) {
                    System.out.println("Error while creating the default partition: " + e.getMessage());
                    System.exit(1);
                } catch (InterruptedException e) {
                    System.out.println("Error while setting the file permissions for the template partition: " + e.getMessage());
                }
            } else {
                System.out.println("the default partition does not need to be migrated");
            }
            removeOriginalConfigurations(deletableOriginalFiles);

        } catch (PartitionException pe) {
            System.out.println(pe.getMessage());
            System.exit(1);
        }
    }

    private static void fixKeystorePaths(File partitionDir) throws FileNotFoundException {
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

    private static void renameUpgradeFiles(File directory, final String pattern) {
        File[] list = directory.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(pattern);
            }
        });

        for (File file : list) {
            String oldName = file.getName();
            String newName = oldName.replace(pattern, "");
            File newFile = new File(directory, newName);
            file.renameTo(newFile);
        }
    }

    private static void copyConfigurations(List<File> filesToCopy, File destinationDir) throws IOException {
        if (!destinationDir.exists()) destinationDir.mkdir();
        for (File currentFile : filesToCopy) {
            if (currentFile.isDirectory()) {
                copyConfigurations(new ArrayList<File>(Arrays.asList(currentFile.listFiles())), new File(destinationDir, currentFile.getName()));
            } else {
                File newFile = new File(destinationDir, currentFile.getName());
                if (currentFile.exists())
                    FileUtils.copyFile(currentFile, newFile);
            }
        }
    }

    private static void removeOriginalConfigurations(List<File> filesToRemove) {
        for (File file : filesToRemove) {
            if (file.exists())
                    FileUtils.deleteFileSafely(file.getAbsolutePath());
        }
    }

    public Map<String, List<PartitionInformation.IpPortPair>> getAllPartitionPorts() {
        Map<String, List<PartitionInformation.IpPortPair>> portMap = new HashMap<String, List<PartitionInformation.IpPortPair>>();
        for (Map.Entry<String,PartitionInformation> which : partitions.entrySet()) {
            List<PartitionInformation.IpPortPair> theseEndpoints = new ArrayList<PartitionInformation.IpPortPair>();
            for (PartitionInformation.HttpEndpointHolder httpEndpointHolder : which.getValue().getHttpEndpoints()) {
                theseEndpoints.add(new PartitionInformation.IpPortPair(httpEndpointHolder));
            }
            for (PartitionInformation.OtherEndpointHolder otherEndpointHolder : which.getValue().getOtherEndpoints()) {
                theseEndpoints.add(new PartitionInformation.IpPortPair(otherEndpointHolder));
            }

            portMap.put(which.getKey(), theseEndpoints);
        }
        return portMap;
    }

    public void renamePartition(String partitionToRename, String newName) throws IOException {
        PartitionInformation originalPi = getPartition(partitionToRename);
        //if this partition exists
        if (originalPi != null) {
            String oldDirPath = OSDetector.getOSSpecificFunctions("").getPartitionBase() + partitionToRename;
            if (new File(oldDirPath).exists()) {
                PartitionActions.changeDirName(partitionToRename,  newName);
            }
            originalPi.setPartitionId(newName);
            partitions.remove(partitionToRename);
            partitions.put(newName, originalPi);
        }
    }

    public static class PartitionException extends Exception {

        public PartitionException() {
            super();
        }

        public PartitionException(String message) {
            super(message);
        }

        public PartitionException(String message, Throwable cause) {
            super(message, cause);
        }

        public PartitionException(Throwable cause) {
            super(cause);
        }
    }
}