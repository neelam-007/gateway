package com.l7tech.server.config;

import com.l7tech.common.util.*;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.KeystoreActionsException;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 24, 2006
 * Time: 12:16:38 PM
 */
@SuppressWarnings({"ALL"})
public class PartitionActions {
    private static final Logger logger = Logger.getLogger(PartitionActions.class.getName());
    private OSSpecificFunctions osFunctions;

    private static final String SERVICE_NAME_KEY = "SERVICE_NAME";
    private static final String SERVICE_DISPLAY_NAME_KEY = "PR_DISPLAYNAME";
    private static final String SERVICE_LOGPREFIX_KEY = "PR_LOGPREFIX";
    private static final String SERVICE_LOG_KEY = "PR_STDOUTPUT";
    private static final String SERVICE_ERRLOG_KEY = "PR_STDERROR";
    private static final String PARTITION_NAME_KEY = "PARTITIONNAMEPROPERTY";

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
        setUnixFilePermissions(new String[]{newPartitionDir.getAbsolutePath()}, "775", newPartitionDir, osFunctions);
        setUnixFilePermissions(new String[]{newPartitionDir.getAbsolutePath() + "/var/attachments"}, "775", newPartitionDir, osFunctions);
        setUnixFilePermissions(new String[]{newPartitionDir.getAbsolutePath() + "/var/modules"}, "775", newPartitionDir, osFunctions);
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
                if (partitionToRename.getOSSpecificFunctions().isUnix()) {
                    message = "Please ensure that the \"" + partitionToRename.getPartitionId() + "\" partition is stopped before proceeding.\n\n" +
                            "Is the partition stopped and ready to be renamed?";
                } else {
                    message = "This will require the \"" + partitionToRename.getPartitionId() + "\" partition to be stopped and renamed.\n\n" +
                            "Is it OK to stop and rename the partition now?";
                }

                confirmationGoAhead = listener.getPartitionActionsConfirmation(message);
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

