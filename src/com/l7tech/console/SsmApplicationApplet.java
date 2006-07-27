/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import org.springframework.context.ApplicationEvent;

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
        // It's OK to call run() more than one time for an Applet, since it can get init() called multiple times
        if (running) return;

        mainWindow = new MainWindow(this);
        running = true;
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
