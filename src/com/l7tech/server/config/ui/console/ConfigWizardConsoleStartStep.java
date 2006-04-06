package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:38 AM
 */
public class ConfigWizardConsoleStartStep extends BaseConsoleStep {

    public ConfigWizardConsoleStartStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
        showNavigation = false;
    }

    boolean validateStep() {
        return true;
    }

    public String getTitle() {
        return "Welcome to the Secure Span Gateway Configuration Wizard";
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        out.println("This wizard will configure a newly installed Secure Span Gateway (SSG)");
        out.println("Press <Enter> to continue");
        out.flush();

        try {
            handleInput(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
