package com.l7tech.server.config.commands;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 10:51:03 AM
 */
public class PartitionConfigCommand extends BaseConfigurationCommand{

    private static final Logger logger = Logger.getLogger(PartitionConfigCommand.class.getName());
    
    public PartitionConfigCommand() {
        super();
    }

    public PartitionConfigCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        boolean success = true;
        PartitionInformation pInfo = PartitionManager.getInstance().getActivePartition();
        try {
            PartitionActions.updateSystemProperties(pInfo, true);
            updateStartupScripts(pInfo);
            enablePartitionForStartup(pInfo);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred during partition configuration. " + ExceptionUtils.getMessage(e), e);
            success = false;
        }
        return success;
    }

    private void enablePartitionForStartup(PartitionInformation pInfo) {
        PartitionActions.enablePartitionForStartup(pInfo);
    }

    private void updateStartupScripts(PartitionInformation pInfo) throws IOException, InterruptedException {
        if (pInfo.getOSSpecificFunctions().isWindows()) {
            updateStartupScriptWindows(pInfo);
        }
    }

    private void updateStartupScriptWindows(PartitionInformation pInfo) throws IOException, InterruptedException {
        PartitionActions.installWindowsService(pInfo);
    }
}