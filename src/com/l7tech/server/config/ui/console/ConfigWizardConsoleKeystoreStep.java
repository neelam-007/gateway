package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.InputStream;
import java.io.PrintWriter;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 9:59:35 AM
 */
public class ConfigWizardConsoleKeystoreStep extends BaseConsoleStep{
    public ConfigWizardConsoleKeystoreStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    protected void doInputCollection() {
    }

    public String getTitle() {
        return "SSG Keystore Setup";
    }
}
