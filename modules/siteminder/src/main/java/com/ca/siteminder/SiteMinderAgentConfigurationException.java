package com.ca.siteminder;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public class SiteMinderAgentConfigurationException extends Exception {
    public SiteMinderAgentConfigurationException(String msg) {
        super(msg);
    }

    public SiteMinderAgentConfigurationException(String msg, Throwable cause) {
        super(msg,cause);
    }

    public SiteMinderAgentConfigurationException(Throwable cause) {
        super(cause);
    }
}
