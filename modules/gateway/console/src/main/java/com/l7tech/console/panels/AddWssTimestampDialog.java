/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AddWssTimestamp;
import com.l7tech.gui.widgets.OkCancelDialog;

import java.awt.*;

/**
 * @author alex
 */
public class AddWssTimestampDialog extends OkCancelDialog<AddWssTimestamp> {
    public AddWssTimestampDialog(Frame owner, boolean modal, AddWssTimestamp assertion, boolean readOnly) {
        super(owner, "Add Timestamp Properties", modal, new AddWssTimestampPanel(assertion), readOnly);
    }

    public AddWssTimestampDialog(Dialog owner, boolean modal, AddWssTimestamp assertion, boolean readOnly) {
        super(owner, "Add Timestamp Properties", modal, new AddWssTimestampPanel(assertion), readOnly);
    }
}
