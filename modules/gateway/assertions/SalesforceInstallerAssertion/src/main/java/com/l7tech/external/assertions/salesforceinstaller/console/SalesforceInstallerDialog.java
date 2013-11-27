package com.l7tech.external.assertions.salesforceinstaller.console;

import com.l7tech.console.panels.bundles.BundleInstallerDialog;
import com.l7tech.external.assertions.salesforceinstaller.SalesforceInstallerAssertion;

import java.awt.*;

public class SalesforceInstallerDialog extends BundleInstallerDialog {
    protected static String BASE_FOLDER_NAME = "Required for Salesforce";

    public SalesforceInstallerDialog (Frame owner) {
        super(owner, BASE_FOLDER_NAME, SalesforceInstallerAssertion.class.getName());
    }
}
