package com.l7tech.external.assertions.managecookie.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.*;
import static com.l7tech.external.assertions.managecookie.ManageCookieAssertion.Operation.*;

public class ManageCookiePropertiesDialog extends AssertionPropertiesOkCancelSupport<ManageCookieAssertion> {
    private static final Logger logger = Logger.getLogger(ManageCookiePropertiesDialog.class.getName());
    private JPanel contentPanel;
    private JTextField nameTextField;
    private JTextField valueTextField;
    private JTextField domainTextField;
    private JTextField pathTextField;
    private JTextField commentTextField;
    private JTextField maxAgeTextField;
    private JCheckBox secureCheckBox;
    private JComboBox operationComboBox;
    private JTextField versionTextField;
    private JTextField nameMatchTextField;
    private JTextField domainMatchTextField;
    private JTextField pathMatchTextField;
    private JCheckBox nameRegexCheckBox;
    private JCheckBox domainRegexCheckBox;
    private JCheckBox pathRegexCheckBox;
    private JPanel matchPanel;
    private JLabel nameMatchLabel;
    private JLabel domainMatchLabel;
    private JLabel pathMatchLabel;
    private JLabel nameLabel;
    private JLabel valueLabel;
    private JLabel versionLabel;
    private JLabel domainLabel;
    private JLabel pathLabel;
    private JLabel maxAgeLabel;
    private JLabel commentLabel;
    private JCheckBox originalName;
    private JCheckBox originalValue;
    private JCheckBox originalVersion;
    private JCheckBox originalDomain;
    private JCheckBox originalPath;
    private JCheckBox originalMaxAge;
    private JCheckBox originalComment;
    private JPanel setAttributesPanel;
    private JCheckBox originalSecure;
    private JCheckBox httpOnlyCheckBox;
    private JCheckBox originalHttpOnly;
    private InputValidator validators;

