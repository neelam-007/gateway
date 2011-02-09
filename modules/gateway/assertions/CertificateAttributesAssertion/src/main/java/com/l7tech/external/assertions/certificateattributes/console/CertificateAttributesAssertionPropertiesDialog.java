package com.l7tech.external.assertions.certificateattributes.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.certificateattributes.CertificateAttributesAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * @author ghuang
 */
public class CertificateAttributesAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CertificateAttributesAssertion> {
    private JPanel contentPanel;
    private JPanel varPrefixPanel;
    private TargetVariablePanel varPrefixTextField;

    public CertificateAttributesAssertionPropertiesDialog(Frame parent, CertificateAttributesAssertion assertion) {
        super(assertion.getClass(), parent, (String)assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        
        varPrefixTextField = new TargetVariablePanel();
        varPrefixPanel.setLayout(new BorderLayout());
        varPrefixPanel.add(varPrefixTextField, BorderLayout.CENTER);
        varPrefixTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(varPrefixTextField.isEntryValid());
            }
        });
    }

    @Override
    public void setData(CertificateAttributesAssertion assertion) {
        varPrefixTextField.setVariable(assertion.getVariablePrefix());
        varPrefixTextField.setAssertion(assertion,getPreviousAssertion());
        varPrefixTextField.setSuffixes(assertion.getVariableSuffixes());
    }

    @Override
    public CertificateAttributesAssertion getData(CertificateAttributesAssertion assertion) throws ValidationException {
        String variableName = varPrefixTextField.getVariable();
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

}
