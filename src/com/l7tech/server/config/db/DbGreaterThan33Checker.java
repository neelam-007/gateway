package com.l7tech.server.config.db;

import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 28, 2005
 * Time: 2:09:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class DbGreaterThan33Checker extends DbVersionChecker {
    public boolean doCheck(Hashtable tableData) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getVersion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
