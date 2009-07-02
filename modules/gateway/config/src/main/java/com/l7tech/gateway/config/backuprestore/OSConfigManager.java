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

        internalOsFolder = new File(ssgHome, INTERNAL_CONFIG_FILES_FOLDER);
        if(!isReboot){
            if (!internalOsFolder.exists()) {
                boolean success = internalOsFolder.mkdir();
                if (!success)
                    throw new IllegalStateException("Could not create folder '" + internalOsFolder.getAbsolutePath() + "'");
            } else {
                //were going to delete it, even if somehow it got turned into a file
                if(internalOsFolder.isFile()) internalOsFolder.delete();
                else FileUtils.deleteDir(internalOsFolder);
                boolean success = internalOsFolder.mkdir();
                if (!success)
                    throw new IllegalStateException("Could not create folder '" + internalOsFolder.getAbsolutePath() + "'");
            }
            //At this point we are guaranteed to have a completely new internalOsFolder directory
        }else{
            //this folder should exist
            if(!internalOsFolder.exists())
                throw new IllegalArgumentException("Folder '"+internalOsFolder.getAbsolutePath()+"' not found");
            if(!internalOsFolder.isDirectory())
                throw new IllegalArgumentException("File '" + internalOsFolder.getAbsoluteFile() + "' is not a directory");
        }

        if(!internalOsFolder.isDirectory())
            throw new IllegalStateException("'"+internalOsFolder.getAbsolutePath()+"' is not a folder");

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
    void copyFilesToInternalFolderPriorToReboot(final File source) throws OSConfigManagerException {
        if(isReboot)
            throw new IllegalStateException("Method cannot be called when OSConfigManager is in the reboot state");
   
        if(source == null) throw new NullPointerException("source cannot be null");
        if(!source.exists()) throw new IllegalArgumentException("source does not exist");
        if(!source.isDirectory()) throw new IllegalArgumentException("source is not a directory");

        try {
            copyFilesFromSource(source);
        } catch (IOException e) {
            throw new OSConfigManagerException("Cannot copy files to internal folder: " + e.getMessage());
        }

    }

    /**
     * Copy files from our internal folder to their ultimate desintation. Unless this code is running as root
     * this method will fail
     */
    void finishRestoreOfFilesOnReboot() throws OSConfigManagerException {
        if(!isReboot)
            throw new IllegalStateException("Method cannot be called when OSConfigManager is not in the reboot state");

        try {
            copyFilesFromSource(internalOsFolder);
        } catch (IOException e) {
            throw new OSConfigManagerException("Cannot copy files to internal folder: " + e.getMessage());
        }

    }

    /**
     * This can also be used to copy files like my.cnf which are restored outside of the os backup component
     * @param source is either our internal folder or a directory where a backup image was unzipped to
     * @throws IOException
     */
    private void copyFilesFromSource(final File source) throws IOException {
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
            final String msg = "Copying file '"+osFile+"' into folder '"+ targetRoot.getAbsolutePath()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

            final File sourceFile = new File(osFile);
            final File targetFile = new File(targetRoot, rawFilePath);

            FileUtils.ensurePath(targetFile.getParentFile());
            FileUtils.copyFile(sourceFile, targetFile);
            systemFileOverWritten = true;
        }
        if (systemFileOverWritten && !isReboot) {
            final String msg = "Certain system files are set to be overwritten the next time the SSG host is restarted" +
                    " ,if this has been configured on host startup";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
        }
    }

    void copyFileFromSource(final File fileToCopy) throws IOException{
        final File targetRoot = new File(internalOsFolder.getParentFile().getAbsolutePath());
        final File targetFile = new File(targetRoot, fileToCopy.getAbsolutePath());
        FileUtils.ensurePath(targetFile.getParentFile());
        FileUtils.copyFile(fileToCopy, targetFile);
        final String msg = "File '" + fileToCopy.getAbsolutePath()+"' is set to be overwritten the next time the SSG " +
                "host is restarted, if this has been configured on host startup";
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
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
}