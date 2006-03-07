package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.InputStream;
import java.io.PrintWriter;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:58:53 AM
 */
public class ConfigWizardConsoleClusteringStep extends BaseConsoleStep{
    public ConfigWizardConsoleClusteringStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected void doInputCollection() {
    }

    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    public String getTitle() {
        return "SSG Clustering Setup";
    }
}
