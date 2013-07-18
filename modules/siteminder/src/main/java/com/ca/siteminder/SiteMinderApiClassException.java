package com.ca.siteminder;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 6/17/13
 */
public class SiteMinderApiClassException extends Exception {
    public SiteMinderApiClassException(String msg) {
        super(msg);
    }

    public SiteMinderApiClassException(String msg, Exception cause) {
        super(msg, cause);
    }
}
