package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.FileUtils;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ResourceUtils;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.jscape.inet.ftp.FtpException;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.util.List;
import java.sql.SQLException;

import org.xml.sax.SAXException;

/**
 * Implemenation of the Restore public api
 * <p/>
 * This class is immutable
 */
final class BackupImpl implements Backup {

    private static final Logger logger = Logger.getLogger(BackupImpl.class.getName());

    private final PrintStream printStream;
    private final boolean isVerbose;
    private final File tmpOutputDirectory;
    private final File ssgHome;
    private final File confDir;
    private final File applianceHome;
    private final File esmHome;
    private final FtpClientConfig ftpConfig;
    private final String pathToImageZipFile;
    private final boolean isPostFiveO;
    //configuration files
    static final String AUDIT_TABLES_CONFIG = "config/backup/cfg/backup_tables_audit"; //this knowledge is ok as its ssg install specific

    /**
     *
     * @param secureSpanHome base instalation of Secure Span products e.g. /opt/SecureSpan
     * @param ftpConfig if not null, where the backup image will be ftp'd to
     * @param pathToImageZipFile can be to a local file, or relative to a log on directory on a ftp server. Cannnot
     * be null
     * @param isPostFiveO if true and pathToImageZipFile contains no path info, then the image will be placed into the
     * images folder in /opt/SecureSpan/Gateway/config/backup
     * @param verbose if true verbose output will be sent to the printStream, if it's not null
     * @param printStream the PrintStream to send verbose output to. Can be null
     * @throws BackupException if the BackupImpl cannot be instantiated
     */
    BackupImpl(final File secureSpanHome,
               final FtpClientConfig ftpConfig,
               final String pathToImageZipFile,
               final boolean isPostFiveO,
               final boolean verbose,
               final PrintStream printStream
    ) throws BackupException {

        if (secureSpanHome == null) throw new NullPointerException("secureSpanHome cannot be null");
        if (!secureSpanHome.exists()) throw new IllegalArgumentException("secureSpanHome directory does not exist");
        if (!secureSpanHome.isDirectory()) throw new IllegalArgumentException("secureSpanHome must be a directory");

        //check that the gateway exists
        final File testSsgHome = new File(secureSpanHome, ImportExportUtilities.GATEWAY);
        if(!testSsgHome.exists()) throw new IllegalArgumentException("Gateway installation not found");
        if(!testSsgHome.isDirectory()) throw new IllegalArgumentException("Gateway incorrectly installed");

        if (pathToImageZipFile == null) throw new NullPointerException("pathToImageZipFile cannot be null");
        if (pathToImageZipFile.trim().isEmpty()) throw new IllegalArgumentException("pathToImageZipFile cannot be null");

        try {
            tmpOutputDirectory = new File(ImportExportUtilities.createTmpDirectory());
        } catch (IOException e) {
            throw new BackupException("Could not create temp output directory for backup: " + e.getMessage());
        }
        
        ssgHome = testSsgHome;
        applianceHome = new File(secureSpanHome, ImportExportUtilities.APPLIANCE);//may not exist, thats ok
        esmHome = new File(secureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER);//may not exist, thats ok

        this.ftpConfig = ftpConfig;//ok if null
        this.pathToImageZipFile = pathToImageZipFile;
        this.printStream = printStream;
        isVerbose = verbose;
        confDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);