    public ManageCookiePropertiesDialog(final Frame parent, final ManageCookieAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void setData(final ManageCookieAssertion assertion) {
        operationComboBox.setSelectedItem(assertion.getOperation());
        final Map<String, CookieAttribute> cookieAttributes = assertion.getCookieAttributes();
        nameTextField.setText(cookieAttributes.containsKey(NAME) ? cookieAttributes.get(NAME).getValue() : StringUtils.EMPTY);
        valueTextField.setText(cookieAttributes.containsKey(VALUE) ? cookieAttributes.get(VALUE).getValue() : StringUtils.EMPTY);
        domainTextField.setText(cookieAttributes.containsKey(DOMAIN) ? cookieAttributes.get(DOMAIN).getValue() : StringUtils.EMPTY);
        pathTextField.setText(cookieAttributes.containsKey(PATH) ? cookieAttributes.get(PATH).getValue() : StringUtils.EMPTY);
        commentTextField.setText(cookieAttributes.containsKey(COMMENT) ? cookieAttributes.get(COMMENT).getValue() : StringUtils.EMPTY);
        maxAgeTextField.setText(cookieAttributes.containsKey(MAX_AGE) ? cookieAttributes.get(MAX_AGE).getValue() : StringUtils.EMPTY);
        versionTextField.setText(cookieAttributes.containsKey(VERSION) ? cookieAttributes.get(VERSION).getValue() : StringUtils.EMPTY);
        secureCheckBox.setSelected(cookieAttributes.containsKey(SECURE) ? Boolean.parseBoolean(cookieAttributes.get(SECURE).getValue()) : false);
        httpOnlyCheckBox.setSelected(cookieAttributes.containsKey(HTTP_ONLY) ? Boolean.parseBoolean(cookieAttributes.get(HTTP_ONLY).getValue()) : false);
        originalName.setSelected(cookieAttributes.containsKey(NAME) ? cookieAttributes.get(NAME).isUseOriginalValue() : false);
        originalValue.setSelected(cookieAttributes.containsKey(VALUE) ? cookieAttributes.get(VALUE).isUseOriginalValue() : false);
        originalDomain.setSelected(cookieAttributes.containsKey(DOMAIN) ? cookieAttributes.get(DOMAIN).isUseOriginalValue() : false);
        originalPath.setSelected(cookieAttributes.containsKey(PATH) ? cookieAttributes.get(PATH).isUseOriginalValue() : false);
        originalComment.setSelected(cookieAttributes.containsKey(COMMENT) ? cookieAttributes.get(COMMENT).isUseOriginalValue() : false);
        originalMaxAge.setSelected(cookieAttributes.containsKey(MAX_AGE) ? cookieAttributes.get(MAX_AGE).isUseOriginalValue() : false);
        originalVersion.setSelected(cookieAttributes.containsKey(VERSION) ? cookieAttributes.get(VERSION).isUseOriginalValue() : false);
        originalSecure.setSelected(cookieAttributes.containsKey(SECURE) ? cookieAttributes.get(SECURE).isUseOriginalValue() : false);
        originalHttpOnly.setSelected(cookieAttributes.containsKey(HTTP_ONLY) ? cookieAttributes.get(HTTP_ONLY).isUseOriginalValue() : false);
        final Map<String, CookieCriteria> criteria = assertion.getCookieCriteria();
        if (criteria.containsKey(NAME)) {
            final CookieCriteria nameCriteria = criteria.get(NAME);
            nameMatchTextField.setText(nameCriteria.getValue());
            nameRegexCheckBox.setSelected(nameCriteria.isRegex());
        }
        if (criteria.containsKey(DOMAIN)) {
            final CookieCriteria domainCriteria = criteria.get(DOMAIN);
            domainMatchTextField.setText(domainCriteria.getValue());
            domainRegexCheckBox.setSelected(domainCriteria.isRegex());
        }
        if (criteria.containsKey(PATH)) {
            final CookieCriteria pathCriteria = criteria.get(PATH);
            pathMatchTextField.setText(pathCriteria.getValue());
            pathRegexCheckBox.setSelected(pathCriteria.isRegex());
        }
        enableDisable();
    }

    @Override
    public ManageCookieAssertion getData(final ManageCookieAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setOperation((Operation) operationComboBox.getSelectedItem());
        assertion.getCookieAttributes().clear();
        setAttributeIfUseOriginalOrNotBlank(assertion, NAME, nameTextField, originalName);
        setAttributeIfUseOriginalOrNotBlank(assertion, VALUE, valueTextField, originalValue);
        setAttributeIfUseOriginalOrNotBlank(assertion, DOMAIN, domainTextField, originalDomain);
        setAttributeIfUseOriginalOrNotBlank(assertion, PATH, pathTextField, originalPath);
        setAttributeIfUseOriginalOrNotBlank(assertion, MAX_AGE, maxAgeTextField, originalMaxAge);
        setAttributeIfUseOriginalOrNotBlank(assertion, VERSION, versionTextField, originalVersion);
        setAttributeIfUseOriginalOrNotBlank(assertion, COMMENT, commentTextField, originalComment);
        setAttributeIfUseOriginalOrNotBlank(assertion, SECURE, String.valueOf(secureCheckBox.isSelected()), originalSecure);
        setAttributeIfUseOriginalOrNotBlank(assertion, HTTP_ONLY, String.valueOf(httpOnlyCheckBox.isSelected()), originalHttpOnly);
        assertion.getCookieCriteria().clear();
        setCriteriaIfNotBlank(assertion, NAME, nameMatchTextField, nameRegexCheckBox);
        setCriteriaIfNotBlank(assertion, DOMAIN, domainMatchTextField, domainRegexCheckBox);
        setCriteriaIfNotBlank(assertion, PATH, pathMatchTextField, pathRegexCheckBox);
        return assertion;
    }

    private void setAttributeIfUseOriginalOrNotBlank(final ManageCookieAssertion assertion, final String attributeName, final String attributeValue, final JCheckBox originalCheckBox) {
        if (originalCheckBox.isSelected() || StringUtils.isNotBlank(attributeValue)) {
            assertion.getCookieAttributes().put(attributeName, new CookieAttribute(attributeName, attributeValue.trim(), originalCheckBox.isSelected()));
        }
    }

    private void setAttributeIfUseOriginalOrNotBlank(final ManageCookieAssertion assertion, final String attributeName, final JTextField attributeTextField, final JCheckBox originalCheckBox) {
        setAttributeIfUseOriginalOrNotBlank(assertion, attributeName, attributeTextField.getText(), originalCheckBox);
    }

    private void setCriteriaIfNotBlank(final ManageCookieAssertion assertion, final String attributeName, final JTextField attributeTextField, final JCheckBox regexCheckBox) {
        if (StringUtils.isNotBlank(attributeTextField.getText())) {
            assertion.getCookieCriteria().put(attributeName, new CookieCriteria(attributeName, attributeTextField.getText().trim(), regexCheckBox.isSelected()));
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        operationComboBox.setModel(new DefaultComboBoxModel
                (new Operation[]{ADD, ADD_OR_REPLACE, REMOVE, UPDATE}));
        operationComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                return super.getListCellRendererComponent(list, value instanceof Operation ? ((Operation) value).getName() : value, index, isSelected, cellHasFocus);
            }
        });
        operationComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                buildValidationRules();
            }
        }));
        final RunOnChangeListener listener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
            }
        });
        originalName.addActionListener(listener);
        originalValue.addActionListener(listener);
        originalDomain.addActionListener(listener);
        originalPath.addActionListener(listener);
        originalVersion.addActionListener(listener);
        originalMaxAge.addActionListener(listener);
        originalComment.addActionListener(listener);
        originalSecure.addActionListener(listener);
        originalHttpOnly.addActionListener(listener);
        validators = new InputValidator(this, getTitle());
        buildValidationRules();
    }

    private void buildValidationRules() {
        validators.clearRules();
        validators.ensureComboBoxSelection("operation", operationComboBox);
        final Operation op = (Operation) operationComboBox.getSelectedItem();
        switch (op) {
            case ADD_OR_REPLACE:
            case ADD:
                validators.constrainTextFieldToBeNonEmpty("name", nameTextField, null);
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "version", versionTextField, false));
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "max age", maxAgeTextField, true));
                break;
            case UPDATE:
            case REMOVE:
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "version", versionTextField, true));
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, "max age", maxAgeTextField, true));
                validators.addRule(new InputValidator.ValidationRule() {
                    @Override
                    public String getValidationError() {
                        String error = null;
                        if (nameMatchTextField.getText().isEmpty() && domainMatchTextField.getText().isEmpty() && pathMatchTextField.getText().isEmpty()) {
                            error = "At least one of name, domain or path to match must be specified.";
                        }
                        return error;
                    }
                });
                break;
            default:
                logger.log(Level.WARNING, "Unrecognized operation: " + op);
        }
    }

    private void enableDisable() {
        final Object op = operationComboBox.getSelectedItem();
        originalName.setEnabled(op == UPDATE);
        originalValue.setEnabled(op == UPDATE);
        originalDomain.setEnabled(op == UPDATE);
        originalPath.setEnabled(op == UPDATE);
        originalMaxAge.setEnabled(op == UPDATE);
        originalComment.setEnabled(op == UPDATE);
        originalVersion.setEnabled(op == UPDATE);
        originalSecure.setEnabled(op == UPDATE);
        originalHttpOnly.setEnabled(op == UPDATE);

        nameLabel.setEnabled(op != REMOVE);
        final boolean addOrAddOrReplace = op == ADD || op == ADD_OR_REPLACE;
        nameTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalName.isSelected()));
        valueLabel.setEnabled(op != REMOVE);
        valueTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalValue.isSelected()));
        domainLabel.setEnabled(op != REMOVE);
        domainTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalDomain.isSelected()));
        pathLabel.setEnabled(op != REMOVE);
        pathTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalPath.isSelected()));
        maxAgeLabel.setEnabled(op != REMOVE);
        maxAgeTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalMaxAge.isSelected()));
        commentLabel.setEnabled(op != REMOVE);
        commentTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalComment.isSelected()));
        versionLabel.setEnabled(op != REMOVE);
        versionTextField.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalVersion.isSelected()));
        secureCheckBox.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalSecure.isSelected()));
        httpOnlyCheckBox.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalHttpOnly.isSelected()));

        final boolean updateOrRemove = op == UPDATE || op == REMOVE;
        matchPanel.setEnabled(updateOrRemove);
        nameMatchLabel.setEnabled(updateOrRemove);
        nameMatchTextField.setEnabled(updateOrRemove);
        nameRegexCheckBox.setEnabled(updateOrRemove);
        domainMatchLabel.setEnabled(updateOrRemove);
        domainMatchTextField.setEnabled(updateOrRemove);
        domainRegexCheckBox.setEnabled(updateOrRemove);
        pathMatchLabel.setEnabled(updateOrRemove);
        pathMatchTextField.setEnabled(updateOrRemove);
        pathRegexCheckBox.setEnabled(updateOrRemove);
    }
}
