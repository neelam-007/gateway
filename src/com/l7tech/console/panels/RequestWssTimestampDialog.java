/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.common.gui.widgets.OkCancelDialog;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class RequestWssTimestampDialog extends OkCancelDialog<RequestWssTimestamp> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestWssTimestampDialog");

    public RequestWssTimestampDialog(Frame owner, boolean modal, RequestWssTimestamp assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion), readOnly);
    }

    public RequestWssTimestampDialog(Dialog owner, boolean modal, RequestWssTimestamp assertion, boolean readOnly) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion), readOnly);
    }
}
