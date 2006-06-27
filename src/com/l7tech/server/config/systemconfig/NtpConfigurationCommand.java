package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:27:43 AM
 */
public class NtpConfigurationCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(NtpConfigurationCommand.class.getName());

    private NtpConfigurationBean netBean;
    protected NtpConfigurationCommand(ConfigurationBean bean) {
        super(bean);
        netBean = (NtpConfigurationBean) bean;
    }

    public boolean execute() {
        boolean success = true;

        success = writeNtpLitterFiles();

        return success;
    }

    private boolean writeNtpLitterFiles() {
        boolean success = true;
        File currentWorkingDir = new File(".");
        File configDir = new File(currentWorkingDir, "configfiles");
        if (configDir.mkdir())
            logger.info("Created new directory for NTP configuration: " + configDir.getAbsolutePath());

        File ntpConfFile = ntpConfFile = new File(configDir, "ntpconfig");
        PrintWriter pw = null;
        try {
            if (ntpConfFile.createNewFile())
                logger.info("created file \"" + ntpConfFile.getAbsolutePath() + "\"");
            else
                logger.info("editing file \"" + ntpConfFile.getAbsolutePath() + "\"");

            pw = new PrintWriter(new FileOutputStream(ntpConfFile));
            pw.println(netBean.getTimeServerAddress());
        } catch (IOException e) {
            logger.severe("Error while writing the NTP configuration file: " + e.getMessage());
            success = false;
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
        return success;
    }
}