        this.isPostFiveO = isPostFiveO;
    }

    public ComponentResult backUpVersion() throws BackupException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpOutputDirectory + File.separator + ImportExportUtilities.VERSION);
            fos.write(BuildInfo.getFormalProductVersion().getBytes());
            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (IOException e) {
            throw new BackupException("Could not write version file: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(fos);
        }
    }

    public ComponentResult backUpComponentMainDb(final String mappingFile, final DatabaseConfig config)
            throws BackupException {

        if (!ImportExportUtilities.isDatabaseAvailableForBackupRestore(config.getHost())) {
            final String msg = "Cannot backup database as it is not local";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            final File dir =
                    createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.MAINDB.getComponentName());

            DBDumpUtil.dump(config, dir.getAbsolutePath(), BackupImage.MAINDB_BACKUP_FILENAME,
                    BackupImage.ORIGINAL_LICENSE_ID_FILENAME, isVerbose, printStream);

            // produce template mapping if necessary
            if (mappingFile != null) {
                final String mappingFileName = ImportExportUtilities.getAbsolutePath(mappingFile);
                createMappingFile(mappingFileName, config);
            }

            //add my.cnf
            final File file = new File(BackupImage.PATH_TO_MY_CNF);
            if (file.exists() && !file.isDirectory()) {
                FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
            } else {
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                        file.getAbsolutePath()+" does not exist on the host, ignoring", isVerbose, printStream);
            }

            return new ComponentResult(ComponentResult.Result.SUCCESS);            
        } catch (SQLException e) {
            throw new BackupException("Cannot dump the database, please ensure the database is running and the " +
                    "credentials are correct");
        } catch (IOException e) {
            throw new BackupException("Cannot back up the database component: " + e.getMessage());
        } catch (SAXException e) {
            throw new BackupException("Cannot back up the database component: " + e.getMessage());
        }
    }

    public ComponentResult backUpComponentAudits(final DatabaseConfig config) throws BackupException {
        if (!ImportExportUtilities.isDatabaseAvailableForBackupRestore(config.getHost())) {
            final String msg = "Cannot backup database as it is not local";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }
        
        try {
            //Create the database folder
            final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.AUDITS.getComponentName());
            //never include audits with the main db dump
            final String auditTablesDefFile = ssgHome.getAbsolutePath() + File.separator + AUDIT_TABLES_CONFIG;
            final List<String> auditTables = ImportExportUtilities.processFile(new File(auditTablesDefFile));
            if(auditTables.isEmpty()) {
                final String msg = "The file '" + auditTablesDefFile +
                        "' lists no audit tables. No audit data will be backed up";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                return new ComponentResult(ComponentResult.Result.SUCCESS);
            }
            DBDumpUtil.auditDump(config, dir.getAbsolutePath(), auditTables, printStream, isVerbose);

            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (SQLException e) {
            throw new BackupException("Cannot backup audits, please ensure the database is running and the credentials " +
                    "are correct");
        } catch (IOException e) {
            throw new BackupException("Cannot create audits back up file: " + e.getMessage());
        }
    }

    public ComponentResult backUpComponentConfig() throws BackupException {
        try {
            final File dir = createComponentDir(
                    tmpOutputDirectory, ImportExportUtilities.ComponentType.CONFIG.getComponentName());

            ImportExportUtilities.copyFiles(confDir, dir, new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    for (String ssgFile : ImportExportUtilities.CONFIG_FILES) {
                        if (ssgFile.equals(name)) return true;
                    }
                    return false;
                }
            }, isVerbose, printStream);

            return new ComponentResult(ComponentResult.Result.SUCCESS);

        } catch (IOException e) {
            throw new BackupException("Cannot back up ssg configuration: " + e.getMessage());
        }
    }

    public ComponentResult backUpComponentOS() throws BackupException {
        if(!applianceHome.exists()){
            final String msg = "Appliance is not installed";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            // copy system config files
            final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.OS.getComponentName());
            final File backupManifest = new File(ssgHome, OSConfigManager.BACKUP_MANIFEST);
            if (!(backupManifest.exists())) {
                final String msg = "File '"+backupManifest.getAbsolutePath()
                        +"' does not exist. Cannot back up os files";
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
            }

            OSConfigManager osConfigManager = new OSConfigManager(ssgHome, false, isVerbose, printStream);
            osConfigManager.backUpOSConfigFilesToFolder(dir);
            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (OSConfigManager.OSConfigManagerException e) {
            throw new BackupException(e.getMessage());
        } catch (IOException e) {
            throw new BackupException(e.getMessage());
        }
    }

    public ComponentResult backUpComponentCA() throws BackupException {
        try {
            final File dir =
                    createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.CA.getComponentName());

            //backup all .property files in the conf folder which are not in CONFIG_FILES
            final boolean propFilesCopied = ImportExportUtilities.copyFiles(confDir, dir, new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (!name.endsWith(".properties")) return false;

                    for (String ssgFile : ImportExportUtilities.CONFIG_FILES) {
                        if (ssgFile.equals(name)) return false;
                    }
                    return true;
                }
            }, isVerbose, printStream);

            if(!propFilesCopied){
                final String msg = "No Custom Assertion property files needed to be backed up";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

            //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/lib
            final boolean jarFilesCopied = ImportExportUtilities.copyFiles(
                    new File(ssgHome, ImportExportUtilities.CA_JAR_DIR), dir, new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            }, isVerbose, printStream);

            if(!jarFilesCopied){
                final String msg = "No Custom Assertion jar files needed to be backed up";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (IOException e) {
            throw new BackupException("Cannot back up custom assertions: " + e.getMessage());
        }
    }

    public ComponentResult backUpComponentMA() throws BackupException {
        try {
            final File dir =
                    createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.MA.getComponentName());

            //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/assertions
            final boolean aarFilesCopied = ImportExportUtilities.copyFiles(
                    new File(ssgHome, ImportExportUtilities.MA_AAR_DIR), dir, new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".aar");
                }
            }, isVerbose, printStream);

            if(!aarFilesCopied){
                final String msg = "No modular assertion aar files needed to be backed up";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (IOException e) {
            throw new BackupException("Cannot back up modular assertions: " + e.getMessage());
        }
    }

    public ComponentResult backUpComponentESM() throws BackupException {

        try {
            //validate the esm looks ok - just a basic check
            ImportExportUtilities.throwIfEsmNotPresent(esmHome);
        } catch (Exception e) {
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, e.getMessage());
        }

        try {
            //this is a BackupException as the ESM is applicable for backup, but as it's running it cannot be
            //backed up
            ImportExportUtilities.throwifEsmIsRunning();
        } catch (IllegalStateException e) {
            throw new BackupException(e.getMessage());
        }

        try {
            final File dir =
                    createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.ESM.getComponentName());

            //opt/SecureSpan/EnterpriseManager/etc/omp.dat
            ImportExportUtilities.copyDir(new File(esmHome, "etc"), new File(dir, "etc"), null, isVerbose, printStream);

            final String varDbFolder = "var" + File.separator + "db";
            final File dbFile = new File(dir, varDbFolder);
            FileUtils.ensurePath(dbFile);
            ImportExportUtilities.copyDir(new File(esmHome, varDbFolder), dbFile, new FilenameFilter(){
                public boolean accept(File dir, String name) {
                    return !name.endsWith("derby.log");
                }
            }, isVerbose, printStream);

            final String emConfigProp = "emconfig.properties";
            final String varEmConfig = "var" + File.separator + emConfigProp;
            final File sourceFile = new File(esmHome, varEmConfig);
            final File targetFile = new File(dir, varEmConfig);
            FileUtils.copyFile(sourceFile, targetFile);

            final String msg = "Copied file '"+ sourceFile.getAbsolutePath()+"' to file '"
                    + targetFile.getAbsolutePath()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

            return new ComponentResult(ComponentResult.Result.SUCCESS);
        } catch (IOException e) {
            throw new BackupException("Cannot back up the ESM: " + e.getMessage());
        }
    }

    public void createBackupImage() throws BackupException {
        //when we are using ftp, we need to store the image file somewhere locally
        //not using the same temp directory as the image data as it causes recursive problems when zipping
        if(ftpConfig != null){
            String newTmpDir = null;
            try{
                newTmpDir = ImportExportUtilities.createTmpDirectory();
                //Whats the file name? We will use just the file name, and create the zip in the tmp directory
                String zipFileName = ImportExportUtilities.getFilePart(pathToImageZipFile);
                zipFileName = newTmpDir+File.separator+zipFileName;
                createImageZip(zipFileName);
                ftpImage(zipFileName, pathToImageZipFile, ftpConfig);
            } catch (IOException e) {
                throw new BackupException("Problem creating back up image zip file: " + e.getMessage());
            } finally{
                if(newTmpDir != null){
                    logger.info("cleaning up temp files at " + newTmpDir);
                    if (printStream != null && isVerbose) printStream.println("Cleaning temporary files at " + newTmpDir);
                    FileUtils.deleteDir(new File(newTmpDir));
                }
            }
        }else{
            createImageZip(pathToImageZipFile);
            final String msg2 = "Successfully created backup image file: " + pathToImageZipFile;
            ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg2, isVerbose, printStream);
        }

    }

    public void deleteTemporaryDirectory() throws IOException {
        logger.info("cleaning up temp files at " + tmpOutputDirectory);
        if (printStream != null && isVerbose) printStream.println("Cleaning temporary files at " + tmpOutputDirectory);
        FileUtils.deleteDir(tmpOutputDirectory);
    }

    @Override
    protected void finalize() throws Throwable {
        //in case the deleteTemporaryDirectory method is not called
        try {
            if(!tmpOutputDirectory.exists()) return;
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                    "deleteTemporaryDirectory was not called on Backup instance", isVerbose, printStream);
            deleteTemporaryDirectory();
        } finally {
            super.finalize();
        }
    }
    
    /**
     * Ftp a local image zip file to a ftp server
     * @param localZipFile The local image zip file. This String includes the path and the file name. Cannot be null or
     * empty
     * @param destPathAndFileName The file name including path info if required, of where the file should be uploaded
     * to on the ftp server. The filename will have a timestamp in the format "yyyyMMddHHmmss_" prepended to the
     * front of the file name. Cannot be null or empty
     * @param ftpConfig the configuration for the ftp server to upload the localZipFile to. Cannot be null
     * @throws BackupException any problem either reading the image zip, or ftp'ing the image
     */
    private void ftpImage(final String localZipFile, final String destPathAndFileName, final FtpClientConfig ftpConfig)
            throws BackupException {
        if(localZipFile == null) throw new NullPointerException("localZipFile cannot be null");
        if(localZipFile.equals("")) throw new IllegalArgumentException("localZipFile cannot equal the empty string");
        if(destPathAndFileName == null) throw new NullPointerException("destPathAndFileName cannot be null");
        if(destPathAndFileName.equals("")) throw new IllegalArgumentException("destPathAndFileName cannot equal the empty string");
        if(ftpConfig == null) throw new NullPointerException("ftpConfig cannot be null");

        InputStream is = null;
        try {
            is = new FileInputStream(new File(localZipFile));
            final String msg = "Ftp file '" + localZipFile+"' to host '" + ftpConfig.getHost()+"' into directory '"
                        + destPathAndFileName+"'";
            ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

            final String filePart = ImportExportUtilities.getFilePart(destPathAndFileName);
            FtpUtils.upload(ftpConfig, is, filePart, true);
            final String msg1 = "Successfully uploaded backup image to ftp server";
            ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg1, isVerbose, printStream);
        } catch (FtpException e) {
            throw new BackupException("Could not ftp image to ftp host '"+ftpConfig.getHost()+"' " +
                    "with user '"+ftpConfig.getUser()+"' :" + e.getMessage());
        } catch (FileNotFoundException e) {
            throw new BackupException("Problem reading image file before ftp: " + e.getMessage());
        } finally{
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * Create an image zip archive from all the files and folders in tmpOutputDirectory.
     * @param zipFileName The name of the zip file to create
     * @throws BackupException if any exception occurs while creating the zip
     */
    public void createImageZip(final String zipFileName) throws BackupException {
        final String msg1 = "compressing image into " + zipFileName;
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);

        ZipOutputStream out = null;
        try {
            if(isPostFiveO){
                //this logic for placing files with no path part into the /config/backup/images directory
                //is only post 5.0
                //does the file name contain a path part?
                final String dirPart = ImportExportUtilities.getDirPart(zipFileName);
                out = new ZipOutputStream(new FileOutputStream(
                        (dirPart == null)?"images"+File.separator+zipFileName: zipFileName));
            }else{
                out = new ZipOutputStream(new FileOutputStream(zipFileName));
            }
            
            final String msg = "Compressing SecureSpan Gateway image into " + zipFileName;
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            final StringBuilder sb = new StringBuilder();
            addDir(tmpOutputDirectory, out, sb);

            //Add the manifest listing all files in the archive to the archive
            final File manifest = new File(tmpOutputDirectory, ImportExportUtilities.MANIFEST_LOG);
            manifest.createNewFile();
            final FileOutputStream fos = new FileOutputStream(manifest);
            fos.write(sb.toString().getBytes());
            fos.close();

            addZipFileToArchive(out, manifest, sb);
        } catch (IOException e) {
            throw new BackupException("Problem creating image file: " + e.getMessage());
        } finally {
            ResourceUtils.closeQuietly(out);
        }
    }

    public File getBackupFolder() {
        return tmpOutputDirectory;
    }

    /**
     * Add any arbitrary file to the zip archive being created. This is used to add the manifest.log file to the archive
     * @param out The zip archive to add the file to. It must be open
     * @param fileToAdd the file to add to the zip. This file should be in tmpOutputDirectory
     * @param sb the StringBuilder to append to, so that a record of each file archived is made.
     * @throws IOException if any exception occurs while writing to the zip archive
     * @throws IllegalStateException if the fileToAdd does not reside in tmpOutputDirectory
     */
    private void addZipFileToArchive(final ZipOutputStream out,
                                     final File fileToAdd,
                                     final StringBuilder sb)
            throws IOException {
        final byte[] tmpBuf = new byte[1024];
        final FileInputStream in = new FileInputStream(fileToAdd.getAbsolutePath());
        if (printStream != null && isVerbose) printStream.println("\t- " + fileToAdd.getAbsolutePath());
        String zipEntryName = fileToAdd.getAbsolutePath();
        if (zipEntryName.startsWith(tmpOutputDirectory.getAbsolutePath())) {
            zipEntryName = zipEntryName.substring(tmpOutputDirectory.getAbsolutePath().length() + 1);
        }else{
            throw new IllegalStateException("File '"+fileToAdd.getAbsoluteFile()+"' does not exist in directory '"
                    +tmpOutputDirectory+"'");
        }
        out.putNextEntry(new ZipEntry(zipEntryName));
        if(sb != null) sb.append(zipEntryName).append("\n");
        // Transfer from the file to the ZIP fileToAdd
        int len;
        while ((len = in.read(tmpBuf)) > 0) {
            out.write(tmpBuf, 0, len);
        }
        // Complete the entry
        out.closeEntry();
        in.close();
    }

    /**
     * Add a directory to a zip archive. The zip archive should be currently open
     * @param dirObj The directory to add to the archive
     * @param out The currently open ZipOutputStream
     * @param sb The StringBuilder used to record each file archived
     * @throws IOException if any exception occurs when writing to the zip archive
     */
    private void addDir(final File dirObj,
                        final ZipOutputStream out,
                        final StringBuilder sb) throws IOException {
        final File[] files = dirObj.listFiles();

        for (final File file : files) {
            if (file.isDirectory()) {
                addDir(file, out, sb);
                continue;
            }
            addZipFileToArchive(out, file, sb);
        }
    }

    /**
     * Create the directory with the name componentName in the directory tmpOutputDirectory. This will delete any
     * existing directory if it exists and recreate it, otherwise it just creates the directory
     *
     * @param tmpOutputDirectory String The directory where the componentName should be created
     * @param componentName      String The name of the new directory
     * @return File representing the created directory
     * @throws IOException if any exception occurs while creating the directory
     */
    private File createComponentDir(final File tmpOutputDirectory, final String componentName) throws IOException {
        final File dir = new File(tmpOutputDirectory, componentName);
        if (dir.exists()) {
            dir.delete();
        }
        dir.mkdir();
        return dir;
    }

    /**
     * Create a mapping file which can be used when migrating tables.
     *
     * @param mappingFileName The path(optional) and name of the mapping file to create
     * @param config          The DatabaseConfig used to connect to the database
     * @throws IOException if any exception occurs when creating the mapping file
     * @throws java.sql.SQLException if a database connection cannot be required
     * @throws org.xml.sax.SAXException if problem creating the mapping xml file
     */
    private void createMappingFile(final String mappingFileName, final DatabaseConfig config) throws IOException,
            SQLException, SAXException {
        if (!ImportExportUtilities.isDatabaseAvailableForBackupRestore(config.getHost())) {
            logger.log(Level.WARNING, "Cannot create maping file as database is not local");
            throw new IllegalStateException("Cannot create maping file as database is not local");
        }

        if (mappingFileName != null) {
            if (!ImportExportUtilities.testCanWriteSilently(mappingFileName)) {
                throw new IllegalArgumentException("cannot write to the mapping template path provided: " + mappingFileName);
            }
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "Successfully tested write permission for file '" + mappingFileName+"'", isVerbose, printStream);
            MappingUtil.produceTemplateMappingFileFromDB(config, mappingFileName, isVerbose, printStream);
        }
    }
}