    public static void updateSystemProperties(PartitionInformation pInfo, boolean logError) throws IOException {
        File systemPropertiesFile = new File(pInfo.getOSSpecificFunctions().getSsgSystemPropertiesFile());

        logger.info("Updating system properties file: " + systemPropertiesFile.getAbsolutePath());

        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            if (!systemPropertiesFile.exists()) systemPropertiesFile.createNewFile();

            fis = new FileInputStream(systemPropertiesFile);
            PropertiesConfiguration props = new PropertiesConfiguration();
            props.load(fis);
            props.setProperty(PartitionInformation.SYSTEM_PROP_PARTITIONNAME, pInfo.getPartitionId());
            String macAddress = getMacAddressForFirstInterface(pInfo);

            if (StringUtils.isNotEmpty(macAddress)) {
                props.setProperty(PartitionInformation.SYSTEM_PROP_MACADDRESS, Utilities.getFormattedMac(macAddress));
            }
            fos = new FileOutputStream(systemPropertiesFile);
            props.save(fos, "iso-8859-1");
        } catch (IOException ioe) {
            if (logError) {
                logger.warning("Error while updating system properties. [" + ioe.getMessage() + "]");                
            }
            throw ioe;
        } catch (ConfigurationException ce) {
            if (logError) {
                logger.warning("Error while updating system properties. [" + ce.getMessage() + "]");
            }
            throw new CausedIOException(ce);
        } finally {
            ResourceUtils.closeQuietly(fis);
            ResourceUtils.closeQuietly(fos);
        }
    }

    private static String getMacAddressForFirstInterface(PartitionInformation pInfo) {
        File applianceDir = new File(pInfo.getOSSpecificFunctions().getSsgInstallRoot(), "appliance");
        //we don't need to detect the mac here if this is an appliance since it will "just work" properly as is
        if (applianceDir.exists()) {
            logger.fine("This is an appliance so no need to find the mac address");
            return null;
        }

        String macAddress = null;
        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            NetworkInterface firstNic = null;
            while (nics.hasMoreElements()) {
                NetworkInterface nic =  nics.nextElement();
                if (nic.isLoopback()) continue;
                if (nic.getHardwareAddress() == null) continue;

                String thisName = nic.getDisplayName();
                if (thisName.equals("eth0")) {
                    logger.fine("This machine has an interface named eth0 so we don't need to find a MAC address");
                    return null;
                }

                if (firstNic == null) {
                    firstNic = nic;
                } else if (thisName.compareTo(firstNic.getDisplayName()) < 0) {
                    firstNic = nic;
                }
            }
            //firstNic should now contain eth0, or the interface with the alphabetically lowest name.
            logger.info("The interface that will be used to determine the MAC address is " + firstNic.getDisplayName());
            logger.info("The MAC address that will be used for cluster identification is is " + HexUtils.hexDump(firstNic.getHardwareAddress()));
            return HexUtils.hexDump(firstNic.getHardwareAddress()).toUpperCase();
        } catch (SocketException e) {
            logger.severe("There was an error while trying to enumerate the network interfacess for this machine. " + ExceptionUtils.getMessage(e));
        }
        return null;
    }

    /**
     * Fixes keystore paths to correspond to the correct partition. Also removes any passwords from the server.xml
     * found in the given partition.
     * @param partitionDir which partition dir should be changed
     * @throws FileNotFoundException
     */
    public static void fixKeystoreValues(File partitionDir, PartitionInformation piToUpdate) throws FileNotFoundException {
        File keystoreProperties = new File(partitionDir, "keystore.properties");
        FileInputStream keystoreConfigFis = null;
        FileOutputStream keystoreConfigFos = null;
        try {
            File newKeystorePath = new File(partitionDir, "keys");

            keystoreConfigFis = new FileInputStream(keystoreProperties);
            Properties props = new Properties();
            props.load(keystoreConfigFis);
            props.setProperty("keystoredir", newKeystorePath.getAbsolutePath());
            keystoreConfigFos = new FileOutputStream(keystoreProperties);
            props.store(keystoreConfigFos, "");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error updating keystore paths.", e);
        } finally {
            ResourceUtils.closeQuietly(keystoreConfigFis);
            ResourceUtils.closeQuietly(keystoreConfigFos);
        }
    }

    public static void prepareNewpartition(PartitionInformation newPartition) throws IOException, SAXException {
        if (newPartition == null) {
            return;
        }

        //edit the system.properties file so the name is correct
        try {
            fixKeystoreValues(new File(newPartition.getOSSpecificFunctions().getPartitionBase() + newPartition.getPartitionId()), newPartition);
            updateSystemProperties(newPartition, false);
        } catch (FileNotFoundException e) {
            logger.warning("Error while preparing the \"" + newPartition.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            throw e;
        } catch (IOException e) {
            logger.warning("Error while preparing the \"" + newPartition.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
            throw e;
        }

        if (newPartition.getOSSpecificFunctions().isWindows())
            prepareWindowsServiceConfig(newPartition);
    }

    private static String getServiceNameForPartition(PartitionInformation pinfo) {
        if (pinfo == null) {
            throw new IllegalArgumentException("Partition Information cannot be null");
        }
        if (StringUtils.equals(pinfo.getPartitionId(), PartitionInformation.DEFAULT_PARTITION_NAME))
            return "SSG";
        
        return pinfo.getPartitionId().replaceAll("_", "") + "SSG";
    }

    private static void writeWindowsServiceConfigFile(PartitionInformation pinfo) throws IOException {
        File configFile = new File(pinfo.getOSSpecificFunctions().getPartitionBase() + pinfo.getPartitionId(),"partition_config.cmd");
        PrintStream os = null;
        logger.info("Modifying the windows service configuration for " + pinfo.getPartitionId());
        try {
            //write out a config file that will set some variables needed by the service installer.
            os = new PrintStream(new FileOutputStream(configFile));
            os.println("set " + PARTITION_NAME_KEY + "=" + pinfo.getPartitionId());
            os.println("set " + SERVICE_NAME_KEY + "=" + getServiceNameForPartition(pinfo));
            os.println("set " + SERVICE_DISPLAY_NAME_KEY + "=" + "SecureSpan Gateway - " + pinfo.getPartitionId() + " Partition");
            os.println("set " + SERVICE_LOGPREFIX_KEY + "=" + pinfo.getPartitionId() + "_ssg_service.log");
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
                    okToProceed = listener.getPartitionActionsConfirmation(message);
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
                    okToProceed = listener.getPartitionActionsConfirmation(message);
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

                DBInformation dbInfo = new DBInformation(osf.getDatabaseConfig());

                DBActions dba = new DBActions(osf);
                boolean removeDb = listener.getPartitionActionsConfirmation(
                        "This wizard can remove the database used by this partition.\n" +
                        "This will remove all data in the database and cannot be undone.\n\n" +
                        "Are you sure you want to delete the database named \"" + dbInfo.getDbName() + "\" ?");

                if (removeDb)
                        dba.dropDatabase(dbInfo.getDbName(), dbInfo.getHostname(), dbInfo.getUsername(), dbInfo.getPassword(), true);
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
                if (parentDir == null)
                    p = Runtime.getRuntime().exec(cmdArray);
                else
                    p = Runtime.getRuntime().exec(cmdArray, null, parentDir);

                InputStream is = new BufferedInputStream(p.getInputStream());
                HexUtils.slurpStreamLocalBuffer(is);
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

    public static boolean startService(PartitionInformation partitionToStart, PartitionActionListener listener) {
        String serviceName = getServiceNameForPartition(partitionToStart);
        int retCode = 0;
        String[] cmdArray = new String[] {
                "sc",
                "start",
                serviceName,
        };
        try {
            retCode = executeCommand(cmdArray, null);
            if (listener != null) {
                if (retCode != 0) {
                    try {
                        listener.showPartitionActionErrorMessage("The \"" + serviceName + "\" service could not be started.");
                    } catch (Exception e) {}
                } else {
                    listener.showPartitionActionMessage("The \"" + serviceName + "\" service was successfully started.");
                }
            }
        } catch (IOException e) {
            if (listener != null)
                try {
                    listener.showPartitionActionErrorMessage("The \"" + serviceName + "\" service could not be started. " + e.getMessage());
                } catch (Exception e1) {}
        } catch (InterruptedException e) {
            if (listener != null)
                try {
                    listener.showPartitionActionErrorMessage("The \"" + serviceName + "\" service could not be started. " + e.getMessage());
                } catch (Exception e1) {}
        }
        return retCode == 0;
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

                    if (!StringUtils.equals(bootProto, NetworkingConfigurationBean.DYNAMIC_BOOT_PROTO)) {
                        List<String> interfaceAddresses = networkConfig.getIpAddresses();
                        for (String interfaceAddress : interfaceAddresses) {
                            allIpAddresses.add(interfaceAddress);
                        }
                    }
                }
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not determine the network interfaces for this gateway. Please run the system configuration wizard");
        }
        return new Vector<String>(allIpAddresses);
    }

    public void setUnixFilePermissions(String[] files, String permissions, File workingDir, OSSpecificFunctions osf) throws IOException, InterruptedException {
        if (!osf.isUnix())
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

        List<PartitionInformation.EndpointHolder> allHolders =
                new ArrayList<PartitionInformation.EndpointHolder>(pinfo.getEndpoints());

        for (PartitionInformation.EndpointHolder holder : allHolders) {

            Integer[] ports = holder.getPorts();
            for (Integer intPort : ports) {
                if ( intPort < 1024) {
                    holder.setValidationMessaqe("The SecureSpan Gateway cannot use ports less than 1024");
                    hadErrors = true;
                } else if (intPort > 65535) {
                    holder.setValidationMessaqe("The maximum port allowed is 65535");
                    hadErrors = true;
                }
            }

            if (!hadErrors) {
                String[] message = new String[1];
                if (holder.isEnabled()) {
                    PartitionInformation.IpPortPair pair = new PartitionInformation.IpPortPair(holder);
                    boolean foundMatch = false;

                    for (PartitionInformation.IpPortPair seenPair : seenPairs) {
                        if (pair.conflictsWith(seenPair, message)) {
                            foundMatch = true;
                            break;
                        }
                    }

                    if (!foundMatch) {
                        holder.setValidationMessaqe("");
                        seenPairs.add(pair);
                    } else {
                        holder.setValidationMessaqe(message[0] + " is already in use in this partition.");
                        hadErrors = true;
                    }
                } else {
                    holder.setValidationMessaqe("");
                }
            }
        }

        return !hadErrors;
    }

    public static boolean validateAllPartitionEndpoints(PartitionInformation currentPartition, boolean incrementEndpoints) {
        return validateAllPartitionEndpoints(currentPartition, incrementEndpoints, incrementEndpoints);
    }

    public static boolean validateAllPartitionEndpoints(PartitionInformation currentPartition, boolean incrementEndpoints, boolean includeDisabled) {
        boolean isOK = validateSinglePartitionEndpoints(currentPartition);

        if (isOK) {
            List<PartitionInformation.EndpointHolder> currentEndpoints =
                    new ArrayList<PartitionInformation.EndpointHolder>(currentPartition.getEndpoints());

            Map<String, List<PartitionInformation.IpPortPair>> portMap = PartitionManager.getInstance().getAllPartitionPorts(includeDisabled);
            //don't compare against the current partition
            portMap.remove(currentPartition.getPartitionId());

            for (PartitionInformation.EndpointHolder currentEndpoint : currentEndpoints) {
                if (currentEndpoint.isEnabled()) {
                    List<String> matches = findMatchingEndpoints(currentEndpoint, portMap);
                    if (!matches.isEmpty()) {
                        if (incrementEndpoints) {
                            incrementPartitionEndpoint(currentEndpoint, portMap);
                            isOK = true;
                        } else {
                            String message = new PartitionInformation.IpPortPair(currentEndpoint).toString() + " is used by partitions: ";
                            boolean first = true;
                            for (String match : matches) {
                                message += (first ? "" : ", ") + match;
                                first = false;
                            }
                            currentEndpoint.setValidationMessaqe(message);
                            isOK = false;
                        }
                    }
                }
            }
        }

        return isOK;
    }

    private static void incrementPartitionEndpoint(PartitionInformation.EndpointHolder currentEndpoint, Map<String, List<PartitionInformation.IpPortPair>> portMap) {
        List<Integer> allPorts = new ArrayList<Integer>(1000);
        for (String partitionName : portMap.keySet()) {
            List<PartitionInformation.IpPortPair> onePartitionsPairs = portMap.get(partitionName);
            for (PartitionInformation.IpPortPair ipPortPair : onePartitionsPairs) {
                allPorts.addAll(Arrays.asList(ipPortPair.getPorts()));
            }
        }

        currentEndpoint.avoidPorts(allPorts.toArray(new Integer[0]));
    }

    private static List<String> findMatchingEndpoints(PartitionInformation.EndpointHolder currentEndpoint,
                                                 Map<String, List<PartitionInformation.IpPortPair>> portMap) {
        List<String> matches = new ArrayList<String>();
        for (Map.Entry<String, List<PartitionInformation.IpPortPair>> partitionEntry : portMap.entrySet()) {
            List<PartitionInformation.IpPortPair> pairs = partitionEntry.getValue();
            PartitionInformation.IpPortPair currentPair = new PartitionInformation.IpPortPair(currentEndpoint);

            for (PartitionInformation.IpPortPair pair : pairs) {
                if (pair.conflictsWith(currentPair, null)) {
                    matches.add(partitionEntry.getKey());
                }
            }
        }

        return matches;
    }

    public static PartitionInformation.HttpEndpointHolder getHttpEndpointByType(PartitionInformation.HttpEndpointType type,
                                                                    List<PartitionInformation.HttpEndpointHolder> endpoints) {
        for (PartitionInformation.HttpEndpointHolder endpoint : endpoints) {
            if (endpoint.endpointType == type) return endpoint;
        }
        return null;
    }

    public static PartitionInformation.FtpEndpointHolder getFtpEndpointByType(PartitionInformation.FtpEndpointType type,
                                                                    List<PartitionInformation.FtpEndpointHolder> endpoints) {
        for (PartitionInformation.FtpEndpointHolder endpoint : endpoints) {
            if (endpoint.getEndpointType() == type) return endpoint;
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

    public static void enablePartitionForStartup(PartitionInformation pInfo){
        OSSpecificFunctions osf = pInfo.getOSSpecificFunctions();
        if (osf.isUnix()) {
            File enableStartupFile = new File(osf.getPartitionBase() + pInfo.getPartitionId(), PartitionInformation.ENABLED_FILE);
            if (pInfo.isShouldDisable()) {
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

    public static void probeForUSBFob(OSSpecificFunctions osFunctions) throws KeystoreActionsException {
        String prober = osFunctions.getSsgInstallRoot() + KeyStoreConstants.MASTERKEY_MANAGE_SCRIPT;
        try {
            ProcResult result = ProcUtils.exec(null, new File(prober), ProcUtils.args("probe"), null, true);
            if (result.getExitStatus() == 0) {
                //all is well, USB stick is there
                logger.info("Detected a supported backup storage device.");
            } else {
                String message = "A supported backup storage device was not found.";
                logger.warning(MessageFormat.format(message + " Return code = {0}, Message={1}", result.getExitStatus(), new String(result.getOutput())));
                throw new KeystoreActionsException(message);
            }
        } catch (IOException e) {
            throw new KeystoreActionsException("An error occurred while attempting to find a supported backup storage device: " + e.getMessage());
        }
    }
}