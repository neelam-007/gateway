package com.l7tech.server.flasher;

import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.BuildInfo;

import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.io.*;
import java.sql.*;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

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
    private String partitionName;
    private boolean fullClone = false;
    private String databaseURL;
    private String databaseUser;
    private String databasePasswd;
    private DBActions dbActions;
    private MappingUtil.CorrespondanceMap mapping = null;
    private String licenseValueBeforeImport = null;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        String inputpathval = arguments.get(IMAGE_PATH.name);
        if (inputpathval == null) {
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i don't know what to import");
        }
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
            System.out.println("Reading SecureSpan image file");
            unzipToDir(inputpathval, tempDirectory);
            if (!(new File(tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_STAGING)).exists()) {
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

            // compare version of the image with version of the target system
            FileInputStream fis =  new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
            byte[] buf = new byte[512];
            int read = fis.read(buf);
            String imgversion = new String(buf, 0, read);
            if (!BuildInfo.getProductVersion().equals(imgversion)) {
                throw new FlashUtilityLauncher.InvalidArgumentException("the version of this image is incompatible " +
                                                                        "with this target system (" + imgversion +
                                                                        " instead of " + BuildInfo.getProductVersion() +
                                                                        ")");
            }

            System.out.println("SecureSpan image file recognized.");
            // get target partition, make sure it already exists
            partitionName = arguments.get(PARTITION.name);
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

            OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partitionName);
            Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                SsgDatabaseConfigBean.PROP_DB_USERNAME,
                SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                SsgDatabaseConfigBean.PROP_DB_URL,
            });
            databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
            databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
            databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);

            try {
                String connectedNode = checkSSGConnectedToDatabase();
                if (StringUtils.isNotEmpty(connectedNode)) {
                    throw new IOException("A SecureSpan Gateway is currently running " +
                                          "and connected to the database. Please shutdown " + connectedNode);
                }
            } catch (SQLException e) {
                throw new IOException("cannot connect to database" + e.getMessage());
            }  catch (InterruptedException e) {
                throw new IOException("interrupted!" + e.getMessage());
            }

            // load mapping if requested
            String mappingPath = arguments.get(MAPPING_PATH.name);
            if (mappingPath != null) {
                System.out.print("Reading mapping file ..");
                try {
                    mapping = MappingUtil.loadMapping(mappingPath);
                } catch (SAXException e) {
                    throw new IOException("Problem loading " + MAPPING_PATH.name + ". Invalid Format. " + e.getMessage());
                }
                System.out.println(". DONE");
            }

            // actually go on with the import
            try {
                saveExistingLicense();
            } catch (SQLException e) {
                throw new IOException("cannot save existing license from database " + e.getMessage());
            }
            // load database dump
            try {
                loadDumpFromExplodedImage();
            } catch (SQLException e) {
                throw new IOException("error loading db dump " + e.getMessage());
            }

            // if applicable, copy all config files to the right place
            if (fullClone) {
                copySystemConfigFiles();
            }

            // apply mapping if applicable
            if (mapping != null) {
                try {
                    MappingUtil.applyMappingChangesToDB(databaseURL, databaseUser, databasePasswd, mapping);
                } catch (SQLException e) {
                    throw new IOException("error mapping staging values " + e.getMessage());
                }
            }

            // reload license if applicable
            try {
                reloadLicense();
            } catch (SQLException e) {
                throw new IOException("error resetting license " + e.getMessage());
            }

        } finally {
            FileUtils.deleteDir(new File(tempDirectory));
        }
    }

    private void reloadLicense() throws IOException, SQLException {
        if (licenseValueBeforeImport != null) {
            System.out.print("Restoring license ..");
            // get the id to use
            byte[] buf = new byte[64];
            FileInputStream fis = new FileInputStream(tempDirectory + File.separator + DBDumpUtil.LICENCEORIGINALID);
            int read = fis.read(buf);
            fis.close();
            long licenseObjectId = Long.parseLong(new String(buf, 0, read));
            Connection c = getConnection();
            try {
                PreparedStatement ps = c.prepareStatement("insert into cluster_properties values (?, 1, \'license\', ?)");
                ps.setLong(1, licenseObjectId);
                ps.setString(2, licenseValueBeforeImport);
                ps.executeUpdate();
                ps.close();
                System.out.println(". DONE");
            } finally {
                c.close();
            }
        }
    }

    private void loadDumpFromExplodedImage() throws IOException, SQLException {
        System.out.print("Loading Database [please wait] ..");
        String dumpFilePath;
        if (fullClone) {
            dumpFilePath = tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_CLONE;
        } else {
            dumpFilePath = tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_STAGING;
        }
        BufferedReader reader = new BufferedReader(new FileReader(dumpFilePath));
        String tmp;
        Connection c = getConnection();
        try {
            c.setAutoCommit(false);
            Statement stmt = c.createStatement();
            try {
                while((tmp = reader.readLine()) != null) {
                    if (tmp.endsWith(";")) {
                        stmt.executeUpdate(tmp.substring(0, tmp.length()-1));
                    } else {
                        throw new SQLException("unexpected statement " + tmp);
                    }
                }
                c.commit();
            } finally {
                stmt.close();
                c.setAutoCommit(true);
            }
        } finally {
            c.close();
        }
        System.out.println(". DONE");
    }

    private void copySystemConfigFiles() throws IOException {
        System.out.print("Cloning SecureSpan Gateway settings ..");

        // todo, solve the target database not present problem
        // look into image's version of hibernate.properties. make sure the username, passwd and url match
        /*
        Map<String, String> dbProps = PropertyHelper.getProperties(tempDirectory + File.separator + "hibernate.properties", new String[] {
                                                                   SsgDatabaseConfigBean.PROP_DB_USERNAME,
                                                                   SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                                                                   SsgDatabaseConfigBean.PROP_DB_URL,
                                                                   });
        */

        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partitionName);
        // copy hibernate.properties
        restoreConfigFile(osFunctions.getDatabaseConfig());
        // copy cluster host name file
        restoreConfigFile(osFunctions.getClusterHostFile());
        // copy logging properties
        restoreConfigFile(osFunctions.getSsgLogPropertiesFile());
        // copy keystore properties file
        restoreConfigFile(osFunctions.getKeyStorePropertiesFile());
        // copy tomcat server settings
        restoreConfigFile(osFunctions.getTomcatServerConfig());
        // copy system properties
        restoreConfigFile(osFunctions.getSsgSystemPropertiesFile());
        // copy keystores and certs
        //String ksdir = osFunctions.getKeystoreDir();
        String ksdir = "/ssg/etc/keys"; // todo, go back to above once partitioning is fully implemented
        restoreConfigFile(ksdir + File.separator + "ca.cer");
        restoreConfigFile(ksdir + File.separator + "ssl.cer");
        restoreConfigFile(ksdir + File.separator + "ca.ks");
        restoreConfigFile(ksdir + File.separator + "ssl.ks");
        System.out.println(". DONE");
    }

    private void restoreConfigFile(String destination) throws IOException {
        File toFile = new File(destination);
        File fromFile = new File(tempDirectory + File.separator + toFile.getName());
        if (fromFile.exists()) {
            toFile.delete();
            FileUtils.copyFile(fromFile, toFile);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection c;
        try {
            c = getDBActions().getConnection(databaseURL, databaseUser, databasePasswd);
        } catch (ClassNotFoundException e) {
            throw new SQLException("cannot get driver " + e.getMessage());
        }
        if (c == null) {
            throw new SQLException("could not connect using url: " + databaseURL +
                                   ". with username " + databaseUser +
                                   ", and password: " + databasePasswd);
        }
        return c;
    }

    private void saveExistingLicense() throws SQLException {
        Connection c = getConnection();
        try {
            Statement selectlicense = c.createStatement();
            ResultSet maybeLicenseRS = selectlicense.executeQuery("select propvalue from cluster_properties where propkey=\'license\'");
            try {
                while (maybeLicenseRS.next()) {
                    String maybeLicense = maybeLicenseRS.getString(1);
                    if (StringUtils.isNotEmpty(maybeLicense)) {
                        licenseValueBeforeImport = maybeLicense;
                        break;
                    }
                }
            } finally {
                maybeLicenseRS.close();
                selectlicense.close();
            }
        } finally {
            c.close();
        }
    }

    /**
     * @return name of ssg node connected to database, null if nothing appears to be connected
     */
    private String checkSSGConnectedToDatabase() throws SQLException, InterruptedException {
        System.out.print("Making sure target is offline .");
        Connection c = getConnection();
        try {
            Statement checkStatusStatement = c.createStatement();
            try {
                ResultSet statusTimeStampList = checkStatusStatement.executeQuery("select statustimestamp, name from cluster_info");
                long longest = 0;
                try {
                    while (statusTimeStampList.next()) {
                        long tmp = statusTimeStampList.getLong(1);
                        if (tmp > longest) {
                            longest = tmp;
                        }
                    }
                } finally {
                    statusTimeStampList.close();
                }
                // we're looping until we've gone beyond the status update frequency while stopping early if
                // we find an update beforehand
                for (int i = 0; i < 11; i++) {
                    Thread.sleep(500);
                    statusTimeStampList = checkStatusStatement.executeQuery("select statustimestamp, name from cluster_info");
                    try {
                        while (statusTimeStampList.next()) {
                            long tmp = statusTimeStampList.getLong(1);
                            if (tmp > longest) {
                                System.out.print(" ");
                                return statusTimeStampList.getString(2);
                            }
                        }
                    } finally {
                        statusTimeStampList.close();
                    }
                    System.out.print(".");
                }
                System.out.println(" DONE");
            } finally {
                checkStatusStatement.close();
            }
        } finally {
            c.close();
        }
        return null;
    }

    public void unzipToDir(String filename, String destinationpath) throws IOException {
        byte[] buf = new byte[1024];
        ZipInputStream zipinputstream;
        ZipEntry zipentry;
        zipinputstream = new ZipInputStream(new FileInputStream(filename));
        zipentry = zipinputstream.getNextEntry();
        while (zipentry != null) {
            // for each entry to be extracted
            String entryName = zipentry.getName();
            if (zipentry.isDirectory()) {
                (new File(destinationpath + File.separator + entryName)).mkdir();
            } else {
                System.out.println("\t- " + entryName);
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

    private DBActions getDBActions() throws ClassNotFoundException {
        if (dbActions == null)
            dbActions = new DBActions();
        return dbActions;
    }
}
