/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console;

import com.l7tech.console.util.Preferences;
import com.l7tech.console.util.PreferencesChangedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version Oct 1, 2004
 */
public class SsmApplication
  extends ApplicationObjectSupport implements ApplicationListener {
    private final Logger log = Logger.getLogger(getClass().getName());
    private static SsmApplication ssmApplication;
    private String resourcePath;
    private MainWindow mainWindow;
    private boolean running = false;

    public SsmApplication() {
        if (ssmApplication != null) {
            throw new IllegalStateException("Already initalized");
        }
        ssmApplication = this;
    }

    public static SsmApplication getApplication() {
        return ssmApplication;
    }

    public Object getBean(String beanName) {
        return getApplicationContext().getBean(beanName);
    }

    public MainWindow getMainWindow() {
        return mainWindow;
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException("SSM already running");
        }
        mainWindow = new MainWindow(this);
        // Window listener
        mainWindow.addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been opened.
             */
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        mainWindow.activateLogonDialog();
                    }
                });
            }

            public void windowClosed(WindowEvent e) {
                saveWindowPosition(mainWindow);
                System.exit(0);
            }
        });
        mainWindow.setVisible(true);
        mainWindow.toFront();
        running = true;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
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

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PreferencesChangedEvent) {
            final Preferences prefs = (Preferences)getApplicationContext().getBean("preferences");
            log.finest("preferences have been updated");
            setLookAndFeel(prefs.getString(Preferences.LOOK_AND_FEEL));
            MainWindow mainWindow = getMainWindow();
            if (mainWindow !=null) {
                mainWindow.setInactivitiyTimeout(prefs.getInactivityTimeout());
            }
        }
    }

    /**
     * set the look and feel
     *
     * @param lookAndFeel a string specifying the name of the class that implements
     *                    the look and feel
     */
    private void setLookAndFeel
      (String
      lookAndFeel) {

        if (lookAndFeel == null) return;
        boolean lfSet = true;

        // if same look and feel quick exit
        if (lookAndFeel.
          equals(UIManager.getLookAndFeel().getClass().getName())) {
            return;
        }

        try {
            Object lafObject =
              Class.forName(lookAndFeel).newInstance();
            UIManager.setLookAndFeel((LookAndFeel)lafObject);
        } catch (Exception e) {
            lfSet = false;
        }
        // there was a problem setting l&f, try crossplatform one (best bet)
        if (!lfSet) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception e) {
                return;
            }
        }
        // update panels with new l&f
        MainWindow mainWindow = getMainWindow();
        SwingUtilities.updateComponentTreeUI(mainWindow);
        mainWindow.validate();
    }

}