/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 9:05:08 AM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.l7tech.util.FileUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.jscape.inet.ftp.FtpException;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * 
 * BackupImageVersion knows what version of a backup image it is and when asked, it can tell you
 * where to find the component your looking for
 *
 * When constructed it will unzip the backup image. Call removeTempDirectory when finished to delete this temp
 * directory
 * 
 * This class is immutable
 */
final class BackupImage {

    /**
     * Image location is an absolute path to where the image file is, locally or on a ftp server
     */
    private final File image;
    private final String tempDirectory;
    private final ImageVersion imageVersion;
    private final PrintStream printStream;
    private final boolean isVerbose;
    private static final Logger logger = Logger.getLogger(BackupImage.class.getName());
    static final String MAINDB_BACKUP_FILENAME = "main_backup.sql";
    static final String AUDIT_BACKUP_FILENAME = "audits.gz";
    private final File versionFile;

    private static final String MY_CNF = "my.cnf";

    /**
     * my.cnf makes up part of a databsae backup. This is the current known path to this file
     */
    static final String PATH_TO_MY_CNF = "/etc/"+ MY_CNF;

    public static class InvalidBackupImage extends Exception{
        public InvalidBackupImage(String message) {
            super(message);
        }
    }
    
    BackupImage(final String imageName,
                        final PrintStream printStream,
                        final boolean isVerbose) throws IOException, InvalidBackupImage {
        if(imageName == null) throw new NullPointerException("image cannot be null");
        if(imageName.isEmpty()) throw new IllegalArgumentException("image cannot be the emtpy string");

        image = new File(imageName);
        if(!image.exists())
            throw new IllegalArgumentException("'"+image.getAbsolutePath()+"' file must exist");
        if(image.isDirectory())
            throw new IllegalArgumentException("'"+image.getAbsolutePath()+"' image must be a file");

        tempDirectory = ImportExportUtilities.createTmpDirectory();
        this.printStream = printStream;
        this.isVerbose = isVerbose;
        this.imageVersion = unzipAndDetermineVersion();

        //verify backup contains the version info
        versionFile = getVersionFileThrowIfNotFound();
    }

