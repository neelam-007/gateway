package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;

import java.util.Map;
import java.util.List;

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
    public String[] explain();
    public String getElementKey();
    public List<String> getManualSteps();
}
