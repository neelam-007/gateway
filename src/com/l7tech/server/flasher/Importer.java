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
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Logger logger = Logger.getLogger(Importer.class.getName());
    // importer options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to import");
    public static final CommandLineOption MODE = new CommandLineOption("-mode", "[clone | stage]");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition name to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping", "location of the staging mapping file");
    public static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name");
    public static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name");
    public static final CommandLineOption DB_PASSWD = new CommandLineOption("-dbp", "database root password (only needed if database needs to be created)");
    public static final CommandLineOption DB_USER = new CommandLineOption("-dbu", "database root username (only needed if database needs to be created)");
    public static final CommandLineOption OS_OVERWRITE = new CommandLineOption("-os", "overwrite os level config files (only allowed in cloning mode and on non-partitioned systems)");

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MODE, PARTITION, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER, OS_OVERWRITE};

    private String tempDirectory;
    private String partitionName;
    private OSSpecificFunctions osFunctions;
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
            logger.info("Error, no image provided for import");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i don't know what to import");
        }
        String mode = arguments.get(MODE.name);
        if (mode == null) {
            logger.info("Error, import mode specified");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + MODE.name);
        }
        if (mode.toLowerCase().equals("clone")) {
            fullClone = true;
        } else if (mode.toLowerCase().equals("stage")) {
            fullClone = false;
        } else {
            logger.info("Error, unknown import mode specified: " + mode);
            throw new FlashUtilityLauncher.InvalidArgumentException(mode + " is invalid value for " + MODE.name);
        }
        // clone only available on linux. check that that we're on linux if mode is clone
        if (fullClone && OSDetector.isWindows()) {
            logger.info("Error, clone mode requested on windows system");
            throw new IOException("Clone mode not supported on Windows systems");
        }
        // uncompress image to temp folder, look for Exporter.DBDUMP_FILENAME
        tempDirectory = Exporter.createTmpDirectory();
        logger.info("Uncompressing image to temporary directory " + tempDirectory);
        try {
            System.out.println("Reading SecureSpan image file");
            unzipToDir(inputpathval, tempDirectory);
            if (!(new File(tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_STAGING)).exists()) {
                logger.info("Error, the image provided does not contain an expected file and is therefore suspicious");
                throw new IOException("the file " + inputpathval + " does not appear to be a valid SSG flash image");
            }

            // if clone is asked, make sure the image was produced from a linux system
            if (fullClone) {
                if (!(new File(tempDirectory + File.separator + "hibernate.properties")).exists()) {
                    logger.info("Error, clone mode requested but image was created on windows and therefore cannot be used for cloning");
                    throw new IOException("this image cannot be used in clone mode. perhaps it was created " +
                            "on a windows system");
                }
            }

            // compare version of the image with version of the target system
            FileInputStream fis =  new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
            byte[] buf = new byte[512];
            int read = fis.read(buf);
            String imgversion = new String(buf, 0, read);
            if (!BuildInfo.getProductVersion().equals(imgversion)) {
                logger.info("cannot import image because target system is running version " +
                            BuildInfo.getProductVersion() + " while image to import is of version " + imgversion);
                throw new IOException("the version of this image is incompatible with this target system (" + imgversion +
                                      " instead of " + BuildInfo.getProductVersion() + ")");
            }

            logger.info("Proceeding with image");
            System.out.println("SecureSpan image file recognized.");
            // get target partition, make sure it already exists
            partitionName = arguments.get(PARTITION.name);
            // check if the system has more than one partition on it
            PartitionManager partitionManager = PartitionManager.getInstance();
            boolean multiplePartitionSystem = partitionManager.isPartitioned();
            if (multiplePartitionSystem) {
                // option PARTITION now mandatory
                if (StringUtils.isEmpty(partitionName)) {
                    logger.info("No partition name specified for import on a partitioned system");
                    throw new IOException("this system is partitioned. The \"" +
                                          PARTITION.name + "\" parameter is required");
                }
                PartitionInformation partitionInfo = partitionManager.getPartition(partitionName);
                if (partitionInfo == null) {
                    logger.info("partition requested does not exist on target system " + partitionName);
                    throw new IOException("this system is partitioned but the " +
                                          "partition \"" + partitionName + "\" is not present.");
                }
            } else {
                // make sure user did not ask for a partition that did not exist
                if (StringUtils.isNotEmpty(partitionName)) {
                    logger.info("This system is not partitioned but used asked to import on partition " + partitionName);
                    throw new IOException("this system is not partitioned. cannot act on partition " + partitionName);
                }
            }
            if (partitionName == null) partitionName = "";
            osFunctions = OSDetector.getOSSpecificFunctions(partitionName);

            boolean newDatabaseCreated = false;
            if (fullClone) {
                logger.info("Checking if we can already connect to target database using image db properties");
                // if clone mode, check if we can already get connection from the target database
                Map<String, String> dbProps = PropertyHelper.getProperties(tempDirectory + File.separator + "hibernate.properties", new String[] {
                    SsgDatabaseConfigBean.PROP_DB_USERNAME,
                    SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                    SsgDatabaseConfigBean.PROP_DB_URL,
                });
                databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
                databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
                databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
                logger.info("using database url " + databaseURL);
                logger.info("using database user " + databaseUser);
                logger.info("using database passwd " + databasePasswd);
                boolean databasepresentandkosher = false;
                try {
                    Connection c = getConnection();
                    try {
                        Statement s = c.createStatement();
                        ResultSet rs = s.executeQuery("select * from hibernate_unique_key");
                        if (rs.next()) {
                            databasepresentandkosher = true;
                        }
                        rs.close();
                        s.close();
                    } finally {
                        c.close();
                    }
                } catch (SQLException e) {
                    databasepresentandkosher = false;
                }
                System.out.println("Using database " + databaseURL);

                // if the database needs to be created, do it
                if (databasepresentandkosher) {
                    logger.info("The database already exists on this system");
                    System.out.println("Existing target database detected");
                } else {
                    logger.info("database need to be created");
                    // get root db username and password
                    String rootDBUsername = arguments.get(DB_USER.name);
                    String rootDBPasswd = arguments.get(DB_PASSWD.name);
                    if (rootDBUsername == null) {
                        throw new FlashUtilityLauncher.InvalidArgumentException("The target database needs to be " +
                                        "created but root database username and password are not provided at " +
                                        "command line. Please provide options " + DB_USER.name +
                                        " and " + DB_PASSWD.name);
                    }
                    if (rootDBPasswd == null) rootDBPasswd = ""; // totally legit
                    // extract db host and name from url
                    String dbHost = null;
                    String dbName = null;
                    Matcher matcher = Pattern.compile("^.*//(.*)/(.*)\\?.*$").matcher(databaseURL);
                    if (matcher.matches()) {
                        dbHost = matcher.group(1);
                        if (dbHost.indexOf(',') >= 0) {
                            dbHost = dbHost.substring(0, dbHost.indexOf(','));
                        }
                        dbName = matcher.group(2);
                    }
                    if (StringUtils.isEmpty(dbHost) || StringUtils.isEmpty(dbName)) {
                        logger.warning("cannot parse host and name from " + databaseURL);
                        throw new IOException("cannot resolve host name or database name from jdbc url in " +
                                              tempDirectory + File.separator + "hibernate.properties");
                    }

                    System.out.print("The target database does not exist. Creating it now ...");
                    try {
                        DBActions.DBActionsResult res = getDBActions(osFunctions).createDb(rootDBUsername, rootDBPasswd,
                                                                                dbHost, dbName, databaseUser,
                                                                                databasePasswd, null, false, true);
                        if (res.getStatus() != DBActions.DB_SUCCESS) {
                            throw new IOException("cannot create database " + res.getErrorMessage());
                        }
                        newDatabaseCreated = true;
                        System.out.println(" DONE");
                    } catch (SQLException e) {
                        System.out.println(" Error " + e.getMessage());
                        throw new IOException("cannot create new database " + e.getMessage());
                    }
                }
            } else {
                logger.info("Staging mode");
                Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
                    SsgDatabaseConfigBean.PROP_DB_USERNAME,
                    SsgDatabaseConfigBean.PROP_DB_PASSWORD,
                    SsgDatabaseConfigBean.PROP_DB_URL,
                });
                databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
                databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
                databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
                logger.info("using database url " + databaseURL);
                logger.info("using database user " + databaseUser);
                logger.info("using database passwd " + databasePasswd);
            }

            // check that target db is not currently used by an SSG
            if (!newDatabaseCreated) {
                try {
                    String connectedNode = checkSSGConnectedToDatabase();
                    if (StringUtils.isNotEmpty(connectedNode)) {
                        logger.info("cannot import on this database because it is being used by a SSG");
                        throw new IOException("A SecureSpan Gateway is currently running " +
                                              "and connected to the database. Please shutdown " + connectedNode);
                    }
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Error looging for database use ", e);
                    throw new IOException("cannot connect to database" + e.getMessage());
                }  catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Error looging for database use ", e);
                    throw new IOException("interrupted!" + e.getMessage());
                }
            }
            
            // load mapping if requested
            String mappingPath = arguments.get(MAPPING_PATH.name);
            if (mappingPath != null) {
                logger.info("loading mapping file");
                System.out.print("Reading mapping file ..");
                try {
                    mapping = MappingUtil.loadMapping(mappingPath);
                } catch (SAXException e) {
                    throw new IOException("Problem loading " + MAPPING_PATH.name + ". Invalid Format. " + e.getMessage());
                }
                System.out.println(". DONE");
            }

            if (!newDatabaseCreated) {
                // actually go on with the import
                logger.info("saving existing license");
                try {
                    saveExistingLicense();
                } catch (SQLException e) {
                    throw new IOException("cannot save existing license from database " + e.getMessage());
                }
            } else {
                logger.info("not trying to save license because new database");
            }

            // load database dump
            logger.info("loading database dump");
            try {
                loadDumpFromExplodedImage();
            } catch (SQLException e) {
                throw new IOException("error loading db dump " + e.getMessage());
            }


            // if applicable, copy all config files to the right place
            if (fullClone) {
                logger.info("copying system files from image to target system");
                copySystemConfigFiles();

                if (arguments.get(OS_OVERWRITE.name) != null) {
                    // overwrite os level system files
                    OSConfigManager.restoreOSConfigFiles(tempDirectory);
                }
            }

            // apply mapping if applicable
            if (mapping != null) {
                logger.info("applying mappings requested");
                try {
                    MappingUtil.applyMappingChangesToDB(osFunctions, databaseURL, databaseUser, databasePasswd, mapping);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "error mapping target", e);
                    throw new IOException("error mapping staging values " + e.getMessage());
                }
            }

            // reload license if applicable
            try {
                reloadLicense();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "error resetting license", e);
                throw new IOException("error resetting license " + e.getMessage());
            }

        } finally {
            logger.info("deleting temp files at " + tempDirectory);
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
        FileReader fr = new FileReader(dumpFilePath);
        BufferedReader reader = new BufferedReader(fr);
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
            fr.close();
            c.close();
        }
        System.out.println(". DONE");
    }

    private void copySystemConfigFiles() throws IOException {
        System.out.print("Cloning SecureSpan Gateway settings ..");

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
        c = getDBActions(osFunctions).getConnection(databaseURL, databaseUser, databasePasswd);
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
                FileUtils.ensurePath((new File(destinationpath + File.separator + entryName)).getParentFile());
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

    private DBActions getDBActions(OSSpecificFunctions osFunctions) throws SQLException {
        if (dbActions == null) {
            try {
                dbActions = new DBActions(osFunctions);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage());
            }
        }
        return dbActions;
    }
}
