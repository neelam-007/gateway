package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.List;
import java.util.Iterator;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:47 AM
 */
public class ConfigWizardConsoleSummaryStep extends BaseConsoleStep{
    private String title = "SSG Configuration Summary";

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

            List commandSummary = getParentWizard().getCommandDescriptions();
            for (Iterator iterator = commandSummary.iterator(); iterator.hasNext();) {
                String[] s = (String[]) iterator.next();
                for (int i = 0; i < s.length; i++) {
                    String s1 = s[i];
                    printText(s1 + "\n");
                }
                printText("\n");

            }
            printText(new String[] {
                    "\n",
                    "Press <Enter> to continue\n"
            });
            handleInput(readLine());
        } catch (IOException e) {
            e.printStackTrace();
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
