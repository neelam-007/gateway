package com.l7tech.server.config.exceptions;

/**
 * User: megery
 * Date: Apr 4, 2006
 * Time: 10:05:28 AM
 */
public class WizardNavigationException extends Exception {
    public static final String NAVIGATE_PREV = "PREV";
    public static final String NAVIGATE_NEXT = "NEXT";

    public WizardNavigationException() {
        super();
    }

    public WizardNavigationException(String string) {
        super(string);
    }

    public WizardNavigationException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
