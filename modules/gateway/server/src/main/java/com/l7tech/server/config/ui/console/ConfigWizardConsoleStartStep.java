package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:38 AM
 */
public class ConfigWizardConsoleStartStep extends BaseConsoleStep {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleStartStep.class.getName());

    public ConfigWizardConsoleStartStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        showNavigation = false;
    }

    public boolean validateStep() {
        return true;
    }

    public String getTitle() {
        return "Welcome to the SecureSpan Gateway Configuration Wizard " + getVersionString();
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(new String[] {
                "This wizard will configure a newly installed SecureSpan Gateway (SSG)" + getEolChar(),
                "Press <Enter> to continue" + getEolChar(),
        });


        try {
            handleInput(consoleWizardUtils.readLine());
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }
}
