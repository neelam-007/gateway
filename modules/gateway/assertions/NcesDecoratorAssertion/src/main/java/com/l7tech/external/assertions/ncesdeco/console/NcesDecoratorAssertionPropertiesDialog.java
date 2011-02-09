/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesdeco.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.external.assertions.ncesdeco.NcesDecoratorAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.util.SoapConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author alex
 */
public class NcesDecoratorAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<NcesDecoratorAssertion> {
    private final NcesDecoratorAssertion assertion;
    private JPanel mainPanel;
    private JRadioButton samlTemplateRadioButton;
    private JRadioButton samlInternalRadioButton;
    private JTextField samlTemplateField;
    private JTextField uuidUriPrefixTextField;
    private JRadioButton macBasedUuidRadioButton;
    private JRadioButton randomUuidRadioButton;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton samlStrDereferenceRadioButton;
    private JRadioButton samlWsuIdRadioButton;
    private JRadioButton saml11RadioButton;
    private JRadioButton saml20RadioButton;
    private JRadioButton wsa200403RadioButton;
    private JRadioButton wsa10RadioButton;
    private JRadioButton wsa200408RadioButton;
    private JRadioButton samlNoneRadioButton;
    private JRadioButton wsaOtherRadioButton;
    private JTextField wsaOtherTextField;
    private JPanel targetMessagePanelHolder;
    private JCheckBox useExistingAddressingCheckBox;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();
    private JCheckBox responseImmediateCheckBox;

    private boolean validOtherMessageVariable= true;