    BackupImage(final FtpClientConfig ftpConfig,
                        final String imageNameAndPath,
                        final PrintStream printStream,
                        final boolean isVerbose)
            throws IOException, InvalidBackupImage, BackupImageException {
        if(ftpConfig == null) throw new NullPointerException("ftpConfig cannot be null");
        if(imageNameAndPath == null) throw new NullPointerException("image cannot be null");
        if(imageNameAndPath.isEmpty()) throw new IllegalArgumentException("image cannot be the emtpy string");

        tempDirectory = ImportExportUtilities.createTmpDirectory();
        this.printStream = printStream;
        this.isVerbose = isVerbose;

        String downloadedFile = tempDirectory + File.separator + "tempzip.zip";
        try {
            final FtpClientConfig ftpToUse = (FtpClientConfig) ftpConfig.clone();
            //download image
            try {
                FtpUtils.download(ftpToUse, downloadedFile, imageNameAndPath);
                String msg = "Downloaded image '"+imageNameAndPath+"' from host '"+ftpToUse.getHost() +"' " +
                        "with user: '" + ftpToUse.getUser()+"' to temp directory '"+tempDirectory+"'";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            } catch (FtpException e) {
                String msg = "Cannot download backup image from ftp host: " + e.getMessage();
                ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, printStream);
                throw new BackupImageException(msg);
            }
        } catch (CloneNotSupportedException e) {
            String msg = "Cannot clone parameter ftpConfig";
            ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, printStream);
            throw new IllegalStateException(msg);
        }
        image = new File(downloadedFile);
        imageVersion = unzipAndDetermineVersion();
        versionFile = getVersionFileThrowIfNotFound();
    }

    public static class BackupImageException extends Exception{
        public BackupImageException(String message) {
            super(message);
        }
    }
    
    /**
     * @return the version this back up represents
     */
    ImageVersion getImageVersion() {
        return imageVersion;
    }

    /**
     * What type of image file is it?
     */
    private ImageVersion unzipAndDetermineVersion() throws IOException {
        final String msg = "Uncompressing image to temporary directory " + tempDirectory;
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

        unzipToDir(image.getAbsolutePath(), tempDirectory);

        //What does the image look like? 5.0 or Buzzcut?
        //5.0 has no directories, its a flat layout

        ImportExportUtilities.ComponentType [] allValues = ImportExportUtilities.ComponentType.values();
        boolean anyDirectoryFound = false;
        for(ImportExportUtilities.ComponentType component: allValues){
            File f = new File(tempDirectory, component.getComponentName());
            if(f.exists() && f.isDirectory()){
                anyDirectoryFound = true;
                break;
            }
        }

        if(anyDirectoryFound) return ImageVersion.AFTER_FIVE_O;
        else return ImageVersion.FIVE_O;
    }

    /**
     * Remove the temp directory this backup image created when it unzipped the image
     */
    void removeTempDirectory(){
        final String msg = "cleaning up temp files at " + tempDirectory;
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
        boolean success = FileUtils.deleteDir(new File(tempDirectory));

        if(!success){
            final String msg1 = "Could not delete temp directory '"+tempDirectory+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg1, isVerbose, printStream);
        }else{
            final String msg1 = "Successfully deleted temp directory '"+tempDirectory+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);
        }
    }

    /**
     * Get the directory which contains the main database backup components. Does not contain audits
     * @return File representing the directory where the backup data can be found. Can be null if after 5.0 and
     * imgae does not contain a maindb folder
     */
    File getMainDbBackupFolder(){
        if(imageVersion == ImageVersion.AFTER_FIVE_O){
            return getFolder(ImportExportUtilities.ComponentType.MAINDB);
        }else{
            return new File(tempDirectory);
        }
    }

    /**
     * Get my.cnf Can only be returned if the image is > 5.0
     * @return File representing my.cnf, never null
     */
    File getDatabaseConfiguration(){
        return new File(getMainDbBackupFolder(), MY_CNF);
    }
    
    /**
     * Get the file containing the audits backup.
     * After 5.0 this will be a gzipped file. 5.0 this will be the same file as the main database file
     * @return File representing the file containing the audits backup. It will be a file not a directory. Can be null
     * when after 5.0 and the image does not contain any audits
     */
    File getAuditsBackupFile(){
        if(imageVersion == ImageVersion.AFTER_FIVE_O){

            final File auditsDir= getFolder(ImportExportUtilities.ComponentType.AUDITS);
            if(auditsDir == null) return null;
            final File auditsFile = new File(auditsDir, AUDIT_BACKUP_FILENAME);
            if(!auditsFile.exists() || auditsFile.isDirectory()) return null;
            return auditsFile;
        }else{
            final File dbFolder = getMainDbBackupFolder();
            return new File(dbFolder, BackupImage.MAINDB_BACKUP_FILENAME);
        }
    }

    /**
     * Get the actual version file, not just the directory containing it
     * @return the version file
     * @throws InvalidBackupImage if the version file is not found. It is required in any backup image zip
     */
    private File getVersionFileThrowIfNotFound() throws InvalidBackupImage {
        //Version is in the same place for 5.0 and post 5.0 (for now)
        final File versionFile = new File(tempDirectory, ImportExportUtilities.ComponentType.VERSION.getComponentName());
        if(!versionFile.exists() || !versionFile.isFile()){
            //version file can never be missing - invalid backup image
            throw new InvalidBackupImage("Invalid backup image. Version file '"+
                    versionFile.getAbsolutePath()+"' not found");
        }
        return versionFile;
    }

    /**
     * Get the actual version file, not just the directory containing it
     * @return the version file
     */
    File getVersionFile(){
        return versionFile;
    }

    /**
     * Get the config folder from the image, or the root folder if the image is from 5.0
     *
     * @return File the config folder. If it's not null, then the folder exists and is a directory
     */
    File getConfigFolder(){
        if(imageVersion == ImageVersion.AFTER_FIVE_O){
            return getFolder(ImportExportUtilities.ComponentType.CONFIG);
        }else{
            return new File(tempDirectory);
        }
    }

    File getRootFolder(){
        return new File(tempDirectory);
    }

    /**
     * Get the os folder from the image, the os folder is the same for 5.0 and post 5.0
     * @return File the os folder. If it's not null, then the folder exists and is a directory
     */
    File getOSFolder(){
        return getFolder(ImportExportUtilities.ComponentType.OS);
    }

    /**
     * Get the ca folder from the image, the ca folder only exists in a post 5.0 image
     * @return File the ca folder. If it's not null, then the folder exists and is a directory
     */
    File getCAFolder(){
        return getFolder(ImportExportUtilities.ComponentType.CA);
    }

    /**
     * Get the ma folder from the image, the ma folder only exists in a post 5.0 image
     * @return File the ma folder. If it's not null, then the folder exists and is a directory
     */
    File getMAFolder(){
        return getFolder(ImportExportUtilities.ComponentType.MA);
    }

    /**
     * Get the esm folder from the image, the esm folder only exists in a post 5.0 image
     * @return File the ma folder. If it's not null, then the folder exists and is a directory
     */
    File getESMFolder(){
        return getFolder(ImportExportUtilities.ComponentType.ESM);
    }

    /**
     * Get the folder for a component from the image.
     * @param component the component to get the folder for
     * @return File folder for the component. If it's not null, then the folder exists and is a directory
     */
    private File getFolder(ImportExportUtilities.ComponentType component){
        final File compFolder = new File(tempDirectory, component.getComponentName());
        if(!compFolder.exists() || !compFolder.isDirectory()) return null;

        return compFolder;
    }

    private void unzipToDir(final String filename, final String destinationpath)
            throws IOException {
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
                    if (isVerbose && printStream != null) System.out.println("\t- " + entryName);
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

    public enum ImageVersion{FIVE_O, AFTER_FIVE_O}
}
