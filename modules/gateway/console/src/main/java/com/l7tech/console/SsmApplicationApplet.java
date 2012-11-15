/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console;

import com.l7tech.console.util.TopComponents;
import com.l7tech.xml.SaxonUtils;
import org.springframework.context.ApplicationEvent;

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
        performAppletSpecificInitialization();
        mainWindow = new MainWindow(this);
        TopComponents.getInstance().registerComponent("mainWindow", mainWindow);
    }

    private void performAppletSpecificInitialization() {
        // Disable Saxon bytecode generation since loading classes won't work in the applet (Bug #13268)
        SaxonUtils.setEnableByteCodeGeneration(false);
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
