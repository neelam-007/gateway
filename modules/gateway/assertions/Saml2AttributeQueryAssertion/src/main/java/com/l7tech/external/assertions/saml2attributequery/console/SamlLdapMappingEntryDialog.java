package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.security.saml.SamlConstants;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 9-Feb-2009
 * Time: 5:53:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class SamlLdapMappingEntryDialog extends JDialog {
    private JTextField samlAttributeField;
    private JTextField ldapAttributeField;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox nameFormatComboBox;
    private boolean confirmed = false;

    public SamlLdapMappingEntryDialog(JDialog owner, boolean modal, String samlAttribute, String ldapAttribute, String nameFormat) {
        super(owner, "Validate Digital Signature", modal);
        setContentPane(mainPanel);

        samlAttributeField.setText(samlAttribute == null ? "" : samlAttribute);
        ldapAttributeField.setText(ldapAttribute == null ? "" : ldapAttribute);

        nameFormatComboBox.addItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        nameFormatComboBox.addItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE);
        nameFormatComboBox.addItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED);

        if(SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE.equals(nameFormat)) {
            nameFormatComboBox.setSelectedItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE);
        } else if(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED.equals(nameFormat)) {
            nameFormatComboBox.setSelectedItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED);
        } else {
            nameFormatComboBox.setSelectedItem(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);
        }

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }

            public void insertUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }

            public void removeUpdate(DocumentEvent evt) {
                okButton.setEnabled(isDataValid());
            }
        };

        samlAttributeField.getDocument().addDocumentListener(documentListener);
        ldapAttributeField.getDocument().addDocumentListener(documentListener);

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isDataValid())
                    return;
                confirmed = true;

                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        okButton.setEnabled(isDataValid());

        pack();

        getRootPane().setDefaultButton(okButton);
        Utilities.runActionOnEscapeKey(getRootPane(), new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cancelButton.doClick(); }
        });

        Utilities.centerOnScreen(this);
    }

    private boolean isDataValid() {
        return samlAttributeField.getText().trim().length() > 0 && ldapAttributeField.getText().trim().length() > 0;
    }

    public String getSamlAttribute() {
        return samlAttributeField.getText().trim();
    }

    public String getNameFormat() {
        return (String)nameFormatComboBox.getSelectedItem();
    }

    public String getLdapAttribute() {
        return ldapAttributeField.getText().trim();
    }

    /** @return true if Ok button was pressed. */
    public boolean isConfirmed() {
        return confirmed;
    }
}
