/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.ext;

import com.l7tech.server.ComponentConfig;

/**
 * @author emil
 * @version Feb 13, 2004
 */
public class TestComponentConfig implements ComponentConfig {
    public String getProperty(String propName) {
        final String prop = CustomAssertionsBootProcess.KEY_CONFIG_FILE;
        if (!prop.equals(propName)) {
            throw new IllegalArgumentException("the only property supported is '" + prop + "'");
        }
        return "/com/l7tech/policy/assertion/ext/testext.properties";
    }
}