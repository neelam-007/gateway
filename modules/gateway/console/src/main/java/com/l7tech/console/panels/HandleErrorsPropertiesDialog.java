package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;

import javax.swing.*;
import java.awt.*;

public class HandleErrorsPropertiesDialog extends AssertionPropertiesOkCancelSupport<HandleErrorsAssertion> {

    private TargetVariablePanel targetVariable;
    private JPanel contentPanel;
    private JPanel prefixPanel;

    public HandleErrorsPropertiesDialog(final Frame parent, final HandleErrorsAssertion assertion) {
        super(HandleErrorsAssertion.class, parent, String.valueOf(assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME)), true);
        initComponents();
        setData(assertion);
    }


    @Override
    public void setData(final HandleErrorsAssertion assertion) {
        targetVariable.setVariable(assertion.getVariablePrefix());
        targetVariable.setAssertion(assertion, getPreviousAssertion());
        targetVariable.setDefaultVariableOrPrefix(HandleErrorsAssertion.VARIABLE_PREFIX);

    }

    @Override
    public HandleErrorsAssertion getData(final HandleErrorsAssertion assertion) throws ValidationException {
        assertion.setVariablePrefix(targetVariable.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        targetVariable = new TargetVariablePanel();

        prefixPanel.setLayout(new BorderLayout());
        prefixPanel.add(targetVariable, BorderLayout.CENTER);
        return contentPanel;
    }
}
