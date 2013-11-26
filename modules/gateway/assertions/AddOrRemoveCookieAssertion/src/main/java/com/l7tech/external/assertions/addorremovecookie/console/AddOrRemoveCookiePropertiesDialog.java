package com.l7tech.external.assertions.addorremovecookie.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.external.assertions.addorremovecookie.AddOrRemoveCookieAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;

public class AddOrRemoveCookiePropertiesDialog extends AssertionPropertiesOkCancelSupport<AddOrRemoveCookieAssertion> {
    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField valueTextField;
    private JTextField domainTextField;
    private JTextField pathTextField;
    private JTextField commentTextField;
    private JTextField maxAgeTextField;
    private JComboBox versionComboBox;
    private JCheckBox secureCheckBox;
    private InputValidator validators;

    public AddOrRemoveCookiePropertiesDialog(final Frame parent, final AddOrRemoveCookieAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void setData(final AddOrRemoveCookieAssertion assertion) {
        nameTextField.setText(assertion.getName());
        valueTextField.setText(assertion.getValue());
        domainTextField.setText(assertion.getDomain());
        pathTextField.setText(assertion.getCookiePath());
        commentTextField.setText(assertion.getComment());
        maxAgeTextField.setText(assertion.getMaxAge());
        versionComboBox.setSelectedItem(assertion.getVersion());
        secureCheckBox.setSelected(assertion.isSecure());
    }

    @Override
    public AddOrRemoveCookieAssertion getData(final AddOrRemoveCookieAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setName(nameTextField.getText().trim());
        assertion.setValue(valueTextField.getText().trim());
        assertion.setDomain(domainTextField.getText().trim());
        assertion.setCookiePath(pathTextField.getText().trim());
        assertion.setMaxAge(maxAgeTextField.getText().trim());
        assertion.setVersion(versionComboBox.getSelectedItem().toString());
        assertion.setSecure(secureCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        versionComboBox.setModel(new DefaultComboBoxModel(new String[]{"1", "0"}));
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty("name", nameTextField, null);
        validators.constrainTextFieldToBeNonEmpty("value", valueTextField, null);
        validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "max age", maxAgeTextField));
        validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "version", versionComboBox));
    }
}
