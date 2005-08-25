package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ConfigurationCommand;

import java.util.Map;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 11, 2005
 * Time: 11:38:38 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class BaseConfigurationBean implements ConfigurationBean {

    protected String ELEMENT_KEY;
    protected Map affectedObjects;

    protected String elementName;
    protected String elementDescription;
    OSSpecificFunctions osFunctions;

    protected String insertTab = "\t";


    public BaseConfigurationBean(String name, String description, OSSpecificFunctions osFunctions) {
        affectedObjects = new HashMap();
        elementName = name;
        elementDescription = description;
        this.osFunctions = osFunctions;
    }

    abstract void reset();
    
    public boolean apply() {
        return (backup() && doApply());
    }

    public String getElementKey() {
        return ELEMENT_KEY;
    }

    private boolean doApply() {
        System.out.println("Applying settings");
        return true;
    }

    private boolean backup() {
        System.out.println("Backup up files");
        return true;
    }

    public String getName() {
        return elementName;
    }

    public String[] getAffectedObjects() {
        return explain();
    }

    public String getDescription() {
        return elementDescription;
    }

    public OSSpecificFunctions getOSFunctions() {
        return osFunctions;
    }

    public abstract String[] explain();
}
