package com.l7tech.server.config.beans;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.partition.PartitionInformation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    protected List<String> explanations;

    protected String insertTab = "\t";
    protected static String eol = System.getProperty("line.separator");

    public BaseConfigurationBean(String name, String description) {
        affectedObjects = new HashMap();
        elementName = name;
        elementDescription = description;
        explanations = new ArrayList<String>();
    }

    public abstract void reset();

    public String getElementKey() {
        return ELEMENT_KEY;
    }

    public String getName() {
        return elementName;
    }

    public String getDescription() {
        return elementDescription;
    }

    public String[] explain() {
        explanations.clear();
        populateExplanations();
        return explanations.toArray(new String[explanations.size()]);
    }


    protected OSSpecificFunctions getOsFunctions() {
        PartitionInformation pi = PartitionManager.getInstance().getActivePartition();
        if (pi == null) {
            //SSG might not be migrated to partitioning yet so migrate it.
            PartitionManager.doMigration();
            pi = PartitionManager.getInstance().getActivePartition();
        }
        return pi.getOSSpecificFunctions();
    }

    protected abstract void populateExplanations();
}
