package com.l7tech.console;

import com.l7tech.console.panels.Utilities;
import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.Registry;

import javax.servlet.GenericServlet;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.Policy;

import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyInitializationException;

/**
 * This class is the SSG console Main entry point.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class Main {
    static Logger log = Logger.getLogger(Main.class.getName());
    // splash screen

    private SplashScreen mainSplashScreen = null;

    /**
     * run the application
     * @param args
     */
    public void run(String[] args) {
        try {
            // AWT event dispatching thread handler
            System.setProperty("sun.awt.exception.handler",
                    "com.l7tech.console.logging.AwtErroHandler");
            System.setProperty("java.security.policy", "etc/jini/policy.all");
            System.setProperty("com.l7tech.util.locator.properties",
                    "/com/l7tech/console/resources/services.properties");
            Policy.setPolicy(new DynamicPolicyProvider());
            final JFrame main;

            /* invoke the splash screen */
            showSplashScreen();

            /* load user preferences and merge them with system props */
            Preferences prefs = Preferences.getPreferences();
            prefs.updateFromProperties(System.getProperties(), false);
            /* so it is visible in help/about */
            prefs.updateSystemProperties();
            // where locator looks for implementaitons
            System.setProperty("com.l7tech.util.locator.properties",
                    "/com/l7tech/console/resources/services.properties");

            main = Registry.getDefault().getWindowManager().getMainWindow();
            // Window listener
            main.addWindowListener(
                    new WindowAdapter() {
                        /**
                         * Invoked when a window has been opened.
                         */
                        public void windowOpened(WindowEvent e) {
                            if (mainSplashScreen != null) {
                                mainSplashScreen.dispose();
                            }
                        }

                        public void windowClosed(WindowEvent e) {
                            saveWindowPosition(main);
                            System.exit(0);
                        }
                    });

            main.setVisible(true);
        } catch (HeadlessException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } catch (PolicyInitializationException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } finally {
        }
    }

    /**
     * the "Splash Screen"
     */
    void showSplashScreen() {
        /* Create the splash screen */
        mainSplashScreen = new SplashScreen();
        mainSplashScreen.pack();
        Utilities.centerOnScreen(mainSplashScreen);
        mainSplashScreen.setVisible(true);
    }


    /**
     * Save the window position preference.  Called when the app is closed.
     */
    private void saveWindowPosition(Window w) {
        Point curWindowLocation = w.getLocation();
        Dimension curWindowSize = w.getSize();
        try {
            Preferences prefs = Preferences.getPreferences();
            prefs.setLastWindowLocation(curWindowLocation);
            prefs.setLastWindowSize(curWindowSize);
            prefs.store();
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to save window position prefs: ", e);
        }
    }

    /**
     * Starts the application.
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        new Main().run(args);
    }
}
