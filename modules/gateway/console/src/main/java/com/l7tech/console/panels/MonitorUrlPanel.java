package com.l7tech.console.panels;

import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.assertion.UsesResourceInfo;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.CollectionUtils;
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
     * error.badhttpurl
     * error.nourl
     *
     * @param assertion UsesResourceInfo assertion bean
     * @param resourceBundle ResourceBundle which contains the above keys
     */
    public MonitorUrlPanel(UsesResourceInfo assertion, ResourceBundle resourceBundle) {
        this.resourceBundle = resourceBundle;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        fetchUrlDescription.setText(resourceBundle.getString("fetchUrlTextBox.description"));
        urlToMonitorLabel.setText(resourceBundle.getString("fetchUrlTextBox.label"));
        manageHttpOptionsButton.setAction( new ManageHttpConfigurationAction( this ) );
        manageHttpOptionsButton.setText(resourceBundle.getString("manageHttpOptionsButton.label"));
        manageHttpOptionsButton.setIcon(null);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        
        setModel(assertion);
    }

    public String check() {
        String url = urlField.getText();
        if (url != null && !url.trim().isEmpty() ) {
            if ( Syntax.getReferencedNames( url, false ).length==0 &&
                 !ValidationUtils.isValidUrl(url.trim(), false, CollectionUtils.caseInsensitiveSet( "http", "https" )) ) {
                return resourceBundle.getString("error.badhttpurl");
            }
            return null;
        }
        return resourceBundle.getString("error.nourl");
    }

    public void setModel(UsesResourceInfo assertion){

        if (assertion.getResourceInfo() instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo)assertion.getResourceInfo();
            urlField.setText(suri.getUrl());
        }
    }

    public void updateModel(UsesResourceInfo assertion) {
        SingleUrlResourceInfo ri = new SingleUrlResourceInfo();
        ri.setUrl(urlField.getText().trim());
        assertion.setResourceInfo(ri);
    }
}
