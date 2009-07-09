package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * OSConfigManager manages the backup and restore of OS configuration files
 *
 * During backup the OSConfigManager will read backup_manifest and place all the files its able to find
 * from this file into a specified directory, maintaining the original folder structure
 *
 * During import the OSConfigManager has 2 responsibilies
 * 1) knowing where to place os files when restoring prior to reboot - these files will orginate in a source
 * directory from an unzipped zip file
 * 2) Knowing where to find os files when this process runs as root following a reboot
 *
 * This class is immutable
 */
final class OSConfigManager {
    private static final Logger logger = Logger.getLogger(OSConfigManager.class.getName());
    static final String BACKUP_MANIFEST = "config/backup/cfg/backup_manifest";
    private static final String INTERNAL_CONFIG_FILES_FOLDER = "config/backup/configfiles";

    private final File backUpManifest;
    private final boolean isVerbose;
    private final PrintStream printStream;

    /**
     * This is the directory where we store files when we restore files while wating for a reboot to happen
     */
    private final File internalOsFolder;

    /**
     * if true, the internal state of this OSConfigManager, is the reboot state, false otherwise
     */
    private final boolean isReboot;
    /**
     * @param ssgHome installation directory of the SSG
     * @param isReboot if true, the SSG is rebooting and this instance can be used to copy files, false otherwise
     * if false, the internal folder which is used to store files is recreated and any contents are removed
     */
    OSConfigManager(final File ssgHome,
                    final boolean isReboot,
                    final boolean isVerbose,
                    final PrintStream printStream){
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome is not a directory");

        //OS files can only be restored on an appliance. Any appliance installation contians backup_manifest
        //if this file does not exist it's a much stronger indicator that this is not an appliance
        //=> this file is a hard requirement, regardless of what ever the caller thinks
        backUpManifest = new File(ssgHome, BACKUP_MANIFEST);
        if(!backUpManifest.exists())
            throw new IllegalStateException("backup_manifest not found in '"+backUpManifest.getAbsolutePath()+"'");

        if(!backUpManifest.isFile())
            throw new IllegalStateException("backup_manifest is not a regular file");

        internalOsFolder = new File(ssgHome, INTERNAL_CONFIG_FILES_FOLDER);//this may not exist and thats ok

        this.isReboot = isReboot;
        this.isVerbose = isVerbose;
        this.printStream = printStream;
    }

    public static class OSConfigManagerException extends Exception{
        public OSConfigManagerException(String message) {
            super(message);
        }
    }
    
    /**
     * Backup all files found from backup_manifest and place them into the destination folder
     *
     * Do not call if the OSConfigManager is in the reboot state
     * 
     * @param destination where to place copied os files. Original directory structure of the copied folder will
     * be maintained, with destination as the new root e.g. a file in /etc/a.txt will be in destination/etc/a.txt
     * @throws OSConfigManagerException if any problem copying files to destination
     */
    void backUpOSConfigFilesToFolder(final File destination) throws OSConfigManagerException{
        if(isReboot)
            throw new IllegalStateException("Method cannot be called when OSConfigManager is in the reboot state");
        if(destination == null) throw new NullPointerException("destination cannot be null");
        if(!destination.exists()) throw new IllegalArgumentException("destination does not exist");
        if(!destination.isDirectory()) throw new IllegalArgumentException("destination is not a directory");

        FileReader fr = null;
        BufferedReader bufferedReader = null;
        String fileToCopy;
        try {
            fr = new FileReader(backUpManifest);
            bufferedReader = new BufferedReader(fr);
            while ((fileToCopy = bufferedReader.readLine()) != null) {
                if (!fileToCopy.startsWith("#")) {
                    File osConfigFileToCopy = new File(fileToCopy);
                    if (osConfigFileToCopy.isFile()) {
                        File target = new File(new File(destination.getAbsolutePath()), osConfigFileToCopy.getPath());
                        FileUtils.ensurePath(target.getParentFile());
                        final String msg = "Copying file '" + osConfigFileToCopy.getAbsolutePath() + "' to '"
                                + target.getAbsolutePath() + "'";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                        FileUtils.copyFile(osConfigFileToCopy, target);

                    } else {
                        final String msg = "File '" + osConfigFileToCopy.getAbsolutePath()
                                + "' does not exist on this host and will not be backed up.";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    }
                }
            }
        } catch (IOException e) {
            throw new OSConfigManagerException("Could not back up os configuration files: " + e.getMessage());
        }
        finally {
            ResourceUtils.closeQuietly(bufferedReader);
            ResourceUtils.closeQuietly(fr);
        }
    }

