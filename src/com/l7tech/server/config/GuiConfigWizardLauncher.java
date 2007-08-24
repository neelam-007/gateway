package com.l7tech.server.config;

import com.l7tech.server.config.ui.gui.ConfigurationWizard;

/**
 * User: megery
 * Date: May 9, 2006
 * Time: 4:39:30 PM
 */
public class GuiConfigWizardLauncher {
    public static void launch(String[] newArgs) {
        ConfigurationWizard.startWizard(newArgs);
    }
}
