package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.NumberField;
import com.l7tech.common.transport.SsgConnector;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;

public class SsgConnectorPropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JTextField portField;
    private JComboBox protocolComboBox;
    private JComboBox interfaceComboBox;
    private JTextField portRangeStartField;
    private JTextField portRangeEndField;
    private JComboBox privateKeyComboBox;
    private JButton managePrivateKeysButton;
    private JButton cipherSuitesButton;
    private JCheckBox enableSecureSpanManagerAccessCheckBox;
    private JCheckBox enableWebBasedAdministrationCheckBox;
    private JCheckBox enablePublishedServiceMessageCheckBox;
    private JCheckBox enableBuiltInServicesCheckBox;
    private JComboBox clientAuthComboBox;
    private JCheckBox enabledCheckBox;

    private SsgConnector connector;
    private boolean confirmed = false;

    public SsgConnectorPropertiesDialog(Frame owner, SsgConnector connector) {
        super(owner, "Listen Port Properties");
        initialize(connector);
    }

    public SsgConnectorPropertiesDialog(Dialog owner, SsgConnector connector) {
        super(owner, "Listen Port Properties");
        initialize(connector);
    }

    private void initialize(SsgConnector connector) {
        this.connector = connector;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        portField.setDocument(new NumberField(5));

        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
    }

    private void modelToView() {
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        // TODO
    }

    private void viewToModel() {
        connector.setName(nameField.getText());
        connector.setPort(Integer.parseInt(portField.getText()));
        // TODO
    }

    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    public SsgConnector getConnector() {
        return connector;
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }
}
