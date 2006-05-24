package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.beans.WindowsServiceBean;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 25, 2005
 * Time: 10:50:42 AM
 * To change this template use File | Settings | File Templates.
 */
public class WindowsServiceCommand extends BaseConfigurationCommand {
    public WindowsServiceCommand(ConfigurationBean bean) {
        super(bean);
    }

    public boolean execute() {
        boolean success = true;
        WindowsServiceBean svcCommand = (WindowsServiceBean) configBean;
        if (svcCommand.isDoService()) {
            System.out.println("Will configure the SSG as service");
        }
        else {
            System.out.println("Will not configure the SSG as a service");
        }
        return success;
    }
}
