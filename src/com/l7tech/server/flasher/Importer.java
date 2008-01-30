package com.l7tech.server.flasher;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.FileUtils;
import com.l7tech.server.config.*;
import com.l7tech.server.config.db.DBActions;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.*;
import java.net.InetAddress;
import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image",
                                                                             "location of image file to import",
                                                                             true, false);
    public static final CommandLineOption MODE = new CommandLineOption("-mode", "[restore | migrate]");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "name of partition to import");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping",
                                                                               "location of the mapping template file",
                                                                               true, false);
    public static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name");
    public static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name");
    public static final CommandLineOption DB_PASSWD = new CommandLineOption("-dbp", "database root password");
    public static final CommandLineOption DB_USER = new CommandLineOption("-dbu", "database root username");
    public static final CommandLineOption OS_OVERWRITE = new CommandLineOption("-os",
                                                                               "overwrite os level config files (only allowed in cloning mode and on non-partitioned systems)",
                                                                               false, true);

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MODE, PARTITION, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER, OS_OVERWRITE};

    private String tempDirectory;
    private String partitionName;
    private OSSpecificFunctions osFunctions;
    private PasswordPropertyCrypto passwordCrypto;
    private boolean fullClone = false;
    private String databaseURL;
    private String databaseUser;
    private String databasePasswd;
    private DBActions dbActions;
    private MappingUtil.CorrespondanceMap mapping = null;
    private String licenseValueBeforeImport = null;
    private String rootDBUsername;
    private String rootDBPasswd;
    private String dbHost;
    private String dbName;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        String inputpathval = FlashUtilityLauncher.getAbsolutePath(arguments.get(IMAGE_PATH.name));
        if (inputpathval == null) {
            logger.info("Error, no image provided for import");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i don't know what to import");
        }
        String mode = arguments.get(MODE.name);
        if (mode == null) {
            logger.info("Error, import mode specified");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + MODE.name);
        }
        if (mode.toLowerCase().equals("restore")) {
            fullClone = true;
        } else if (mode.toLowerCase().equals("migrate")) {
            fullClone = false;
        } else {
            logger.info("Error, unknown import mode specified: " + mode);
            throw new FlashUtilityLauncher.InvalidArgumentException(mode + " is invalid value for " + MODE.name);
        }
        // clone only available on linux. check that that we're on linux if mode is clone
        if (fullClone && OSDetector.isWindows()) {
            logger.info("Error, restore mode requested on windows system");
            throw new IOException("Restore mode not supported on Windows systems");
        }
        // uncompress image to temp folder, look for Exporter.DBDUMP_FILENAME
        tempDirectory = Exporter.createTmpDirectory();
        logger.info("Uncompressing image to temporary directory " + tempDirectory);
        try {
            System.out.println("Reading SecureSpan image file " + inputpathval);
            unzipToDir(inputpathval, tempDirectory);
            if (!(new File(tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_STAGING)).exists()) {
                logger.info("Error, the image provided does not contain an expected file and is therefore suspicious");
                throw new IOException("the file " + inputpathval + " does not appear to be a valid SSG flash image");
            }

            // if clone is asked, make sure the image was produced from a linux system
            if (fullClone) {
                if (!(new File(tempDirectory + File.separator + "hibernate.properties")).exists()) {
                    logger.info("Error, restore mode requested but image was created on windows and therefore cannot be used for restoring");
                    throw new IOException("this image cannot be used in restore mode. perhaps it was created " +
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

            // retrieve source partition name
            fis =  new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
            read = fis.read(buf);
            String srcpartitionname = new String(buf, 0, read);


            logger.info("Proceeding with image");
            System.out.println("SecureSpan image file recognized.");
            // get target partition, make sure it already exists
            partitionName = arguments.get(PARTITION.name);
            // check if the system has more than one partition on it
            PartitionManager partitionManager = PartitionManager.getInstance();
            boolean multiplepartitions = false;
            if (partitionManager.isPartitioned()) {
                Set<String> partitions = partitionManager.getPartitionNames();
                if (partitions.size() > 1) {
                    multiplepartitions = true;
                }
                // option PARTITION now mandatory (unless there is only one)
                if (StringUtils.isEmpty(partitionName)) {
                    if (partitions.size() == 1) {
                        partitionName = partitions.iterator().next();
                        String feedback = "No partition requested, assuming partition " + partitionName;
                        logger.info(feedback);
                        System.out.println(feedback);
                    } else {
                        logger.info("No partition name specified for import on a partitioned system");
                        throw new IOException("this system is partitioned. The \"" +
                                          PARTITION.name + "\" parameter is required");
                    }
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
            passwordCrypto = osFunctions.getPasswordPropertyCrypto();

            Map<String, String> dbProps = PropertyHelper.getProperties(tempDirectory + File.separator + "hibernate.properties", new String[] {
                DBInformation.PROP_DB_USERNAME,
                DBInformation.PROP_DB_PASSWORD,
                DBInformation.PROP_DB_URL,
            });
            databaseURL = dbProps.get(DBInformation.PROP_DB_URL);
            databaseUser = dbProps.get(DBInformation.PROP_DB_USERNAME);
            String imageDbPasswdRaw = dbProps.get(DBInformation.PROP_DB_PASSWORD);
            databasePasswd = passwordCrypto.decryptIfEncrypted(imageDbPasswdRaw);
            logger.info("using database url " + databaseURL);
            logger.info("using database user " + databaseUser);
            logger.info("using database passwd " + databasePasswd);
            // get root db username and password
            rootDBUsername = arguments.get(DB_USER.name);
            rootDBPasswd = arguments.get(DB_PASSWD.name);
            if (rootDBUsername == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide options " + DB_USER.name +
                                " and " + DB_PASSWD.name);
            }
            if (rootDBPasswd == null) rootDBPasswd = ""; // totally legit

            // extract db host and name from url
            dbHost = null;
            dbName = null;
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
            if (dbHost.equalsIgnoreCase(InetAddress.getLocalHost().getCanonicalHostName()) ||
                dbHost.equals(InetAddress.getLocalHost().getHostAddress())) {
                // The database server is on local machine. So use "localhost" instead of
                // FQDN in case user (such as "root") access is restricted to localhost.
                logger.fine("Recognizing \"" + dbHost + "\" as \"localhost\".");
                dbHost = "localhost";
            }

            boolean newDatabaseCreated = false;
            if (fullClone) {
                logger.info("Checking if we can already connect to target database using image db properties");
                // if clone mode, check if we can already get connection from the target database

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
                logger.info("Migration mode");
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
                    logger.log(Level.WARNING, "Cannot connect to target database", e);
                    throw new IOException("Cannot connect to database");
                }  catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Error looking for database use ", e);
                    throw new IOException("interrupted!" + e.getMessage());
                }
            }
            
            // load mapping if requested
            String mappingPath = FlashUtilityLauncher.getAbsolutePath(arguments.get(MAPPING_PATH.name));
            if (mappingPath != null) {
                logger.info("loading mapping file " + mappingPath);
                System.out.print("Reading mapping file " + mappingPath);
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
                throw new IOException(e);
            }


            // if applicable, copy all config files to the right place
            if (fullClone) {
                logger.info("copying system files from image to target system");
                copySystemConfigFiles();

                if (arguments.get(OS_OVERWRITE.name) != null) {
                    if (multiplepartitions) {
                        System.out.println("Ignoring option " + OS_OVERWRITE.name + " because this system is partitioned");
                    } else {
                        // overwrite os level system files
                        OSConfigManager.restoreOSConfigFilesToTmpTarget(tempDirectory);
                    }
                }

                if (!partitionName.equals(srcpartitionname)) {
                    // the source partition is different from the target partition
                    logger.warning("the source and target partition names are different. some overwritten " +
                                   "config files must be hacked");
                    PartitionInformation partitionInfo = partitionManager.getPartition(partitionName);
                    try {
                        PartitionActions.prepareNewpartition(partitionInfo);
                    } catch (SAXException e) {
                        logger.log(Level.WARNING, "cannot prepare target partition", e);
                    }
                }
            } else if (arguments.get(OS_OVERWRITE.name) != null) {
                String issue = "Ignoring option " + OS_OVERWRITE.name + " because it is only supported in restore mode";
                logger.warning(issue);
                System.out.println(issue);
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

    private void doLoadDump(Connection c, String dumpFilePath, String msg) throws IOException, SQLException {
        System.out.print(msg + " [please wait] ..");
        FileReader fr = new FileReader(dumpFilePath);
        BufferedReader reader = new BufferedReader(fr);
        String tmp;
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
                // commit at the end if everything updated correctly
                logger.info("Database dump import loaded succesfully, committing now.");
                c.commit();
            } catch (SQLException e) {
                System.out.println("Error loading database dump. Rolling back now. " + e.getMessage());
                c.rollback();
                throw e;
            } catch (IOException e) {
                System.out.println("Error loading database dump. Rolling back now. " + e.getMessage());
                c.rollback();
                throw e;
            } finally {
                stmt.close();
                c.setAutoCommit(true);
            }
        } finally {
            reader.close();
            fr.close();
        }
        System.out.println(". DONE");
    }

    private void loadDumpFromExplodedImage() throws IOException, SQLException {

        String dumpFilePath;
        if (fullClone) {
            dumpFilePath = tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_CLONE;
        } else {
            dumpFilePath = tempDirectory + File.separator + DBDumpUtil.DBDUMPFILENAME_STAGING;
        }

        // create temporary database copy to test the import
        System.out.print("Creating copy of target database for testing import ..");
        DBInformation dbi = new DBInformation(dbHost, dbName, databaseUser, databasePasswd, rootDBUsername, rootDBPasswd);
        String testdbname = "TstDB_" + System.currentTimeMillis();
        getDBActions(osFunctions).copyDatabase(dbi, testdbname, null);
        System.out.println(" DONE");

        try {
            // load that image on the temp database
            String msg = "Loading image on temporary database";
            Connection c = getDBActions(osFunctions).getConnection(dbHost, testdbname, rootDBUsername, rootDBPasswd);
            try {
                doLoadDump(c, dumpFilePath, msg);
            } finally {
                c.close();
            }
        } finally {
            // delete the temporary database
            System.out.print("Deleting temporary database .. ");
            Connection c = getDBActions(osFunctions).getConnection(dbHost, "", rootDBUsername, rootDBPasswd);
            try {
                Statement stmt = c.createStatement();
                try {
                    stmt.executeUpdate("drop database " + testdbname + ";");
                    System.out.println(" DONE");
                } finally {
                    stmt.close();
                }
            } finally {
                c.close();
            }
        }

        // importing on the real target database
        String msg = "Loading image on target database";
        Connection c = getConnection();
        try {
            doLoadDump(c, dumpFilePath, msg);
        } finally {
            c.close();
        }
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
        String ksdir = osFunctions.getKeystoreDir();
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
            if (!toFile.getParentFile().exists()) {
                logger.warning("the parent directory for the target file " + toFile.getPath() + " does not " +
                               "exist on this target system. perhaps this system is not configured properly. " +
                               "trying to create directory");
                FileUtils.ensurePath(toFile.getParentFile());
            }
            if (toFile.exists()) {
                logger.info("overwriting local " + toFile.getPath());
                toFile.delete();
            } else {
                logger.info("adding local file " + toFile.getPath());
            }
            FileUtils.copyFile(fromFile, toFile);
        } else {
            logger.info("image does not contain config file " + fromFile.getName() + " leaving this file alone");
        }
    }

    private Connection getConnection() throws SQLException {
        Connection c;
        try {
            c = getDBActions(osFunctions).getConnection(databaseURL, databaseUser, databasePasswd);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "unexpected", e);
            throw new SQLException(e.getMessage());
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
