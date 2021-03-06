package com.l7tech.external.assertions.jwt.console;

import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.external.assertions.jwt.JsonWebTokenConstants;
import com.l7tech.external.assertions.jwt.JwkKeyInfo;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class JWKCreateDialog extends JDialog {
    private JPanel contentPane;
    private JComboBox publicUseComboBox;
    private JTextField keyIdTextField;
    private PrivateKeysComboBox privateKeysComboBox;
    private JButton cancelButton;
    private JButton OKButton;


    private boolean confirmed = false;
    private JwkKeyInfo jwkKeyInfo;

    private SimpleTableModel tableModel;

    private boolean readOnly;

    public JWKCreateDialog(Window owner, SimpleTableModel<JwkKeyInfo> tableModel) {
        this(owner, null, tableModel, false);
    }

    public JWKCreateDialog(Window owner, JwkKeyInfo jwkKeyInfo, SimpleTableModel<JwkKeyInfo> tableModel, boolean readOnly) {
        super(owner, "Key Information Dialog");
        this.tableModel = tableModel;
        this.readOnly = readOnly;
        initComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(OKButton);
        this.jwkKeyInfo = jwkKeyInfo;

        setData(jwkKeyInfo);
    }

    private void setData(JwkKeyInfo jwkKeyInfo) {
        if (jwkKeyInfo == null) return;
        //model to view

        if(jwkKeyInfo.getSourceKeyGoid() != null && (jwkKeyInfo.getSourceKeyAlias() != null && !jwkKeyInfo.getSourceKeyAlias().trim().isEmpty())){
            int sel = privateKeysComboBox.select(jwkKeyInfo.getSourceKeyGoid(), jwkKeyInfo.getSourceKeyAlias());
            privateKeysComboBox.setSelectedIndex(sel < 0 ? 0 : sel);
        }

        keyIdTextField.setText(jwkKeyInfo.getKeyId());
        publicUseComboBox.setSelectedItem(jwkKeyInfo.getPublicKeyUse());

    }


    public boolean isConfirmed() {
        return confirmed;
    }

    public JwkKeyInfo getJwkKeyInfo() {
        JwkKeyInfo jwi = new JwkKeyInfo();
        jwi.setSourceKeyGoid(privateKeysComboBox.getSelectedKeystoreId());
        jwi.setSourceKeyAlias(privateKeysComboBox.getSelectedKeyAlias());
        jwi.setKeyId(keyIdTextField.getText().trim());
        jwi.setPublicKeyUse(publicUseComboBox.getSelectedItem().toString());
        return jwi;
    }

    private boolean validateData() {
        if(jwkKeyInfo == null || !jwkKeyInfo.getKeyId().equals(keyIdTextField.getText().trim())){
            for(int i = 0; i < tableModel.getRowCount(); i++){
                JwkKeyInfo key = (JwkKeyInfo) tableModel.getRowObject(i);
                if(key.getKeyId().equals(keyIdTextField.getText().trim())){
                    DialogDisplayer.showMessageDialog(this,
                            "Key ID '" + keyIdTextField.getText().trim() + "' already exists.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE, null);
                    return false;
                }
            }
        }
        return true;
    }

    private void initComponents() {
        Utilities.setEscKeyStrokeDisposes(this);
        OKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateData()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        OKButton.setEnabled(false);
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                confirmed = false;
                dispose();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmed = false;
                dispose();
            }
        });

        keyIdTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                OKButton.setEnabled(!readOnly && !keyIdTextField.getText().trim().isEmpty());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                OKButton.setEnabled(!readOnly && !keyIdTextField.getText().trim().isEmpty());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                OKButton.setEnabled(!readOnly && !keyIdTextField.getText().trim().isEmpty());
            }
        });
        publicUseComboBox.setModel(new DefaultComboBoxModel(JsonWebTokenConstants.PUBLIC_KEY_USE.values().toArray(new String[JsonWebTokenConstants.PUBLIC_KEY_USE.size()])));
    }

}
