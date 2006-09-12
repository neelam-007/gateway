package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.BaseConfigurationCommand;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Jun 22, 2006
 * Time: 10:27:43 AM
 */
public class NtpConfigurationCommand extends BaseConfigurationCommand {
    private static final Logger logger = Logger.getLogger(NtpConfigurationCommand.class.getName());

    private NtpConfigurationBean ntpBean;
    protected NtpConfigurationCommand(ConfigurationBean bean) {
        super(bean);
        ntpBean = (NtpConfigurationBean) bean;
    }

    public boolean execute() {
        boolean success = true;

        success = writeNtpLitterFiles();

        return success;
    }

    private boolean writeNtpLitterFiles() {
        boolean success = true;
        if (StringUtils.isNotEmpty(ntpBean.getTimeServerAddress())) {
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
                pw.println(ntpBean.getTimeServerAddress());
            } catch (IOException e) {
                logger.severe("Error while writing the NTP configuration file: " + e.getMessage());
                success = false;
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
        return success;
    }
}
