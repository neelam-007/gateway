/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 20, 2009
 * Time: 3:26:47 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;

//class just to implement JRField so we can adapt it above
public class JRFieldAdapter implements JRField {
    public String getDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class getValueClass() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getValueClassName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDescription(String s) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public JRPropertiesHolder getParentProperties() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public JRPropertiesMap getPropertiesMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean hasProperties() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object clone() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
