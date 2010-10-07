package com.l7tech.console.panels;

import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Panel that allows the user to enter a URL to monitor.
 *
 * @author mike
 */
public class MonitorUrlPanel extends JPanel {
    private JPanel mainPanel;
    private JTextField urlField;
    private JLabel fetchUrlDescription;
    private JLabel urlToMonitorLabel;
    private JButton manageHttpOptionsButton;
    private ResourceBundle resourceBundle;

    /**
     * The following keys are required in the resource bundle:
     * fetchUrlTextBox.description
     * fetchUrlTextBox.label
     * error.badurl
     * error.nourl
     *
     * @param assertion UsesResourceInfo assertion bean
     * @param resourceBundle ResourceBundle which contains the above keys
     */
    public MonitorUrlPanel(UsesResourceInfo assertion, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (assertion.getResourceInfo() instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo)assertion.getResourceInfo();
            urlField.setText(suri.getUrl());
        }

        fetchUrlDescription.setText(resourceBundle.getString("fetchUrlTextBox.description"));
        urlToMonitorLabel.setText(resourceBundle.getString("fetchUrlTextBox.label"));
        manageHttpOptionsButton.setAction( new ManageHttpConfigurationAction( this ) );
        manageHttpOptionsButton.setText(resourceBundle.getString("manageHttpOptionsButton.label"));
        manageHttpOptionsButton.setIcon(null);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    public String check() {
        String url = urlField.getText();
        if (url != null && !url.trim().isEmpty() ) {
            if ( Syntax.getReferencedNames( url ).length==0 &&
                 !ValidationUtils.isValidUrl(url.trim()) ) {
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
