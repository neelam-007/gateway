package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep{
    public ConfigWizardConsoleResultsStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected void doInputCollection() {
    }

    public void collectInput(InputStream in, OutputStream out) {
    }

    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    public String getTitle() {
        return "SSG Configuration Results";
    }
}
