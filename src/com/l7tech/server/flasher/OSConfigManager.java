package com.l7tech.server.flasher;

import com.l7tech.common.util.FileUtils;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import java.util.ArrayList;

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
public class OSConfigManager {
    private static final Logger logger = Logger.getLogger(OSConfigManager.class.getName());
    private static final String SETTINGS_PATH = "./cfg/grandmaster_flash";
    private static final String SYSCONFIGFILES_PATH = "../sysconfigwizard/configfiles";
    private static final String SUBDIR = File.separator + "os";
    private final String tmpDirPath;

    /**
     * stored os level config files into temp directory. which files to store is controlled by the hidden
     * config file grandmaster_flash
     * @param destination the name of the temp directory where the image source is being stored before compression
     * @throws IOException if something does not work
     */
    public static void saveOSConfigFiles(String destination) throws IOException {
        OSConfigManager me = new OSConfigManager(destination);
        me.doSave();
    }

    /**
     * restore os level files stored from an exploded ssg image
     * @param source the exploded image
     * @throws IOException if something goes wrong
     */
    public static void restoreOSConfigFiles(String source) throws IOException {
        OSConfigManager me = new OSConfigManager(source);
        me.doLoad();
    }

    private void doSave() throws IOException {
        FileReader fr = new FileReader(SETTINGS_PATH);
        BufferedReader grandmasterreader = new BufferedReader(fr);
        String tmp;
        try {
            while((tmp = grandmasterreader.readLine()) != null) {
                if (!tmp.startsWith("#")) {
                    File osconfigfile = new File(tmp);
                    if (osconfigfile.exists()) {
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

    private void doLoad() throws IOException {
        final String osfilesroot = tmpDirPath + SUBDIR;
        ArrayList<String> listofosfiles = listDir(osfilesroot);
        boolean systemfileoverwritten = false;
        for (String osfiletorestore : listofosfiles) {
            String restoretarget;
            if (osfiletorestore.startsWith(osfilesroot)) {
                restoretarget = osfiletorestore.substring(osfilesroot.length());
            } else {
                // if this happens, it's a bug
                throw new RuntimeException("unexpected path for " + osfiletorestore);
            }
            logger.info("Restoring " + osfiletorestore + " into " + restoretarget);
            File fromFile = new File(osfiletorestore);
            File toFile = new File(restoretarget);
            // todo, change this code to use the ssgconfig
            System.out.println("TODO Overwriting " + restoretarget);
            /*
            toFile.delete();
            FileUtils.copyFile(fromFile, toFile);
            systemfileoverwritten = true;
            */
        }
        if (systemfileoverwritten) {
            // check if the sysconfig has pending overwrites
            ArrayList<String> res = listDir(SYSCONFIGFILES_PATH);
            if (res != null && res.size() > 0) {
                String issue = "System files have been overwritten but there seems to be pending sysconfig " +
                               "overwrites which may conflict. You may need to reboot SecureSpan Gateway and " +
                               "try restore afterwards.";
                logger.warning(issue);
                System.out.println(issue);
                StringBuffer buf = new StringBuffer("List of pending sysconfig overwrites: ");
                for (String s : res) {
                    buf.append(s).append(", ");
                }
                logger.warning(buf.toString());
            } else {
                System.out.println("\nCertain system files have been overwritten, you may need to reboot the " +
                                   "SecureSpan Gateway.");
            }
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
