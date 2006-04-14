package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.List;
import java.util.Iterator;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep{
    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz, OSSpecificFunctions osFunctions) {
        super(parentWiz, osFunctions);
    }

    void doUserInterview(boolean validated) throws WizardNavigationException {
        printText("Here are the results\n");
        List logs = ListHandler.getLogList();
        if (logs != null) {
            for (Iterator iterator = logs.iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                printText(s + "\n");
            }
        }

        printText("Press <Enter> to finish the wizard\n");

        try {
            handleInput(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return "SSG Configuration Results";
    }

    boolean validateStep() {
        return true;
    }
}
