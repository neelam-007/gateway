package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.apiportalintegration.ManageApiKeyAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for ManageApiKeyAssertion.
 */
public class ManageApiKeyPropertiesDialog extends AssertionPropertiesOkCancelSupport<ManageApiKeyAssertion> {
    private static final String ADD = "Add";
    private static final String UPDATE = "Update";
    private static final String REMOVE = "Remove";
    private JPanel contentPane;
    private JTextField variablePrefixTextField;
    private JTextField apiKeyTextField;
    private JTextField apiKeyElementTextField;
    private JComboBox actionComboBox;

    public ManageApiKeyPropertiesDialog(final Frame parent, final ManageApiKeyAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME),
                true);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();
        actionComboBox.addItem(ADD);
        actionComboBox.addItem(UPDATE);
        actionComboBox.addItem(REMOVE);
    }

    @Override
    public void setData(final ManageApiKeyAssertion assertion) {
        variablePrefixTextField.setText(assertion.getVariablePrefix());
        apiKeyTextField.setText(assertion.getApiKey());
        apiKeyElementTextField.setText(assertion.getApiKeyElement());
        if(StringUtils.isNotBlank(assertion.getAction())){
            actionComboBox.setSelectedItem(assertion.getAction());
        }else{
            actionComboBox.setSelectedIndex(0);
        }
    }

    @Override
    public ManageApiKeyAssertion getData(final ManageApiKeyAssertion assertion) throws ValidationException {
        assertion.setAction((String) actionComboBox.getSelectedItem());
        assertion.setApiKey(apiKeyTextField.getText());
        assertion.setApiKeyElement(apiKeyElementTextField.getText());
        assertion.setVariablePrefix(variablePrefixTextField.getText());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
