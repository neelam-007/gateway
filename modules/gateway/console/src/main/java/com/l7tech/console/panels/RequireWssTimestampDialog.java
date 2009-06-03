/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class RequireWssTimestampDialog extends AssertionOkCancelDialog<RequireWssTimestamp> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequireWssTimestampDialog");

    public RequireWssTimestampDialog(Frame owner, RequireWssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), new RequireWssTimestampPanel(assertion), assertion);
    }
}
