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

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Thick-client version of SsmApplication.
 */
public class SsmApplicationHeavy extends SsmApplication implements ApplicationListener  {
    private final Logger log = Logger.getLogger(getClass().getName());
    private static SsmApplication ssmApplication;
    private boolean running = false;

    public SsmApplicationHeavy() {
        if (ssmApplication != null) {
            throw new IllegalStateException("Already initalized");
        }
        ssmApplication = this;
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException("SSM already running");
        }
        if (!isSuppressAutoLookAndFeel()) setAutoLookAndFeel();
        mainWindow = new MainWindow(this);
        // Window listener
        mainWindow.setVisible(true);
        mainWindow.toFront();
        running = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainWindow.activateLogonDialog();
            }
        });
    }

    boolean isApplet() {
        return false;
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
    protected void setLookAndFeel
      (String
      lookAndFeel)
    {
        if (!isSuppressAutoLookAndFeel()) {
            setAutoLookAndFeel();
            return;
        }

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