package com.l7tech.gateway.config.flasher;

import com.l7tech.util.*;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.ClusterPassphraseManager;
import com.l7tech.server.management.config.node.DatabaseConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.xml.sax.SAXException;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;

import com.l7tech.gateway.config.flasher.FlashUtilityLauncher.InvalidArgumentException;
import com.l7tech.gateway.config.flasher.FlashUtilityLauncher.FatalException;

/**
 * The utility that imports an SSG image
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
class Importer extends ImportExportUtility {

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
            "overwrite os level config files",
            false, true);
    public static final CommandLineOption CREATE_NEW_DB = new CommandLineOption("-newdb" ,"create new database");

    public static final CommandLineOption CONFIG_ONLY = new CommandLineOption("-config", "only restore configuration files, no database restore", false, true);
    public static final CommandLineOption CLUSTER_PASSPHRASE = new CommandLineOption("-cp", "the cluster passphrase for the (resulting) database");
    public static final CommandLineOption GATEWAY_DB_USERNAME = new CommandLineOption("-gdbu", "gateway database username");
    public static final CommandLineOption GATEWAY_DB_PASSWORD = new CommandLineOption("-gdbp", "gateway database password");

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER,
            OS_OVERWRITE, Exporter.AUDIT, CONFIG_ONLY, CLUSTER_PASSPHRASE, GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD,
            CREATE_NEW_DB};

    public static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "Ignored parameter for partition", true, false),
            new CommandLineOption("-mode", "Ignored parameter for mode type", true, false) };


    private static final String CONFIG_PATH = Exporter.FLASHER_CHILD_DIR;
    private static final String[] CONFIG_FILES = new String[]{
            "ssglog.properties",
            "system.properties",
    };

    private String tempDirectory;
    private MappingUtil.CorrespondanceMap mapping = null;
    private String licenseValueBeforeImport = null;
    private String rootDBUsername;
    private String rootDBPasswd;
    private String gatewayDbUsername;
    private String gatewayDbPassword;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private boolean includeAudit = false;
    private String suppliedClusterPassphrase;
    private boolean newDatabaseCreated = false;

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, FlashUtilityLauncher.FatalException, IOException {
        String inputpathval = FlashUtilityLauncher.getAbsolutePath(arguments.get(IMAGE_PATH.name));
        if (inputpathval == null) {
            logger.info("Error, no image provided for import");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ", required for importing image");
        }

        suppliedClusterPassphrase = arguments.get(CLUSTER_PASSPHRASE.name);
        if (suppliedClusterPassphrase == null) {
            logger.info("Error, no cluster passphrase provided for import");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + CLUSTER_PASSPHRASE.name + ".");
        }

        // uncompress image to temp folder, look for Exporter.DBDUMP_FILENAME
        tempDirectory = Exporter.createTmpDirectory();
        logger.info("Uncompressing image to temporary directory " + tempDirectory);
        try {
            System.out.println("Reading SecureSpan image file " + inputpathval);
            unzipToDir(inputpathval, tempDirectory, true);
            if (!(new File(tempDirectory + File.separator + DBDumpUtil.MAIN_BACKUP_FILENAME)).exists()) {
                logger.info("Error, the image provided does not contain an expected file and is therefore suspicious");
                throw new IOException("the file " + inputpathval + " does not appear to be a valid SSG flash image");
            }

            // check whether or not we are expected to include audits
            String auditval = arguments.get(Exporter.AUDIT.name);
            if (auditval != null && !auditval.toLowerCase().equals("no") && !auditval.toLowerCase().equals("false")) {
                //check if an audit backup file exists in the image
                if (new File(tempDirectory + File.separator + DBDumpUtil.AUDIT_BACKUP_FILENAME).exists()) {
                    includeAudit = true;
                } else {
                    System.out.println("Ignoring " + Exporter.AUDIT.name + " option...");
                    logger.info(Exporter.AUDIT.name + " option was requested but no audit backup exists in image; the option will be ignored");
                }
            }

            boolean configOnly = false;
            //check if we are only importing config
            if (arguments.get(CONFIG_ONLY.name) != null) {
                logger.info("Configuration only option requested.  Database will not be imported.");
                configOnly = true;
            }

            // compare version of the image with version of the target system
            FileInputStream fis = new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
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

            rootDBUsername = arguments.get(DB_USER.name);
            rootDBPasswd = arguments.get(DB_PASSWD.name);
            if (rootDBUsername == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide options: " + DB_USER.name +
                        " and " + DB_PASSWD.name);
            }
            if (rootDBPasswd == null) rootDBPasswd = ""; // totally legit

            gatewayDbUsername = arguments.get(GATEWAY_DB_USERNAME.name);
            gatewayDbPassword = arguments.get(GATEWAY_DB_PASSWORD.name);
            if (gatewayDbPassword == null) gatewayDbPassword = ""; // totally legit

            // Replace database host name and database name in URL by those from command line options.
            dbHost = arguments.get(DB_HOST_NAME.name);
            if (dbHost == null) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide option: " + DB_HOST_NAME.name);
            } else if (dbHost.indexOf(':') > 0) {
                dbHost = dbHost.split(":", 2)[0];
                dbPort = dbHost.split(":", 2)[1];
            } else {
                dbPort = "3306";
            }

            boolean isCreateNewDatabase = false;
            dbName = arguments.get(DB_NAME.name);
            if (!arguments.containsKey(DB_NAME.name) && !arguments.containsKey(CREATE_NEW_DB.name)) {
                throw new FlashUtilityLauncher.InvalidArgumentException("Please provide option either: " + DB_NAME.name + " or " + CREATE_NEW_DB.name);
            } else if (arguments.containsKey(DB_NAME.name) && arguments.containsKey(CREATE_NEW_DB.name)) {
                throw new InvalidArgumentException("either specify " + DB_NAME.name + " or " + CREATE_NEW_DB.name);
            } else {
                if (arguments.containsKey(CREATE_NEW_DB.name)) {
                    isCreateNewDatabase = true;
                    dbName = arguments.get(CREATE_NEW_DB.name);
                }
            }

            boolean cleanRestore = false;
            MasterPasswordManager mpm = new MasterPasswordManager(new DefaultMasterPasswordFinder(new File(new File(CONFIG_PATH), Exporter.OMP_DAT_FILE)));
            String databaseUser = gatewayDbUsername;
            String databasePass = gatewayDbPassword;
            File nodePropsFile = new File(new File(CONFIG_PATH), Exporter.NODE_PROPERTIES_FILE);

            final PropertiesConfiguration nodeConfig = new PropertiesConfiguration();
            nodeConfig.setAutoSave(false);
            nodeConfig.setListDelimiter((char) 0);
            try {
                if (nodePropsFile.exists()) {
                    nodeConfig.load(nodePropsFile);
                    databaseUser = nodeConfig.getString("node.db.config.main.user") == null ? databaseUser : nodeConfig.getString("node.db.config.main.user");
                    databasePass = nodeConfig.getString("node.db.config.main.pass") == null ? databasePass : nodeConfig.getString("node.db.config.main.pass");
                    databasePass = new String(mpm.decryptPasswordIfEncrypted(databasePass));
                    logger.info("Using database gateway username/password defined from node.properties");

                    if (!isCreateNewDatabase && !dbName.equals(nodeConfig.getString("node.db.config.main.name"))) {
                        throw new InvalidArgumentException("provided database name does not match with database name in node.properties file.  If you " +
                                "wish to create a new database, use the " + CREATE_NEW_DB.name + " option");
                    }
                } else {
                    nodeConfig.setProperty("node.id", UUID.randomUUID().toString().replace("-", ""));
                    nodeConfig.setProperty("node.db.config.main.user", databaseUser);
                    nodeConfig.setProperty("node.db.config.main.pass", mpm.encryptPassword(databasePass.toCharArray()));
                    cleanRestore = true;
                }

                //no database gateway user defined
                if (databaseUser == null) {
                    logger.info("No database gateway username defined.  Was not found in node.properties and not found in options");
                    throw new FlashUtilityLauncher.InvalidArgumentException("Please provide options: " + GATEWAY_DB_USERNAME.name +
                            " and " + GATEWAY_DB_PASSWORD.name);
                }

                //write the nodeConfig properties but do not save yet
                nodeConfig.setProperty("node.db.config.main.host", dbHost);
                nodeConfig.setProperty("node.db.config.main.port", dbPort);
                nodeConfig.setProperty("node.db.config.main.name", dbName);
                nodeConfig.setProperty("node.cluster.pass", mpm.encryptPassword(suppliedClusterPassphrase.toCharArray()));
            } catch (ConfigurationException e) {
                throw new IOException("Cannot update settings in \"" + nodePropsFile.getAbsolutePath() + "\".", e);
            }

            if (!configOnly) {
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress( InetAddress.getByName(dbHost) );
                if ( networkInterface != null ) {
                    // The database server is on local machine. So use "localhost" instead of
                    // FQDN in case user (such as "root") access is restricted to localhost.
                    logger.fine("Recognizing \"" + dbHost + "\" as \"localhost\" for network interface \""+ networkInterface.getDisplayName() +"\".");
                    dbHost = "localhost";
                } else if ( SyspropUtil.getBoolean("com.l7tech.config.backup.localDbOnly", true) ) {
                    // We will not modifiy a non-local database
                    throw new FlashUtilityLauncher.FatalException( "Database host \""+dbHost+"\" is a remote database. Database restore requires a local database." );
                }

                //boolean newDatabaseCreated = false;
                logger.info("Checking if we can already connect to target database using image db properties");
                // if clone mode, check if we can already get connection from the target database

                System.out.println("Using database host " + dbHost);
                System.out.println("Using database port " + dbPort);
                System.out.println("Using database name " + dbName);

                // if the database needs to be created, do it
                if (!isCreateNewDatabase) {
                    //check if the local node is running
                    if (isLocalNodeRunning()) {
                        throw new FatalException("local gateway may be running, please shutdown the local gateway");
                    }
                    
                    logger.info("Using database specified from node.properties file.");
                    System.out.println("Existing target database detected");
                } else {
                    logger.info("Requested to create new database");
                    System.out.print("Requested to create new database. Creating it now ...");
                    DBActions dba = new DBActions();
                    DatabaseConfig config = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, databaseUser, databasePass);
                    config.setDatabaseAdminUsername(rootDBUsername);
                    config.setDatabaseAdminPassword(rootDBPasswd);
                    DBActions.DBActionsResult res = dba.createDb(config, null, "../etc/sql/ssg.sql", false);
                    if (res.getStatus() != DBActions.StatusType.SUCCESS) {
                        throw new IOException("Cannot create database " + res.getErrorMessage());
                    }
                    newDatabaseCreated = true;
                    System.out.println(" DONE");
                }

                // check that target db is not currently used by an SSG
                if (!newDatabaseCreated) {
                    try {
                        List<String> runningSsg = getRunningSSG(true, null, 10000);
                        if (!runningSsg.isEmpty()) {
                            StringBuffer runningGateways = new StringBuffer();
                        for (int i=0; i < runningSsg.size()-1; i++) {
                            runningGateways.append(runningSsg.get(i) + ", ");
                        }
                        runningGateways.append(runningSsg.get(runningSsg.size()-1));

                            throw new FatalException("Possible SecureSpan Gateway(s) may be running and connected to the database." +
                                    "  Please shutdown the following gateway(s): " + runningGateways.toString());
                        }
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Cannot connect to target database", e);
                        throw new IOException("Cannot connect to database");
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Error looking for database use ", e);
                        throw new IOException("interrupted!" + e.getMessage());
                    }
                }

                // load mapping if requested
                String mappingPath = FlashUtilityLauncher.getAbsolutePath(arguments.get(MAPPING_PATH.name));
                if (mappingPath != null) {
                    if (!cleanRestore) {
                        logger.info("loading mapping file " + mappingPath);
                        System.out.print("Reading mapping file " + mappingPath);
                        try {
                            mapping = MappingUtil.loadMapping(mappingPath);
                        } catch (SAXException e) {
                            throw new FlashUtilityLauncher.FatalException("Problem loading " + MAPPING_PATH.name + ". Invalid Format. " + e.getMessage());
                        }
                        System.out.println(". DONE");
                    } else {
                        logger.info("restoring to an unconfigured system, mapping option will be ignored");
                    }
                }

                if (!newDatabaseCreated) {
                    // actually go on with the import
                    logger.info("saving existing license");
                    try {
                        saveExistingLicense();
                    } catch (SQLException e) {
                        throw new FlashUtilityLauncher.FatalException("Error saving existing license from database " + e.getMessage());
                    }
                } else {
                    logger.info("not trying to save license because new database");
                }

                // load database dump
                logger.info("loading database dump");
                try {
                    loadDumpFromExplodedImage();
                } catch (SQLException e) {
                    throw new IOException("Error loading database.", e);
                }

                // apply mapping if applicable
                if (mapping != null) {
                    logger.info("applying mappings requested");
                    try {
                        DatabaseConfig config = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, rootDBUsername, rootDBPasswd);
                        MappingUtil.applyMappingChangesToDB(config, mapping);
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "error mapping target", e);
                        throw new FlashUtilityLauncher.FatalException("error mapping staging values " + e.getMessage());
                    }
                }

                // reload license if applicable
                try {
                    reloadLicense();
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "error resetting license", e);
                    throw new IOException("error resetting license " + e.getMessage());
                }
            }

            // copy all config files to the right place
            logger.info("copying system files from image to target system");
            copySystemConfigFiles();

            try {
                //if we've made it this far we can save the node.properties file
                nodeConfig.save(nodePropsFile);
            } catch (ConfigurationException e) {
                throw new IOException("Cannot update settings in \"" + nodePropsFile.getAbsolutePath() + "\".", e);
            }
            if (arguments.get(OS_OVERWRITE.name) != null) {
                if ( new File("/opt/SecureSpan/Appliance").isDirectory() ) {
                    if ( new File(new File(tempDirectory),  "os").exists() ) {
                        // overwrite os level system files
                        OSConfigManager.restoreOSConfigFilesToTmpTarget( tempDirectory );
                    } else {
                        logger.info("No OS files are available in the image.  This option will be ignored.");
                    }
                } else {
                    logger.info("OS configuration files can only be restored on an appliance.  This option will be ignored.");
                }
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

    private void doLoadDump(Connection c, /*String dumpFilePath,*/ String msg) throws IOException, SQLException {
        System.out.print(msg + " [please wait] ..");

        String mainDumpFilePath = tempDirectory + File.separator + DBDumpUtil.MAIN_BACKUP_FILENAME;
        String auditDumpFilePath = tempDirectory + File.separator + DBDumpUtil.AUDIT_BACKUP_FILENAME;

        FileReader auditFileReader = null;
        BufferedReader auditBackupReader = null;
        if (includeAudit) {
            auditFileReader = new FileReader(auditDumpFilePath);
            auditBackupReader = new BufferedReader(auditFileReader);
        }

        FileReader mainFileReader = new FileReader(mainDumpFilePath);
        BufferedReader mainBackupReader = new BufferedReader(mainFileReader);
        String tmp;
        try {
            c.setAutoCommit(false);
            Statement stmt = c.createStatement();
            try {
                //always test importing the main backup
                while ((tmp = mainBackupReader.readLine()) != null) {
                    if (tmp.endsWith(";")) {
                        stmt.executeUpdate(tmp.substring(0, tmp.length() - 1));
                    } else {
                        throw new SQLException("unexpected statement " + tmp);
                    }
                }

                //do the audit backup if its asked for
                if (includeAudit) {
                    while ((tmp = auditBackupReader.readLine()) != null) {
                        if (tmp.endsWith(";")) {
                            stmt.executeUpdate(tmp.substring(0, tmp.length() - 1));
                        } else {
                            throw new SQLException("unexpected statement " + tmp);
                        }
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
            mainBackupReader.close();
            mainFileReader.close();

            if (includeAudit) {
                auditBackupReader.close();
                auditFileReader.close();
            }
        }
        System.out.println(". DONE");
    }

    private void loadDumpFromExplodedImage() throws IOException, SQLException, FlashUtilityLauncher.InvalidArgumentException, FlashUtilityLauncher.FatalException {
        // create temporary database copy to test the import
        System.out.print("Creating copy of target database for testing import ..");
        String testdbname = "TstDB_" + System.currentTimeMillis();

        DatabaseConfig sourceConfig = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, rootDBUsername, rootDBPasswd);
        sourceConfig.setDatabaseAdminUsername(rootDBUsername);
        sourceConfig.setDatabaseAdminPassword(rootDBPasswd);

        DatabaseConfig targetConfig = new DatabaseConfig(sourceConfig);
        targetConfig.setName(testdbname);

        DBActions dba = new DBActions();
        dba.copyDatabase(sourceConfig, targetConfig, true, null);
        System.out.println(" DONE");

        try {
            // load that image on the temp database
            String msg = "Loading image on temporary database";
            Connection c = dba.getConnection(targetConfig, true, false);
            try {
                doLoadDump(c, msg);

                //if not a new DB, verify the supplied cluster passphrase is correct
                if (!newDatabaseCreated) {
                    ClusterPassphraseManager cpm = new ClusterPassphraseManager(targetConfig);
                    if (cpm.getDecryptedSharedKey(suppliedClusterPassphrase) == null) {
                        throw new FlashUtilityLauncher.FatalException("Incorrect cluster passphrase.");
                    }
                }
            } finally {
                ResourceUtils.closeQuietly( c );
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
                    ResourceUtils.closeQuietly( stmt );
                }
            } finally {
                ResourceUtils.closeQuietly( c );
            }
        }

        // importing on the real target database
        String msg = "Loading image on target database";
        Connection c = getConnection();
        try {
            doLoadDump(c, msg);
        } finally {
            ResourceUtils.closeQuietly( c );
        }
    }

    private void copySystemConfigFiles() throws IOException {
        System.out.print("Cloning SecureSpan Gateway settings ..");

        for (String file : CONFIG_FILES) {
            restoreConfigFile(CONFIG_PATH + file);
        }

        System.out.println(". DONE");
    }

    private void restoreConfigFile(String destination) throws IOException {
        File toFile = new File(destination);
        File fromFile = new File(tempDirectory + File.separator + toFile.getName());
        if (fromFile.exists()) {
            if (toFile.getParentFile() == null || !toFile.getParentFile().exists()) {
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

    /**
     * Gets running SSG based on the timestamp that is updated by each SSG.  This method makes it best to determine
     * running SSG(s).  Because we are only comparing the timestamps that are updated by SSG(s), there is no guarantee
     * that a gateway might have been starting to start up while checking is performed.
     *
     * @param outputMessages    TRUE if want to output status messages, otherwise FALSE for no messages to be output
     * @param config            Database configuration to be used for connect to database
     * @param sleepTime         The time to sleep in between each sample query to determine the active SSG(s).  Default time is set to 10secs.
     *                          The value must be greater than 10 secs otherwise it'll revert to default 10 secs.
     * @return                  List of SSG(s) name that are considered to be running.  Doesn't guarantee that the gateway
     *                          is actually running or shutdown.  Will never return NULL.
     * @throws SQLException
     * @throws InterruptedException
     */
    private List<String> getRunningSSG(boolean outputMessages, DatabaseConfig config, long sleepTime) throws SQLException, InterruptedException {
        final String gatewayList = "SELECT nodeid, statustimestamp, name FROM cluster_info";
        final long intervalSleepTime = sleepTime <= 10000 ? 10000 : sleepTime;
        Map<String, Long> availableSsg = new HashMap<String, Long>();
        List<String> runningSSG = new ArrayList<String>();

        if (outputMessages) {
            System.out.print("Making sure targets are offline .");
        }

        Connection connection = null;
        Statement statement = null;
        ResultSet results = null;
        try {
            connection = config == null ? getConnection() : (new DBActions()).getConnection(config, false);
            statement = connection.createStatement();

            //cosmetic dots
            if (outputMessages) {
                for (int i = 0; i < 2; i++) {
                    Thread.sleep(500);
                    System.out.print(".");
                }
            }

            //gets all the cluster nodes
            try {
                results = statement.executeQuery(gatewayList);
                while (results.next()) {
                    availableSsg.put(results.getString(1), results.getLong(2));
                }
            } finally {
                ResourceUtils.closeQuietly(results);
            }

            //sleep to allow any running gateway to update their timestamp
            Thread.sleep(intervalSleepTime);

            //cosmetic dots
            if (outputMessages) {
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(500);
                    System.out.print(".");
                }
                System.out.println(" DONE");
            }

            //determine if there were any changes to the timestamp for each nodes
            try {
                results = statement.executeQuery(gatewayList);
                while (results.next()) {
                    String nodeid = results.getString(1);
                    if (availableSsg.containsKey(nodeid)) {
                        //check if new timestamp
                        Long previousTimestamp = availableSsg.get(nodeid);
                        if (!previousTimestamp.equals(results.getLong(2))) {
                            runningSSG.add(results.getString(3));
                        }
                    } else {
                        //could be newly added node in which case, we'll just assume it's running
                        runningSSG.add(results.getString(3));
                    }
                }
            } finally {
                ResourceUtils.closeQuietly(results);
            }
        } finally {
            ResourceUtils.closeQuietly(results);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(connection);
        }

        return runningSSG;
    }

    public void unzipToDir(final String filename, final String destinationpath, boolean outputMessages) throws IOException {
        ZipInputStream zipinputstream = null;
        try {
            zipinputstream = new ZipInputStream( new FileInputStream(filename) );
            ZipEntry zipentry = zipinputstream.getNextEntry();
            while ( zipentry != null ) {
                // for each entry to be extracted
                String entryName = zipentry.getName();
                final File outputFile = new File(destinationpath + File.separator + entryName);

                if ( zipentry.isDirectory() ) {
                    outputFile.mkdirs();
                } else {
                    if (outputMessages) {
                        System.out.println("\t- " + entryName);
                    }
                    FileUtils.ensurePath( outputFile.getParentFile() );
                    FileOutputStream fileoutputstream = null;
                    try {
                        fileoutputstream = new FileOutputStream( outputFile );
                        IOUtils.copyStream( zipinputstream, fileoutputstream );
                    } finally {
                        ResourceUtils.closeQuietly( fileoutputstream );
                    }
                    zipinputstream.closeEntry();
                }
                zipentry = zipinputstream.getNextEntry();
            }
        } finally {
            ResourceUtils.closeQuietly( zipinputstream );
        }
    }

    @Override
    public List<CommandLineOption> getIgnoredOptions() {
        return Arrays.asList(ALL_IGNORED_OPTIONS);
    }

    @Override
    public List<CommandLineOption> getValidOptions() {
        return Arrays.asList(ALLOPTIONS);
    }

    @Override
    public String getUtilityType() {
        return "import";
    }

    @Override
    public void preProcess(Map<String, String> args) throws InvalidArgumentException, IOException, FatalException {
        //skip the whole pre-processing
        if (args.containsKey(SKIP_PRE_PROCESS.name)) {
            return;
        }

        //image file to import
        if (!args.containsKey(IMAGE_PATH.name)) {
            throw new InvalidArgumentException("missing option " + IMAGE_PATH.name + ", required for importing image");
        } else {
            verifyFileExistence(args.get(IMAGE_PATH.name), false);  //check if file exists

            //unzip the file to check for version and mandatory files, we should always remove the files afterwards
            try {
                tempDirectory = Exporter.createTmpDirectory();
                unzipToDir(args.get(IMAGE_PATH.name), tempDirectory, false);

                //check for version
                try {
                    FileInputStream fis = new FileInputStream(tempDirectory + File.separator + Exporter.VERSIONFILENAME);
                    byte[] buf = new byte[512];
                    int read = fis.read(buf);
                    String imageVersion = new String(buf, 0, read);
                    verifyDatabaseVersion(imageVersion);    //check database version is correct
                } catch (FileNotFoundException fnfe) {
                    throw new InvalidArgumentException("version file not found in image file");
                }

                //check for mandatory back up file exists in the zip file
                if (!(new File(tempDirectory + File.separator + DBDumpUtil.MAIN_BACKUP_FILENAME)).exists()) {
                    throw new IOException("the file " + args.get(IMAGE_PATH.name) + " does not appear to be a valid SSG flash image");
                }
            } finally {
                //always delete tmp directory
                FileUtils.deleteDir(new File(tempDirectory));
            }
        }

        //cluster password required
        if (!args.containsKey(CLUSTER_PASSPHRASE.name)) {
            throw new InvalidArgumentException("missing option " + CLUSTER_PASSPHRASE.name);
        }

        //root admin username/password, host, database name required
        if (!args.containsKey(DB_HOST_NAME.name)) {
            throw new InvalidArgumentException("missing option " + DB_HOST_NAME.name);
        }

        if (!args.containsKey(DB_NAME.name) && !args.containsKey(CREATE_NEW_DB.name)) {
            throw new InvalidArgumentException("missing option " + DB_NAME.name + " or " + CREATE_NEW_DB.name);
        } else if (args.containsKey(DB_NAME.name) && args.containsKey(CREATE_NEW_DB.name)) {
            throw new InvalidArgumentException("either specify " + DB_NAME.name + " or " + CREATE_NEW_DB.name);
        }

        if (!args.containsKey(DB_USER.name)) {
            throw new InvalidArgumentException("missing option " + DB_USER.name);
        }

        //check permission and file existence for mapping option
        if (args.containsKey(MAPPING_PATH.name)) {
            verifyFileExistence(args.get(MAPPING_PATH.name), false);    //check file exists
            try {
                //try to parse through the mapping to see if formatted to what we are expecting
                mapping = MappingUtil.loadMapping(args.get(MAPPING_PATH.name));
            } catch (SAXException saxe) {
                throw new FatalException("problem loading " + MAPPING_PATH.name + ": " + ExceptionUtils.getMessage(saxe));
            }
        }

        //check database connection
        try {
            String rootUsername = args.get(DB_USER.name);   //mandatory
            String rootPassword = args.get(DB_PASSWD.name); //mandatory

            //optional, we'll determine if these info can be retrieved from node.properties, if not then we'll need to
            //ask them to enter those options
            String gatewayUsername = args.get(GATEWAY_DB_USERNAME.name);
            String gatewayPassword = args.get(GATEWAY_DB_PASSWORD.name);

            if (rootPassword == null) {
                rootPassword = "";
            }

            if (gatewayPassword == null) {
                gatewayPassword = "";
            }

            int port = 3306;
            String host = args.get(DB_HOST_NAME.name);
            if (host.indexOf(':') > 0) {
                host = host.split(":", 2)[0];
                port = Integer.parseInt(host.split(":", 2)[1]);
            }

            MasterPasswordManager mpm = new MasterPasswordManager(new DefaultMasterPasswordFinder(new File(new File(CONFIG_PATH), Exporter.OMP_DAT_FILE)));
            File nodePropsFile = new File(new File(CONFIG_PATH), Exporter.NODE_PROPERTIES_FILE);
            final PropertiesConfiguration nodeConfig = new PropertiesConfiguration();
            nodeConfig.setAutoSave(false);
            nodeConfig.setListDelimiter((char) 0);

            if (nodePropsFile.exists()) {
                nodeConfig.load(nodePropsFile);
                gatewayUsername = nodeConfig.getString("node.db.config.main.user") == null ? gatewayUsername : nodeConfig.getString("node.db.config.main.user");
                gatewayPassword = nodeConfig.getString("node.db.config.main.pass") == null ? gatewayPassword : nodeConfig.getString("node.db.config.main.pass");
                gatewayPassword = new String(mpm.decryptPasswordIfEncrypted(gatewayPassword));
            } else {
                //node.properties file does not exists, and no gateway username defined, we need to ask for this
                if (gatewayUsername == null) {
                    throw new InvalidArgumentException("Please provide options: " + GATEWAY_DB_USERNAME.name +
                            " and " + GATEWAY_DB_PASSWORD.name);
                }
            }

            //test root user conneciton
            if (args.containsKey(DB_NAME.name)) {
                //test gateway user connection
                verifyDatabaseConnection(new DatabaseConfig(host, port, args.get(DB_NAME.name), gatewayUsername, gatewayPassword), false);

                //if the database alerady exists, check if all gateway have been shut down
                if (verifyDatabaseExists(host, args.get(DB_NAME.name), port, rootUsername, rootPassword)) {
                    //database doesnt exists check if gateway are shut down

                    List<String> runningSsg = getRunningSSG(false, new DatabaseConfig(host, port, args.get(DB_NAME.name), rootUsername, rootPassword), 10000);
                    if (!runningSsg.isEmpty()) {
                        StringBuffer runningGateways = new StringBuffer();
                        for (int i=0; i < runningSsg.size()-1; i++) {
                            runningGateways.append(runningSsg.get(i) + ", ");
                        }
                        runningGateways.append(runningSsg.get(runningSsg.size()-1));

                        throw new FatalException("Possible SecureSpan Gateway(s) may be running and connected to the database." +
                                "  Please shutdown the following gateway(s): " + runningGateways.toString());
                    }
                }
            } else if (args.containsKey(CREATE_NEW_DB.name)) {
                //check if the database exists already
                if (verifyDatabaseExists(host, args.get(CREATE_NEW_DB.name), port, gatewayUsername, gatewayPassword)) {
                    throw new FatalException("The database " + args.get(CREATE_NEW_DB.name) + " already exists");
                }

                //check if the local node is running
                if (isLocalNodeRunning()) {
                    throw new IOException("local gateway may be running, please shutdown the local gateway");
                }
            }

        } catch (ConfigurationException ce) {
            throw new IOException("failed to load information from " + Exporter.NODE_PROPERTIES_FILE);
        } catch (SQLException sqle) {
            throw new IOException("database error: " + ExceptionUtils.getMessage(sqle));
        } catch (InterruptedException ie) {
            throw new FatalException("database error: " + ExceptionUtils.getMessage(ie));
        }
    }
}
