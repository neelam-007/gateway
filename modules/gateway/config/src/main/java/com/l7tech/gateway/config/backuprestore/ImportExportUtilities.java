package com.l7tech.gateway.config.backuprestore;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;
import java.sql.*;
import java.net.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.util.*;

/**
 * <p>
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * </p>
 *
 * Utility class for Exporting and Importing backup image zip files
 *
 */    //todo [Donal] this class should be package private
public class ImportExportUtilities {
    private static final Logger logger = Logger.getLogger(ImportExportUtilities.class.getName());

    private static enum ARGUMENT_TYPE {VALID_OPTION, IGNORED_OPTION, INVALID_OPTION, VALUE}

    /** System property used as base directory for all relative paths. */
    public static final String BASE_DIR_PROPERTY = "com.l7tech.server.backuprestore.basedir";
    public static final String OMP_DAT = "omp.dat";
    public static final String NODE_PROPERTIES = "node.properties";
    public static final String SSGLOG_PROPERTIES = "ssglog.properties";
    public static final String SYSTEM_PROPERTIES = "system.properties";
    /**
     * These configuration files are expected in ssg_home/node/default/etc/conf/ and constitute
     * the complete backing up of SSG configuration files
     */
    public static final String[] CONFIG_FILES = new String[]{
            SSGLOG_PROPERTIES,
            SYSTEM_PROPERTIES,
            NODE_PROPERTIES,
            OMP_DAT
    };
    public static final String VERSION = "version";
    public static final String MANIFEST_LOG = "manifest.log";
    /**
     * Default installation of the ssg appliance
     */
    public static final String OPT_SECURE_SPAN_APPLIANCE = "/opt/SecureSpan/Appliance";

    /**
     * Relative installation location of the Gateway
     */
    public static final String GATEWAY = "Gateway";

    /**
     * Relative installation location of the Appliance
     */
    public static final String APPLIANCE = "Appliance";

    /**
     * Relative installation location of the Enterprise Service Manager
     */
    public static final String ENTERPRISE_SERVICE_MANAGER = "EnterpriseManager";


    public static final String NODE_CONF_DIR = "node/default/etc/conf/";
    public static final String CA_JAR_DIR = "runtime/modules/lib";
    public static final String MA_AAR_DIR = "runtime/modules/assertions";
    public static final String POST_FIVE_O_DEFAULT_BACKUP_FOLDER = "config/backup/images";
    static final String UNIQUE_TIMESTAMP = "yyyyMMddHHmmss";
    private static final String FTP_PROTOCOL = "ftp://";

    public static final String SSG_SQL = "/config/etc/sql/ssg.sql";
    static final String EXCLUDE_FILES_PATH = "cfg/exclude_files";

    static void throwifEsmIsRunning() {
        final File esmPid = new File("/var/run/ssemd.pid");
        if(esmPid.exists())
                throw new IllegalStateException("Enterprise Service Manager is running");
    }

    static void throwIfEsmNotPresent(final File esmHome) {
        if(!esmHome.exists())
                throw new IllegalStateException("The Enterprise Service Manager is not installed");
        if(!esmHome.isDirectory())
                throw new IllegalStateException("The Enterprise Service Manager is not correctly installed");
    }
    
