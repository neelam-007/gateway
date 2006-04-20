/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;

import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author alex
 */
public class RequestWssTimestampDialog extends OkCancelDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestWssTimestampDialog");

    public RequestWssTimestampDialog(Frame owner, boolean modal, RequestWssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion));
    }

    public RequestWssTimestampDialog(Dialog owner, boolean modal, RequestWssTimestamp assertion) {
        super(owner, resources.getString("dialog.title"), modal, new RequestWssTimestampPanel(assertion));
    }
}
