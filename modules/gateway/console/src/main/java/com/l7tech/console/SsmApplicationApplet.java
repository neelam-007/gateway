/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import org.springframework.context.ApplicationEvent;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.LogonDialog;

/**
 * Disables some SsmApplication features when running as Applet.
 */
public class SsmApplicationApplet extends SsmApplication {

    public SsmApplicationApplet() {
    }

    public Object getBean(String beanName) {
        return getApplicationContext().getBean(beanName);
    }

    public synchronized void run() {
        // It's OK to call run() more than one time for an Applet, since it can get init() called multiple times
        if (LogonDialog.isSameApplet()) return;
        mainWindow = new MainWindow(this);
        TopComponents.getInstance().registerComponent("mainWindow", mainWindow);
    }

    public boolean isApplet() {
        return true;
    }

    public void showHelpTopicsRoot() {
        AppletMain appletMain = (AppletMain)TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
        if (appletMain == null)
            throw new IllegalStateException("In applet mode but no appletMain registered");
        appletMain.showHelpTopicsRoot();
    }

    public void onApplicationEvent(ApplicationEvent event) {
        // Ignore preference changes, it's an applet
    }

    protected void setLookAndFeel(String lookAndFeel) {
        // Do nothing, it's an applet
    }
}
