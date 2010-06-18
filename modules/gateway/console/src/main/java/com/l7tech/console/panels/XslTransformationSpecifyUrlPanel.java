/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author mike
 */
public class XslTransformationSpecifyUrlPanel extends JPanel {//todo [Donal] rename class following Json schema commit
    private JPanel mainPanel;
    private JTextField urlField;
    private JLabel fetchUrlDescription;
    private JLabel urlToMonitorLabel;
    private ResourceBundle resourceBundle;

    public XslTransformationSpecifyUrlPanel(UsesResourceInfo assertion, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (assertion.getResourceInfo() instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo)assertion.getResourceInfo();
            urlField.setText(suri.getUrl());
        }

        fetchUrlDescription.setText(resourceBundle.getString("fetchUrlTextBox.description"));
        urlToMonitorLabel.setText(resourceBundle.getString("fetchUrlTextBox.label"));
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String check() {
        String url = urlField.getText();
        if (url != null && !url.trim().isEmpty() ) {
            if ( !ValidationUtils.isValidUrl(url.trim()) ) {
                return resourceBundle.getString("error.badurl");
            }
            return null;
        }
        return resourceBundle.getString("error.nourl");
    }

    public void updateModel(UsesResourceInfo assertion) {
        SingleUrlResourceInfo ri = new SingleUrlResourceInfo();
        ri.setUrl(urlField.getText().trim());
        assertion.setResourceInfo(ri);
    }
}
