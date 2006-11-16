package com.l7tech.server.flasher;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.common.util.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * The utility that exports an SSG image file.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Exporter {
    // exporter options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to export");
    public static final CommandLineOption AUDIT = new CommandLineOption("-ia", "[yes | no] whether or not to include audit tables in resulting image");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition name to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it", "path of the output mapping template");
    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, PARTITION, MAPPING_PATH};

    private boolean includeAudit = false;
    private String tmpDirectory;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        // check that we can write output at located asked for
        String outputpathval = arguments.get(IMAGE_PATH.name);
        if (outputpathval == null) {
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i dont know where to output the image to.");
        }
        if (!testCanWrite(outputpathval)) {
            throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to " + outputpathval);
        }

        // check whether or not we are expected to include audit in export
        String auditval = arguments.get(AUDIT.name);
        if (auditval != null && auditval.toLowerCase().equals("yes")) {
            includeAudit = true;
        }

        //parititons have names like "dev", "prod" etc. so we'll use that instead of integers.
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
        tmpDirectory = createTmpDirectory();

        // Read database connection settings for the partition at hand
        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partitionName);
        Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
            SsgDatabaseConfigBean.PROP_DB_USERNAME,
            SsgDatabaseConfigBean.PROP_DB_PASSWORD,
            SsgDatabaseConfigBean.PROP_DB_URL,
        });
        String databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
        String databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
        String databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);

        // dump the database
        try {
            DBDumpUtil.dump(databaseURL, databaseUser, databasePasswd, includeAudit, tmpDirectory);
        } catch (SQLException e) {
            throw new IOException("cannot dump the database " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new IOException("cannot dump the database " + e.getMessage());
        }

        // produce template mapping if necessary
        partitionName = arguments.get(MAPPING_PATH.name);
        if (partitionName != null) {
            if (!testCanWrite(partitionName)) {
                throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to the mapping template path provided: " + partitionName);
            }
            // read policy files from this dump, collect all potential mapping in order to produce mapping template file
            try {
                MappingUtil.produceTemplateMappingFileFromDatabaseConnection(databaseURL, databaseUser, databasePasswd, partitionName);
            } catch (SQLException e) {
                // should not happen
                throw new RuntimeException("problem producing template mapping file", e);
            } catch (SAXException e) {
                // should not happen
                throw new RuntimeException("problem producing template mapping file", e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("problem producing template mapping file", e);
            }
        }
        // we dont support full image on windows (windows only supports staging use case)
        if (!OSDetector.isWindows()) {
            // copy all config files we want into this temp directory
            File hibprops = new File(osFunctions.getDatabaseConfig());
            File clusterprops = new File(osFunctions.getClusterHostFile());
            File ssglogprops = new File(osFunctions.getSsgLogPropertiesFile());
            File ksprops = new File(osFunctions.getKeyStorePropertiesFile());
            File tomcatprops = new File(osFunctions.getTomcatServerConfig());
            File sysProps = new File(osFunctions.getSsgSystemPropertiesFile());
            // todo, what else? what about actual keystores?
            FileUtils.copyFile(hibprops, new File(tmpDirectory + File.separator + hibprops.getName()));
            if (clusterprops.exists()) {
                FileUtils.copyFile(clusterprops, new File(tmpDirectory + File.separator + clusterprops.getName()));
            }
            if (sysProps.exists()) {
                FileUtils.copyFile(sysProps, new File(tmpDirectory + File.separator + sysProps.getName()));
            }
            FileUtils.copyFile(ssglogprops, new File(tmpDirectory + File.separator + ssglogprops.getName()));
            FileUtils.copyFile(ksprops, new File(tmpDirectory + File.separator + ksprops.getName()));
            FileUtils.copyFile(tomcatprops, new File(tmpDirectory + File.separator + tomcatprops.getName()));
        }

        // zip the temp directory into the requested image file (outputpathval)
        zipDir(outputpathval, tmpDirectory);
        FileUtils.deleteDir(new File(tmpDirectory));
    }

    private boolean testCanWrite(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.close();
        } catch (FileNotFoundException e) {
            System.err.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        }
        (new File(path)).delete();
        return true;
    }

    public static String createTmpDirectory() throws IOException {
        File tmp = File.createTempFile("ssgflash", "tmp");
        tmp.delete();
        tmp.mkdir();
        return tmp.getPath();
    }

    private void zipDir(String zipFileName, String dir) throws IOException {
        File dirObj = new File(dir);
        if (!dirObj.isDirectory()) {
            throw new IOException(dir + " is not a directory");
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        System.out.println("Creating : " + zipFileName);
        addDir(dirObj, out);
        out.close();
    }

    private void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (File file : files) {
            if (file.isDirectory()) {
                addDir(file, out);
                continue;
            }
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            System.out.println(" Adding: " + file.getAbsolutePath());
            String zipEntryName = file.getAbsolutePath();
            if (zipEntryName.startsWith(tmpDirectory)) {
                zipEntryName = zipEntryName.substring(tmpDirectory.length() + 1);
            }
            out.putNextEntry(new ZipEntry(zipEntryName));
            // Transfer from the file to the ZIP file
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
    }    
}
