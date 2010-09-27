package com.l7tech.server.config.systemconfig;

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
public class NtpConfigurationCommand extends BaseConfigurationCommand<NtpConfigurationBean> {
    private static final Logger logger = Logger.getLogger(NtpConfigurationCommand.class.getName());

    protected NtpConfigurationCommand(NtpConfigurationBean bean) {
        super(bean);
    }

    @Override
    public boolean execute() {
        return writeNtpLitterFiles() & writeTimezoneLitterFiles();
    }

    private boolean writeTimezoneLitterFiles() {
        boolean success = true;
        if (StringUtils.isNotEmpty(configBean.getTimezone())) {
            File configDir = checkConfigDir();

            if (configDir == null) {
                logger.severe("Could not create the configuration directory. System configuration will not be complete.");
                return false;
            }

            File timezoneConfigFile = new File(configDir, "timezone");
            PrintWriter pw = null;
            try {
                if (timezoneConfigFile.createNewFile())
                    logger.info("created file \"" + timezoneConfigFile.getAbsolutePath() + "\"");
                else
                    logger.info("editing file \"" + timezoneConfigFile.getAbsolutePath() + "\"");
                pw = new PrintWriter(new FileOutputStream(timezoneConfigFile));
                pw.println(configBean.getTimezone());
            } catch (IOException e) {
                logger.severe("Error while writing the timezone configuration file: " + e.getMessage());
                success = false;
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }
        }
        return success;
    }

    private File  checkConfigDir() {
        File currentWorkingDir = new File(".");
        File configDir = new File(currentWorkingDir, "configfiles");
        if (configDir.mkdir())
            logger.info("Created new directory for NTP and Timezone configuration: " + configDir.getAbsolutePath());

        return configDir;
    }

    private boolean writeNtpLitterFiles() {
        boolean success = true;
        if (!configBean.getTimeServers().isEmpty()) {
            File currentWorkingDir = new File(".");
            File configDir = new File(currentWorkingDir, "configfiles");
            if (configDir.mkdir())
                logger.info("Created new directory for NTP and Timezone configuration: " + configDir.getAbsolutePath());

            File ntpConfFile = new File(configDir, "ntpconfig");
            PrintWriter pw = null;
            try {
                if (ntpConfFile.createNewFile())
                    logger.info("created file \"" + ntpConfFile.getAbsolutePath() + "\"");
                else
                    logger.info("editing file \"" + ntpConfFile.getAbsolutePath() + "\"");

                pw = new PrintWriter(new FileOutputStream(ntpConfFile));

                for (String tsInfo : configBean.getTimeServers()) {
                    pw.println(tsInfo);
                }
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
