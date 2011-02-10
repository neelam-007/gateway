/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequireWssTimestamp;

import javax.swing.*;
import java.awt.*;

/**
 * @author alex
 */
public class RequireWssTimestampDialog extends AssertionPropertiesOkCancelSupport<RequireWssTimestamp> {

    RequireWssTimestampPanel contentPanel;
    final RequireWssTimestamp assertion;
    public RequireWssTimestampDialog(Frame owner, RequireWssTimestamp assertion) {
        super(RequireWssTimestamp.class, owner, assertion, true);
        this.assertion = assertion;
        super.initComponents();
    }



    @Override
    public void setData(RequireWssTimestamp assertion) {
        contentPanel.setModel(assertion,getPreviousAssertion());
    }

    @Override
    public RequireWssTimestamp getData(RequireWssTimestamp assertion) throws ValidationException {
        return contentPanel.getData();
    }

    @Override
    protected JPanel createPropertyPanel() {

        contentPanel = new RequireWssTimestampPanel(assertion);
        return contentPanel;
    }
}
