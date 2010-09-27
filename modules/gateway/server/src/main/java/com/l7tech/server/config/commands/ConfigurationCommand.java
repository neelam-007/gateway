package com.l7tech.server.config.commands;

import com.l7tech.server.config.beans.ConfigurationBean;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 16, 2005
 * Time: 2:51:55 PM
 */
public interface ConfigurationCommand {

    public boolean execute();

    String[] getActions();
}
