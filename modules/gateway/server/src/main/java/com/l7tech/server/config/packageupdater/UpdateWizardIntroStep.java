package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.wizard.BaseConsoleStep;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 2:00:07 PM
 */
public class UpdateWizardIntroStep extends BaseConsoleStep {
    private static final Logger logger = Logger.getLogger(UpdateWizardIntroStep.class.getName());
    private static final String TITLE = "SecureSpan Gateway Appliance Update Wizard";

    public UpdateWizardIntroStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        showNavigation = false;
    }

    public boolean validateStep() {
        return true;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
         printText(new String[] {
                "This wizard will install selected updates to the SecureSpan Gateway Appliance (SSG)" + getEolChar(),
                "Press <Enter> to continue" + getEolChar(),
        });

        try {
            handleInput(consoleWizardUtils.readLine());
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    public String getTitle() {
        return TITLE;
    }
}
