package com.l7tech.console;

import com.l7tech.console.panels.Utilities;
import com.l7tech.console.util.Preferences;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: emil
 * Date: May 9, 2003
 * Time: 2:02:27 PM
 * To change this template use Options | File Templates.
 */
public class Main {
    // splash screen
    private static SplashScreen mainSplashScreen = null;

    /**
     * the "Splash Screen"
     *
     * @param screenSize to use when determining the size of the splash screen
     */
    static void showSplashScreen(Dimension screenSize) {
        /* Create the splash screen */
        mainSplashScreen = new SplashScreen();
        mainSplashScreen.pack();
        Utilities.centerOnScreen(mainSplashScreen);
        mainSplashScreen.setVisible(true);
    }


    /**
     * Save the window position preference.  Called when the app is closed.
     */
    private static void saveWindowPosition(Window w) {
      Point curWindowLocation = w.getLocation();
      Dimension curWindowSize = w.getSize();
      try {
        Preferences prefs = Preferences.getPreferences();
        prefs.setLastWindowLocation(curWindowLocation);
        prefs.setLastWindowSize(curWindowSize);
        prefs.store();
      } catch (IOException e) {
        MainWindow.log.debug("unable to save window position prefs: ", e);
      }
    }

    /**
     * Starts the application.
     * @param args an array of command-line arguments
     */
    public static void main(String[] args) {
        try {
            final MainWindow main;
            /* Create the frame */
            /* Calculate the screen size */
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            /* invoke the splash screen */
            showSplashScreen(screenSize);

            /* load user preferences and merge them with system props */
            Preferences prefs = Preferences.getPreferences();
            prefs.updateFromProperties(System.getProperties(), false);
            /* so it is visible in help/about */
            prefs.updateSystemProperties();

            System.setProperty("com.l7tech.util.locator.properties",
                               "/com/l7tech/console/resources/services.properties");

            main = new MainWindow("SSG console");
            // Window listener
            main.addWindowListener(
                                  new WindowAdapter() {
                                    /**
                                     * Invoked when a window has been opened.
                                     */
                                    public void windowOpened(WindowEvent e) {
                                      if (mainSplashScreen !=null) {
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
        } finally {
        }
    }
}