    /**
     * Copy files from a source folder into our internal folder where they will be stored while the system
     * reboots. On reboot, if the host is so configured, this internal folder will be searched and files found
     * will be copied to their ultimate destination with root priviledes. If this 'hook' is not configured, then
     * the files will just sit in our internal folder. Calling this method will always delete all files found
     * in our internal folder
     *
     * Do not call if the OSConfigManager is in the reboot state
     *
     * @param source the directory containing the files to copy. Must exist and be a directory
     */
    void copyFilesToInternalFolderPriorToReboot(final File source){
        if(isReboot)
            throw new IllegalStateException("Method cannot be called when OSConfigManager is in the reboot state");

        //we must be able to empty or create this folder if this method is called
        emptyCreateOrThrowInternalFolder(true);

        if(source == null) throw new NullPointerException("source cannot be null");
        if(!source.exists()) throw new IllegalArgumentException("source does not exist");
        if(!source.isDirectory()) throw new IllegalArgumentException("source is not a directory");

        if(!copyFilesFromSource(source)){
            final String msg = "No files were copied from source '" + source.getAbsolutePath()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
        }
    }

    /**
     * Copy files from our internal folder to their ultimate destination. To successfully copy the files, this method
     * must be running as root
     *
     * All files copied successfully are deleted from the internal folder
     * All files which could not be copied are not deleted from the internal folder
     * Both the success / fail of copying the file from the internal folder is logged
     * Both the success / fail of deleting a successfully copied file from the internal folder is logged
     * @return true if any files were restored on reboot
     */
    boolean finishRestoreOfFilesOnReboot(){
        if(!isReboot)
            throw new IllegalStateException("Method cannot be called when OSConfigManager is not in the reboot state");

        final String msg2 = "Restoring os files on reboot...";
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg2, isVerbose, printStream);

        if(!internalOsFolder.exists() || !internalOsFolder.isDirectory()){
            final String msg = "No files were required to be restored on appliance reboot";
            //this is info logging, as the appliance can reboot before any restore / migrate is ever done
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            return false;
        }

