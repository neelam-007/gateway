package com.l7tech.server.partition;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.FileUtils;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import org.w3c.dom.Document;
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
    private static String[] whitelistConfigFiles = new String[] {
        "hibernate.properties",
        "keystore.properties",
        "uddi.properties",
        "ssglog.properties",
        "system.properties",
        "krb5.conf",
        "login.config",
        "cluster_hostname-dist",
        "cluster_hostname",
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

    public void addPartition(String partitionId) {
        //inside partitionDir is a server.xml that we need to parse
        String serverXmlPath = OSDetector.getOSSpecificFunctions(partitionId).getTomcatServerConfig();
        InputStream is = null;
        try {
            is = new FileInputStream(serverXmlPath);
            Document dom = XmlUtil.parse(is);
            PartitionInformation pi;
            pi = new PartitionInformation(partitionId, dom, false);
            partitions.put(pi.getPartitionId(), pi);
        } catch (FileNotFoundException e) {
            logger.warning("Could not find a server.xml for partition \"" + partitionId + "\". This partition " +
                    "will not be enumerated");
            logger.warning(e.getMessage());
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
            if (is != null) try {
                is.close();
            } catch (IOException e) {}
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

    public void doMigration() {
        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions("");
        File oldSsgConfigDirectory = new File(osf.getSsgInstallRoot() + "etc/conf");
        File oldKeystoreDirectory = new File(osf.getSsgInstallRoot() + "etc/keys");
        File oldTomcatServerConfig = new File(osf.getSsgInstallRoot() + "tomcat/conf/server.xml");
        final File partitionsBaseDir = new File(osf.getPartitionBase());
        final File defaultPartitionDir = new File(partitionsBaseDir, PartitionInformation.DEFAULT_PARTITION_NAME);
        final File templatePartitionDir = new File(partitionsBaseDir, PartitionInformation.TEMPLATE_PARTITION_NAME);

        final Set<String> whitelist = new HashSet<String>(Arrays.asList(whitelistConfigFiles));
        try {
            List<File> originalFiles = new ArrayList<File>(
                    Arrays.asList(oldSsgConfigDirectory.listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            return  whitelist.contains(pathname.getName());
                        }
                    }
            )));

            originalFiles.add(oldTomcatServerConfig);
            originalFiles.add(oldKeystoreDirectory);


            File partitionControlFile = new File(osf.getOriginalPartitionControlScriptName());
            originalFiles.add(partitionControlFile);

            if (!partitionsBaseDir.exists()) {
                System.out.println("Creating Partition Root Directory");
                if (!partitionsBaseDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE PARTITION ROOT DIRECTORY - Please check file permissions and re-run this tool.");
                }
            }
            if (!defaultPartitionDir.exists()) {
                System.out.println("Creating Default Partition Directory");
                if (!defaultPartitionDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE THE DEFAULT PARTITION. The SSG will not run without a default partition. Please check file permissions and re-run this tool.");
                }
            }
            if (!templatePartitionDir.exists()) {
                System.out.println("Creating Template Partition Directory");
                if (!templatePartitionDir.mkdir()) {
                    throw new PartitionException("COULD NOT CREATE THE PARTITION TEMPLATE DIRECTORY. New partitions cannot be created without the partition template. Please check file permissions and re-run this tool.");
                }
            }

            if (templatePartitionDir.listFiles().length == 0) {
                System.out.println("Copying original configuration files to the template partition.");
                try {
                    copyConfigurations(originalFiles, templatePartitionDir);
                } catch (IOException e) {
                    System.out.println("Error while creating the template partition: " + e.getMessage());
                    System.exit(1);
                }
            }

            if (defaultPartitionDir.listFiles().length == 0) {
                System.out.println("Copying original configuration files to the default partition.");
                try {
                    copyConfigurations(originalFiles, defaultPartitionDir);
                } catch (IOException e) {
                    System.out.println("Error while creating the default partition: " + e.getMessage());
                    System.exit(1);
                }
            } else {
                System.out.println("the default partition does not need to be migrated");
            }
            removeOriginalConfiguations(originalFiles);

        } catch (PartitionException pe) {
            System.out.println(pe.getMessage());
            System.exit(1);
        }
    }

    private void copyConfigurations(List<File> filesToCopy, File destinationDir) throws IOException {
        if (!destinationDir.exists()) destinationDir.mkdir();
        for (File currentFile : filesToCopy) {
            if (currentFile.isDirectory()) {
                copyConfigurations(new ArrayList<File>(Arrays.asList(currentFile.listFiles())), new File(destinationDir, currentFile.getName()));
            } else {
                File newFile = new File(destinationDir, currentFile.getName());
                FileUtils.copyFile(currentFile, newFile);
            }
        }
    }

    private void removeOriginalConfiguations(List<File> filesToRemove) {
        for (File file : filesToRemove) {
            if (file.exists())
                FileUtils.deleteFileSafely(file.getAbsolutePath());
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
