package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.util.regex.Pattern;
import java.io.IOException;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 2:00:39 PM
 */
public class UpdateWizardPackageQuestionsStep extends BaseConsoleStep {
    private static final String TITLE = "SecureSpan Gateway Appliance Update Wizard - Locate Update Packages";

    PackageUpdateConfigBean configBean;

    public UpdateWizardPackageQuestionsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        configBean = new PackageUpdateConfigBean("Package Update Information","");
    }

    public boolean validateStep() {
        return false;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {

        try {
            String location = getData(
                    new String[] {
                        "Enter the path to the update package you wish to install:"
                    },
                    "",
                    (Pattern) null,
                    "Invalid Path");
            configBean.setPackageLocation(location);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getTitle() {
        return TITLE;
    }
}
