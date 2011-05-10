/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.csrfprotection.console;

import com.l7tech.gui.widgets.TextEntryPanel;
import com.l7tech.util.ValidationUtils;

import java.util.ResourceBundle;

public class DomainInputPanel extends TextEntryPanel{
    public DomainInputPanel(String domainValue) {
        super(null, "domainValueProperty", domainValue);
    }

    @Override
    protected String getSyntaxError(String model) {
        if (!ValidationUtils.isValidDomain(model)) {
            return resourceBundle.getString("invalidDomain");
        }
        return null;
    }

    // - PRIVATE

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("com.l7tech.external.assertions.csrfprotection.console.resources.CsrfProtectionAssertionPropertiesDialog");
}
