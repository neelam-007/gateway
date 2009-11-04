package com.l7tech.external.assertions.certificateattributes.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;

/**
 * @author ghuang
 */
public class CertificateAttributesAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CertificateAttributesAssertion> {
    private JPanel contentPanel;
    private JTextField varPrefixTextField;

    public CertificateAttributesAssertionPropertiesDialog(Frame parent, CertificateAttributesAssertion assertion) {
        super(assertion.getClass(), parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void initComponents() {
        varPrefixTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        }));
        super.initComponents();
    }

    @Override
    public void setData(CertificateAttributesAssertion assertion) {
        varPrefixTextField.setText(assertion.getVariablePrefix());
    }

    @Override
    public CertificateAttributesAssertion getData(CertificateAttributesAssertion assertion) throws ValidationException {
        String variableName = varPrefixTextField.getText();
        if (variableName == null || variableName.trim().isEmpty()) {
            throw new ValidationException("A variable prefix must be specified.");
        }
        assertion.setVariablePrefix(variableName.trim());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }

    private void enableOrDisableOkButton() {
        String prefix = varPrefixTextField.getText();
        getOkButton().setEnabled(prefix != null && !prefix.trim().isEmpty());
    }
}
