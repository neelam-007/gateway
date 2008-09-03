package com.l7tech.server.config.wizard;

import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:47 AM
 */
public class ConfigWizardConsoleSummaryStep extends BaseConsoleStep{
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleSummaryStep.class.getName());
    private String title = "Configuration Summary";

    public ConfigWizardConsoleSummaryStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
    }

    public ConfigWizardConsoleSummaryStep(ConfigurationWizard parentWiz, String title) {
        this(parentWiz);
        this.title = title;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {

        try {
            printText("The following configuration changes will be made:\n");

            List<String[]> commandSummary = getParentWizard().getCommandDescriptions();
            for (String[] strings : commandSummary) {
                for (String s : strings) {
                    printText(s + "\n");
                }
                printText("\n");
            }

            printText(new String[] {
                    "\n",
                    "Press <Enter> to continue\n"
            });
            handleInput(readLine());
        } catch (IOException e) {
            logger.severe("Exception caught: " + e.getMessage());
        }
    }

    public String getTitle() {
        return title;
    }

    public boolean shouldApplyConfiguration() {
        return true;
    }

    public boolean validateStep() {
        return true;
    }
}
