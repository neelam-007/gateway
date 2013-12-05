package com.l7tech.external.assertions.managecookie.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import java.awt.*;
import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.*;

public class ManageCookiePropertiesDialog extends AssertionPropertiesOkCancelSupport<ManageCookieAssertion> {
    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField valueTextField;
    private JTextField domainTextField;
    private JTextField pathTextField;
    private JTextField commentTextField;
    private JTextField maxAgeTextField;
    private JComboBox versionComboBox;
    private JCheckBox secureCheckBox;
    private JComboBox operationComboBox;
    private InputValidator validators;

    public ManageCookiePropertiesDialog(final Frame parent, final ManageCookieAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
        enableDisable();
    }

    @Override
    public void setData(final ManageCookieAssertion assertion) {
        operationComboBox.setSelectedItem(assertion.getOperation());
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
    public ManageCookieAssertion getData(final ManageCookieAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setOperation((ManageCookieAssertion.Operation) operationComboBox.getSelectedItem());
        assertion.setName(nameTextField.getText().trim());
        assertion.setValue(valueTextField.getText().trim());
        assertion.setDomain(domainTextField.getText().trim());
        assertion.setCookiePath(pathTextField.getText().trim());
        assertion.setMaxAge(maxAgeTextField.getText().trim());
        assertion.setVersion((Integer) versionComboBox.getSelectedItem());
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
        operationComboBox.setModel(new DefaultComboBoxModel(new ManageCookieAssertion.Operation[]{ManageCookieAssertion.Operation.ADD, ManageCookieAssertion.Operation.REMOVE}));
        operationComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value instanceof ManageCookieAssertion.Operation ? ((ManageCookieAssertion.Operation) value).getName() : value, index, isSelected, cellHasFocus);
            }
        });
        operationComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
            }
        }));
        versionComboBox.setModel(new DefaultComboBoxModel(new Integer[]{1, 0}));
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty("name", nameTextField, null);
        validators.ensureComboBoxSelection("version", versionComboBox);
        validators.ensureComboBoxSelection("operation", operationComboBox);
        final IntegerOrContextVariableValidationRule maxAgeRule = new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "max age", maxAgeTextField);
        maxAgeRule.setAllowEmpty(true);
        validators.addRule(maxAgeRule);
    }

    private void enableDisable() {
        final Object op = operationComboBox.getSelectedItem();
        valueTextField.setEnabled(op != REMOVE);
        domainTextField.setEnabled(op != REMOVE);
        pathTextField.setEnabled(op != REMOVE);
        maxAgeTextField.setEnabled(op != REMOVE);
        commentTextField.setEnabled(op != REMOVE);
        versionComboBox.setEnabled(op != REMOVE);
        secureCheckBox.setEnabled(op != REMOVE);
    }
}
