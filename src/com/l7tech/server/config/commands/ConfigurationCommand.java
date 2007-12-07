package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:51:55 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConfigurationCommand {
    public boolean execute();
    public boolean executeSilent();
    String[] getActions();
    public ConfigurationBean getConfigBean();
    public void setConfigBean(ConfigurationBean configBean);
}