    /**
     * Quietly test if the given path have permissions to write.  This method is similar to
     * testCanWrite except that it will not output any error messages, instead, they will be logged.
     *
     * @param path  Path to test
     * @return  TRUE if can write on the given path, otherwise FALSE.
     */
    static boolean testCanWriteSilently(final String path) {
        try {
            final FileOutputStream fos = new FileOutputStream(path);
            fos.close();
            (new File(path)).delete();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Validate that the image file exists and that we can write to it
     * @param pathToImageFile String representing the relative or absolute path to the image file. Cannot be nul
     * @throws java.io.IOException if we cannot write to the pathToImageFile file
     */
    static void validateImageFile(final String pathToImageFile) throws IOException {
        if (pathToImageFile == null) {
            throw new NullPointerException("pathToImageFile cannot be null");
        } else {
            //fail if file exists
            throwIfFileExists(pathToImageFile);
        }

        if (!testCanWriteSilently(pathToImageFile)) {
            throw new IOException("Cannot write image to " + pathToImageFile);
        }
    }

    /**
     * Read the fileToProcess and extract all lines of text, except for lines starting with '#', and return
     * them in a list of strings
     * @param fileToProcess file to read
     * @return List<String> of each line of text found in the file
     * @throws java.io.IOException if any problems reading the file
     */
    static List<String> processFile(final File fileToProcess) throws IOException {
        final List<String> returnList = new ArrayList<String>();
        if(!fileToProcess.exists() || !fileToProcess.isFile()) return returnList;
        FileReader omitFileReader = null;
        BufferedReader omitBufferedReader = null;

        try {
            omitFileReader = new FileReader(fileToProcess);
            omitBufferedReader = new BufferedReader(omitFileReader);

            String line;
            while ((line = omitBufferedReader.readLine()) != null) {
                if (!line.startsWith("#")) {//ignore comments
                    String fileName = line.trim();
                    if (!fileName.isEmpty() && !returnList.contains(fileName)) {//no duplicates and no empty table names
                        returnList.add(fileName);
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly(omitBufferedReader);
            ResourceUtils.closeQuietly(omitFileReader);
        }
        return returnList;
    }

    /**
     * Classloader designed to ONLY to load BuildInfo from Gateway jar from the standard install directory of
     * /opt/SecureSpan/Gateway/runtime/Gateway.jar
     */
    private static class GatewayJarClassLoader extends ClassLoader{
        private final ClassLoader loader;

        GatewayJarClassLoader(final File gatewayJarFile) throws MalformedURLException {
            final URL gatewayJar = gatewayJarFile.toURI().toURL();
            loader = new URLClassLoader(new URL[]{gatewayJar}, null);
        }

        public Class<?> loadClass(final String name) throws ClassNotFoundException {
            if(name.equals("com.l7tech.util.BuildInfo")){
                return loader.loadClass(name);
            }
            throw new ClassNotFoundException(GatewayJarClassLoader.class.getName()+" only supports class BuildInfo");
        }
    }

    /**
     * Are we running on a pre 5.0 system? If so a BackupRestoreLauncher.FatalException is thrown. This method will
     * return the version of the SSG installed
     * @param gatewayJarFile File representing Gateway.jar, from the local SSG installation
     * @return an int arrary with the major, minor and subversion values for the installed SSG as indexs 0, 1 and 2
     * @throws RuntimeException if the SSG installation cannot be found, based on the existence of Gateway.jar. Also
     * thrown if it's not possible to determine the SSG version from the SSG installation
     */
    public static int [] throwIfLessThanFiveO(final File gatewayJarFile){
        try {
            if(!gatewayJarFile.exists()) throw new IllegalStateException("Cannot find SSG installation");

            final GatewayJarClassLoader gatewayJarClassLoader = new GatewayJarClassLoader(gatewayJarFile);
            final Class clazz = gatewayJarClassLoader.loadClass("com.l7tech.util.BuildInfo");

            Method method = clazz.getMethod("getProductVersionMajor");
            Object test = method.invoke(clazz);
            final int majorVersion = Integer.parseInt(test.toString());

            method = clazz.getMethod("getProductVersionMinor");
            test = method.invoke(clazz);
            final int minorVersion = Integer.parseInt(test.toString());

            method = clazz.getMethod("getProductVersionSubMinor");
            test = method.invoke(clazz);
            final int subMinorVersion = Integer.parseInt(test.toString());

            if(majorVersion < 5) throw new UnsupportedOperationException("Pre 5.0 SSG installations are not supported");

            return new int[]{majorVersion, minorVersion, subMinorVersion};

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        }
    }

    /**
     * ComponentType is used for the directory names in the image zip file, and for processing selective
     * backup component parameters
     */
    protected enum ComponentType{
        OS("os", "Operating System files"),
        CONFIG("config", "SSG Config"),
        MAINDB("maindb", "Main database backup sql (no audits)"),
        AUDITS("audits", "Database audits"),
        CA("ca", "Custom Assertions"),
        MA("ma", "Modular Assertions"),
        VERSION("version", "SSG Version Information"),
        ESM("esm", "Enterprise Service Manager");

        ComponentType(String componentName, String description) {
            this.componentName = componentName;
            this.description = description;
        }

        public String getComponentName() {
            return componentName;
        }

        public String getDescription() {
            return description;
        }

        private final String componentName;
        private String description;
    }

    /**
     * Validae the ftp program parameters. If any ftp param is supplied, then they all must. This method enforces that
     * constraint. If all ftp params are supplied then true is returned, otherwise false
     * @param allParams map of program parameters
     * @return true if ftp parameters were supplied and they all exist. False if NO ftp params were supplied
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException if an incomplete set of ftp parameters were supplied, or if the ftp
     * host name is invalid
     */
    public static boolean checkAndValidateFtpParams(final Map<String, String> allParams) throws InvalidProgramArgumentException {
        //check if ftp requested
        for(Map.Entry<String, String> entry: allParams.entrySet()){
            if(entry.getKey().startsWith("-ftp")){
                //make sure they are all there
                for(final CommandLineOption clo: CommonCommandLineOptions.ALL_FTP_OPTIONS){
                    if(!allParams.containsKey(clo.getName())) throw new InvalidProgramArgumentException("Missing argument: " + clo.getName());
                    if(clo == CommonCommandLineOptions.FTP_HOST){
                        String hostName = allParams.get(CommonCommandLineOptions.FTP_HOST.getName());
                        try {
                            if(!hostName.startsWith(FTP_PROTOCOL)) hostName = FTP_PROTOCOL +hostName;
                            final URL url = new URL(hostName);
                            if(url.getPort() == -1)
                                throw new InvalidProgramArgumentException("-ftp_host value requires a port number");
                        } catch (MalformedURLException e) {
                            throw new InvalidProgramArgumentException(e.getMessage());
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Extract ftp parameters from the supplied args and create and return an FtpClientConfig.
     * Only call when you know an -ftp* parameter was supplied. Will throw an exception if expected values are not
     * found in args
     * @param args the command line arguments
     * @param validOptions all possible valid options. Cannot be null
     * @param ignoredOptions options which are no longer supported, but are ignored. Cannot be null
     * @param imageName the value of the -image parameter. Cannot be null or the empty string or just spaces 
     * @return a FtpClientConfig object if ftp params were supplied, null otherwise
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException if any
     * required ftp parameter is missing
     */
    public static FtpClientConfig getFtpConfig(final String[] args,
                                                  final List<CommandLineOption> validOptions,
                                                  final List<CommandLineOption> ignoredOptions,
                                                  final String imageName)
            throws InvalidProgramArgumentException {
        if(args == null) throw new NullPointerException("args cannot be null");
        if(args.length == 0) throw new IllegalArgumentException("args contains no values");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.trim().isEmpty()) throw new IllegalArgumentException("imageName cannot be the empty or just spaces");

        final String ftpHost = getAndValidateSingleArgument(args, CommonCommandLineOptions.FTP_HOST, validOptions, ignoredOptions);
        final String ftpUser = getAndValidateSingleArgument(args, CommonCommandLineOptions.FTP_USER, validOptions, ignoredOptions);
        final String ftpPass = getAndValidateSingleArgument(args, CommonCommandLineOptions.FTP_PASS, validOptions, ignoredOptions);

        return getFtpConfig(imageName, ftpHost, ftpUser, ftpPass);
    }

    /**
     * Extract ftp parameters from the programParams and create and return an FtpClientConfig. If no -ftp_* parameters
     * were passed into createBackupImage, then this will return null
     *
     * This can be called when it's not known whether ftp has been requested or not
     * @param programParams The parameters passed into createBackupImage
     * @return a FtpClientConfig object if ftp params were supplied, null otherwise
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException if any required ftp parameter is missing
     */
    public static FtpClientConfig getFtpConfig(final Map<String, String> programParams)
            throws InvalidProgramArgumentException {
        if(programParams == null) throw new NullPointerException("programParams cannot be null");
        //not for much of a reason other than there is no use case when they would be empty
        if(programParams.isEmpty()) throw new IllegalArgumentException("programParams cannot be empty");
        
        String ftpHost = programParams.get(CommonCommandLineOptions.FTP_HOST.getName());
        if(ftpHost == null) return null;
        
        //as ftp host was supplied, validate all required ftp params exist
        checkAndValidateFtpParams(programParams);

        final String ftpUser = programParams.get(CommonCommandLineOptions.FTP_USER.getName());
        final String ftpPass = programParams.get(CommonCommandLineOptions.FTP_PASS.getName());
        if(ftpUser == null || ftpPass == null) throw new NullPointerException("ftp_user and ftp_pass must be non null");

        final String imageName = programParams.get(Exporter.IMAGE_PATH.getName());
        return getFtpConfig(imageName, ftpHost, ftpUser, ftpPass);
    }

    private static FtpClientConfig getFtpConfig(final String imageName,
                                                final String ftpHost,
                                                final String ftpUser,
                                                final String ftpPass) throws InvalidProgramArgumentException {
        final String ftpHostToUse;
        if(!ftpHost.startsWith(FTP_PROTOCOL)){
            ftpHostToUse = FTP_PROTOCOL +ftpHost;
        }else{
            ftpHostToUse = ftpHost;
        }

        final URL url;
        try {
            url = new URL(ftpHostToUse);
        } catch (MalformedURLException e) {
            //won't happen due to above check
            throw new InvalidProgramArgumentException(e.getMessage());
        }

        final FtpClientConfig ftpConfig = FtpClientConfigImpl.newFtpConfig(url.getHost());
        ftpConfig.setPort(url.getPort());
        ftpConfig.setUser(ftpUser);
        ftpConfig.setPass(ftpPass);

        final String dirPart = getDirPart(imageName);
        if(dirPart != null){
            ftpConfig.setDirectory(dirPart);
        }
        return ftpConfig;
    }

    /**
     * If any of the options from ImportExportUtilities.ALL_COMPONENTS) is included in the args, return true
     * as we are doing a selective backup
     * @param args program parameters converted to a map of string to their values
     * @return true if a selective backup should be done, false otherwise
     */
    static boolean isSelectiveBackup(final Map<String, String> args){
        for(CommandLineOption option: CommonCommandLineOptions.ALL_COMPONENTS){
            if(args.containsKey(option.getName())) return true;
        }
        return false;
    }

    static Pair<String, String> getDbHostAndPortPair(final String host){
        if (host == null) throw new NullPointerException("host cannot be null");
        if(host.trim().isEmpty()) throw new IllegalArgumentException("host cannot be an empty string or just spaces");

        if (host.indexOf(':') != -1) {
            String [] parts = host.split(":",2);
            return new Pair<String, String>(parts[0], parts[1]);
        } else {
            return new Pair<String, String>(host, "3306");
        }
    }

    /**
     * @param path  either a relative path or an absolute path
     * @return if <code>path</code> is absolute, it is simply returned;
     *         if <code>path</code> is relative, it is resolved by prefixing it with the directory specified by the system property {@link #BASE_DIR_PROPERTY};
     *         if <code>path</code> is null, returns null
     * @throws RuntimeException if the system property {@link #BASE_DIR_PROPERTY} is not set when it is needed
     */
    public static String getAbsolutePath(final String path) {
        if (path == null) return null;

        if (new File(path).isAbsolute()) {
            return path;
        }

        final String baseDir = System.getProperty(BASE_DIR_PROPERTY);
        if (baseDir == null) {
            throw new RuntimeException("System property \"" + BASE_DIR_PROPERTY + "\" not set.");
        }
        return baseDir + File.separator + path;
    }

    /**
     * Determine if the given host is local. Any exceptions are logged and false returned.
     * @param host String name of the host to check if it is local or not
     * @return true if host is local. False otherwise. False if any exception occurs when looking up the host.
     */
    public static boolean isHostLocal(final String host) {
        try{
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress( InetAddress.getByName(host) );
            if ( networkInterface != null ) return true;
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING,  "Could not look up database host: " + e.getMessage());
        } catch (SocketException e) {
            logger.log(Level.WARNING,  "Socket exception looking up database host: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieve a DatabaseConfig object using the supplied node.properties file and omp.dat file. This provides all
     * information required to connect to the database represented in node.properties
     * @param nodePropsFile node.properties
     * @param ompFile omp.dat
     * @return DatabaseConfig representing the db in node.properties
     * @throws java.io.IOException if any exception occurs while reading the supplied files
     */
    public static DatabaseConfig getNodeConfig(final File nodePropsFile, final File ompFile) throws IOException {
        final MasterPasswordManager decryptor = ompFile.exists() ?
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) :
                null;

        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        final DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new CausedIOException("Database configuration not found.");
        }

        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        logger.info("Using database host '" + config.getHost() + "'.");
        logger.info("Using database port '" + config.getPort() + "'.");
        logger.info("Using database name '" + config.getName() + "'.");
        logger.info("Using database user '" + config.getNodeUsername() + "'.");
        return config;
    }

    /**
     * Validate that we have only received command line arguments that we expect. No validation of values
     * is done.
     * @throws InvalidProgramArgumentException, if any unexpected argument is found
     */
    public static void softArgumentValidation(final String[] args,
                                              final List<CommandLineOption> validOptions,
                                              final List<CommandLineOption> ignoredOptions)
            throws InvalidProgramArgumentException{
        if(args == null) throw new NullPointerException("args cannot be null");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");

        //skip the first argument, because it's already used to determine the utility type (export/import)
        for (int i = 1; i < args.length; i = i + 2) {
            final String argument = args[i];

            ARGUMENT_TYPE argType = determineArgumentType(argument, validOptions, ignoredOptions);
            if (argType == ARGUMENT_TYPE.INVALID_OPTION) {
                throw new InvalidProgramArgumentException("option " + argument + " is invalid");
            }
        }
    }

    /**
     * See if a particular argument is included in the command line arguments
     * @param args
     * @param checkArgument
     * @param validOptions
     * @param ignoredOptions
     * @return
     * @throws InvalidProgramArgumentException
     */
    public static boolean checkArgumentExistence(final String[] args,
                                                 final String checkArgument,
                                                 final List<CommandLineOption> validOptions,
                                                 final List<CommandLineOption> ignoredOptions)
            throws InvalidProgramArgumentException{
        if(args == null) throw new NullPointerException("args cannot be null");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");

        //skip the first argument, because it's already used to determine the utility type (export/import)
        for (int i = 1; i < args.length; i++) {
            final String argument = args[i];

            ARGUMENT_TYPE argType = determineArgumentType(argument, validOptions, ignoredOptions);

            switch(argType){
                case VALID_OPTION:
                    if(argument.equals(checkArgument)){
                        return true;
                    }
            }
        }
        return false;
    }

    /**
     * This is indended pre any call to getAndValidateCommandLineOptions, where we need just one argument.
     * This is used to get the -image parameter during restore, as it's required
     * @param args
     * @param requiredOption
     * @return String value for argument, never null, as an exception will have been thrown if it's not found
     */
    public static String getAndValidateSingleArgument(final String[] args,
                                                      final CommandLineOption requiredOption,
                                                      final List<CommandLineOption> validOptions,
                                                      final List<CommandLineOption> ignoredOptions)
            throws InvalidProgramArgumentException {
        if(args == null) throw new NullPointerException("args cannot be null");
        if(args.length == 0) throw new IllegalArgumentException("args contains no values");
        if(requiredOption == null) throw new NullPointerException("requiredOption cannot be null");
        if(requiredOption.isHasNoValue()) throw new IllegalArgumentException("requiredOption must require a value");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");

        //skip the first argument, because it's already used to determine the utility type (export/import)
        for (int i = 1; i < args.length; i++) {
            final String argument = args[i];

            ARGUMENT_TYPE argType = determineArgumentType(argument, validOptions, ignoredOptions);
            switch(argType){
                case VALID_OPTION:
                    if (argument.equals(requiredOption.getName())) {
                        if ((i + 1) > args.length)
                            throw new InvalidProgramArgumentException("option " + argument + " expects a value");
                        String value = args[i + 1];
                        if (value == null || value.isEmpty()) {
                            throw new InvalidProgramArgumentException("option " + argument + " expects a value");
                        }
                        //do a sanity check that it's actually a value and not an option
                        ARGUMENT_TYPE testType = determineArgumentType(value, validOptions, ignoredOptions);
                        if(testType != ARGUMENT_TYPE.VALUE){
                            throw new InvalidProgramArgumentException("option " + argument + " expects a value");
                        }
                        return value;
                    }
                    break;
                case INVALID_OPTION:
                    throw new InvalidProgramArgumentException("option " + argument + " is invalid");
            }
        }

        throw new InvalidProgramArgumentException("missing option "
                + requiredOption.getName()
                + ", required for exporting this image");
    }
    
    /**
     * Convert command line parameters into a map, where any command line value which requires a value, is mapped to
     * it's value in the returned map.
     * validOptions lists which arguments to expect, each CommandLineOption within it, specifies whether or not it
     * must be followed by a parameter on the command line.
     * ignoredOptions is provided for backwards comapability where an old script may supply a parameter which has been
     * removed, but for which we will not error out because of //todo [Donal] look at removing this
     * @param args  The command line arguments. Cannot be null
     * @param validOptions list of valid command line argument. Canot be null
     * @param ignoredOptions list of arguments which are allowed, but will be ignored. Cannot be null
     * @return  A map of the command line argumetents to their values, if any
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException
     * If an unexpected arguments is found, or an argument is missing it's value, or the value of an argument represents
     * a file, and that file does not exist or cannot be written to
     */
    public static Map<String, String> getAndValidateCommandLineOptions(final String[] args,
                                                                       final List<CommandLineOption> validOptions,
                                                                       final List<CommandLineOption> ignoredOptions)
            throws BackupRestoreLauncher.InvalidProgramArgumentException {
        if(args == null) throw new NullPointerException("args cannot be null");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");
        
        final Map<String, String> arguments = new HashMap<String, String>();
        int index = 1;  //skip the first argument, because it's already used to determine the utility type (export/import)

        //todo [Donal] refactor loop
        while (index < args.length) {
            final String argument = args[index];

            switch (determineArgumentType(argument, validOptions, ignoredOptions)) {
                case VALID_OPTION:
                    if (optionHasNoValue(argument, validOptions)) {
                        arguments.put(argument, "");
                        index++;
                    } else if (!optionIsValuePath(argument, validOptions)) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions))) {
                            arguments.put(argument, args[index+1]);
                            index = index + 2;  //already processed one, so we'll need to advance to the next after the processed
                        } else {
                            //expecting a value for this parameter but there was none
                            throw new InvalidProgramArgumentException("option " + argument + " expects value");
                        }
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args, validOptions, ignoredOptions);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions)) && index+1 < nextOptionIndex) {
                            String fullValue = getFullValue(args, index+1, nextOptionIndex);
                            arguments.put(argument, fullValue);
                            index = nextOptionIndex;
                        } else {
                            throw new InvalidProgramArgumentException("option " + argument + " expects value");
                        }
                    }
                    break;
                case IGNORED_OPTION:
                    if (optionHasNoValue(argument, ignoredOptions)) {
                        logger.info("Option '" + argument + "' is ignored.");
                        index++;
                    } else if (!optionIsValuePath(argument, ignoredOptions)) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions))) {
                            index = index + 2;
                        } else {
                            //we dont care, just continue to process
                            index++;
                        }
                        logger.info("Option '" + argument + " " + args[index] + "' is ignored.");
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args, validOptions, ignoredOptions);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions)) && index+1 < nextOptionIndex) {
                            String fullValue = getFullValue(args, index+1, nextOptionIndex);
                            index = nextOptionIndex;
                            logger.info("Option '" + argument + " " + fullValue + "' is ignored.");
                        } else {
                            //we dont care, just continue to process
                            index++;
                            logger.info("Option '" + argument + "' is ignored.");
                        }
                    }
                    break;
                default:
                    throw new BackupRestoreLauncher.InvalidProgramArgumentException("option " + argument + " is invalid");
            }
        }

        return arguments;
    }

    /**
     * Verify that the given database configuration can be used to connect to a database
     *
     * @param config    Database configuration informaiton. Cannot be null
     * @param isRootAccount Flag to use as root account or gateway account
     * @throws NullPointerException if config is null
     * @throws java.sql.SQLException if any database access error occurs when trying to obtain a connection. This will
     * wrap the underlying SQLException thrown by the database driver
     */
    public static void verifyDatabaseConnection(final DatabaseConfig config,
                                                final boolean isRootAccount) throws SQLException {
        if (config == null) throw new NullPointerException("config cannot be null");

        Connection connection = null;
        try {
            connection = (new DBActions()).getConnection(config, isRootAccount);
        } catch (SQLException sqle) {
            throw new SQLException("cannot connect to database host '" + config.getHost()
                    + "' in database '" + config.getName() + "' with user '"
                    + (isRootAccount ? config.getDatabaseAdminUsername() : config.getNodeUsername()) + "'", sqle);
        } finally{
            ResourceUtils.closeQuietly(connection);
        }
    }

    /**
     * Confirm that the file fileName exists
     * @param fileName the file name to confirm exists
     * @throws IllegalArgumentException if the file fileName does not exist
     */
    public static void throwIfFileExists(final String fileName){
        if (fileName == null) throw new NullPointerException("fileName cannot be null");
        final File file = new File(fileName);
        if(file.exists()) throw new IllegalArgumentException("file '" + fileName + "' should not exist");
    }

    /**
     * Confirm that the file fileName, does not exists
     * @param fileName the file name to confirm does not exists
     * @throws IllegalArgumentException if the file fileName does exist
     */
    public static void throwIfFileDoesNotExist(final String fileName){
        if (fileName == null) throw new NullPointerException("fileName cannot be null");
        final File file = new File(fileName);
        if(!file.exists()) throw new IllegalArgumentException("file '" + fileName + "' should exist");
    }

    /**
     * Test if can write and create the file.
     *
     * @param fileName  The file to be created
     * @throws IOException
     * @throws NullPointerException if fileName is null
     * @throws IllegalArgumentException if fileName alredy exists
     */
    public static void verifyCanWriteFile(final String fileName) throws IOException {
        if(fileName == null) throw new NullPointerException("fileName cannot be null");
        throwIfFileExists(fileName);
        
        final File file = new File(fileName);
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write("I can write to this file".getBytes());
            //file exists
        } finally {
            ResourceUtils.closeQuietly(fos);
            file.delete();
        }
    }

    /**
     * When migrating and a pre existing database is being used, then the version of the database must match
     * the version of the installed SSG.
     *
     * @param version   The database version to be compared to the installed SSG version
     * @throws UnsupportedOperationException if the version do not match
     */
    public static void throwIfDbVersionDoesNotMatchSSG(final String version){
        final String ssgVersion = BuildInfo.getProductVersion();
        if (!ssgVersion.equals(version)) {
             throw new UnsupportedOperationException("Database version '"+version+"' does not match the SSG version " +
                     "'"+ssgVersion+"'. Values must match to Import using -migrate");
        }
    }

    /**
     * Verify if the database exists.
     *
     * @param host  The database host
     * @param dbName    The database name
     * @param port  The port
     * @param username  The username to be used for login
     * @param password  The password for the specified username
     * @return
     */
    public static boolean verifyDatabaseExists(final String host,
                                               final String dbName,
                                               final int port,
                                               final String username,
                                               final String password) {
        Connection c = null;
        Statement s = null;
        ResultSet rs = null;
        try {
            DatabaseConfig config = new DatabaseConfig(host, port, dbName, username, password);
            c = new DBActions().getConnection(config, false);
            s = c.createStatement();
            rs = s.executeQuery("select * from hibernate_unique_key");
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(s);
            ResourceUtils.closeQuietly(c);
        }
        return false;
    }

    /**
     * Create a directory in the system temp folder. The returned string represents a directory that exists. Caller
     * should delete the directory when finished. The created directory will be prefixed with "ssg_backup_restore"
     * @return String path of the created directory
     * @throws IOException if the temp directory cannot be created
     */
    public static String createTmpDirectory() throws IOException {
        final File tmp = File.createTempFile("ssg_backup_restore", "tmp");
        tmp.delete();
        tmp.mkdir();
        logger.info("created temporary directory at " + tmp.getPath());
        return tmp.getPath();
    }

    /**
     * Traverses the list of options and determine the length of the longest option name.
     *
     * @param options   The list of options
     * @return          The length of the longest option name
     */
    public static int getLargestNameStringSize(final List<CommandLineOption> options) {
        int largestStringSize = 0;
        for (CommandLineOption option : options) {
            if (option.getName() != null && option.getName().length() > largestStringSize) {
                largestStringSize = option.getName().length();
            }
        }
        return largestStringSize;
    }

    /**
     * Creates the padding spaces string
     *
     * @param space The number of spaces to create
     * @return  The space string
     */
    public static String createSpace(final int space) {
        StringBuffer spaces = new StringBuffer();
        for (int i=0; i <= space; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    /**
     * Verify that a directory exists
     * @param directoryName name of the directory to check if it exists or not. Cannot be null
     * @throws IllegalArgumentException if the directoryName is null, directoryName does not exist or does not represent a directory
     * @throws NullPointerException if directory name is null
     */
    public static void verifyDirExistence(final String directoryName){
        if (directoryName == null) throw new NullPointerException("directoryName cannot be null");

        // non empty check allows the utility to work with backup servlet temporary files
        final File file = new File(directoryName);
        if(!file.exists()){
            throw new IllegalArgumentException("Directory '" + directoryName + "' does not exist");
        }

        if(!file.isDirectory()){
            throw new IllegalArgumentException("File '" + directoryName + "' is not a directory");
        }
    }

    /**
     * Copy the files from destinationDir to sourceDir. Does not copy directories
     * @param destinationDir The directory containing the files to copy
     * @param sourceDir The directory to copy the files to
     * @param fileFilter FilenameFilter can be null. Use to filter the files in sourceDir
     * @throws IOException if any exception occurs while copying the files
     */
    public static void copyFiles(final File sourceDir,
                                 final File destinationDir,
                                 final FilenameFilter fileFilter,
                                 final boolean isVerbose,
                                 final PrintStream printStream)
            throws IOException{
        if(!sourceDir.exists())
            throw new IllegalArgumentException("Directory '"+sourceDir.getAbsolutePath()+"' does not exist");
        if(!sourceDir.isDirectory())
            throw new IllegalArgumentException("Directory '"+sourceDir.getAbsolutePath()+"' is not a directory");

        if(!destinationDir.exists())
            throw new IllegalArgumentException("Directory '"+destinationDir.getAbsolutePath()+"' does not exist");
        if(!destinationDir.isDirectory())
            throw new IllegalArgumentException("Directory '"+destinationDir.getAbsolutePath()+"' is not a directory");

        final String [] filesToCopy = sourceDir.list(fileFilter);

        for ( final String filename : filesToCopy ) {
            final File file = new File(sourceDir, filename);
            if ( file.exists() && !file.isDirectory()) {
                final File destFile = new File(destinationDir.getAbsolutePath() + File.separator + file.getName());
                if(destFile.exists()){
                    final boolean success = destFile.delete();
                    final String msg = "File '" + destFile.getAbsolutePath()+"' exists. File was "
                            + ((success)? "successfully": "unsuccessfully") + " deleted";
                    logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                }
                FileUtils.copyFile(file, destFile);
                final String msg = "File '" + file.getAbsolutePath()+"' copied to '" + destFile.getAbsolutePath()+"'";
                logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }
        }
    }

    public static void copyDir(final File sourceDir, final File destDir) throws IOException {
        copyDir(sourceDir,  destDir, null, false, null);
    }

    public static void copyDir(final File sourceDir, final File destDir, final FilenameFilter sourceFilter)
            throws IOException {
        copyDir(sourceDir,  destDir, sourceFilter, false, null);
    }

    public static void copyDir(final File sourceDir,
                               final File destDir,
                               final FilenameFilter sourceFilter,
                               final boolean isVerbose,
                               final PrintStream printStream) throws IOException {
        if(!sourceDir.exists())
            throw new IllegalArgumentException("Directory '"+sourceDir.getAbsolutePath()+"' does not exist");
        if(!sourceDir.isDirectory())
            throw new IllegalArgumentException("Directory '"+sourceDir.getAbsolutePath()+"' is not a directory");

        if(destDir.exists()){
            //if it exists, delete it
            if(!FileUtils.deleteDirContents(destDir)){
                throw new IllegalStateException("Directory '" + destDir.getAbsolutePath()
                        + "' could not be deleted before copying files");
            }
        }

        //make sure destination exists
        if(!destDir.exists()){
            if(!destDir.mkdir()) {
                throw new IllegalStateException("Directory '" + destDir.getAbsolutePath()
                        + "' does not exist and could not be created");
            }
        }

        //it is ok for sourceFilter to be null, if it is, then listFiles just returns everything in the dir
        final File [] allFiles = sourceDir.listFiles(sourceFilter);
        for(final  File f: allFiles){
            if(f.isDirectory()) {
                //not passing the source filter down, it's only for the intital directory, for now anyway
                copyDir(f, new File(destDir, f.getName()), null, isVerbose, printStream);
            } else {
                final File destFile = new File(destDir.getAbsolutePath() + File.separator + f.getName());
                FileUtils.copyFile(f, destFile);
                String msg = "File '" + f.getAbsolutePath()+"' copied to '" + destFile.getAbsolutePath()+"'";
                logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }
        }
    }

    /**
     * Get the directory part of a path and filename string e.g. /home/layer7/temp/image1.zip
     * The path information is optional
     *
     * This method supports both the unix / and the windows \ as the user may supply a unix or windows path name
     * when the export / import is used in conjection with ftp
     * @param imageName The String name to extract the path information from
     * @return The path information not including the final / or \, null if no path information in the imageName.
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty
     */
    public static String getDirPart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

        if(imageName.equals("/")) return imageName;
        if(imageName.equals("\\")) throw new IllegalArgumentException("\\ is an invalid imageName");
        
        int lastIndex = imageName.lastIndexOf("/");
        if(lastIndex == -1) lastIndex = imageName.lastIndexOf("\\");
        if(lastIndex != -1){
            return imageName.substring(0, lastIndex);
        }
        return null;
    }

    /**
     * Get the file part of a path and filename string e.g. /home/layer7/temp/image1.zip
     * The path information is optional
     * If the file name only contains a path ending in either / or \, then an empty string will be returned
     *
     * This method supports both the unix / and the windows \ as the user may supply a unix or windows path name
     * when the export / import is used in conjection with ftp
     * @param imageName The String name to extract the file name information from
     * @return The file name, never null
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty or is equal to / or \
     */
    public static String getFilePart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

        if(imageName.equals("/")) throw new IllegalArgumentException("/ is an invalid imageName");
        if(imageName.equals("\\")) throw new IllegalArgumentException("\\ is an invalid imageName");

        int lastIndex = imageName.lastIndexOf("/");
        if(lastIndex == -1) lastIndex = imageName.lastIndexOf("\\");
        if(lastIndex == -1){
            return imageName;
        }
        return imageName.substring(lastIndex + 1, imageName.length());
    }

    /**
     * Determines if the provided option has path as an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name has path as option, otherwise FALSE.
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException     The option name is not found in the available option list
     */
    private static boolean optionIsValuePath(String optionName,
                                             final List<CommandLineOption> options)
            throws BackupRestoreLauncher.InvalidProgramArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.getName().equals(optionName)) {
                return commandLineOption.isValuePath();
            }
        }
        //this is a programming error
        throw new IllegalArgumentException("option " + optionName + " does not exist in options");
    }

    /**
     * Determines if the provided option expects a value
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name expects a value, otherwise FALSE.
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException     The option name is not found in the available option list
     */
    private static boolean optionHasNoValue(final String optionName,
                                            final List<CommandLineOption> options)
            throws InvalidProgramArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.getName().equals(optionName)) {
                return commandLineOption.isHasNoValue();
            }
        }
        //this is a programming error
        throw new IllegalArgumentException("option " + optionName + " does not exist in options");
    }

    /**
     * Determines if the provided option name is an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name is an option, otherwise FALSE.
     */
    private static boolean isOption(final String optionName, final List<CommandLineOption> options) {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.getName().equals(optionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the argument type as either an option or a value to a particular option
     *
     * @param argument  argument for comparison
     * @return  The ARGUMENT_TYPE of the parse argument.
     */
    private static ARGUMENT_TYPE determineArgumentType(final String argument,
                                                       final List<CommandLineOption> validOptions,
                                                       final List<CommandLineOption> ignoredOptions) {
        if (argument != null && argument.startsWith("-")) {
            if (isOption(argument, validOptions))
                return ARGUMENT_TYPE.VALID_OPTION;
            else if (isOption(argument, ignoredOptions))
                return ARGUMENT_TYPE.IGNORED_OPTION;
            else
                return ARGUMENT_TYPE.INVALID_OPTION;
        } else {
            return ARGUMENT_TYPE.VALUE;
        }
    }

    /**
     * Finds the index location of the next option if available
     *
     * @param startIndex    The starting index of the location to start searching.  DO NOT start at the index of the
     *                      previous option index location.
     *                      eg. if parameter were "-p1 val1 -p2 val2 -p3 ...   If p1 was the previous option, then the
     *                      start index should be at val1
     * @param args          The list of arguments
     * @return              The index location of the next available option found, if reached to end of argument list, then
     *                      it'll return the size of the argument list
     */
    private static int findIndexOfNextOption(final int startIndex,
                                             final String[] args,
                                             final List<CommandLineOption> validOptions,
                                             final List<CommandLineOption> ignoredOptions) {
        int nextOptionIndex = startIndex;
        while (args != null && nextOptionIndex+1 < args.length) {
            if (!ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[++nextOptionIndex], validOptions, ignoredOptions))) {
                return nextOptionIndex;
            }
        }
        return args != null ? args.length : nextOptionIndex+1;
    }

    /**
     * Parses the values given by the provided boundaries.
     *
     * @param args  The list of arguments
     * @param startIndex    The starting index
     * @param endIndex      The ending index, does not include this index
     * @return      The parsed value withing the boundaries.
     */
    private static String getFullValue(final String[] args, final int startIndex, final int endIndex) {
        final StringBuffer buffer = new StringBuffer();
        for (int i=startIndex; i < args.length && i < endIndex; i++) {
            buffer.append(args[i] + " ");
        }
        return buffer.toString() != null ? buffer.toString().trim() : buffer.toString();
    }

    /**
     * Filter a list of components based on the presence of component names as keys in programFlagsAndValues
     * @param allComponents
     * @param programFlagsAndValues
     * @param <UT>
     * @return
     */
    static <UT extends SsgComponent> List<UT> filterComponents( final List<UT> allComponents,
                                                 final Map<String, String> programFlagsAndValues){

        final List <UT> returnList = new ArrayList<UT>();

        for(UT comp: allComponents){

            if(comp.getComponentType() == ImportExportUtilities.ComponentType.VERSION){
                //We always include the version component
                returnList.add(comp);
                continue;
            }

            if(programFlagsAndValues.containsKey("-"+comp.getComponentType().getComponentName())){
                returnList.add(comp);
            }
        }

        return returnList;
    }

    /**
     * Method for logging as this is used everywhere to both log and print if we have a PrintStream and
     * the -v option was used
     *
     * Calls other logAndPrintMessage with newLien = true
     * @param log
     * @param level
     * @param message
     * @param verbose
     * @param printStream
     */
    static void logAndPrintMessage(final Logger log,
                                   final Level level,
                                   final String message,
                                   final boolean verbose,
                                   final PrintStream printStream){
        logAndPrintMessage(log, level, message, verbose, printStream, true);
    }

    /**
     * Method for logging as this is used everywhere to both log and print if we have a PrintStream and
     * the -v option was used
     *
     * @param log
     * @param level
     * @param message
     * @param verbose
     * @param printStream
     * @param newLine if true, the output to the PrintStream will be on a new line
     */
    static void logAndPrintMessage(final Logger log,
                                   final Level level,
                                   final String message,
                                   final boolean verbose,
                                   final PrintStream printStream,
                                   final boolean newLine){
        if(verbose && printStream != null){
            if(newLine) System.out.println(message);
            else System.out.print(message); 
        }
        log.log(level, message);
    }

    /**
     * Gets running SSG based on the timestamp that is updated by each SSG.  This method makes it best to determine
     * running SSG(s).  Because we are only comparing the timestamps that are updated by SSG(s), there is no guarantee
     * that a gateway might have been starting to start up while checking is performed.
     *
     * @param config            Database configuration to be used for connect to database
     * @param sleepTime         The time to sleep in between each sample query to determine the active SSG(s).
     *  Default time is set to 10secs. The value must be greater than 10 secs otherwise it'll revert to default 10 secs.
     * @param verbose
     * @param printStream
     * @return                  List of SSG(s) name that are considered to be running.  Doesn't guarantee that the gateway
     *                          is actually running or shutdown.  Will never return NULL.
     * @throws SQLException
     * @throws InterruptedException
     */
    static List<String> getRunningSSG(final DatabaseConfig config,
                                      final long sleepTime,
                                      final boolean verbose,
                                      final PrintStream printStream)
            throws SQLException, InterruptedException {
        final String gatewayList = "SELECT nodeid, statustimestamp, name FROM cluster_info";
        final long intervalSleepTime = sleepTime <= 10000 ? 10000 : sleepTime;
        final Map<String, Long> availableSsg = new HashMap<String, Long>();
        final List<String> runningSSG = new ArrayList<String>();

        if (verbose && printStream != null) System.out.print("Making sure targets are offline .");

        Connection connection = null;
        Statement statement = null;
        ResultSet results = null;
        try {
            connection = (new DBActions()).getConnection(config, false);
            statement = connection.createStatement();

            //cosmetic dots - only do this when someone is actually watching, by virtue of -v or api
            if (verbose && printStream != null) {
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

            //cosmetic dots - again only when someone is watching
            if ((verbose && printStream != null)) {
                for (int i = 0; i < 3; i++) {
                    Thread.sleep(500);
                    System.out.print(".");
                }
                System.out.println(" Done");
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
}