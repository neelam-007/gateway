package com.l7tech.server.config.commands;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:53:52 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseConfigurationCommand implements ConfigurationCommand {
    private static final Logger logger = Logger.getLogger(BaseConfigurationCommand.class.getName());

    protected OSSpecificFunctions osFunctions;
    protected ConfigurationBean configBean;
    DateFormat formatter;

    private Date currentTime;

    protected BaseConfigurationCommand(ConfigurationBean bean) {
        this.configBean = bean;
        osFunctions = OSDetector.getOSSpecificFunctions();
        formatter = new SimpleDateFormat("E_MMM_d_yyyy_HH_mm");

    }

    protected void backupFiles(File[] files, String backupName) {
        if (currentTime == null) {
            currentTime = Calendar.getInstance().getTime();
        }

        String backupFileName = backupName + "_" + formatter.format(currentTime);
        String fullBackupPath = osFunctions.getSsgInstallRoot() + "configwizard/" + backupFileName + ".zip";
        if (files != null && files.length > 0) {
            logger.info("creating ZIP file: " + fullBackupPath);


            ZipOutputStream zos = null;
            try {
                zos = new ZipOutputStream(new FileOutputStream(fullBackupPath));
                File origFile;
                FileInputStream fis;
                for (int i = 0; i < files.length; i++) {
                    origFile = files[i];
                    if (origFile != null && origFile.exists()) {
                        zos.putNextEntry(new ZipEntry(origFile.getAbsolutePath()));

                        fis = new FileInputStream(origFile);

                        byte[] buf = new byte[10240];
                        int len = 0;
                        while ((len = fis.read(buf)) > 0) {
                            zos.write(buf, 0, len);
                        }
                        zos.closeEntry();
                        fis.close();
                    }
                }
            } catch (IOException e) {
                logger.warning("unable to create backup zip file: [" + e.getMessage() + "]");
            } finally {
                if (zos != null) {
                    try { zos.finish(); } catch (IOException e) {}
                    try { zos.close(); } catch (IOException e) {}
                }
            }
        }
    }

    public String[] getActions() {
        if (configBean != null) return configBean.explain();
        return null;
    }

}
