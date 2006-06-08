package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.ConfigurationWizard;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:12:55 AM
 */
public class SystemConfigurationWizard extends ConfigurationWizard {
    public SystemConfigurationWizard(InputStream in, PrintStream out) {
        super(in, out);

        //this will throw if we are on windows so we'll fail early
        osFunctions.getNetworkConfigurationDirectory();
    }
}
