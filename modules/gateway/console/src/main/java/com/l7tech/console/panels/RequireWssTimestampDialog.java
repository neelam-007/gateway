/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;
import com.l7tech.policy.assertion.AssertionMetadata;

import java.awt.*;

/**
 * @author alex
 */
public class RequireWssTimestampDialog extends AssertionOkCancelDialog<RequireWssTimestamp> {

    public RequireWssTimestampDialog(Frame owner, RequireWssTimestamp assertion) {
        super(owner, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString(), new RequireWssTimestampPanel(assertion), assertion);
    }
}
