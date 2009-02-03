package com.l7tech.gateway.config.flasher;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.FileUtils;
import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.xml.sax.SAXException;

import java.io.*;
import java.sql.*;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.InetAddress;

/**
 * The utility that imports an SSG image
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
class Importer {

    private static final Logger logger = Logger.getLogger(Importer.class.getName());
    // importer options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image",
                                                                             "location of image file to import",
                                                                             true, false);
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
    public static final CommandLineOption CONFIG_ONLY = new CommandLineOption("-config", "only restore configuration files, no database restore", false, true);
    public static final CommandLineOption CLUSTER_PASSPHRASE = new CommandLineOption("-cp", "the cluster passphrase for the (resulting) database");

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER, OS_OVERWRITE, Exporter.AUDIT, CONFIG_ONLY, CLUSTER_PASSPHRASE};

    private static final String CONFIG_PATH = "../../node/default/etc/conf/";
    private static final String[] CONFIG_FILES = new String[]{
        "ssglog.properties",
        "system.properties",
    };

    private String tempDirectory;
    private MappingUtil.CorrespondanceMap mapping = null;
    private String licenseValueBeforeImport = null;
    private String rootDBUsername;
    private String rootDBPasswd;
    private String dbHost;
    private String dbPort;
    private String dbName;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        String inputpathval = FlashUtilityLauncher.getAbsolutePath(arguments.get(IMAGE_PATH.name));
        if (inputpathval == null) {
            logger.info("Error, no image provided for import");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i don't know what to import");
        }
//        if ( arguments.get("-mode")!=null && !"restore".equals(arguments.get("-mode").toLowerCase()) ) {
//            throw new FlashUtilityLauncher.InvalidArgumentException("-mode is not supported");
//        }

        // uncompress image to temp folder, look for Exporter.DBDUMP_FILENAME
        tempDirectory = Exporter.createTmpDirectory();
        logger.info("Uncompressing image to temporary directory " + tempDirectory);
        try {
            System.out.println("Reading SecureSpan image file " + inputpathval);
            unzipToDir(inputpathval, tempDirectory);
            if (!(new File(tempDirectory + File.separator + DBDumpUtil.MAIN_BACKUP_FILENAME)).exists()) {
                logger.info("Error, the image provided does not contain an expected file and is therefore suspicious");
                throw new IOException("the file " + inputpathval + " does not appear to be a valid SSG flash image");
            }

            // compare version of the image with version of the target system
            FileInputStream fis =  new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
            byte[] buf = new byte[512];
            int read = fis.read(buf);
            String imgversion = new String(buf, 0, read);
            if (!BuildInfo.getProductVersion().equals(imgversion)) {
                logger.info("Cannot import image because target system is running version " +
                            BuildInfo.getProductVersion() + " while image to import is of version " + imgversion);
                throw new IOException("the version of this image is incompatible with this target system (" + imgversion +
                                      " instead of " + BuildInfo.getProductVersion() + ")");
            }

            logger.info("Proceeding with image");
            System.out.println("SecureSpan image file recognized.");

//            //parititons have names like "dev", "prod" etc. so we'll use that instead of integers.
//            String partName = arguments.get("-p");
//            if ( partName!=null && !"default_".equals(partName) ) {
//                throw new FlashUtilityLauncher.InvalidArgumentException("Partitions are no longer supported.");
//            }

            rootDBUsername = arguments.get(DB_USER.name);
            rootDBPasswd = arguments.get(DB_PASSWD.name);
            if (rootDBUsername == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide options: " + DB_USER.name +
                                " and " + DB_PASSWD.name);
            }
            if (rootDBPasswd == null) rootDBPasswd = ""; // totally legit

            // Replace database host name and database name in URL by those from command line options.
            dbHost = arguments.get(DB_HOST_NAME.name);
            if (dbHost == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide option: " + DB_HOST_NAME.name);
            } else if (dbHost.indexOf(':')>0) {
                dbHost = dbHost.split(":",2)[0];
                dbPort = dbHost.split(":",2)[1];
            } else {
                dbPort = "3306";
            }

            dbName = arguments.get(DB_NAME.name);
            if (dbName == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide option: " + DB_NAME.name);
            }

            MasterPasswordManager mpm = new MasterPasswordManager( new DefaultMasterPasswordFinder( new File(new File(CONFIG_PATH), "omp.dat") ) );
            String databaseUser = "gateway";
            String databasePass = rootDBPasswd;
            File nodePropsFile = new File(new File(CONFIG_PATH), "node.properties");
            try {
                final PropertiesConfiguration dbConfig = new PropertiesConfiguration();
                dbConfig.setAutoSave(false);
                dbConfig.setListDelimiter((char)0);

                if ( nodePropsFile.exists() ) {
                    dbConfig.load(nodePropsFile);
                    databaseUser = dbConfig.getString("node.db.config.main.user")==null ? databaseUser : dbConfig.getString("node.db.config.main.user");
                    databasePass = dbConfig.getString("node.db.config.main.pass")==null ? databasePass : dbConfig.getString("node.db.config.main.pass");
                    databasePass = new String(mpm.decryptPasswordIfEncrypted(databasePass));
                } else {
                    dbConfig.setProperty("node.id", UUID.randomUUID().toString().replace("-",""));
                    dbConfig.setProperty("node.db.config.main.user", databaseUser);
                    dbConfig.setProperty("node.db.config.main.pass", mpm.encryptPassword(databasePass.toCharArray()));
                }

                dbConfig.setProperty("node.db.config.main.host", dbHost);
                dbConfig.setProperty("node.db.config.main.port", dbPort);
                dbConfig.setProperty("node.db.config.main.name", dbName);

                dbConfig.save(nodePropsFile);                
            } catch (ConfigurationException e) {
                throw new IOException("Cannot replace database settings in \"" + nodePropsFile.getAbsolutePath() + "\".", e);
            }

            if (dbHost.equalsIgnoreCase(InetAddress.getLocalHost().getCanonicalHostName()) ||
                dbHost.equals(InetAddress.getLocalHost().getHostAddress())) {
                // The database server is on local machine. So use "localhost" instead of
                // FQDN in case user (such as "root") access is restricted to localhost.
                logger.fine("Recognizing \"" + dbHost + "\" as \"localhost\".");
                dbHost = "localhost";
            }

            boolean newDatabaseCreated = false;
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
            System.out.println("Using database host " + dbHost);
            System.out.println("Using database port " + dbPort);
            System.out.println("Using database name " + dbName);

            // if the database needs to be created, do it
            if (databasepresentandkosher) {
                logger.info("The database already exists on this system");
                System.out.println("Existing target database detected");
            } else {
                logger.info("database need to be created");
                System.out.print("The target database does not exist. Creating it now ...");
                DBActions dba = new DBActions();
                DatabaseConfig config = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, databaseUser, databasePass);
                config.setDatabaseAdminUsername( rootDBUsername );
                config.setDatabaseAdminPassword( rootDBPasswd );
                DBActions.DBActionsResult res = dba.createDb( config, null, "../etc/sql/ssg.sql", false );
                if ( res.getStatus() != DBActions.StatusType.SUCCESS) {
                    throw new IOException("cannot create database " + res.getErrorMessage());
                }
                newDatabaseCreated = true;
                System.out.println(" DONE");
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


            // copy all config files to the right place
            logger.info("copying system files from image to target system");
            copySystemConfigFiles();

            if (arguments.get(OS_OVERWRITE.name) != null) {
                // overwrite os level system files
                OSConfigManager.restoreOSConfigFilesToTmpTarget(tempDirectory);
            }

            // apply mapping if applicable
            if (mapping != null) {
                logger.info("applying mappings requested");
                try {
                    DatabaseConfig config = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, rootDBUsername, rootDBPasswd);
                    MappingUtil.applyMappingChangesToDB(config, mapping);
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
        String dumpFilePath = tempDirectory + File.separator + DBDumpUtil.MAIN_BACKUP_FILENAME;

        // create temporary database copy to test the import
        System.out.print("Creating copy of target database for testing import ..");
        String testdbname = "TstDB_" + System.currentTimeMillis();

        DatabaseConfig sourceConfig = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, rootDBUsername, rootDBPasswd);
        sourceConfig.setDatabaseAdminUsername( rootDBUsername );
        sourceConfig.setDatabaseAdminPassword( rootDBPasswd );

        DatabaseConfig targetConfig = new DatabaseConfig(sourceConfig);
        targetConfig.setName(testdbname);

        DBActions dba = new DBActions();
        dba.copyDatabase( sourceConfig, targetConfig, true, null);
        System.out.println(" DONE");

        try {
            // load that image on the temp database
            String msg = "Loading image on temporary database";
            Connection c = dba.getConnection(targetConfig, true, false);
            try {
                doLoadDump(c, dumpFilePath, msg);
            } finally {
                c.close();
            }
        } finally {
            // delete the temporary database
            System.out.print("Deleting temporary database .. ");
            Connection c = dba.getConnection(targetConfig, true);
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

        for ( String file : CONFIG_FILES ) {
            restoreConfigFile(CONFIG_PATH + file);
        }

        System.out.println(". DONE");
    }

    private void restoreConfigFile(String destination) throws IOException {
        File toFile = new File(destination);
        File fromFile = new File(tempDirectory + File.separator + toFile.getName());
        if (fromFile.exists()) {
            if (toFile.getParentFile()==null || !toFile.getParentFile().exists() ) {
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
        return new DBActions().getConnection(new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, rootDBUsername, rootDBPasswd), false);
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

}
