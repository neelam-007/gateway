/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gui.util;

import com.l7tech.util.BuildInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import edu.stanford.ejalbert.BrowserLauncher;
import edu.stanford.ejalbert.BrowserLauncherRunner;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility code for launching web help.
 */
public class HelpUtil {
    private static final Logger logger = Logger.getLogger(HelpUtil.class.getName());

    /**
     * the default help URL.  May be overridden once we connect to the Gateway.
     */
    private static final String PROP_DEFAULT_HELP_URL = "com.l7tech.gui.util.HelpUtil.defaultHelpUrl";
    private static final String DEFAULT_DEFAULT_HELP_URL = "https://docops.ca.com/gateway";
    private static final String DEFAULT_VERSIONED_HELP_URL = "https://docops.ca.com/display/gateway";
    private static final String XVC_HELP_URI = "https://docops.ca.com/xvc";
    public static final String DEFAULT_HELP_URL = SyspropUtil.getString( PROP_DEFAULT_HELP_URL, DEFAULT_DEFAULT_HELP_URL );
    private static BrowserLauncher browserLauncher;
    private static String helpUrl = null;

    /**
     * Set a custom help url to override the default.
     *
     * @param helpUrl a new custom help URL, or null to clear it and use the default.
     */
    public static void setHelpUrl( @Nullable String helpUrl ) throws IllegalArgumentException {

        /**
         * Using this Regex to validate that only http(s) urls are allowed for the help location.  Main reason
         * for this is that otherwise opening this up through SSM Applet will allow access to the file system
         * outside the JVM sandbox and could pose a security risk
         */
        if ( helpUrl == null ) {
            HelpUtil.helpUrl = null;
        } else if ( helpUrl.matches("^https?://.*$")) {
            HelpUtil.helpUrl = helpUrl;
        } else {
            HelpUtil.helpUrl = null;
            String errorMsg = "Unable to launch browser for help: does not use http(s) protocol";
            logger.log(Level.WARNING, errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    /**
     * @return the currently active help URL, which may be the default help URL.
     */
    public static String getHelpUrl(@Nullable final String gatewayVersion) {
        return (helpUrl != null && helpUrl.trim().length() > 0) ? helpUrl : (StringUtils.isEmpty(gatewayVersion) ? DEFAULT_HELP_URL : (DEFAULT_VERSIONED_HELP_URL + gatewayVersion));
    }

    /**
     * Opens the root help topics in an external web browser.  If a browser is found, it is launched and this
     * method returns immediately.  If this operation fails, a modal error message dialog will be displayed
     * and this method will not return until the user acknowledges it.
     *
     * @param parentComponent  parent component if an error dialog must be displayed, or null to use the default
     *                         frame (same as JOptionPane)
     */
    //TODO: Refactor when the XVC is no longer supported/released
    public static void showHelpTopicsRoot( Frame parentComponent, boolean isXVChelp ) {
        showHelpTopicsRoot(parentComponent, isXVChelp, null);
    }

    public static void showHelpTopicsRoot( Frame parentComponent, boolean isXVChelp, @Nullable final String gatewayVersion ) {
        Exception problem = null;

        String url = helpUrl;
        try {
            if ( url == null )
                url = isXVChelp ? XVC_HELP_URI : (gatewayVersion == null ? DEFAULT_HELP_URL : (DEFAULT_VERSIONED_HELP_URL + gatewayVersion));

            logger.info("Launching web help URL: " + url);
            BrowserLauncherRunner runner = new BrowserLauncherRunner(getBrowserLauncher(), url, null);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();
        } catch (Exception e) {
            problem = e;
        }

        if (problem != null) {
            logger.log(Level.WARNING, "Unable to launch browser for help: " + ExceptionUtils.getMessage(problem), problem);
            JOptionPane.showMessageDialog(parentComponent,
                                          "Unable to open the help system. To view the help system, open the following URL\n" +
                                                  "in your preferred web browser: \n\n  " + url,
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
