package com.l7tech.server.flasher;

import com.l7tech.common.util.FileUtils;

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
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
public class OSConfigManager {
    private static final Logger logger = Logger.getLogger(OSConfigManager.class.getName());
    private static final String SETTINGS_PATH = "./cfg/grandmaster_flash";
    private static final String SUBDIR = File.separator + "os" + File.separator;
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
                        File target = new File(tmpDirPath + SUBDIR + osconfigfile.getPath());
                        ensurePath(target.getParentFile());
                        FileUtils.copyFile(osconfigfile, target);
                    } else {
                        logger.info("os config file " + osconfigfile.getPath() + " does not exist on this system andwill not be included in image");
                    }
                }
            }
        } finally {
            grandmasterreader.close();
            fr.close();
        }
    }

    private void ensurePath(File in) {
        if (in.getParentFile() != null) {
            ensurePath(in.getParentFile());
        }
        if (!in.exists()) {
            logger.fine("creating " + in.getPath());
            in.mkdir();
        }
    }

    private void doLoad() throws IOException {
        // todo
    }

    private OSConfigManager(String path) {
        tmpDirPath = path;
    }
}
