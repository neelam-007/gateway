package com.l7tech.server.config.wizard;

import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import java.util.*;
import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep {

    private String title = "Configuration Results";
    private static final String CONFIG_ERRORS_TEXT = EOL + "*** Configuration problems detected: There were warnings and/or errors during configuration, see the logs above for details. ***" + EOL;
    private String successMsg = "The configuration was successfully applied." + EOL + "You must restart the SSG in order for the configuration to take effect." + EOL;

    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        showNavigation = false;
    }

    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz, String title) {
        this(parentWiz);
        this.title = title;
    }

    @Override
    public void doUserInterview(boolean validated) throws WizardNavigationException {
        ConfigurationWizard wizard = getParentWizard();

        if (wizard.isHadFailures())
            printText("There were errors during configuration, see below for details" + EOL);
        else {
            printText(successMsg);
        }

        printText(EOL + "The following is a summary of the actions taken by the wizard" + EOL);

        printText(EOL);

        List<String> logs = ListHandler.getLogList();
        if (logs != null) {
            for (String log : logs) {
                if (log != null) printText(log + EOL);
            }
        }

        if (wizard.isHadFailures()) {
            printText(CONFIG_ERRORS_TEXT);
            printText(EOL + "The wizard will now exit." + EOL);
        }
        else
            printText(EOL + "Configuration complete. The wizard will now exit." + EOL);
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean validateStep() {
        return true;
    }

    @Override
    public boolean isShowQuitMessage() {
        return false;
    }

    public void setSuccessMessage(String msg) {
        successMsg = msg;
    }
}
