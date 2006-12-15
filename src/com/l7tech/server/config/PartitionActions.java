package com.l7tech.server.config;

import com.l7tech.common.util.FileUtils;
import com.l7tech.server.config.systemconfig.NetworkingConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    public PartitionActions(OSSpecificFunctions osf) {
        osFunctions = osf;
    }

    public void changeDirName(String oldPartitionId, String newPartitionId) throws IOException {
        String oldDirPath = osFunctions.getPartitionBase() + oldPartitionId;
        File oldDir = new File(oldDirPath);
        if (!oldDir.exists()) throw new FileNotFoundException("Could not find the directory: " + oldDirPath);

        String newDirPath = osFunctions.getPartitionBase() + newPartitionId;
        File newDir = new File(newDirPath);
        if (newDir.exists()) throw new IOException(
                "Cannot rename \"" + oldPartitionId + "\" to \"" + newPartitionId + "\". " + newDir.getPath() + " already exists");

        oldDir.renameTo(newDir);
    }

    public File createNewPartition(String partitionDir) throws IOException {
        String fullPath = osFunctions.getPartitionBase() + partitionDir;
        File newPartitionDir = new File(fullPath);
        if (!newPartitionDir.exists()) {
            logger.info("Creating \"" + partitionDir + "\" Directory");
            newPartitionDir.mkdir();
        }
        return newPartitionDir;
    }

    public void copyTemplateFiles(File partitionPath) throws IOException {
        if (!partitionPath.isDirectory()) {
            throw new IllegalArgumentException("Partition destination must be a directory");
        }

        File templateDir = new File(osFunctions.getPartitionBase() + PartitionInformation.TEMPLATE_PARTITION_NAME);
        if (!templateDir.exists()) {
            throw new FileNotFoundException("Could not find \"" + templateDir.getName() + "\". Cannot copy template configuration files");
        }

        copyFilesInDirectory(templateDir, partitionPath);
    }

    private void copyFilesInDirectory(File sourceDirectory, File destination) throws IOException {
        File[] templateFiles = sourceDirectory.listFiles();
        if (templateFiles != null) {
            for (File currentFile : templateFiles) {
                if (currentFile.isDirectory()) {
                    copyFilesInDirectory(currentFile, destination);
                } else {
                    File newFile = new File(destination, currentFile.getName());
                    FileUtils.copyFile(currentFile, newFile);
                }
            }
        }
    }

    public boolean removePartition(PartitionInformation partitionToRemove) {
        boolean success = false;
        if (partitionToRemove.getOSSpecificFunctions().isWindows()) {
            try {
                uninstallService(partitionToRemove.getOSSpecificFunctions());
                success = true;
            } catch (IOException e) {
                logger.warning("Could not uninstall the SSG service for the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
                success = false;
            } catch (InterruptedException e) {
                logger.warning("Could not uninstall the SSG service for the \"" + partitionToRemove.getPartitionId() + "\" partition. [" + e.getMessage() + "]");
                success = false;
            }
        }

        if (success) {
            File deleteMe = new File(osFunctions.getPartitionBase() + partitionToRemove.getPartitionId());
            success = FileUtils.deleteDir(deleteMe);
        }
        return success;
    }

    private void uninstallService(OSSpecificFunctions osSpecificFunctions) throws IOException, InterruptedException {
        String commandFile = osSpecificFunctions.getSpecificPartitionControlScriptName();
        String partitionName = osSpecificFunctions.getPartitionName();
        String[] cmdArray = new String[] {
            commandFile,
            "uninstall",
        };

        //install the service

        Process p = null;
        try {
            logger.info("Uninstalling windows service for \"" + partitionName + "\" partition.");
            File parentDir = new File(commandFile).getParentFile();
            p = Runtime.getRuntime().exec(cmdArray, null, parentDir);
            p.waitFor();
        } finally {
            if (p != null)
                p.destroy();
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

        List<String> commandLine = new ArrayList<String>();
        commandLine.add("chmod");
        commandLine.add(permissions);
        for (String file : files) {
            commandLine.add(file);
        }


        Process changer = null;
        try {
            String[] commandsArray = commandLine.toArray(new String[0]);
            changer = Runtime.getRuntime().exec(commandsArray, null, workingDir);
            changer.waitFor();
        } finally {
            if (changer != null)
                changer.destroy();
        }
    }

    public static boolean validatePartitionEndpoints(PartitionInformation pinfo) {
        boolean hadErrors = false;

        Set<String> seenPorts = new HashSet<String>();

        List<PartitionInformation.EndpointHolder> allHolders = new ArrayList<PartitionInformation.EndpointHolder>();
        allHolders.addAll(pinfo.getHttpEndpoints());
        allHolders.addAll(pinfo.getOtherEndpoints());

        for (PartitionInformation.EndpointHolder holder : allHolders) {
            int intPort;
            try {
                intPort = Integer.parseInt(holder.port);
            } catch (NumberFormatException e) {
                intPort = 0;
                holder.port = "";
            }

            if ( intPort < 1024) {
                holder.validationMessaqe = "The SecureSpan Gateway cannot use ports less than 1024";
            } else {
                if (seenPorts.add(holder.port)) {
                    holder.validationMessaqe = "";
                } else {
                    holder.validationMessaqe = "Port " + holder.port + " is already in use in this partition.";
                    hadErrors = true;
                }
            }
        }

        return !hadErrors;
    }

    public static boolean validateAllPartitionEndpoints(PartitionInformation currentPartition) {
        boolean isOK = validatePartitionEndpoints(currentPartition);

        List<PartitionInformation.EndpointHolder> currentEndpoints = new ArrayList<PartitionInformation.EndpointHolder>();
        currentEndpoints.addAll(currentPartition.getHttpEndpoints());
        currentEndpoints.addAll(currentPartition.getOtherEndpoints());

        if (isOK) {
            Map<String, List<String>> portMap = PartitionManager.getInstance().getAllPartitionPorts();
            //don't compare against the current partition
            portMap.remove(currentPartition.getPartitionId());

            for (PartitionInformation.EndpointHolder currentEndpoint : currentEndpoints) {
                for (Map.Entry<String,List<String>> partitionEntry : portMap.entrySet()) {
                    if (partitionEntry.getValue().contains(currentEndpoint.port)) {
                        currentEndpoint.validationMessaqe = "Port " + currentEndpoint.port + " is used by the \"" + partitionEntry.getKey() + "\" partition.";
                        isOK = false;
                    } else {
                        currentEndpoint.validationMessaqe = "";
                    }
                }
            }
        }

        return isOK;
    }
}