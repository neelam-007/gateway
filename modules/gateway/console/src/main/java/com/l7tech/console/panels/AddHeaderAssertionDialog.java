package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterPropertyDescriptor;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AddHeaderAssertionDialog extends AssertionPropertiesOkCancelSupport<AddHeaderAssertion> {
    private static final Logger logger = Logger.getLogger(AddHeaderAssertionDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(AddHeaderAssertionDialog.class.getName());

    private static final String ADD = "Add";
    private static final String ADD_OR_REPLACE = "Add or Replace";
    private static final String REMOVE = "Remove";

    private static final String COMPONENT_LABEL_SUFFIX = ":";
    private static final String METADATA_TYPES_CLUSTER_PROP = "transport.metadata.manageableTypes";

    private JPanel contentPane;
    private SquigglyTextField headerNameTextField;
    private SquigglyTextField headerValueTextField;
    private JCheckBox nameExpressionCheckBox;
    private JCheckBox valueExpressionCheckBox;
    private JComboBox<String> typeComboBox;
    private JComboBox<String> operationComboBox;

    private InputValidator inputValidator;

    public AddHeaderAssertionDialog(final Window owner, final AddHeaderAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
        enableDisable();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        operationComboBox.setModel(new DefaultComboBoxModel<>(new String[] {ADD, ADD_OR_REPLACE, REMOVE}));

        operationComboBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisable();
                evaluatePattern(headerNameTextField, nameExpressionCheckBox);
                evaluatePattern(headerValueTextField, valueExpressionCheckBox);
            }
        }));

        typeComboBox.setModel(new DefaultComboBoxModel<>(getMetadataTypes()));

        headerNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { evaluatePattern(headerNameTextField, nameExpressionCheckBox); }
            @Override
            public void removeUpdate(DocumentEvent e) { evaluatePattern(headerNameTextField, nameExpressionCheckBox); }
            @Override
            public void changedUpdate(DocumentEvent e) { evaluatePattern(headerNameTextField, nameExpressionCheckBox); }
        });

        headerValueTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { evaluatePattern(headerValueTextField, valueExpressionCheckBox); }
            @Override
            public void removeUpdate(DocumentEvent e) { evaluatePattern(headerValueTextField, valueExpressionCheckBox); }
            @Override
            public void changedUpdate(DocumentEvent e) { evaluatePattern(headerValueTextField, valueExpressionCheckBox); }
        });

        TextComponentPauseListenerManager.registerPauseListener(headerNameTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long millis) {
                evaluatePattern(headerNameTextField, nameExpressionCheckBox);
            }
        }, 700);

        TextComponentPauseListenerManager.registerPauseListener(headerValueTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long millis) {
                evaluatePattern(headerValueTextField, valueExpressionCheckBox);
            }
        }, 700);

        Utilities.enableDefaultFocusTraversal(headerNameTextField);
        Utilities.enableDefaultFocusTraversal(headerValueTextField);
        Utilities.attachDefaultContextMenu(headerNameTextField);
        Utilities.attachDefaultContextMenu(headerValueTextField);

        nameExpressionCheckBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                evaluatePattern(headerNameTextField, nameExpressionCheckBox);
            }
        }));

        valueExpressionCheckBox.addActionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                evaluatePattern(headerValueTextField, valueExpressionCheckBox);
            }
        }));

        // --- Validation ---
        inputValidator = new InputValidator(this, getResourceString("validationErrorTitle"));

        inputValidator.constrainTextFieldToBeNonEmpty("Header Name", headerNameTextField, null);

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                try {
                    Syntax.getReferencedNames(headerNameTextField.getText());

                    if (nameExpressionCheckBox.isEnabled() && nameExpressionCheckBox.isSelected()) {
                        Pattern.compile(headerNameTextField.getText());
                    }
                } catch (VariableNameSyntaxException e) {
                    return "Error with header name variable '" + e.getMessage() + "'.";
                } catch (PatternSyntaxException e) {
                    return "Invalid regular expression pattern: " + e.getMessage() + ".";
                }

                return null;
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                try {
                    if (null != headerValueTextField.getText()) {
                        Syntax.getReferencedNames(headerValueTextField.getText());

                        if (valueExpressionCheckBox.isEnabled() && valueExpressionCheckBox.isSelected()) {
                            Pattern.compile(headerValueTextField.getText());
                        }
                    }
                } catch (VariableNameSyntaxException e) {
                    return "Error with header value variable '" + e.getMessage() + "'.";
                } catch (PatternSyntaxException e) {
                    return "Invalid regular expression pattern: " + e.getMessage() + ".";
                }

                return null;
            }
        });
    }

    private void evaluatePattern(SquigglyTextField textArea, JCheckBox checkBox) {
        textArea.setToolTipText(null);
        boolean patternWarning = false;

        Pattern pattern = null;

        if (checkBox.isEnabled() && checkBox.isSelected() &&
                textArea.getText() != null && !"".equals(textArea.getText())) {
            try {
                String text = textArea.getText();

                if (Syntax.getReferencedNames(text, false).length > 0) {
                    text = Pattern.quote(Syntax.regexPattern.matcher(text).replaceAll("\\${$1}"));
                }

                pattern = Pattern.compile(text);

                // Warn on leading or trailing whitespace
                if (textArea.getText().matches("^\\s.*|.*\\s$")) {
                    textArea.setToolTipText("Note: pattern contains leading or trailing whitespace");
                    patternWarning = true;
                } else {
                    textArea.setToolTipText("OK");
                }
            } catch (PatternSyntaxException e1) {
                textArea.setToolTipText(e1.getDescription() + " index: " + e1.getIndex());
            }
        } else {
            textArea.setNone();
            return;
        }

        textArea.setAll();

        if (pattern == null) {
            textArea.setColor(Color.RED);
            textArea.setSquiggly();
        } else {
            if (patternWarning) {
                textArea.setColor(Color.green);
                textArea.setSquiggly();
            } else {
                textArea.setNone();
            }
        }
    }

    private void enableDisable() {
        nameExpressionCheckBox.setEnabled(REMOVE.equals(operationComboBox.getSelectedItem()));
        valueExpressionCheckBox.setEnabled(REMOVE.equals(operationComboBox.getSelectedItem()));
    }

    @Override
    public void setData(final AddHeaderAssertion assertion) {
        if (null != assertion.getMetadataType()) {
            typeComboBox.setSelectedItem(assertion.getMetadataType());

            if (!assertion.getMetadataType().equals(typeComboBox.getSelectedItem())) {
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) typeComboBox.getModel();
                model.insertElementAt(assertion.getMetadataType(), 0);
                typeComboBox.setSelectedItem(assertion.getMetadataType());
            }
        }

        if (assertion.getOperation() == AddHeaderAssertion.Operation.REMOVE) {
            operationComboBox.setSelectedItem(REMOVE);
        } else if (assertion.getOperation() == AddHeaderAssertion.Operation.ADD) {
            if (assertion.isRemoveExisting()) {
                operationComboBox.setSelectedItem(ADD_OR_REPLACE);
            } else {
                operationComboBox.setSelectedItem(ADD);
            }
        }

        headerNameTextField.setText(assertion.getHeaderName() == null ? "" : assertion.getHeaderName());
        headerValueTextField.setText(assertion.getHeaderValue() == null ? "" : assertion.getHeaderValue());
        nameExpressionCheckBox.setSelected(assertion.isEvaluateNameAsExpression());
        valueExpressionCheckBox.setSelected(assertion.isEvaluateValueExpression());
    }

    @Override
    public AddHeaderAssertion getData(final AddHeaderAssertion assertion) throws ValidationException {
        // perform validation
        final String error = inputValidator.validate();

        if (error != null) {
            throw new ValidationException(error);
        }

        assertion.setMetadataType(typeComboBox.getItemAt(typeComboBox.getSelectedIndex()));

        if (REMOVE.equals(operationComboBox.getSelectedItem())) {
            assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        } else if (ADD.equals(operationComboBox.getSelectedItem()) ||
                ADD_OR_REPLACE.equals(operationComboBox.getSelectedItem())) {
            assertion.setOperation(AddHeaderAssertion.Operation.ADD);
            assertion.setRemoveExisting(ADD_OR_REPLACE.equals(operationComboBox.getSelectedItem()));
        }

        assertion.setHeaderName(headerNameTextField.getText());
        assertion.setHeaderValue(headerValueTextField.getText());
        assertion.setEvaluateNameAsExpression(nameExpressionCheckBox.isSelected());
        assertion.setEvaluateValueExpression(valueExpressionCheckBox.isSelected());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private String[] getMetadataTypes() {
        try {
            ClusterProperty prop =
                    Registry.getDefault().getClusterStatusAdmin().findPropertyByName(METADATA_TYPES_CLUSTER_PROP);

            if (null != prop) {
                return prop.getValue().split("\\n");
            } else {
                for (ClusterPropertyDescriptor descriptor :
                        Registry.getDefault().getClusterStatusAdmin().getAllPropertyDescriptors()) {
                    if (METADATA_TYPES_CLUSTER_PROP.equals(descriptor.getName())) {
                        return descriptor.getDefaultValue().split("\\n");
                    }
                }
            }
        } catch (FindException e) {
            String msg = "Unable to find cluster property '" + METADATA_TYPES_CLUSTER_PROP + "'";

            DialogDisplayer.showMessageDialog(this.getParent(), msg,
                    "Failed to retrieve available manageable types", JOptionPane.ERROR_MESSAGE, null);

            logger.log(Level.WARNING, msg, e);
        }

        return new String[0];
    }

    /**
     * Returns the value of the specified resource string. If the string has a label suffix, e.g. a colon,
     * it is removed.
     * @param key the key of the resource
     * @return the resource string
     */
    private static String getResourceString(String key) {
        final String value = resources.getString(key);

        if (value.endsWith(COMPONENT_LABEL_SUFFIX)) {
            return value.substring(0, value.lastIndexOf(COMPONENT_LABEL_SUFFIX));
        }

        return value;
    }
}
