package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 23, 2005
 * Time: 4:49:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class LoggingConfigBean extends BaseConfigurationBean {
    public LoggingConfigBean(String name, String description, OSSpecificFunctions osFunctions) {
        super(name, description, osFunctions);
    }

    void reset() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String[] explain() {
        return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
    }
}
