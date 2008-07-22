package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 8, 2008
 * Time: 11:23:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class DBVersion50Checker extends DbVersionChecker{
    private final Logger logger = Logger.getLogger(getClass().getName());
    /*
    * Inital change for 5.0 is the constraint on the identity provider foreign key in rbac_assignments
    * model doesn't allow for this yet so until there are other changes to detect just logging a warning
    * and returning false for now
    * */
    public boolean doCheck(Map<String, Set<String>> tableData) {
        logger.log(Level.WARNING, "4.6 to 5.0 db checker doCheck not yet implemented");
        return false;  
    }

    public String getVersion() {
        return "5.0";
    }
}
