/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import org.springframework.context.ApplicationEvent;

import javax.swing.*;

/**
 * Disables some SsmApplication features when running as Applet.
 */
public class SsmApplicationApplet extends SsmApplication {
    private boolean running = false;

    public SsmApplicationApplet() {
    }

    public Object getBean(String beanName) {
        return getApplicationContext().getBean(beanName);
    }

    public synchronized void run() {
        if (running) {
            throw new IllegalStateException("SSM already running");
        }
        mainWindow = new MainWindow(this);
        running = true;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                mainWindow.activateLogonDialog();
            }
        });
    }

    boolean isApplet() {
        return true;
    }

    public void onApplicationEvent(ApplicationEvent event) {
        // Ignore preference changes, it's an applet
    }

    protected void setLookAndFeel(String lookAndFeel) {
        // Do nothing, it's an applet
    }
}
