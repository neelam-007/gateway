package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.util.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * </p>
 *
 * Utility class for Exporting and Importing backup image zip files
 *
 */
public class ImportExportUtilities {
    private static final Logger logger = Logger.getLogger(ImportExportUtilities.class.getName());
    public static final String LOCAL_DB_ONLY = "com.l7tech.config.backup.localDbOnly";
    private static final String SSEM_PID_FILE = "/var/run/ssemd.pid";

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
    public static final List<String> CONFIG_FILES = Collections.unmodifiableList(Arrays.asList(
            SSGLOG_PROPERTIES,
            SYSTEM_PROPERTIES,
            NODE_PROPERTIES,
            OMP_DAT));

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
    public static final String LIB_EXT_DIR = "runtime/lib/ext";
    public static final String POST_FIVE_O_DEFAULT_BACKUP_FOLDER = "config/backup/images";
    static final String UNIQUE_TIMESTAMP = "yyyyMMddHHmmss";
    private static final String FTP_PROTOCOL = "ftp://";

    public static final String SSG_DB_XML = "/config/etc/db/ssg.xml";

    /**
     * If system property com.l7tech.config.backup.localDbOnly is set to false, then any host is ok, otherwise
     * the host must be local
     * @param host string host to see if its ok to restore to
     * @return true if can restore to the host, false otherwise
     */
    static boolean isDatabaseAvailableForBackupRestore(final String host){
        return !ConfigFactory.getBooleanProperty( LOCAL_DB_ONLY, true )
                || isHostLocal(host);
    }
    
