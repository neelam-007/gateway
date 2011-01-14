package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:53:52 PM
 */
public abstract class BaseConfigurationCommand<CBT extends ConfigurationBean> implements ConfigurationCommand {

    // - PUBLIC

    @Override
    public String[] getActions() {
        return ( configBean != null ) ? configBean.explain() : null;
    }

    // - PROTECTED

    protected CBT configBean;
    protected DateFormat formatter;

    protected BaseConfigurationCommand(CBT bean) {
        formatter = new SimpleDateFormat("E_MMM_d_yyyy_HH_mm");
        this.configBean = bean;
    }

    protected boolean writeConfigFile(String filename, String configData) {
        boolean success = true;
        if (!StringUtils.isEmpty(configData)) {
            File currentWorkingDir = new File(".");
            File configDir = new File(currentWorkingDir, "configfiles");
            if (configDir.mkdir())
                logger.info("Created new configuration directory: " + configDir.getAbsolutePath());

            File configFile = new File(configDir, filename);
            PrintWriter configWriter = null;
            try {
                if (configFile.createNewFile())
                    logger.info("created file \"" + configFile.getAbsolutePath() + "\"");
                else
                    logger.info("editing file \"" + configFile.getAbsolutePath() + "\"");

                configWriter = new PrintWriter(new FileOutputStream(configFile));
                configWriter.print(configData);

            } catch (IOException e) {
                logger.severe("Could not create file:" + configFile.getAbsolutePath());
                success = false;
            } finally {
                if (configWriter != null)
                    configWriter.close();
            }
        }
        return success;
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(BaseConfigurationCommand.class.getName());

}
