/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;
import com.l7tech.gui.widgets.OkCancelDialog;

import java.awt.*;

/**
 * @author alex
 */
public class ResponseWssTimestampDialog extends OkCancelDialog<ResponseWssTimestamp> {
    public ResponseWssTimestampDialog(Frame owner, boolean modal, ResponseWssTimestamp assertion, boolean readOnly) {
        super(owner, "Response Timestamp Validity Period", modal, new ResponseWssTimestampPanel(assertion), readOnly);
    }

    public ResponseWssTimestampDialog(Dialog owner, boolean modal, ResponseWssTimestamp assertion, boolean readOnly) {
        super(owner, "Response Timestamp Validity Period", modal, new ResponseWssTimestampPanel(assertion), readOnly);
    }
}
