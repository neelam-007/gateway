package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
}
