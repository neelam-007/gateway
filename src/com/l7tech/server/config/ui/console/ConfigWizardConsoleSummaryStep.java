package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:47 AM
 */
public class ConfigWizardConsoleSummaryStep extends BaseConsoleStep{
    public ConfigWizardConsoleSummaryStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        out.println("Press <Enter> to continue");
        out.flush();

        try {
            handleInput(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return "SSG Configuration Summary";
    }

    public boolean shouldApplyConfiguration() {
        return true;
    }

    boolean validateStep() {
        return true;
    }
}
