package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityProviderConfigManager;

import java.util.logging.Logger;

/**
 * A locator to be used by test classes.
 * To make it current, do the following:
 * System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.TestLocator");
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 24, 2004<br/>
 * $Id$<br/>
 */
public class TestLocator extends Locator {
    public Matches lookup(Template template) {
        logger.warning("lookup called but not implemented!");
        return null;
    }

    public Object lookup(Class clazz) {
        if (clazz.equals(IdentityProviderConfigManager.class)) {
            // todo, return some TestIdProvConfMan
            logger.warning("todo");
        }
        return null;
    }

    private static Logger logger = Logger.getLogger(TestLocator.class.getName());
}
