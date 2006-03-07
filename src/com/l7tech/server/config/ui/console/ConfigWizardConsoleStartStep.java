package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:38 AM
 */
public class ConfigWizardConsoleStartStep extends BaseConsoleStep {
    public ConfigWizardConsoleStartStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        super(parent_, is, pw);
    }

    protected void doInputCollection() {
        out.println("This wizard will configure a Secure Span Gateway (SSG).");
        out.println("Press <Enter> to continue");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        try {
            br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        goForward();
    }

    protected ConfigurationCommand makeConfigCommand() {
        return null;
    }

    public String getTitle() {
        return "Welcome to the Secure Span Gateway Configuration Wizard";
    }
}
