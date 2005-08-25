package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;

import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:29:25 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ConfigurationBean {
    public String getName();
    public String getDescription();
    public String[] getAffectedObjects();
    public OSSpecificFunctions getOSFunctions();
    public String getElementKey();
    
    public boolean apply();
}
