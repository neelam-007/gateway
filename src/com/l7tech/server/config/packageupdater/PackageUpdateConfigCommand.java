package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.commands.BaseConfigurationCommand;
import com.l7tech.server.config.beans.ConfigurationBean;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 4:16:10 PM
 */
public class PackageUpdateConfigCommand extends BaseConfigurationCommand {
    PackageUpdateConfigBean packageBean;

    protected PackageUpdateConfigCommand(ConfigurationBean bean) {
        super(bean);
        packageBean = (PackageUpdateConfigBean) bean;
    }

    public boolean execute() {
        return true;
    }
}
