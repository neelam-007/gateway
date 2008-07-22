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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Utility code for launching web help.
 */
public class HelpUtil {
    private static final Logger logger = Logger.getLogger(HelpUtil.class.getName());

    /**
     * the path to WebHelp start file, relative to the working dir.
     */
    public static final String HELP_FILE_NAME = "help/_start.htm";
    private static BrowserLauncher browserLauncher;

    /**
     * Opens the root help topics in an external web browser.  If a browser is found, it is launched and this
     * method returns immediately.  If this operation fails, a modal error message dialog will be displayed
     * and this method will not return until the user acknowledges it.
     *
     * @param homePath  the application home directory, that contains the "help" subdirectory.  Must not be null.
     * @param parentComponent  parent component if an error dialog must be displayed, or null to use the default
     *                         frame (same as JOptionPane)
     */
    public static void showHelpTopicsRoot(String homePath, Frame parentComponent) {
        String applicationHome = homePath;
        if (!applicationHome.endsWith("/")) applicationHome += "/";
        String helpPath =applicationHome + HELP_FILE_NAME;
        File helpFile = new File(helpPath);
        Exception problem = null;

        try {
            String helpUrl = helpFile.getCanonicalFile().toURI().toURL().toExternalForm();
            logger.info("Launching web help URL: " + helpUrl);
            BrowserLauncherRunner runner = new BrowserLauncherRunner(getBrowserLauncher(), helpUrl, null);
            Thread launcherThread = new Thread(runner);
            launcherThread.start();
        } catch (BrowserLaunchingInitializingException e) {
            problem = e;
        } catch (UnsupportedOperatingSystemException e) {
            problem = e;
        } catch (IOException e) {
            problem = e;
        }

        if (problem != null) {
            logger.log(Level.WARNING, "Unable to launch browser for webhelp: " + ExceptionUtils.getMessage(problem), problem);
            JOptionPane.showMessageDialog(parentComponent,
                                          "Unable to open the help system. To view the help system, open the following file\n" +
                                                  "in your preferred web browser: \n\n  " + helpPath,
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
