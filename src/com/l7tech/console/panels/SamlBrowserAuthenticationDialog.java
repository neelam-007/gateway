package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AuthenticationProperties;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * User: steve
 * Date: Sep 20, 2005
 * Time: 5:14:07 PM
 * $Id$
 */
public class SamlBrowserAuthenticationDialog extends JDialog {

    //- PUBLIC

    public SamlBrowserAuthenticationDialog(AuthenticationProperties authProps, Frame owner, boolean modal)
            throws HeadlessException
    {
        super(owner, "Configure Authentication", modal);
        this.authProps = authProps;
        this.authChanged = false;

        ButtonGroup methodGroup = new ButtonGroup();
        methodGroup.add(basicAuthRadioButton);
        methodGroup.add(formAuthRadioButton);

        listModel = new DefaultComboBoxModel();
        fieldList.setModel(listModel);
        fieldList.setPrototypeCellValue("asdfasdfasdfasdf = asdfasdfasdf");
        fieldList.setBorder(new LineBorder(Color.BLACK, 1));

        if(!AuthenticationProperties.METHOD_FORM.equals(authProps.getMethod())) {
            basicAuthRadioButton.setSelected(true);
        }
        else {
            formAuthRadioButton.setSelected(true);
        }

        formTargetTextfield.setText(authProps.getFormTarget());
        usernameTextfield.setText(authProps.getUsernameFieldname());
        passwordTextfield.setText(authProps.getPasswordFieldname());
        requestBeforeSubmitCheckbox.setSelected(authProps.isRequestForm());
        redirectAfterSubmitCheckbox.setSelected(authProps.isRedirectAfterSubmit());
        enableCookiesCheckbox.setSelected(authProps.isEnableCookies());
        preserveFormFieldsCheckbox.setSelected(authProps.isCopyFormFields());

        for (Iterator i = authProps.getAdditionalFields().entrySet().iterator(); i.hasNext();) {
            Map.Entry fieldEntry = (Map.Entry) i.next();
            String name = (String) fieldEntry.getKey();
            String value = (String) fieldEntry.getValue();
            listModel.addElement(new FieldInfo(name, value));
        }

        ActionListener updateActionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateButtons();
            }
        };

        basicAuthRadioButton.addActionListener(updateActionListener);
        formAuthRadioButton.addActionListener(updateActionListener);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                if (formAuthRadioButton.isSelected()) {
                    SamlBrowserAuthenticationDialog.this.authProps.setMethod(AuthenticationProperties.METHOD_FORM);

                    SamlBrowserAuthenticationDialog.this.authProps.setRequestForm(requestBeforeSubmitCheckbox.isSelected());
                    if(requestBeforeSubmitCheckbox.isSelected()) {
                        SamlBrowserAuthenticationDialog.this.authProps.setCopyFormFields(preserveFormFieldsCheckbox.isSelected());
                        SamlBrowserAuthenticationDialog.this.authProps.setEnableCookies(enableCookiesCheckbox.isSelected());
                    }
                    else {
                        SamlBrowserAuthenticationDialog.this.authProps.setCopyFormFields(false);
                        SamlBrowserAuthenticationDialog.this.authProps.setEnableCookies(false);
                    }
                    SamlBrowserAuthenticationDialog.this.authProps.setRedirectAfterSubmit(redirectAfterSubmitCheckbox.isSelected());
                    SamlBrowserAuthenticationDialog.this.authProps.setFormTarget(formTargetTextfield.getText());
                    SamlBrowserAuthenticationDialog.this.authProps.setUsernameFieldname(usernameTextfield.getText());
                    SamlBrowserAuthenticationDialog.this.authProps.setPasswordFieldname(passwordTextfield.getText());
                    HashMap fields = new HashMap();
                    for (int i = 0; i < listModel.getSize(); i++) {
                        FieldInfo info = (FieldInfo)listModel.getElementAt(i);
                        fields.put(info.name, info.value);
                    }
                    SamlBrowserAuthenticationDialog.this.authProps.setAdditionalFields(fields);
                } else {
                    // create a new one so all other settings are trashed
                    SamlBrowserAuthenticationDialog.this.authProps.setAuthenticationProperties(new AuthenticationProperties());
                }
                authChanged = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SamlBrowserAuthenticationDialog.this.authProps = null;
                dispose();
            }
        });

        requestBeforeSubmitCheckbox.addActionListener(updateActionListener);

        DocumentListener updateListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateButtons(); }
            public void insertUpdate(DocumentEvent e) { updateButtons(); }
            public void removeUpdate(DocumentEvent e) { updateButtons(); }
        };

        formTargetTextfield.getDocument().addDocumentListener(updateListener);
        usernameTextfield.getDocument().addDocumentListener(updateListener);
        passwordTextfield.getDocument().addDocumentListener(updateListener);

        fieldAddButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit(new FieldInfo());
            }
        });

        fieldModifyButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                edit((FieldInfo)listModel.getSelectedItem());
            }
        });

        fieldRemoveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.removeElement(listModel.getSelectedItem());
                updateButtons();
            }
        });

        fieldList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateButtons();
            }
        });

        updateButtons();
        getContentPane().add(mainPanel);
    }

    /**
     * See if the authentication info may have been updated.
     *
     * @return true if potentially modified
     */
    public boolean isModified() {
        return authChanged;
    }

    //- PRIVATE

    private AuthenticationProperties authProps;
    private boolean authChanged;

    private JCheckBox preserveFormFieldsCheckbox;
    private JCheckBox enableCookiesCheckbox;
    private JCheckBox requestBeforeSubmitCheckbox;
    private JCheckBox redirectAfterSubmitCheckbox;
    private JButton fieldRemoveButton;
    private JButton fieldModifyButton;
    private JButton fieldAddButton;
    private JList fieldList;
    private DefaultComboBoxModel listModel;
    private JTextField formTargetTextfield;
    private JTextField passwordTextfield;
    private JTextField usernameTextfield;
    private JRadioButton formAuthRadioButton;
    private JRadioButton basicAuthRadioButton;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JPanel formAuthPanel;

    private void updateButtons() {
        boolean ok = false;

        boolean formSelected = formAuthRadioButton.isSelected();
        boolean canRemoveOrModify = formSelected && fieldList.getSelectedValue() != null;
        fieldRemoveButton.setEnabled(canRemoveOrModify);
        fieldModifyButton.setEnabled(canRemoveOrModify);

        boolean requestForm = requestBeforeSubmitCheckbox.isSelected();
        enableCookiesCheckbox.setEnabled(requestForm);
        preserveFormFieldsCheckbox.setEnabled(requestForm);

        doEnable(formAuthPanel, formSelected);

        boolean usernameFieldSupplied = usernameTextfield.getText()!=null && usernameTextfield.getText().length()>0;
        boolean passwordFieldSupplied = passwordTextfield.getText()!=null && passwordTextfield.getText().length()>0;
        boolean valid = !formSelected
                      || (usernameFieldSupplied==passwordFieldSupplied
                        && (requestForm || usernameFieldSupplied)
                        && validUrl(formTargetTextfield.getText(), requestForm));
        okButton.setEnabled(valid);
    }

    private void doEnable(JComponent component, boolean enable) {
        Utilities.setEnabled(component,enable);
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

    private boolean validUrl(String urlText, boolean allowEmpty) {
        boolean present = false;
        boolean ok = false;

        present = urlText != null && urlText.length() > 0;

        if (present) {
            try {
                URL test = new URL(urlText);
                String host = test.getHost();
                ok = host!=null && host.length()>0;
            } catch (MalformedURLException e) {
            }
        }
        else if (allowEmpty) {
            ok = true;
        }

        return ok;
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
}
