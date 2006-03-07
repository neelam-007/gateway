package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Dec 19, 2005
 * Time: 9:59:16 AM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardConsoleDatabaseStep extends BaseConsoleStep{
    public ConfigWizardConsoleDatabaseStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected void doInputCollection() {
    }


    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    public String getTitle() {
        return "SSG Database Setup";
    }

}
