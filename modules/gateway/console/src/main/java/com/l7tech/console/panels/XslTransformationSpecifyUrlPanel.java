/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;

/**
 * @author mike
 */
class XslTransformationSpecifyUrlPanel extends JPanel {
    private JPanel mainPanel;
    private JTextField urlField;
    private XslTransformationPropertiesDialog xslTransformationPropertiesDialog;

    public XslTransformationSpecifyUrlPanel(XslTransformationPropertiesDialog xslTransformationPropertiesDialog, XslTransformation assertion) {
        this.xslTransformationPropertiesDialog = xslTransformationPropertiesDialog;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (assertion.getResourceInfo() instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo)assertion.getResourceInfo();
            urlField.setText(suri.getUrl());
        }

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    String check() {
        String url = urlField.getText();
        if (url != null && !url.trim().isEmpty() ) {
            if ( !ValidationUtils.isValidUrl(url.trim()) ) {
                return xslTransformationPropertiesDialog.getResources().getString("error.badurl");
            }
        }
        return xslTransformationPropertiesDialog.getResources().getString("error.nourl");
    }

    void updateModel(XslTransformation assertion) {
        SingleUrlResourceInfo ri = new SingleUrlResourceInfo();
        ri.setUrl(urlField.getText().trim());
        assertion.setResourceInfo(ri);
    }
}
