package com.l7tech.server.flasher;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.common.util.FileUtils;

import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang.StringUtils;

/**
 * The utility that imports an SSG image
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Importer {
    // importer options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to import");
    public static final CommandLineOption MODE = new CommandLineOption("-mode", "[clone | stage]");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition name to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping", "location of the staging mapping file");
    public static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name");
    public static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name");
    public static final CommandLineOption DB_PASSWD = new CommandLineOption("-dbp", "database root password");
    public static final CommandLineOption DB_USER = new CommandLineOption("-dbu", "database root username");

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MODE, PARTITION, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER};

    private String tempDirectory;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        String inputpathval = arguments.get(IMAGE_PATH.name);
        if (inputpathval == null) {
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i dont know what to import");
        }
        boolean fullClone = false;
        String mode = arguments.get(MODE.name);
        if (mode == null) {
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + MODE.name);
        }
        if (mode.toLowerCase().equals("clone")) {
            fullClone = true;
        } else if (mode.toLowerCase().equals("stage")) {
            fullClone = false;
        } else {
            throw new FlashUtilityLauncher.InvalidArgumentException("invalid value for " + MODE.name);
        }
        // clone only available on linux. check that that we're on linux if mode is clone
        if (fullClone && OSDetector.isWindows()) {
            throw new FlashUtilityLauncher.InvalidArgumentException("invalid value for " + MODE.name);
        }
        // uncompress image to temp folder, look for Exporter.DBDUMP_FILENAME
        tempDirectory = Exporter.createTmpDirectory();
        try {
            unzipToDir(inputpathval, tempDirectory);
            if (!(new File(tempDirectory + File.separator + Exporter.DBDUMP_FILENAME)).exists()) {
                throw new FlashUtilityLauncher.InvalidArgumentException("the file " + inputpathval +
                                                                        " does not appear to be a valid SSG flash image");
            }
            // if clone is asked, make sure the image was produced from a linux system
            if (fullClone) {
                if (!(new File(tempDirectory + File.separator + "hibernate.properties")).exists()) {
                    throw new FlashUtilityLauncher.InvalidArgumentException("this image cannot be used in clone mode. " +
                                                                            "perhaps it was created on a windows system");
                }
            }
            // get target partition, make sure it already exists
            String partitionName = arguments.get(PARTITION.name);
            // check if the system has more than one partition on it
            PartitionManager partitionManager = PartitionManager.getInstance();
            boolean multiplePartitionSystem = partitionManager.isPartitioned();
            if (multiplePartitionSystem) {
                // option PARTITION now mandatory
                if (StringUtils.isEmpty(partitionName))
                    throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned. The \"" + PARTITION.name + "\" parameter is required");
                PartitionInformation partitionInfo = partitionManager.getPartition(partitionName);
                if (partitionInfo == null)
                    throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned but the partition \"" + partitionName + "\" is not present.");
            } else {
                // make sure user did not ask for a partition that did not exist
                if (StringUtils.isNotEmpty(partitionName))
                    throw new FlashUtilityLauncher.InvalidArgumentException("this system is not partitioned. cannot act on partition number " + partitionName);
            }
            if (partitionName == null) partitionName = "";
            // check that target db is not currently used by an SSG
            
            // todo
            // actually go on with the import
            // todo
        } finally {
            FileUtils.deleteDir(new File(tempDirectory));
        }
    }

    public void unzipToDir(String filename, String destinationpath) throws IOException {
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream = null;
        ZipEntry zipentry;
        zipinputstream = new ZipInputStream(new FileInputStream(filename));
        zipentry = zipinputstream.getNextEntry();
        while (zipentry != null) {
            // for each entry to be extracted
            String entryName = zipentry.getName();
            if (zipentry.isDirectory()) {
                (new File(destinationpath + File.separator + entryName)).mkdir();
            } else {
                System.out.println("unzipping " + entryName);
                FileOutputStream fileoutputstream;
                File newFile = new File(entryName);
                String directory = newFile.getParent();
                if (directory == null) {
                    if (newFile.isDirectory()) break;
                }
                fileoutputstream = new FileOutputStream(destinationpath + File.separator + entryName);
                int n;
                while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                    fileoutputstream.write(buf, 0, n);
                }
                fileoutputstream.close();
                zipinputstream.closeEntry();
            }
            zipentry = zipinputstream.getNextEntry();
        }
        zipinputstream.close();
    }
}
