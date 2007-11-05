package com.l7tech.server.partition;

import com.l7tech.common.util.FileUtils;
import com.l7tech.server.config.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
        "cluster_hostname.rpmsave",
        "cluster_hostname",
        "cluster_hostname-dist.rpmsave",
        "cluster_hostname-dist",
        "kerberos.keytab",
        "kerberos.keytab.rpmsave"
    };

    private static PartitionManager instance;

    private Map<String, PartitionInformation> partitions;

    private PartitionInformation activePartition;

    public static PartitionManager getInstance() {
        if (instance == null) {
            instance = new PartitionManager();
            instance.enumeratePartitions();
        }
        return instance;
    }

    //private constructor - must access this class through getInstance()
    private PartitionManager() {
        partitions = new HashMap<String, PartitionInformation>();
    }

    public void enumeratePartitions() {
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
        OSSpecificFunctions osf = OSDetector.getOSSpecificFunctions(partitionId);
        PartitionInformation pi;

        pi = new PartitionInformation(partitionId);
        pi.setNewPartition(false);
        pi.setEnabled(false);
        pi.shouldDisable(); //make sure that unless something changes, this stays disabled.
        new File(osf.getPartitionBase() + partitionId, PartitionInformation.ENABLED_FILE).delete();
        partitions.put(pi.getPartitionId(), pi);
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
            if (templatePartitionDir.listFiles().length <= 1) {
                System.out.println("Copying original configuration files to the template partition.");
                try {
                    copyConfigurations(originalFiles, templatePartitionDir);
                    for (File originalFile : originalFiles) {
                        fileNames.add(originalFile.getName());
                    }
                    pa.setUnixFilePermissions(fileNames.toArray(new String[fileNames.size()]), "775", templatePartitionDir, osf);

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
                    // copy template configuration
                    List<File> templateFiles = new ArrayList<File>();
                    templateFiles.add(oldKeystoreDirectory);
                    templateFiles.add(oldTomcatServerConfig);
                    templateFiles.addAll(Arrays.asList(templatePartitionDir.listFiles()));

                    copyConfigurations(templateFiles, defaultPartitionDir);
                    if (filesInOldConfigDirectory.length <= 1) {//no originals, this is a new install, copy the ones from the partition template
                        fileNames.clear();
                        for (File templateFile : templateFiles) {
                            fileNames.add(templateFile.getName());
                        }
                        pa.setUnixFilePermissions(fileNames.toArray(new String[fileNames.size()]), "775", defaultPartitionDir, osf);
                    } else {
                        // copy old config, backup any existing files with the given extension.
                        copyConfigurations(originalFiles, defaultPartitionDir, osf.getUpgradedNewFileExtension());
                        fileNames.clear();
                        for (File originalFile : originalFiles) {
                            fileNames.add(originalFile.getName());
                        }
                        pa.setUnixFilePermissions(fileNames.toArray(new String[fileNames.size()]), "775", defaultPartitionDir, osf);
                        renameUpgradeFiles(defaultPartitionDir, osf.getUpgradedOldFileExtension());
                        if (osf.isUnix()) {
                            File f = new File(osf.getPartitionBase() + "default_/" + "enabled");
                            if (!f.exists())
                                f.createNewFile();
                        }
                    }
                    String s = defaultPartitionDir.getAbsolutePath() + "/var/attachments";
                    pa.setUnixFilePermissions(new String[]{s}, "775", defaultPartitionDir, osf);
                    s = defaultPartitionDir.getAbsolutePath() + "/var/modules";
                    pa.setUnixFilePermissions(new String[]{s}, "775", defaultPartitionDir, osf);

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
            copyNewTemplateFilesToPartitions(templatePartitionDir, partitionsBaseDir);
            updatePartitionConfigsWithNewChanges(templatePartitionDir, partitionsBaseDir);

        } catch (PartitionException pe) {
            System.out.println(pe.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error while copying new files into the destination partitions");
            System.out.println(e.getMessage());
            System.exit(1);
        } catch (SAXException e) {
            System.out.println("Error while modifying new files in the partitions");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void updatePartitionConfigsWithNewChanges(File templatePartitionDir, File partitionsBaseDir) throws IOException, SAXException {
        File[] templateFiles = templatePartitionDir.listFiles();

        File[] listOfPartitions = partitionsBaseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().equalsIgnoreCase(PartitionInformation.TEMPLATE_PARTITION_NAME);
            }
        });

        for (File destinationPartition: listOfPartitions) {
            PasswordPropertyCrypto passwordEncryptor =
                    OSDetector.getOSSpecificFunctions(destinationPartition.getName()).getPasswordPropertyCrypto(); 
            updateConfigInPartition(templateFiles, destinationPartition, passwordEncryptor);
        }
    }

    private static void updateConfigInPartition(File[] templateFiles, File destinationPartition, PasswordPropertyCrypto passwordEncryptor)
            throws IOException, SAXException
    {
        for (File templateFile : templateFiles) {
            File partitionFile = new File(destinationPartition, templateFile.getName());
            if (templateFile.isDirectory()) {
                File[] templateSubDirFiles = templateFile.listFiles();
                if (!partitionFile.exists()) partitionFile.mkdir();
                updateConfigInPartition(templateSubDirFiles, partitionFile, passwordEncryptor);
            } else {
                if (templateFile.getName().endsWith("properties")) {
                    PropertyHelper.mergePropertiesInPlace(partitionFile, templateFile, true, passwordEncryptor);
                }
            }
        }
    }

    private static void copyNewTemplateFilesToPartitions(final File templatePartitionDir, final File partitionsBaseDir) throws IOException {
        //TODO make sure this doesn't clobber anything that's already there.
        File[] listOfPartitions = partitionsBaseDir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory() && !pathname.getName().equalsIgnoreCase(PartitionInformation.TEMPLATE_PARTITION_NAME);
            }
        });

        File[] files = templatePartitionDir.listFiles();
        List<File> sourceFiles = Arrays.asList(files);
        for (File destinationPartition: listOfPartitions) {
            copyConfigurations(sourceFiles, destinationPartition);
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
        copyConfigurations(filesToCopy, destinationDir, null);       
    }

    private static void copyConfigurations(List<File> filesToCopy, File destinationDir, String backupExtn) throws IOException {
        if (!destinationDir.exists()) {
            if(!destinationDir.mkdir())
                System.out.println("Error while creating directory '"+destinationDir.getAbsolutePath()+"'.");
        }        

        for (File currentFile : filesToCopy) {
            if (currentFile.isDirectory()) {
                copyConfigurations(
                        new ArrayList<File>(Arrays.asList(currentFile.listFiles())), 
                        new File(destinationDir, currentFile.getName()),
                        backupExtn);
            } else {
                File newFile = new File(destinationDir, currentFile.getName());

                // backup existing file if required
                if (newFile.exists()) {
                    if (backupExtn != null) {
                        File targetFile = new File(destinationDir, currentFile.getName());
                        File backupFile = new File(destinationDir, currentFile.getName() + "." + backupExtn);
                        if (!targetFile.renameTo(backupFile))
                            System.out.println("Could not create backup for file '"+newFile.getAbsolutePath()+"'.");
                    } else {
                        continue;
                    }
                }

                if (currentFile.exists())
                    FileUtils.copyFile(currentFile, newFile);
            }
        }
    }

    private static void removeOriginalConfigurations(List<File> filesToRemove) {
        for (File file : filesToRemove) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    FileUtils.deleteDir(file);
                } else {
                    FileUtils.deleteFileSafely(file.getAbsolutePath());
                }
            }
        }
    }

    public Map<String, List<PartitionInformation.IpPortPair>> getAllPartitionPorts(boolean includeDisabled) {
        Map<String, List<PartitionInformation.IpPortPair>> portMap = new HashMap<String, List<PartitionInformation.IpPortPair>>();
        for (Map.Entry<String,PartitionInformation> which : partitions.entrySet()) {
            List<PartitionInformation.IpPortPair> theseEndpoints = new ArrayList<PartitionInformation.IpPortPair>();

            for (PartitionInformation.EndpointHolder endpointHolder : which.getValue().getEndpoints()) {
                if (includeDisabled || endpointHolder.isEnabled())
                    theseEndpoints.add(new PartitionInformation.IpPortPair(endpointHolder));
            }

            portMap.put(which.getKey(), theseEndpoints);
        }
        return portMap;
    }

    public void renamePartition(String partitionToRename, String newName) throws IOException {
        PartitionInformation originalPi = getPartition(partitionToRename);
        //if this partition exists
        if (originalPi != null) {
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
