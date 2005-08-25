package com.l7tech.server.config.commands;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
    OSSpecificFunctions osFunctions;
    ConfigurationBean configBean;
    DateFormat formatter;

    private Date currentTime;

    protected BaseConfigurationCommand(ConfigurationBean bean, OSSpecificFunctions osFunctions) {
        this.configBean = bean;
        this.osFunctions = osFunctions;
        formatter = new SimpleDateFormat("E_MMM_d_yyyy_HH_mm");

    }

    public String[] getActionSummary() {
        return configBean.getAffectedObjects();
    }

    protected void backupFiles(File[] files, String backupName) throws IOException {
        if (currentTime == null) {
            currentTime = Calendar.getInstance().getTime();
        }

        if (files != null && files.length > 0) {
            String backupFileName = backupName + "_" + formatter.format(currentTime);
            String fullBackupPath = osFunctions.getSsgInstallRoot() + backupFileName + ".zip";
//            System.out.println("creating ZIP file: " + fullBackupPath);

            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(fullBackupPath));

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
            zos.finish();
            zos.close();
        }
    }
}
