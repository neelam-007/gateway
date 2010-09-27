package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.wizard.BaseConsoleStep;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.wizard.ConsoleWizardUtils;

import java.io.IOException;
import java.util.logging.Logger;

import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

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

    @Override
    public boolean validateStep() {
        return true;
    }

    @Override
    public void doUserInterview(boolean validated) throws WizardNavigationException {
         printText(new String[] {
                "This wizard will install selected updates to the SecureSpan Gateway Appliance (SSG)" + EOL,
                "Press [Enter] to continue" + EOL,
        });

        try {
            handleInput(ConsoleWizardUtils.readLine());
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
}
