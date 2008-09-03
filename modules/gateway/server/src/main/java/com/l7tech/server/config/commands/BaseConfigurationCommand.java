package com.l7tech.server.config.commands;


import com.l7tech.server.config.beans.ConfigurationBean;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:53:52 PM
 */
public abstract class BaseConfigurationCommand implements ConfigurationCommand {
    private static final Logger logger = Logger.getLogger(BaseConfigurationCommand.class.getName());

    protected ConfigurationBean configBean;
    protected DateFormat formatter;
    protected boolean cloningMode = false;

    protected BaseConfigurationCommand(ConfigurationBean bean) {
        this();
        this.configBean = bean;
    }

    public BaseConfigurationCommand() {
        formatter = new SimpleDateFormat("E_MMM_d_yyyy_HH_mm");
    }

    public boolean executeSilent() {
        cloningMode = true;
        return execute();
    }

    public String[] getActions() {
        return (configBean != null)?configBean.explain():null;
    }

    public ConfigurationBean getConfigBean() {
        return this.configBean;
    }

    public void setConfigBean(ConfigurationBean configBean) {
        this.configBean = configBean;
    }

    public boolean isCloningMode() {
        return cloningMode;
    }

    public void setCloningMode(boolean cloningMode) {
        this.cloningMode = cloningMode;
    }
}
