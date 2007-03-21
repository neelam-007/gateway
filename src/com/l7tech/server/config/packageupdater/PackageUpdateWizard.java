package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.ui.console.ConfigurationWizard;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 1:58:01 PM
 */
public class PackageUpdateWizard extends ConfigurationWizard {
    public PackageUpdateWizard(InputStream in, PrintStream out) {
        super(in, out);
    }
}
