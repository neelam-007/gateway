package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.InputStream;
import java.io.PrintWriter;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:47 AM
 */
public class ConfigWizardConsoleSummaryStep extends BaseConsoleStep{
    public ConfigWizardConsoleSummaryStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected void doInputCollection() {
    }

    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    public String getTitle() {
        return "SSG Configuration Summary";
    }
}
