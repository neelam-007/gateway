package com.l7tech.console.security.rbac;

import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.SecurityZone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SecurityZonePropertiesDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField nameField;
    private JTextArea descriptionField;

    private boolean confirmed = false;

    public SecurityZonePropertiesDialog(Window owner, SecurityZone securityZone, boolean readOnly) {
        super(owner, "Security Zone Properties", DEFAULT_MODALITY_TYPE);
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));
        Utilities.setEscAction(this, cancelButton);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose();
            }
        });


        okButton.setEnabled(!readOnly);

        setData(securityZone);
    }

    void setData(SecurityZone zone) {
        nameField.setText(zone.getName());
        descriptionField.setText(zone.getDescription());
    }

    public SecurityZone getData(SecurityZone zone) {
        zone.setName(nameField.getText());
        zone.setDescription(descriptionField.getText());
        return zone;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
