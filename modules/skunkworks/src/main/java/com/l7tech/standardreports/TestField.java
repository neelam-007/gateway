/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Sep 29, 2008
 * Time: 1:52:46 PM
 * Implementation of JRField which stores one property 'name', used for testing {@link TimePeriodDataSource}
 */
package com.l7tech.standardreports;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;

public class TestField implements JRField {

    private String name;

    public TestField(String name){
        this.name = name;
    }

    public String getDescription() {
        return null;
    }

    public String getName() {
        return name;
    }

    public Class getValueClass() {
        return null;
    }

    public String getValueClassName() {
        return null;
    }

    public void setDescription(String s) {
    }

    public JRPropertiesHolder getParentProperties() {
        return null;
    }

    public JRPropertiesMap getPropertiesMap() {
        return null;
    }

    public boolean hasProperties() {
        return false;
    }

    public Object clone() {
        return null;
    }
}
