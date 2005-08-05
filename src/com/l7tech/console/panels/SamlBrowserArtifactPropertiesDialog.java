/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.SamlBrowserArtifact;
import com.l7tech.common.http.GenericHttpClient;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.HashMap;

/**
 * @author alex
 */
public class SamlBrowserArtifactPropertiesDialog extends JDialog {
    private SamlBrowserArtifact samlBrowserArtifactAssertion;
    private boolean assertionChanged = false;

    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private JTextField ssoEndpointUrlField;
    private JTextField artifactQueryParamField;
    private JRadioButton httpGetRadio;
    private JRadioButton httpPostRadio;
    private JTextField usernameField;
    private JTextField passwordField;
    private JList fieldsList;
    private JButton addFieldButton;
    private JButton removeFieldButton;
    private JLabel fieldsLabel;
    private JLabel passwordLabel;
    private JLabel usernameLabel;
    private DefaultComboBoxModel listModel;
    private JButton modifyFieldButton;

    public SamlBrowserArtifact getAssertion() {
        return samlBrowserArtifactAssertion;
    }

    public SamlBrowserArtifactPropertiesDialog(SamlBrowserArtifact assertion, Frame owner, boolean modal)
            throws HeadlessException
    {
        super(owner, "Configure SAML Browser/Artifact", modal);
        this.samlBrowserArtifactAssertion = assertion;
        ssoEndpointUrlField.setText(assertion.getSsoEndpointUrl());
        artifactQueryParamField.setText(assertion.getArtifactQueryParameter());
        ButtonGroup bg = new ButtonGroup();
        bg.add(httpGetRadio);
        bg.add(httpPostRadio);

        listModel = new DefaultComboBoxModel();
        fieldsList.setModel(listModel);
        fieldsList.setPrototypeCellValue("asdfasdfasdfasdf = asdfasdfasdf");

        if (assertion.getMethod().equals(GenericHttpClient.METHOD_GET)) {
            httpGetRadio.setSelected(true);
        } else {
            httpPostRadio.setSelected(true);
            usernameField.setText(assertion.getUsernameFieldname());
            passwordField.setText(assertion.getPasswordFieldname());
            for (Iterator i = assertion.getExtraFields().keySet().iterator(); i.hasNext();) {
                String name = (String)i.next();
                String value = (String)assertion.getExtraFields().get(name);
                listModel.addElement(new FieldInfo(name, value));
            }
        }

        httpGetRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateButtons();
            }
        });

        httpPostRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateButtons();
            }
        });


        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlBrowserArtifactAssertion.setSsoEndpointUrl(ssoEndpointUrlField.getText());
                samlBrowserArtifactAssertion.setArtifactQueryParameter(artifactQueryParamField.getText());
                if (httpPostRadio.isSelected()) {
                    samlBrowserArtifactAssertion.setMethod(GenericHttpClient.METHOD_POST);
                    samlBrowserArtifactAssertion.setUsernameFieldname(usernameField.getText());
                    samlBrowserArtifactAssertion.setPasswordFieldname(passwordField.getText());
                    HashMap fields = new HashMap();
                    for (int i = 0; i < listModel.getSize(); i++) {
                        FieldInfo info = (FieldInfo)listModel.getElementAt(i);
                        fields.put(info.name, info.value);
                    }
                    samlBrowserArtifactAssertion.setExtraFields(fields);
                } else {
                    samlBrowserArtifactAssertion.setMethod(GenericHttpClient.METHOD_GET);
                }
                assertionChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlBrowserArtifactAssertion = null;
                dispose();
            }
        });

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        ssoEndpointUrlField.getDocument().addDocumentListener(updateListener);
        artifactQueryParamField.getDocument().addDocumentListener(updateListener);

        addFieldButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit(new FieldInfo());
            }
        });

        modifyFieldButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit((FieldInfo)listModel.getSelectedItem());
            }
        });

        removeFieldButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.removeElement(listModel.getSelectedItem());
                updateButtons();
            }
        });

        fieldsList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });

        updateButtons();
        getContentPane().add(mainPanel);
    }

    private void edit(FieldInfo fieldInfo) {
        SamlBrowserArtifactFieldDialog fieldDialog = new SamlBrowserArtifactFieldDialog(this, true);
        Utilities.centerOnScreen(fieldDialog);
        fieldDialog.getNameField().setText(fieldInfo.name);
        fieldDialog.getValueField().setText(fieldInfo.value);
        fieldDialog.pack();
        fieldDialog.setVisible(true);
        if (fieldDialog.isModified()) {
            listModel.removeElement(fieldInfo);
            listModel.addElement(new FieldInfo(fieldDialog.getNameField().getText(), fieldDialog.getValueField().getText()));
            updateButtons();
        }
    }

    private static class FieldInfo {
        private String name;
        private String value;

        public FieldInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public FieldInfo() {
        }

        public String toString() {
            return name + " = " + value;
        }
    }

    public boolean isAssertionChanged() {
        return assertionChanged;
    }

    private void updateButtons() {
        boolean ok;
        String url = ssoEndpointUrlField.getText();
        ok = url != null && url.length() > 0;
        if (ok) try {
            new URL(url);
        } catch (MalformedURLException e) {
            ok = false;
        }
        String queryParam = artifactQueryParamField.getText();
        ok = ok && queryParam != null && queryParam.length() > 0;

        okButton.setEnabled(ok);

        boolean isPost = httpPostRadio.isSelected();
        usernameLabel.setEnabled(isPost);
        usernameField.setEnabled(isPost);
        passwordLabel.setEnabled(isPost);
        passwordField.setEnabled(isPost);
        fieldsLabel.setEnabled(isPost);
        fieldsList.setEnabled(isPost);
        addFieldButton.setEnabled(isPost);
        boolean canRemoveOrModify = isPost && fieldsList.getSelectedValue() != null;
        removeFieldButton.setEnabled(canRemoveOrModify);
        modifyFieldButton.setEnabled(canRemoveOrModify);

    }
}
