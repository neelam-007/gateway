package com.l7tech.server.flasher;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.BuildInfo;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
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
    private static final Logger logger = Logger.getLogger(Exporter.class.getName());
    // exporter options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to export");
    public static final CommandLineOption AUDIT = new CommandLineOption("-ia", "to include audit tables in resulting image");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition name to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it", "path of the output mapping template");
    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, PARTITION, MAPPING_PATH};
    public static final String VERSIONFILENAME = "version";

    private boolean includeAudit = false;
    private String tmpDirectory;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        // check that we can write output at located asked for
        String outputpathval = arguments.get(IMAGE_PATH.name);
        if (outputpathval == null) {
            logger.info("no target image path specified");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i dont know where to output the image to.");
        }
        if (!testCanWrite(outputpathval)) {
            logger.warning("cannot write to the target image path specified: " + outputpathval);
            throw new IOException("cannot write to " + outputpathval);
        }

        // check whether or not we are expected to include audit in export
        String auditval = arguments.get(AUDIT.name);
        if (auditval != null && !auditval.toLowerCase().equals("no") && !auditval.toLowerCase().equals("false")) {
            includeAudit = true;
        }

        //parititons have names like "dev", "prod" etc. so we'll use that instead of integers.
        String partitionName = arguments.get(PARTITION.name);

        // check if the system has more than one partition on it
        PartitionManager partitionManager = PartitionManager.getInstance();
        boolean multiplePartitionSystem = partitionManager.isPartitioned();

        if (multiplePartitionSystem) {
            // option PARTITION now mandatory
            if (StringUtils.isEmpty(partitionName)) {
                logger.info("no partition name specified");
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned. The \"" + PARTITION.name + "\" parameter is required");
            }

            PartitionInformation partitionInfo = partitionManager.getPartition(partitionName);
            if (partitionInfo == null) {
                logger.info("the partition to export does not exist: " + partitionName);
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned but the partition \"" + partitionName + "\" is not present.");
            }

        } else {
            // make sure user did not ask for a partition that did not exist
            if (StringUtils.isNotEmpty(partitionName)) {
                logger.info("there are no partition on this system, yet the user asked to export partition named: " + partitionName);
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is not partitioned. cannot act on partition name " + partitionName);
            }
        }
        if (partitionName == null) partitionName = "";
        tmpDirectory = createTmpDirectory();
        try {
            logger.info("created temporary directory at " + tmpDirectory);

            // Read database connection settings for the partition at hand
            OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partitionName);
            Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[]{
                    SsgDatabaseConfigBean.PROP_DB_USERNAME,
                    SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                    SsgDatabaseConfigBean.PROP_DB_URL,
            });
            String databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
            String databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
            String databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
            logger.info("using database url " + databaseURL);
            logger.info("using database user " + databaseUser);
            logger.info("using database passwd " + databasePasswd);

            // dump the database
            try {
                DBDumpUtil.dump(databaseURL, databaseUser, databasePasswd, includeAudit, tmpDirectory);
            } catch (SQLException e) {
                logger.log(Level.INFO, "exception dumping database", e);
                throw new IOException("cannot dump the database " + e.getMessage());
            } catch (ClassNotFoundException e) {
                logger.log(Level.INFO, "database driver unavailable", e);
                throw new IOException("cannot dump the database " + e.getMessage());
            }

            // record version of this image
            FileOutputStream fos = new FileOutputStream(tmpDirectory + File.separator + VERSIONFILENAME);
            fos.write(BuildInfo.getProductVersion().getBytes());
            fos.close();

            // produce template mapping if necessary
            partitionName = arguments.get(MAPPING_PATH.name);
            if (partitionName != null) {
                if (!testCanWrite(partitionName)) {
                    throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to the mapping template path provided: " + partitionName);
                }
                // read policy files from this dump, collect all potential mapping in order to produce mapping template file
                try {
                    MappingUtil.produceTemplateMappingFileFromDB(databaseURL, databaseUser, databasePasswd, partitionName);
                } catch (SQLException e) {
                    // should not happen
                    logger.log(Level.WARNING, "unexpected problem producing template mapping file ", e);
                    throw new RuntimeException("problem producing template mapping file", e);
                } catch (SAXException e) {
                    // should not happen
                    logger.log(Level.WARNING, "unexpected problem producing template mapping file ", e);
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
                // temp patch to address the half done partition work
                if (!tomcatprops.exists()) {
                    logger.warning("tomcat properties are not where expected. this could be caused by an imcomplete partition migration");
                    tomcatprops = new File("/ssg/tomcat/conf/server.xml");
                    if (!tomcatprops.exists()) {
                        throw new IOException("tomcat properties cannot be found anywhere");
                    }
                }
                File sysProps = new File(osFunctions.getSsgSystemPropertiesFile());
                String ksdir = osFunctions.getKeystoreDir();
                if (!(new File(ksdir)).exists()) {
                    logger.warning("keystore directory not found where expected. this could be caused by an imcomplete partition migration");
                    ksdir = "/ssg/etc/keys";
                    if (!(new File(ksdir)).exists()) {
                        throw new IOException("keystore directory cannot be found anywhere");
                    }
                }
                File caCer = new File(ksdir + File.separator + "ca.cer");
                File sslCer = new File(ksdir + File.separator + "ssl.cer");
                File caKS = new File(ksdir + File.separator + "ca.ks");
                File sslKS = new File(ksdir + File.separator + "ssl.ks");
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
                FileUtils.copyFile(caCer, new File(tmpDirectory + File.separator + caCer.getName()));
                FileUtils.copyFile(sslCer, new File(tmpDirectory + File.separator + sslCer.getName()));
                FileUtils.copyFile(caKS, new File(tmpDirectory + File.separator + caKS.getName()));
                FileUtils.copyFile(sslKS, new File(tmpDirectory + File.separator + sslKS.getName()));

                // copy system config files
                OSConfigManager.saveOSConfigFiles(tmpDirectory);
            }
            // zip the temp directory into the requested image file (outputpathval)
            logger.info("compressing image into " + outputpathval);
            zipDir(outputpathval, tmpDirectory);
        } finally {
            logger.info("cleaning up temp files at " + tmpDirectory);
            System.out.println("Cleaning temporary files at " + tmpDirectory);
            FileUtils.deleteDir(new File(tmpDirectory));
        }
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
        logger.info("tested write permission for " + path);
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
        System.out.println("Compressing SecureSpan Gateway image into " + zipFileName);
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
            System.out.println("\t- " + file.getAbsolutePath());
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
