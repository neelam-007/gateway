package com.l7tech.external.assertions.managecookie.console;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.IntegerOrContextVariableValidationRule;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.console.util.jcalendar.JTextFieldDateTimeEditor;
import com.l7tech.external.assertions.managecookie.ManageCookieAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.NameValuePair;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Date;
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
    private JCheckBox originalExpires;
    private JLabel expiresLabel;
    private JDateTimeChooser expiresChooser;
    private InputValidator validators;

    public ManageCookiePropertiesDialog(final Frame parent, final ManageCookieAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void setData(final ManageCookieAssertion assertion) {
        operationComboBox.setSelectedItem(assertion.getOperation());
        final Map<String, NameValuePair> cookieAttributes = assertion.getCookieAttributes();
        nameTextField.setText(cookieAttributes.containsKey(NAME) ? cookieAttributes.get(NAME).getValue() : StringUtils.EMPTY);
        valueTextField.setText(cookieAttributes.containsKey(VALUE) ? cookieAttributes.get(VALUE).getValue() : StringUtils.EMPTY);
        domainTextField.setText(cookieAttributes.containsKey(DOMAIN) ? cookieAttributes.get(DOMAIN).getValue() : StringUtils.EMPTY);
        pathTextField.setText(cookieAttributes.containsKey(PATH) ? cookieAttributes.get(PATH).getValue() : StringUtils.EMPTY);
        commentTextField.setText(cookieAttributes.containsKey(COMMENT) ? cookieAttributes.get(COMMENT).getValue() : StringUtils.EMPTY);
        maxAgeTextField.setText(cookieAttributes.containsKey(MAX_AGE) ? cookieAttributes.get(MAX_AGE).getValue() : StringUtils.EMPTY);
        ((JTextFieldDateTimeEditor) expiresChooser.getDateEditor()).setText(cookieAttributes.containsKey(EXPIRES) ? cookieAttributes.get(EXPIRES).getValue() : StringUtils.EMPTY);
        versionTextField.setText(cookieAttributes.containsKey(VERSION) ? cookieAttributes.get(VERSION).getValue() : StringUtils.EMPTY);
        secureCheckBox.setSelected(cookieAttributes.containsKey(SECURE) ? Boolean.parseBoolean(cookieAttributes.get(SECURE).getValue()) : false);
        httpOnlyCheckBox.setSelected(cookieAttributes.containsKey(HTTP_ONLY) ? Boolean.parseBoolean(cookieAttributes.get(HTTP_ONLY).getValue()) : false);
        originalName.setSelected(!cookieAttributes.containsKey(NAME));
        originalValue.setSelected(!cookieAttributes.containsKey(VALUE));
        originalDomain.setSelected(!cookieAttributes.containsKey(DOMAIN));
        originalPath.setSelected(!cookieAttributes.containsKey(PATH));
        originalComment.setSelected(!cookieAttributes.containsKey(COMMENT));
        originalMaxAge.setSelected(!cookieAttributes.containsKey(MAX_AGE));
        originalExpires.setSelected(!cookieAttributes.containsKey(EXPIRES));
        originalVersion.setSelected(!cookieAttributes.containsKey(VERSION));
        originalSecure.setSelected(!cookieAttributes.containsKey(SECURE));
        originalHttpOnly.setSelected(!cookieAttributes.containsKey(HTTP_ONLY));

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
        maybeSetAttribute(assertion, NAME, nameTextField, originalName);
        maybeSetAttribute(assertion, VALUE, valueTextField, originalValue);
        maybeSetAttribute(assertion, DOMAIN, domainTextField, originalDomain);
        maybeSetAttribute(assertion, PATH, pathTextField, originalPath);
        maybeSetAttribute(assertion, MAX_AGE, maxAgeTextField, originalMaxAge);
        maybeSetAttribute(assertion, EXPIRES, (JTextFieldDateTimeEditor) expiresChooser.getDateEditor(), originalExpires);
        maybeSetAttribute(assertion, VERSION, versionTextField, originalVersion);
        maybeSetAttribute(assertion, COMMENT, commentTextField, originalComment);
        maybeSetAttribute(assertion, SECURE, secureCheckBox.isSelected() ? "true" : "false", originalSecure);
        maybeSetAttribute(assertion, HTTP_ONLY, httpOnlyCheckBox.isSelected() ? "true" : "false", originalHttpOnly);
        assertion.getCookieCriteria().clear();
        setCriteriaIfNotBlank(assertion, NAME, nameMatchTextField, nameRegexCheckBox);
        setCriteriaIfNotBlank(assertion, DOMAIN, domainMatchTextField, domainRegexCheckBox);
        setCriteriaIfNotBlank(assertion, PATH, pathMatchTextField, pathRegexCheckBox);
        return assertion;
    }

    private void maybeSetAttribute(final ManageCookieAssertion assertion, final String attributeName, final String attributeValue, final JCheckBox originalCheckBox) {
        final Operation operation = assertion.getOperation();
        if (operation == UPDATE && !originalCheckBox.isSelected()) {
            assertion.getCookieAttributes().put(attributeName, new NameValuePair(attributeName, attributeValue));
        } else if (StringUtils.isNotBlank(attributeValue) && (operation == ADD || operation == ADD_OR_REPLACE)) {
            assertion.getCookieAttributes().put(attributeName, new NameValuePair(attributeName, attributeValue));
        }
    }

    private void maybeSetAttribute(final ManageCookieAssertion assertion, final String attributeName, final JTextField attributeTextField, final JCheckBox originalCheckBox) {
        maybeSetAttribute(assertion, attributeName, attributeTextField.getText().trim(), originalCheckBox);
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
        originalExpires.addActionListener(listener);
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
                validators.constrainTextFieldToBeNonEmpty(NAME, nameTextField, null);
                validators.constrainTextFieldToBeNonEmpty(VALUE, valueTextField, null);
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, VERSION, versionTextField, true));
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, MAX_AGE, maxAgeTextField, true));
                break;
            case UPDATE:
                validators.addRule(new InputValidator.ValidationRule() {
                    @Override
                    public String getValidationError() {
                        String error = null;
                        if (originalName.isSelected() && originalValue.isSelected() && originalVersion.isSelected() &&
                                originalDomain.isSelected() && originalPath.isSelected() && originalMaxAge.isSelected() &&
                                originalComment.isSelected() && originalSecure.isSelected() && originalHttpOnly.isSelected()) {
                            error = "At least one attribute must be updated.";
                        } else {
                            try {
                                ensureCheckboxOrTextFieldHasInput(NAME, nameTextField, originalName);
                                ensureCheckboxOrTextFieldHasInput(VALUE, valueTextField, originalValue);
                                ensureCheckboxOrTextFieldHasInput(VERSION, versionTextField, originalVersion);
                                ensureCheckboxOrTextFieldHasInput(DOMAIN, domainTextField, originalDomain);
                                ensureCheckboxOrTextFieldHasInput(PATH, pathTextField, originalPath);
                                ensureCheckboxOrTextFieldHasInput(MAX_AGE, maxAgeTextField, originalMaxAge);
                                ensureCheckboxOrTextFieldHasInput(EXPIRES, (JTextFieldDateTimeEditor) expiresChooser.getDateEditor(), originalExpires);
                                ensureCheckboxOrTextFieldHasInput(COMMENT, commentTextField, originalComment);
                            } catch (final ValidationException e) {
                                error = e.getMessage();
                            }
                        }
                        return error;
                    }
                });
            case REMOVE:
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, VERSION, versionTextField, true));
                validators.addRule(new IntegerOrContextVariableValidationRule(0, Integer.MAX_VALUE, MAX_AGE, maxAgeTextField, true));
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

    private void ensureCheckboxOrTextFieldHasInput(final String fieldName, final JTextField textField, final JCheckBox checkBox) {
        // if the original value isn't selected they must provide a value
        if (!checkBox.isSelected() && StringUtils.isEmpty(textField.getText())) {
            throw new ValidationException("The " + fieldName + " field must not be empty.");
        }
    }

    private void enableDisable() {
        final Object op = operationComboBox.getSelectedItem();
        originalName.setEnabled(op == UPDATE);
        originalValue.setEnabled(op == UPDATE);
        originalDomain.setEnabled(op == UPDATE);
        originalPath.setEnabled(op == UPDATE);
        originalMaxAge.setEnabled(op == UPDATE);
        originalExpires.setEnabled(op == UPDATE);
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
        expiresLabel.setEnabled(op != REMOVE);
        expiresChooser.setEnabled(addOrAddOrReplace || (op == UPDATE && !originalExpires.isSelected()));
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

    private void createUIComponents() {
        final JTextFieldDateTimeEditor dateEditor = new JTextFieldDateTimeEditor() {
            @Override
            public void caretUpdate(CaretEvent event) {
                super.caretUpdate(event);
                final String[] referencedNames = Syntax.getReferencedNames(getText(), false);
                if (referencedNames.length > 0) {
                    // it's a context variable and therefore valid
                    setForeground(darkGreen);
                }
            }
        };
        dateEditor.setMinSelectableDate(new Date());
        expiresChooser = new JDateTimeChooser(null, null, CookieUtils.NETSCAPE_RFC850_DATEFORMAT, dateEditor);
    }
}
