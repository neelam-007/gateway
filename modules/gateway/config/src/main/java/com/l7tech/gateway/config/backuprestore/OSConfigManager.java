package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.FileUtils;
import com.l7tech.util.ResourceUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Encapsulates import and export of os level system config files.
 * Which os level files are exported/imported is controlled by the config file
 * backup_manifest.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 22, 2006<br/>
 */
class OSConfigManager {
    private static final Logger logger = Logger.getLogger(OSConfigManager.class.getName());
    private static final String BACKUP_MANIFEST = "config/backup/cfg/backup_manifest";
    private static final String SYSTMP_PATH = "configfiles";
    private final File pathToBackupFolder;
    private static File backUpManifest;

    private OSConfigManager(String path) {
        pathToBackupFolder = new File(path);
    }

    /**
     * stored os level config files into temp directory. which files to store is controlled by the config file
     * backup_manifest
     * @param destination the name of the temp directory where the image source is being stored before compression
     * @param ssgHome home directory of the Flasher; use <code>null</code> if the JVM was already launched from there
     * @throws IOException if something does not work
     * @throws IllegalStateException if the backup_manifest file is not found or is a directory
     */
    public static void saveOSConfigFiles(String destination, File ssgHome) throws IOException {
        backUpManifest = new File(ssgHome, BACKUP_MANIFEST);
        if (!backUpManifest.exists()) throw new IllegalStateException("backup_manifest does not exist");
        if (backUpManifest.isDirectory()) throw new IllegalStateException("backup_manifest is a directory");

        OSConfigManager me = new OSConfigManager(destination);
        me.copyOSFilesToBackupFolder();
    }

    /**
     * restore os level files stored from an exploded ssg image
     * @param source the exploded image
     * @throws IOException if something goes wrong
     */
    public static void restoreOSConfigFilesToTmpTarget(String source) throws IOException {
        OSConfigManager me = new OSConfigManager(source);
        me.doLoadToTmpTarget();
    }

    public static void restoreOSConfigFilesForReal() throws IOException {
        OSConfigManager me = new OSConfigManager("");
        me.doLoadToRealTarget();
    }

    private void copyOSFilesToBackupFolder() throws IOException {
        FileReader fr = new FileReader(backUpManifest);
        BufferedReader bufferedReader = new BufferedReader(fr);
        String fileToCopy;
        try {
            while((fileToCopy = bufferedReader.readLine()) != null) {
                if (!fileToCopy.startsWith("#")) {
                    File osConfigFileToCopy = new File(fileToCopy);
                    if ( osConfigFileToCopy.isFile() ) {
                        logger.info("Saving " + osConfigFileToCopy.getPath());
                        File target = new File(new File(pathToBackupFolder.getAbsolutePath()), osConfigFileToCopy.getPath());
                        FileUtils.ensurePath(target.getParentFile());
                        FileUtils.copyFile(osConfigFileToCopy, target);
                    } else {
                        logger.info("os config file " + osConfigFileToCopy.getPath() + " does not exist on this " +
                                    "system and will not be included in image");
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly(bufferedReader);
            ResourceUtils.closeQuietly(fr);
        }
    }

    private void doLoadToRealTarget() throws IOException {
        logger.info("Listing files that potentially need restore");
        String path = new File(SYSTMP_PATH).getAbsolutePath();
        List<String> listofosfiles = listDir(path);
        if (listofosfiles != null) for (String osfiletorestore : listofosfiles) {
            String realtarget;
            if ( osfiletorestore.startsWith(path) ) {
                realtarget = osfiletorestore.substring(path.length());
            } else {
                // if this happens, it's a bug
                throw new RuntimeException("unexpected path for " + osfiletorestore);
            }
            logger.info("Restoring " + osfiletorestore + " into " + realtarget);
            File fromFile = new File(osfiletorestore);
            File toFile = new File(realtarget);
            if (toFile.canWrite()) {
                if (toFile.exists()) {
                    toFile.delete();
                }
                FileUtils.ensurePath(toFile.getParentFile());
                FileUtils.copyFile(fromFile, toFile);
            } else {
                logger.severe("cannot restore " + realtarget + " because i dont have necessary permissions. perhaps " +
                              "this was incorrectly invoked at non-root user");
            }
        }
        File tmp = new File(SYSTMP_PATH);
        if (tmp.exists()) {
            FileUtils.deleteDir(tmp);
        }
    }

    private void doLoadToTmpTarget() throws IOException {
        final String osfilesroot = pathToBackupFolder.getAbsolutePath();
        List<String> listofosfiles = listDir(osfilesroot);
        boolean systemfileoverwritten = false;
        for (String osfiletorestore : listofosfiles) {
            String tmptarget;
            if (osfiletorestore.startsWith(osfilesroot)) {
                tmptarget = SYSTMP_PATH + osfiletorestore.substring(osfilesroot.length());
            } else {
                // if this happens, it's a bug
                throw new RuntimeException("unexpected path for " + osfiletorestore);
            }
            logger.info("Restoring " + osfiletorestore + " into " + tmptarget);
            File fromFile = new File(osfiletorestore);
            File toFile = new File(tmptarget);
            System.out.println("Flagging " + toFile.getName() + " for daemon overwrite");
            if (toFile.exists()) {
                toFile.delete();
            }
            FileUtils.ensurePath(toFile.getParentFile());
            FileUtils.copyFile(fromFile, toFile);
            systemfileoverwritten = true;
        }
        if (systemfileoverwritten) {
            System.out.println("\nCertain system files have been overwritten, you may need to reboot the " +
                               "SecureSpan Gateway.");
        }
    }

    private List<String> listDir( final String path ) {
        final File dir = new File(path);
        if ( dir.exists() && dir.isDirectory() ) {
            List<String> output = new ArrayList<String>();
            File[] children = dir.listFiles();
            if ( children != null ) {
                for ( File childfile : children ) {
                    if ( childfile.isDirectory() ) {
                        List<String> subdirlist = listDir( childfile.getAbsolutePath() );
                        if ( subdirlist != null ) {
                            output.addAll( subdirlist );
                        }
                    } else {
                        output.add(  childfile.getAbsolutePath() );
                    }
                }
            }
            return output;
        } else return null;
    }
}