package com.l7tech.gateway.config.flasher;

import com.l7tech.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Encapsulates import and export of os level system config files.
 * Which os level files are exported/imported is controlled by the hidden config file
 * grandmaster_flash.
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
    private static final String SETTINGS_PATH = "./cfg/grandmaster_flash";
    private static final String SYSTMP_PATH = "../migration/configfiles";
    private static final String SUBDIR = File.separator + "os";
    private final String tmpDirPath;
    private File flasherHome;

    /**
     * stored os level config files into temp directory. which files to store is controlled by the hidden
     * config file grandmaster_flash
     * @param destination the name of the temp directory where the image source is being stored before compression
     * @param flasherHome home directory of the Flasher; use <code>null</code> if the JVM was already launched from there
     * @throws IOException if something does not work
     */
    public static void saveOSConfigFiles(String destination, File flasherHome) throws IOException {
        OSConfigManager me = new OSConfigManager(destination);
        me.flasherHome = flasherHome;
        me.doSave();
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

    private void doSave() throws IOException {
        File settingsPath = new File(flasherHome, SETTINGS_PATH);
        if (settingsPath.isFile()) {
            FileReader fr = new FileReader(settingsPath);
            BufferedReader grandmasterreader = new BufferedReader(fr);
            String tmp;
            try {
                while((tmp = grandmasterreader.readLine()) != null) {
                    if (!tmp.startsWith("#")) {
                        File osconfigfile = new File(tmp);
                        if (osconfigfile.isFile()) {
                            logger.info("saving " + osconfigfile.getPath());
                            File target = new File(tmpDirPath + SUBDIR + File.separator + osconfigfile.getPath());
                            FileUtils.ensurePath(target.getParentFile());
                            FileUtils.copyFile(osconfigfile, target);
                        } else {
                            logger.info("os config file " + osconfigfile.getPath() + " does not exist on this " +
                                        "system and will not be included in image");
                        }
                    }
                }
            } finally {
                grandmasterreader.close();
                fr.close();
            }
        }
    }

    private void doLoadToRealTarget() throws IOException {
        logger.info("Listing files that potentially need restore");
        ArrayList<String> listofosfiles = listDir(SYSTMP_PATH);
        if (listofosfiles != null) for (String osfiletorestore : listofosfiles) {
            String realtarget;
            if (osfiletorestore.startsWith(SYSTMP_PATH)) {
                realtarget = osfiletorestore.substring(SYSTMP_PATH.length());
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
        final String osfilesroot = tmpDirPath + SUBDIR;
        ArrayList<String> listofosfiles = listDir(osfilesroot);
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

    private ArrayList<String> listDir(String path) {
        File dir = new File (path);
        if (dir.exists() && dir.isDirectory()) {
            ArrayList<String> output = new ArrayList<String>();
            String[] children = dir.list();
            for (String child : children) {
                File childfile = new File(path + File.separator + child);
                if (childfile.isDirectory()) {
                    ArrayList<String> subdirlist = listDir(path + File.separator + child);
                    if (subdirlist != null) {
                        output.addAll(subdirlist);
                    }
                } else {
                    output.add(path + File.separator + child);
                }
            }
            return output;
        } else return null;
    }

    private OSConfigManager(String path) {
        tmpDirPath = path;
    }
}