        final boolean someFilesCopied= copyFilesFromSource(internalOsFolder);
        final String msg1 = "Restored " +((someFilesCopied) ? "some" : "no") + " os files on reboot";
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);

        //Delete the contents of the internal folder
        if(FileUtils.deleteDirContents(internalOsFolder)){
            final String deleteMsg = "Sucessfully deleted contents of internal folder";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, deleteMsg, isVerbose, printStream);
        }else{
            final String deleteMsg = "Could not delete contents of internal folder: '"
                    + internalOsFolder.getAbsolutePath()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, deleteMsg, isVerbose, printStream);
        }
        return someFilesCopied;
    }

    /**
     * The configfiles folder must exist or be capable of being created for this method to be called.
     * If it exists, it will not be emptied. If it does not exist, then it must be successfull in creating it
     * @param fileToCopy
     * @throws IOException
     */
    void copyFileToInternalFolder(final File fileToCopy) throws IOException{

        emptyCreateOrThrowInternalFolder(false);
        final File targetRoot = new File(internalOsFolder.getParentFile().getAbsolutePath());
        final File targetFile = new File(targetRoot, fileToCopy.getAbsolutePath());
        FileUtils.ensurePath(targetFile.getParentFile());
        FileUtils.copyFile(fileToCopy, targetFile);
        final String msg = "File '" + fileToCopy.getAbsolutePath()+"' is set to be overwritten the next time the SSG " +
                "host is restarted, if this has been configured on host startup";
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
    }

    /**
     * This can also be used to copy files like my.cnf which are restored outside of the os backup component
     *
     * Note: This method catches all it's IO Exceptions as it reports a list of successfully copied files
     * @param source is either our internal folder or a directory where a backup image was unzipped to
     * @return true if some files were copied from the source, false otherwise
     */
    private boolean copyFilesFromSource(final File source){
        final List<String> allFiles = getFlattenedDirectoryForFilesOnly(source);
        final String sourceRoot = source.getAbsolutePath();

        final File targetRoot;
        if(isReboot){
            //root of the entire operating system
            //allow system property to override this for tests
            String rootDir = SyspropUtil.getString("com.l7tech.config.backuprestore.osrootdir", "/");
            targetRoot = new File(rootDir);
        }else{
            targetRoot = new File(internalOsFolder.getAbsolutePath());
        }

        boolean systemFileOverWritten = false;
        for (String osFile : allFiles) {
            //the source folder contains files with each file having a full path structure
            //for each file, we need to copy it to the appropriate target, removing all of source's file
            //structure  so that source/etc/a.txt ends up in target/etc/a.txt

            final String rawFilePath = osFile.substring(sourceRoot.length());

            final File sourceFile = new File(osFile);
            final File targetFile = new File(targetRoot, rawFilePath);

            try{
                FileUtils.ensurePath(targetFile.getParentFile());
                FileUtils.copyFile(sourceFile, targetFile);
                systemFileOverWritten = true;
                final String msg = "Copied file '"+osFile+"' into folder '"+ targetFile.getAbsolutePath()+"'";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }catch(IOException e){
                final String msg = "Could not copy file '"+osFile+"' into folder '"+ targetFile.getAbsolutePath()+"' :"
                        + e.getMessage();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }
        }
        
        if (systemFileOverWritten && !isReboot) {
            final String msg = "Certain system files are set to be overwritten the next time the SSG host is restarted" +
                    ", if this has been configured on host startup";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
        }

        return systemFileOverWritten;
    }

    /**
     * Get a list of the absolute paths of each file in a directory, regardless of the depth of child folders.
     * Empty directorys will not be included
     * @param dir the folder to get the list of files from
     * @return list of strings, one for each file found in any directory or directory of dir. Never null, but can be
     * empty if no files were found
     */
    private List<String> getFlattenedDirectoryForFilesOnly( final File dir) {
        if ( dir.exists() && dir.isDirectory() ) {
            final List<String> output = new ArrayList<String>();
            final File[] children = dir.listFiles();
            if ( children != null ) {
                for ( final File childfile : children ) {
                    if ( childfile.isDirectory() ) {
                        final List<String> subdirlist = getFlattenedDirectoryForFilesOnly( childfile);
                        output.addAll( subdirlist );
                    } else {
                        output.add(  childfile.getAbsolutePath() );
                    }
                }
            }
            return output;
        } else return Collections.emptyList();
    }

    /**
     * Ensure the internal folder exists. If it doesn't, it's created. If it can not be created an exception will
     * be thrown
     * @param deleteIfFound if true, the internal folder will be emptied
     */
    private void emptyCreateOrThrowInternalFolder(final boolean deleteIfFound) {
        if (!internalOsFolder.exists()) {
            boolean success = internalOsFolder.mkdir();
            if (!success)
                throw new IllegalStateException("Could not create folder '" + internalOsFolder.getAbsolutePath() + "'");
        } else {
            //were going to delete it, if somehow it got turned into a file
            if(internalOsFolder.isFile()){
                internalOsFolder.delete();
                boolean success = internalOsFolder.mkdir();
                if (!success)
                    throw new IllegalStateException("Could not create folder '" + internalOsFolder.getAbsolutePath() + "'");
            }
            else {
                //if it exists, then just empty the contents, this will allow this to work as the gateway user
                //as it doesn't have permissions to delete and recreate the directory
                final String msg = "Deleting contents of internal folder";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                if(deleteIfFound){
                    if(FileUtils.deleteDirContents(internalOsFolder)){
                        final String msg1="Successfully deleted contents of internal folder";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);
                    }else{
                        final String msg1="Could not delete contents of internal folder";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg1, isVerbose, printStream);
                    }
                }
            }
        }
    }
    
}