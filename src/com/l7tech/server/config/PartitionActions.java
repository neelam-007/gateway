package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.common.util.FileUtils;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
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
        File newPartitionDir = new File(partitionDir);
        if (newPartitionDir.exists()) {
            logger.info("The directory \"" + partitionDir + "\" already exists");
        } else {
            logger.info("Creating directory \"" + partitionDir + "\"");
            newPartitionDir .mkdir();
        }
        return newPartitionDir;
    }

    public void copyTemplateFiles(File partitionPath) throws IOException {
        File outputDirectory = partitionPath;
        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException("Partition destination must be a directory");
        }

        File templateDir = new File(osFunctions.getPartitionBase() + PartitionInformation.TEMPLATE_PARTITION_NAME);
        if (!templateDir.exists()) {
            throw new FileNotFoundException("Could not find \"" + templateDir.getName() + "\". Cannot copy template configuration files");
        }

        copyFilesInDirectory(templateDir, outputDirectory);
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

    public boolean removePartitionDirectory(PartitionInformation partitionToRemove) {
        File deleteMe = new File(osFunctions.getPartitionBase() + partitionToRemove.getPartitionId());
        return FileUtils.deleteDir(deleteMe);
    }
}
