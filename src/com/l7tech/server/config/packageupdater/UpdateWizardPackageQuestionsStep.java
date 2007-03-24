package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.ui.console.BaseConsoleStep;
import com.l7tech.server.config.ui.console.ConfigurationWizard;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.File;
import java.util.regex.Pattern;

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
        boolean isValid = false;
        try {
            checkValidLocation();
            checkValidUpdatePackage();
            isValid = true;
        } catch (UpdateWizardException e) {
            printText("*** " + e.getMessage() + " ***");
        }
        return isValid;
    }

    private void checkValidUpdatePackage() {

    }

    private void checkValidLocation() throws UpdateWizardException {
        String path = configBean.getPackageLocation();
        if (StringUtils.isEmpty(path)) {
            throw new UpdateWizardException("A valid path to the update package was not specified.");
        }

        File updatePath = new File(path);
        if (updatePath.exists()) {
            if (updatePath.isFile()) {
                checkZipFile(updatePath);
            }
        } else {
            throw new UpdateWizardException("No valid update package was found at \"" + path + "\"");
        }
    }

    private void checkZipFile(File updatePath) {
        if (updatePath == null) {
            
        }
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
