package com.l7tech.server.config.commands;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.PartitionConfigBean;
import com.l7tech.server.partition.PartitionInformation;

import java.util.logging.Logger;

/**
 * User: megery
 * Date: Nov 17, 2006
 * Time: 10:51:03 AM
 */
public class PartitionConfigCommand extends BaseConfigurationCommand{
    private static final Logger logger = Logger.getLogger(PartitionConfigCommand.class.getName());

    public PartitionConfigCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        PartitionConfigBean partitionBean = (PartitionConfigBean) configBean;
        PartitionInformation pInfo = partitionBean.getPartitionInfo();

        OSSpecificFunctions partitionFunctions = pInfo.getOSSpecificFunctions();

        logger.info("executing partitioning command");
        if (pInfo.isNewPartition()) {
            logger.info("Creating \"" + partitionFunctions.getPartitionBase() + "/" + pInfo.getPartitionId() + "\" Directory");
        } else {
            logger.info("Updating configuration in \"" + partitionFunctions.getPartitionBase() + "\" Directory");
        }

        return true;
    }
}