    private final RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            enableDisable();
        }
    });

    public NcesDecoratorAssertionPropertiesDialog(Window owner, final NcesDecoratorAssertion assertion) {
        super(owner, assertion);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        if (assertion.getSamlAssertionVersion() == 2) {
            saml20RadioButton.setSelected(true);
        } else {
            saml11RadioButton.setSelected(true);
        }

        if (assertion.isSamlIncluded()) {
            final String template = assertion.getSamlAssertionTemplate();
            if (template != null && !template.isEmpty()) {
                samlTemplateRadioButton.setSelected(true);
                samlTemplateField.setText(template);
            } else {
                samlInternalRadioButton.setSelected(true);
                samlTemplateField.setText("${issuedSamlAssertion}");
            }
        } else {
            samlNoneRadioButton.setSelected(true);
            samlTemplateField.setText("${issuedSamlAssertion}");
        }

        addEnableListener(samlInternalRadioButton, samlTemplateRadioButton, samlNoneRadioButton,
                          wsa10RadioButton, wsa200403RadioButton, wsa200408RadioButton, wsaOtherRadioButton);
        wsaOtherTextField.getDocument().addDocumentListener(enableDisableListener);
        samlTemplateField.getDocument().addDocumentListener(enableDisableListener);

        uuidUriPrefixTextField.setText(assertion.getMessageIdUriPrefix());
        final String wsaNs = assertion.getWsaNamespaceUri();
        if ( SoapConstants.WSA_NAMESPACE.equals(wsaNs)) {
            wsa200403RadioButton.setSelected(true);
        } else if ( SoapConstants.WSA_NAMESPACE2.equals(wsaNs)) {
            wsa200408RadioButton.setSelected(true);
        } else if (wsaNs == null || SoapConstants.WSA_NAMESPACE_10.equals(wsaNs)) {
            wsa10RadioButton.setSelected(true);
        } else {
            wsaOtherRadioButton.setSelected(true);
            wsaOtherTextField.setText(wsaNs);
        }
        useExistingAddressingCheckBox.setSelected( assertion.isUseExistingWsa() );

        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                validOtherMessageVariable = (Boolean)evt.getNewValue();
                enableDisable();
            }
        });
        targetMessagePanelHolder.add( targetMessagePanel );
        targetMessagePanel.setTitle("Apply NCES Decoration To");
        targetMessagePanel.setModel(assertion,getPreviousAssertion());
        responseImmediateCheckBox = new JCheckBox("Apply immediately");
        responseImmediateCheckBox.setToolTipText("Uncheck to accumulate additional response decoration requirements");
        targetMessagePanel.setResponseExtra(responseImmediateCheckBox);

        if (assertion.isNodeBasedUuid()) {
            macBasedUuidRadioButton.setSelected(true);
        } else {
            randomUuidRadioButton.setSelected(true);
        }

        if (assertion.isSamlUseStrTransform()) {
            samlStrDereferenceRadioButton.setSelected(true);
        } else {
            samlWsuIdRadioButton.setSelected(true);
        }

        responseImmediateCheckBox.setSelected(!assertion.isDeferDecoration());

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String err = targetMessagePanel.check();
                if (err != null) {
                    DialogDisplayer.showMessageDialog(NcesDecoratorAssertionPropertiesDialog.this,
                            "Invalid message target: " + err, "Invalid Message Target", null);
                    return;
                }

                assertion.setUseExistingWsa(useExistingAddressingCheckBox.isSelected());
                assertion.setMessageIdUriPrefix(uuidUriPrefixTextField.getText());
                assertion.setNodeBasedUuid(macBasedUuidRadioButton.isSelected());
                assertion.setDeferDecoration(!responseImmediateCheckBox.isSelected());
                targetMessagePanel.updateModel(assertion);

                if (samlNoneRadioButton.isSelected()) {
                    assertion.setSamlIncluded(false);
                    assertion.setSamlAssertionTemplate(null);
                    assertion.setSamlAssertionVersion(0);
                    assertion.setSamlUseStrTransform(false);
                } else {
                    assertion.setSamlIncluded(true);
                    assertion.setSamlAssertionTemplate(samlTemplateRadioButton.isSelected() ? samlTemplateField.getText() : null);
                    assertion.setSamlAssertionVersion(saml11RadioButton.isSelected() ? 1 : 2);
                    assertion.setSamlUseStrTransform(samlStrDereferenceRadioButton.isSelected());
                }

                String wsaNs;
                if (wsa200403RadioButton.isSelected()) {
                    wsaNs = SoapConstants.WSA_NAMESPACE;
                } else if (wsa200408RadioButton.isSelected()) {
                    wsaNs = SoapConstants.WSA_NAMESPACE2;
                } else if (wsa10RadioButton.isSelected()) {
                    wsaNs = SoapConstants.WSA_NAMESPACE_10;
                } else {
                    wsaNs = wsaOtherTextField.getText();
                }

                assertion.setWsaNamespaceUri(wsaNs);
                ok = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        enableDisable();

        add(mainPanel);
    }

    private void addEnableListener(final AbstractButton... things) {
        for (AbstractButton thing : things) {
            thing.addActionListener(enableDisableListener);
        }
    }

    private void enableDisable() {
        saml11RadioButton.setEnabled(samlInternalRadioButton.isSelected());
        saml20RadioButton.setEnabled(samlInternalRadioButton.isSelected());
        samlTemplateField.setEnabled(samlTemplateRadioButton.isSelected());
        wsaOtherTextField.setEnabled(wsaOtherRadioButton.isSelected());

        final boolean someSamlSelected = !samlNoneRadioButton.isSelected();
        samlStrDereferenceRadioButton.setEnabled(someSamlSelected);
        samlWsuIdRadioButton.setEnabled(someSamlSelected);

        boolean canOk = true;
        if (wsaOtherRadioButton.isSelected() && wsaOtherTextField.getText().trim().isEmpty()) canOk = false;
        if (samlTemplateRadioButton.isSelected() && samlTemplateField.getText().trim().isEmpty()) canOk = false;
        if (! validOtherMessageVariable) canOk = false;

        okButton.setEnabled(canOk);
    }

    boolean ok = false;

    @Override
    public JDialog getDialog() {
        return this;
    }

    @Override
    public boolean isConfirmed() {
        return ok;
    }

    @Override
    public void setData(NcesDecoratorAssertion assertion) {
    }

    @Override
    public NcesDecoratorAssertion getData(NcesDecoratorAssertion assertion) {
        return assertion;
    }
}
