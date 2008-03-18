/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.OkCancelDialog;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class ReplayProtectionPropertiesDialog extends OkCancelDialog<RequestWssReplayProtection> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.ReplayProtectionPropertiesDialog");

    public ReplayProtectionPropertiesDialog(Frame owner, boolean modal, RequestWssReplayProtection assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new ReplayProtectionPropertiesPanel(assertion), readOnly);
    }

    public ReplayProtectionPropertiesDialog(Dialog owner, boolean modal, RequestWssReplayProtection assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new ReplayProtectionPropertiesPanel(assertion), readOnly);
    }
}