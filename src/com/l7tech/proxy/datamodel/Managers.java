package com.l7tech.proxy.datamodel;

/**
 * Used to obtain datamodel classes.
 * User: mike
 * Date: Jun 3, 2003
 * Time: 2:45:08 PM
 * To change this template use Options | File Templates.
 */
public class Managers {

    /**
     * Get the SsgManager.
     * @return the SsgManager instance.
     */
    public static SsgManager getSsgManager() {
        return SsgManagerImpl.getInstance();
    }
}