    static void throwifEsmIsRunning() {
        final File esmPid = new File(SSEM_PID_FILE);
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
        final File file = new File(path);
        try {
            final FileOutputStream fos = new FileOutputStream(file);
            fos.close();
        } catch (Exception e) {
            return false;
        } finally {
            final boolean deleted = file.delete();
            if(!deleted) logger.log(Level.WARNING, "Cannot delete file " + file.getAbsolutePath());
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
     * @return List<String> of each line of text found in the file. Can be empty, never null
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
     * Get the list of excluded files. The file names returned are the absolute path file names extracted from the
     * file excludeFilePath.
     * @param ssgHome SSG installation directory
     * @param excludeFilePath String path to file name which contains the list of files to exclude
     * @return List of Strings, each is a file which should not be restored.
     * @throws IOException if any file read errors.
     */
    static List<String> getExcludedFiles(final File ssgHome, final String excludeFilePath) {
        final List<String> excludedFiles = new ArrayList<String>();
        final File excludeFile = new File(ssgHome, excludeFilePath);
        if(excludeFile.exists() && excludeFile.isFile()) {
            try {
                excludedFiles.addAll(ImportExportUtilities.processFile(excludeFile));
            } catch (IOException e) {
                logger.log(Level.WARNING, "Problem reading excluded file '" + excludeFile.getAbsolutePath()+"'. " +
                        ExceptionUtils.getMessage(e));
            }
        }

        return excludedFiles;
    }

    /**
     * Classloader designed to ONLY load BuildInfo from Gateway jar from the standard install directory of
     * /opt/SecureSpan/Gateway/runtime/Gateway.jar
     */
    private static class GatewayJarClassLoader extends ClassLoader{
        private final ClassLoader loader;

        GatewayJarClassLoader(final File gatewayJarFile) throws MalformedURLException {
            final URL gatewayJar = gatewayJarFile.toURI().toURL();
            loader = new URLClassLoader(new URL[]{gatewayJar}, null);
        }

        @Override
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
     * @return an int array with the major, minor and subversion values for the installed SSG as indexes 0, 1 and 2
     * @throws RuntimeException if the SSG installation cannot be found, based on the existence of Gateway.jar. Also
     * thrown if it's not possible to determine the SSG version from the SSG installation
     */
    public static int [] throwIfLessThanFiveO(final File gatewayJarFile){
        try {
            if(!gatewayJarFile.exists()) throw new IllegalStateException("Cannot find Gateway installation");

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

            if(majorVersion < 5) throw new UnsupportedOperationException("Pre 5.0 Gateway installations are not supported");

            return new int[]{majorVersion, minorVersion, subMinorVersion};

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Cannot determine Gateway version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine Gateway version: " + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Cannot determine Gateway version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine Gateway version: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot determine Gateway version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine Gateway version: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Cannot determine Gateway version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine Gateway version: " + e.getMessage());
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Cannot determine Gateway version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine Gateway version: " + e.getMessage());
        }
    }

    /**
     * ComponentType is used for the directory names in the image zip file, and for processing selective
     * backup component parameters
     */
    protected enum ComponentType{
        OS("os", "Operating System files"),
        CONFIG("config", "Gateway Config"),
        MAINDB("maindb", "Database backup and data excluding audit data"),
        AUDITS("audits", "Database audit data"),
        CA("ca", "Custom Assertions"),
        MA("ma", "Modular Assertions"),
        EXT("ext", "Gateway/runtime/lib/ext"),
        VERSION("version", "Gateway Version Information"),
        ESM("esm", "Enterprise Service Manager"),
        //Only required for migrate when only config is being restored
        NODE_IDENTITY("node identity", "node.properties & omp.dat");

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
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException if an
     * incomplete set of ftp parameters were supplied, or if the ftp host name is invalid
     */
    public static boolean checkAndValidateFtpParams(final Map<String, String> allParams)
            throws InvalidProgramArgumentException {
        //check if ftp requested
        for(Map.Entry<String, String> entry: allParams.entrySet()){
            if(entry.getKey().startsWith("-ftp")){
                //make sure they are all there
                for(final CommandLineOption clo: CommonCommandLineOptions.ALL_FTP_OPTIONS){
                    if(!allParams.containsKey(clo.getName()))
                        throw new InvalidProgramArgumentException("Missing argument: " + clo.getName());
                    
                    if(clo == CommonCommandLineOptions.FTP_HOST){
                        String hostName = allParams.get(CommonCommandLineOptions.FTP_HOST.getName());
                        try {
                            if(!hostName.startsWith(FTP_PROTOCOL)) hostName = FTP_PROTOCOL +hostName;
                            new URL(hostName);//validate its ok
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
     * as we are doing a selective backup / restore
     * @param args program parameters converted to a map of string to their values
     * @return true if a selective backup should be done, false otherwise
     */
    static boolean isSelective(final Map<String, String> args){
        for(CommandLineOption option: CommonCommandLineOptions.ALL_COMPONENTS){
            if(args.containsKey(option.getName())) return true;
        }
        return false;
    }

    static Pair<String, String> getDbHostAndPortPair(final String host){
        if (host == null) throw new NullPointerException("host cannot be null");
        if(host.trim().isEmpty()) throw new IllegalArgumentException("host cannot be an empty string or just spaces");

        Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(host, "3306");
        if (hostAndPort.left != null && hostAndPort.left.startsWith("[") && hostAndPort.left.endsWith("]")) {
            return new Pair<String, String>(hostAndPort.left.substring(1, hostAndPort.left.length()-1), hostAndPort.right);
        } else {
            return hostAndPort;
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

        final String baseDir = ConfigFactory.getProperty( BASE_DIR_PROPERTY );
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
    static boolean isHostLocal(final String host) {
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
     * @param nodePropsFile node.properties. Cannot be null, must exist
     * @param ompFile omp.dat. Cannot be null, may not exist
     * @return DatabaseConfig representing the db in node.properties
     * @throws java.io.IOException if any exception occurs while reading the supplied files
     */
    public static DatabaseConfig getDatabaseConfig(final File nodePropsFile, final File ompFile) throws IOException {
        if (nodePropsFile == null) throw new NullPointerException("nodePropsFile cannot be null");
        if (ompFile == null) throw new NullPointerException("ompFile cannot be null");
        
        final MasterPasswordManager decryptor = ompFile.exists() ?
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile)) :
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
     * This is intended pre any call to getAndValidateCommandLineOptions, where we need just one argument.
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
        if(!requiredOption.isHasValue()) throw new IllegalArgumentException("requiredOption must require a value");
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
                        if (value == null || value.trim().isEmpty()) {
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

        throw new InvalidProgramArgumentException("missing option " + requiredOption.getName());
    }
    
    /**
     * Convert command line parameters into a map, where any command line value which requires a value, is mapped to
     * it's value in the returned map.
     * validOptions lists which arguments to expect, each CommandLineOption within it, specifies whether or not it
     * must be followed by a parameter on the command line.
     * ignoredOptions is provided for backwards compatibility where an old script may supply a parameter which has been
     * removed, but for which we will not error out because of
     *
     * If the value of a parameter represents a file, this method does not validate that the file exists
     * 
     * @param args  The command line arguments. Cannot be null
     * @param validOptions list of valid command line argument. Cannot be null
     * @param ignoredOptions list of arguments which are allowed, but will be ignored. Cannot be null
     * @param isVerbose if true and printStream is not null, any ignore options will be written to the printStream
     * @param printStream, if not null, any info messages will be written to this stream, only required to advise
     * of ignored options
     * @return  A map of the command line arguments to their values, if any
     * @throws InvalidProgramArgumentException if an unexpected arguments is found, or an argument is missing it's value
     */
    public static Map<String, String> getAndValidateCommandLineOptions(final String[] args,
                                                                       final List<CommandLineOption> validOptions,
                                                                       final List<CommandLineOption> ignoredOptions,
                                                                       final boolean isVerbose,
                                                                       final PrintStream printStream)
            throws BackupRestoreLauncher.InvalidProgramArgumentException {
        if(args == null) throw new NullPointerException("args cannot be null");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");
        
        final Map<String, String> arguments = new HashMap<String, String>();
        int index = 1;  //skip the first argument, because it's already used to determine the utility type (export/import)

        while (index < args.length) {
            final String argument = args[index];
            index++;//move index along to either next param or param value
            switch (determineArgumentType(argument, validOptions, ignoredOptions)) {
                case VALID_OPTION:
                    if (!optionHasValue(argument, validOptions)) {
                        arguments.put(argument, "");
                    }else if(index < args.length  && ARGUMENT_TYPE.VALUE.equals(
                            determineArgumentType(args[index], validOptions, ignoredOptions))){
                        arguments.put(argument, args[index]);
                        index++;
                    }else{
                        throw new InvalidProgramArgumentException("option " + argument + " expects value");
                    }
                    break;
                case IGNORED_OPTION:
                    if (!optionHasValue(argument, ignoredOptions)) {
                        final String msg ="Option '" + argument + "' is ignored";
                        logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    }else if(index < args.length && ARGUMENT_TYPE.VALUE.equals(
                            determineArgumentType(args[index], validOptions, ignoredOptions))){
                        final String msg = "Option '" + argument + " " + args[index] + "' is ignored";
                        logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                        index++;
                    }else{
                        //it was supposed to have a value, but doesn't, but we still want the user to know
                        //the supplied option is ignored
                        final String msg ="Option '" + argument + "' is ignored";
                        logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
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
     * @param config    Database configuration information. Cannot be null
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
        if(file.exists()) throw new IllegalArgumentException("file '" + fileName + "' already exists");
    }

    /**
     * Test if can write and create the file.
     *
     * @param fileName  The file to be created
     * @throws IOException
     * @throws NullPointerException if fileName is null
     * @throws IllegalArgumentException if fileName already exists
     */
    public static void verifyCanWriteFile(final String fileName) throws IOException {
        if(fileName == null) throw new NullPointerException("fileName cannot be null");
        throwIfFileExists(fileName);
        
        final File file = new File(fileName);
        FileOutputStream fos = null;
        final String errorMessage = "Cannot write to file " + file.getAbsolutePath();
        try {
            final boolean fileCreated = file.createNewFile();
            if(!fileCreated) throw new IOException("Could not create file " + file.getAbsolutePath());
            fos = new FileOutputStream(file);
            fos.write("I can write to this file".getBytes());
            //file exists
        } catch (IOException e){
            throw new IOException(errorMessage);
        } finally {
            ResourceUtils.closeQuietly(fos);
            final boolean fileDeleted = file.delete();
            if(!fileDeleted) throw new IOException(errorMessage);
        }
    }

    /**
     * Create a directory in the system temp folder. The returned string represents a directory that exists. Caller
     * should delete the directory when finished. The created directory will be prefixed with "ssg_backup_restore"
     * @return String path of the created directory
     * @throws IOException if the temp directory cannot be created
     */
    public static String createTmpDirectory() throws IOException {
        final File tmp = FileUtils.createTempDirectory("ssg_backup_restore", "tmp", null, false);
        logger.info("Created temporary directory at " + tmp.getPath());
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
     * Copy the files from destinationDir to sourceDir. Does not copy directories
     * @param sourceDir The directory to copy the files to
     * @param destinationDir The directory containing the files to copy
     * @param fileFilter FilenameFilter can be null. Use to filter the files in sourceDir
     * @param isVerbose if true, verbose output will be written to the console
     * @param printStream if not null and isVerbose is true, each file copied will be written to the print stream
     * @throws IOException if any exception occurs while copying the files
     * @return true if any files were copied
     */
    public static boolean copyFiles(final File sourceDir,
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

        boolean filesCopied = false;
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
                filesCopied = true;
                final String msg = "File '" + file.getAbsolutePath()+"' copied to '" + destFile.getAbsolutePath()+"'";
                logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }
        }

        return filesCopied;
    }

    public static void copyDir(final File sourceDir, final File destDir) throws IOException {
        copyDir(sourceDir,  destDir, null, false, null);
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
                //not passing the source filter down, it's only for the initial directory, for now anyway
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
     * when the export / import is used in conjunction with ftp
     * @param imageName The String name to extract the path information from
     * @return The path information not including the final / or \, null if no path information in the imageName.
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty
     */
    public static String getDirPart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.trim().isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

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
     * when the export / import is used in conjunction with ftp
     * @param imageName The String name to extract the file name information from
     * @return The file name, never null
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty or is equal to / or \
     */
    public static String getFilePart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.trim().isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

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
     * Determines if the provided CommandLineOption expects a value
     *
     * @param optionName The name of the CommandLineOptions
     * @param options    The list of options used to find the option if in the list
     * @return true, if the option name expects a value, false otherwise
     * @throws InvalidProgramArgumentException
     *          if the option name is not found in options
     */
    private static boolean optionHasValue(final String optionName,
                                          final List<CommandLineOption> options)
            throws InvalidProgramArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.getName().equals(optionName)) {
                return commandLineOption.isHasValue();
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
     * @param validOptions the list of allowed values
     * @param ignoredOptions options which are not valid, but will be ignored, and their values if applicable, if
     * supplied
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
     * Filter a list of components based on the presence of component names as keys in programFlagsAndValues
     *
     * This should only be called if you know a selective backup / restore is required as the return list will
     * only contain components from allComponents which are explicitly supplied on the command line
     * @param allComponents the list of components to filter
     * @param programFlagsAndValues the map of command line arguments, which may contain selective component flags
     * e.g. -maindb or -audits etc.
     * @param <UT> implementation of SsgComponent
     * @return a list of SsgComponent s which were explicitly asked for on the command line
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
        logAndPrintMessage(log, level, "\t" + message, verbose, printStream, true);
    }

    static void logAndPrintMajorMessage(final Logger log,
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
            if(newLine) printStream.println(message);
            else printStream.print(message); 
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

        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "\tMaking sure targets are offline .",
                verbose, printStream, false);

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

    public static class UtilityResult {
        private final Status status;
        private final List<String> failedComponents;
        private final Exception exception;
        private final boolean rebootMaybeRequired;

        public enum Status{
            SUCCESS(),
            FAILURE(),
            PARTIAL_SUCCESS()
        }

        UtilityResult(final Status status,
                      final List<String> failedComponents,
                      final Exception exception) {
            this.status = status;
            this.failedComponents = failedComponents;
            this.exception = exception;
            this.rebootMaybeRequired = false;
        }

        UtilityResult(final Status status,
                      final List<String> failedComponents,
                      final boolean rebootMaybeRequired) {
            this.status = status;
            this.failedComponents = failedComponents;
            this.exception = null;
            this.rebootMaybeRequired = rebootMaybeRequired;
        }

        UtilityResult(final Status status){
            this.status = status;
            this.rebootMaybeRequired = false;
            this.failedComponents = null;
            this.exception = null;
        }

        UtilityResult(final Status status,
                      final boolean rebootMaybeRequired){
            this.status = status;
            this.rebootMaybeRequired = rebootMaybeRequired;
            this.failedComponents = null;
            this.exception = null;
        }

        public List<String> getFailedComponents() {
            return failedComponents;
        }

        public Status getStatus() {
            return status;
        }

        public Exception getException() {
            return exception;
        }

        public boolean isRebootMaybeRequired() {
            return rebootMaybeRequired;
        }
    }
}
