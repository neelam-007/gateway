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
    public ConfigWizardConsoleSummaryStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {

        try {
            printText("The following configuration changes will be made:\n");

            List commandSummary = getParentWizard().getCommandDescription();
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
