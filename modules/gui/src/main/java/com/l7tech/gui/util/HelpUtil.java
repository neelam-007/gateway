/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.util;

import com.l7tech.util.ExceptionUtils;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.BrowserLauncherRunner;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Utility code for launching web help.
 */
public class HelpUtil {
    private static final Logger logger = Logger.getLogger(HelpUtil.class.getName());

    /**
     * the default help URL.  May be overridden once we connect to the Gateway.
     */
    public static final String DEFAULT_HELP_URL = "http://wiki.ca.com/gateway";
    private static BrowserLauncher browserLauncher;
    private static String helpUrl = null;

    /**
     * Set a custom help url to override the default.
     *
     * @param helpUrl a new custom help URL, or null to clear it and use the default.
     */
    public static void setHelpUrl( String helpUrl ) throws IllegalArgumentException {

        /**
         * Using this Regex to validate that only http(s) urls are allowed for the help location.  Main reason
         * for this is that otherwise opening this up through SSM Applet will allow access to the file system
         * outside the JVM sandbox and could pose a security risk
         */
        if (helpUrl.matches("^https?://.*$"))
                HelpUtil.helpUrl = helpUrl;
        else {
            String errorMsg = new String ("Unable to launch browser for help: does not use http(s) protocol");
            logger.log(Level.WARNING, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

    }

    /**
     * @return the currently active help URL, which may be the default help URL.
     */
    public static String getHelpUrl() {
        return helpUrl != null ? helpUrl : DEFAULT_HELP_URL;
    }

    /**
     * Opens the root help topics in an external web browser.  If a browser is found, it is launched and this
     * method returns immediately.  If this operation fails, a modal error message dialog will be displayed
     * and this method will not return until the user acknowledges it.
     *
     * @param parentComponent  parent component if an error dialog must be displayed, or null to use the default
     *                         frame (same as JOptionPane)
     */
    public static void showHelpTopicsRoot( Frame parentComponent ) {
        Exception problem = null;

        try {
            if ( helpUrl == null )
                helpUrl = DEFAULT_HELP_URL;
            logger.info("Launching web help URL: " + helpUrl);
            BrowserLauncherRunner runner = new BrowserLauncherRunner(getBrowserLauncher(), helpUrl, null);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();
        } catch (Exception e) {
            problem = e;
        }

        if (problem != null) {
            logger.log(Level.WARNING, "Unable to launch browser for help: " + ExceptionUtils.getMessage(problem), problem);
            JOptionPane.showMessageDialog(parentComponent,
                                          "Unable to open the help system. To view the help system, open the following URL\n" +
                                                  "in your preferred web browser: \n\n  " + helpUrl,
                                          "Cannot Open Help",
                                          JOptionPane.WARNING_MESSAGE);
        }
    }

    private static BrowserLauncher getBrowserLauncher() throws BrowserLaunchingInitializingException, UnsupportedOperatingSystemException {
        if (browserLauncher == null) {
            browserLauncher = new BrowserLauncher(null);
        }
        return browserLauncher;
    }
}
