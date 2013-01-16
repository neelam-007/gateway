package com.l7tech.gui.util;

import com.l7tech.gui.NumberField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

/**
 * Runs validation rules, optionally doing so whenever an Ok button is pressed, optionally displaying a dialog
 * whenever a validation rule fails.  Also provides utility methods for easily creating common validation rules.
 * TODO i18n
 */
public class InputValidator implements FocusListener {
    public static final String MUST_BE_NUMERIC = "The {0} field must be a number between {1,number,#} and {2,number,#}.";
    public static final String MUST_BE_INTEGER = "The {0} field must be an integer between {1,number,#} and {2,number,#}.";
    private final String dialogTitle;
    private final Component dialogParent;
    private final List<ValidationRule> rules = new ArrayList<ValidationRule>();
    private final Map<ModelessFeedback, String> feedbacks = new HashMap<ModelessFeedback, String>();
    private final Map<ModelessFeedback, String> hiddenFeedbacks = new HashMap<ModelessFeedback, String>();
    private final Set<Component> focusListening = new HashSet<Component>();
    private AbstractButton buttonToEnable = null;
    private Component componentToMakeVisible = null;
    private DocumentListener validatingDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            isValid();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            isValid();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            isValid();
        }
    };
    private PropertyChangeListener validatingEnableStateListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String prop = evt.getPropertyName();
            if (prop == null || "enabled".equals(prop)) {
                // Revalidate when monitored field is enabled or disabled
                isValid();
            }
        }
    };

    /**
     * Create a validator that will display validation error messages using the specified component the dialog
     * parent, and using the specified text as the dialog title.
     *
     * @param dialogParent  the parent component for error dialogs.  Must not be null.
     * @param dialogTitle   the title to use for error dialogs.  Must not be null.
     */
    public InputValidator(Component dialogParent, String dialogTitle) {
        if (dialogParent == null || dialogTitle == null) throw new NullPointerException();
        this.dialogParent = dialogParent;
        this.dialogTitle = dialogTitle;
    }

    /** Arbitrary validation rule.  If a rule pertains to a particular component, use {@link ComponentValidationRule} instead. */
    public static interface ValidationRule {
        /** @return the validation error message, or null if the rule succeeded. */
        String getValidationError();
    }

    /** A validation rule that pertains to a particular component. */
    public static abstract class ComponentValidationRule implements ValidationRule {
        protected final Component component;

        protected ComponentValidationRule(Component component) {
            this.component = component;
            if (component == null) throw new IllegalArgumentException("Component must not be null");
        }

        public Component getComponent() {
            return component;
        }
    }

    /**
     * A ComponentValidationRule which only validates the component if it is enabled.
     */
    public static abstract class EnabledComponentValidationRule extends ComponentValidationRule {
        final ValidationRule additionalConstraints;

        /**
         * @param component the component to validate.
         * @param additionalConstraints any additional constraints which should be validated.
         */
        protected EnabledComponentValidationRule(@NotNull final Component component, @Nullable ValidationRule additionalConstraints) {
            super(component);
            this.additionalConstraints = additionalConstraints;
        }

        /**
         * Validate the component and execute any additional validations.
         * @return an error message if the component is invalid.
         */
        protected abstract String validateComponent();

        @Override
        public String getValidationError() {
            String error = null;
            if (getComponent().isEnabled()) {
                error = validateComponent();
                if (error == null && additionalConstraints != null) {
                    error = additionalConstraints.getValidationError();
                }
            }
            return error;
        }
    }

    /**
     * Validation Rule associated with a number spinner.
     */
    public static class NumberSpinnerValidationRule extends ComponentValidationRule {
        private String numberTextFieldName;
        private Comparable minimum;
        private Comparable maximum;

        public NumberSpinnerValidationRule(JSpinner numSpinner, String numberTextFieldName) {
            super(numSpinner);

            if (! (numSpinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException("This spinner is not a number spinner.");
            } else if (numberTextFieldName == null || numberTextFieldName.isEmpty()) {
                throw new IllegalArgumentException("The number field name is empty or not specified.");
            }

            // Keep the current value in the spinner.  This is good for user watching the validation result.
            ((JSpinner.DefaultEditor) numSpinner.getEditor()).getTextField().setFocusLostBehavior(JFormattedTextField.PERSIST);

            this.numberTextFieldName = numberTextFieldName;
            minimum = ((SpinnerNumberModel)numSpinner.getModel()).getMinimum();
            maximum = ((SpinnerNumberModel)numSpinner.getModel()).getMaximum();

            if (minimum != null && !(minimum instanceof Integer || minimum instanceof Long)) {
                throw new IllegalArgumentException("Invalid minimum");
            } else if (maximum != null && !(maximum instanceof Integer || maximum instanceof Long)) {
                throw new IllegalArgumentException("Invalid maximum");
            } else if (minimum != null && maximum != null) {
                long min = (minimum instanceof Integer)? (Integer)minimum : (Long)minimum;
                long max = (maximum instanceof Integer)? (Integer)maximum : (Long)maximum;
                if (min > max) throw new IllegalArgumentException("Minimum must not be greater than Maximum");
            }
        }

        @Override
        public String getValidationError() {
            JSpinner spinner = (JSpinner)component;
            if (! component.isEnabled()) return null;
            String valueText = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().getText();

            try {
                Number currentNum = ((JSpinner.NumberEditor)spinner.getEditor()).getFormat().parse(valueText);
                long currentValue;
                if (currentNum instanceof Integer) currentValue = (Integer)currentNum;
                else if (currentNum instanceof Long) currentValue = (Long)currentNum;
                else throw new NumberFormatException("Invalid number in the spinner");

                if (minimum == null && maximum != null) {
                    long max = (maximum instanceof Integer)? (Integer)maximum : (Long)maximum;
                    if (currentValue > max) {
                        return "'" + numberTextFieldName + "' is greater than the maximum limit, " + maximum + ".";
                    }
                } else if (minimum != null && maximum == null) {
                    long min = (minimum instanceof Integer)? (Integer)minimum : (Long)minimum;
                    if (currentValue < min) {
                        return "'" + numberTextFieldName + "' is less than the minimum limit, " + minimum + ".";
                    }
                } else if (minimum != null && maximum != null) {
                    long min = (minimum instanceof Integer)? (Integer)minimum : (Long)minimum;
                    long max = (maximum instanceof Integer)? (Integer)maximum : (Long)maximum;
                    if (currentValue < min || currentValue > max) {
                        return "'" + numberTextFieldName + "' must be a number between " + minimum + " and " + maximum + ".";
                    }
                }
                spinner.commitEdit();
            } catch (Exception e) {
                final StringBuilder errorBuilder = new StringBuilder();
                errorBuilder.append( "'" ).append( valueText ).append( "' is not a number. A valid number must be " );
                if ( minimum == null && maximum != null ) {
                    errorBuilder.append( "less than or equal to " ).append( maximum ).append( "." );
                } else if ( minimum != null && maximum == null ) {
                    errorBuilder.append( "greater than or equal to " ).append( minimum ).append( "." );
                } else {
                    errorBuilder.append( "between " ).append( minimum ).append( " and " ).append( maximum ).append( "." );
                }
                return errorBuilder.toString();
            }

            return null;
        }

        /**
         * Validate the given validator when the spinner is changed.
         *
         * <p>WARNING: The JSpinner may only validate after focus is lost,
         * which can mean a button may have been pressed before the
         * re-validation occurs.</p>
         *
         * @param validator The validator to validate
         * @param spinner The spinner to listen to
         */
        public static void validateOnChange( final InputValidator validator, final JSpinner spinner ) {
            final RunOnChangeListener listener = new RunOnChangeListener(){
                @Override
                protected void run() {
                    validator.isValid();
                }
            };
            if ( spinner.getEditor() instanceof JSpinner.DefaultEditor ) {
                ((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().getDocument().addDocumentListener(listener);
            }
            spinner.addChangeListener( listener );
        }
    }

    /**
     * Register a custom validation rule that has already been configured to monitor a component for changes.
     * @param rule the rule to add
     */
    public void addRule(ValidationRule rule) {
        if (rule == null) throw new NullPointerException();
        rules.add(rule);
        if (rule instanceof ComponentValidationRule) {
            ComponentValidationRule compRule = (ComponentValidationRule)rule;
            Component c = compRule.getComponent();
            if (c instanceof JTextComponent && c instanceof ModelessFeedback)
                monitorFocus(c);
        }
    }

    /**
     * Add a list of rules into the original rule list.
     * @param others: a list of validation rules to be added.
     */
    public void addRules(Collection<ValidationRule> others) {
        if (others != null && !others.isEmpty()) {
            rules.addAll(others);
        }
    }

    /**
     * Remove a validation rule.
     * @param rule the rule to remove
     * @return true iff. a rule was removed
     */
    public boolean removeRule(ValidationRule rule) {
        return rules.remove(rule);
    }

    /**
     * Configures the specified text component to use a {@link com.l7tech.gui.NumberField} instance as its Document,
     * and registers a validation rule that requires the specified text field to be a valid number in the specified range.
     * <p/>
     * The field will <b>not</b> be validated if it is disabled -- validation rules for disabled fields will always
     * succeed.
     * <p/>
     * If this validator has been configured to disable an Ok button when invalid, the text component will also
     * have a document change listener installed that triggers a call to validate().
     *
     * @param fieldName  the name of the field, for use in a generated error message.  Must not be null.
     * @param comp  the component to validate.   Must not be null.
     * @param min the minimum allowable value, inclusive
     * @param max the maximum allowable value, inclusive
     * @return the validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule constrainTextFieldToNumberRange(final String fieldName,
                                                          final JTextComponent comp,
                                                          final long min,
                                                          final long max)
    {
        return constrainTextFieldToNumberRange( fieldName, comp, min, max, false );
    }

    /**
     * Configures the specified text component to use a {@link com.l7tech.gui.NumberField} instance as its Document,
     * and registers a validation rule that requires the specified text field to be a valid number in the specified range.
     * <p/>
     * If the field allows empty values then the text can be empty.
     * <p/>
     * The field will <b>not</b> be validated if it is disabled -- validation rules for disabled fields will always
     * succeed.
     * <p/>
     * If this validator has been configured to disable an Ok button when invalid, the text component will also
     * have a document change listener installed that triggers a call to validate().
     *
     * @param fieldName  the name of the field, for use in a generated error message.  Must not be null.
     * @param comp  the component to validate.   Must not be null.
     * @param min the minimum allowable value, inclusive
     * @param max the maximum allowable value, inclusive
     * @param allowEmpty true if an empty field value is permitted.
     * @return the validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule constrainTextFieldToNumberRange(final String fieldName,
                                                          final JTextComponent comp,
                                                          final long min,
                                                          final long max,
                                                          final boolean allowEmpty )
    {
        if (comp == null) throw new NullPointerException();
        int maxlen = Math.max(Long.toString(min).length(), Long.toString(max).length());
        comp.setDocument(new NumberField(maxlen + 1));

        final ValidationRule rule = buildTextFieldNumberRangeValidationRule( fieldName, comp, min, max, allowEmpty );

        addRuleForComponent(comp, rule);
        return rule;
    }

    /**
     * Build a validator for a number range. You can add this validator using addRule(...) (etc)
     * <p/>
     * If the field allows empty values then the text can be empty.
     * <p/>
     * The field will <b>not</b> be validated if it is disabled -- validation rules for disabled fields will always
     * succeed.
     *
     * @param fieldName  the name of the field, for use in a generated error message.  Must not be null.
     * @param comp  the component to validate.   Must not be null.
     * @param min the minimum allowable value, inclusive
     * @param max the maximum allowable value, inclusive
     * @param allowEmpty true if an empty field value is permitted.
     * @return the validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule buildTextFieldNumberRangeValidationRule( final String fieldName,
                                                                   final JTextComponent comp,
                                                                   final long min,
                                                                   final long max,
                                                                   final boolean allowEmpty ) {
        final String mess = MessageFormat.format(MUST_BE_NUMERIC, fieldName, min, max);
        return new ComponentValidationRule(comp) {
            @Override
            public String getValidationError() {
                if (!comp.isEnabled())
                    return null;

                String val = comp.getText();
                if ( allowEmpty && val.isEmpty() ) {
                    return null;
                } else {
                    try {
                        long ival = Long.parseLong(val);
                        if (ival >= min && ival <= max) return null;
                    } catch (Exception e) {
                        // fallthrough and return error message
                    }
                    return mess;
                }
            }
        };
    }

    /**
     * Configure the specified text component so that validation will fail if it has no text in it, or if
     * extra constraints failed.
     * <p/>
     * The field will <b>not</b> be validated if it is disabled -- validation rules for disabled fields will always
     * succeed.
     * <p/>
     * If this validator has been configured to disable an Ok button when invalid, the text component will also
     * have a document change listener installed that triggers a call to validate().
     * <p/>
     * FieldName will be used for the "field must not be empty" error message, but any ValidationRule supplied
     * by the caller is responsible for its own error message text.
     *
     * @param fieldName  the name of the field, for use in a generated error message.  Must not be null.
     * @param comp  the component to validate.   Must not be null.
     * @param additionalConstraints additional validation constraints for this field, or null to stick with the basic "must not be empty" rule.
     * @return the validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule constrainTextFieldToBeNonEmpty(final String fieldName,
                                                         final JTextComponent comp,
                                                         @Nullable final ValidationRule additionalConstraints)
    {
        final ValidationRule rule = new EnabledComponentValidationRule(comp, additionalConstraints) {
            @Override
            protected String validateComponent() {
                String error = null;
                final String val = comp.getText();
                if (val == null || val.trim().isEmpty()) {
                    error = "The " + fieldName + " field must not be empty.";
                }
                return error;
            }
        };

        addRuleForComponent(comp, rule);
        return rule;
    }

    /**
     * Registers a ValidationRule on the text field to ensure it does not contain over a max number of characters.
     *
     * If the text field is not enabled, no validation will be performed.
     *
     * @param fieldName the name of the text field.
     * @param comp the text field to validate.
     * @param maxChars the maximum number of characters allowed in the text field.
     * @param additionalConstraints any additional constraints that should be validated.
     * @return the ValidationRule registered on the component which ensures it does not contain over a max number of characters.
     */
    public ValidationRule constrainTextFieldToMaxChars(@NotNull final String fieldName,
                                                       @NotNull final JTextComponent comp,
                                                       final int maxChars,
                                                       @Nullable final ValidationRule additionalConstraints) {
        final ValidationRule rule = new EnabledComponentValidationRule(comp, additionalConstraints) {
            @Override
            protected String validateComponent() {
                String error = null;
                final String val = comp.getText();
                if (val != null && val.length() > maxChars) {
                    error = "The " + fieldName + " field must have a maximum of " + maxChars + " characters.";
                }
                return error;
            }
        };

        addRuleForComponent(comp, rule);
        return rule;
    }

    /**
     * Add a validation rule to enforce selection in a JComboBox.
     *
     * WARNING
     * WARNING This does not currently support validation on change.
     * WARNING
     *
     * @param fieldName The name of the field, for use in a generated error message.  Must not be null.
     * @param comp The component to validate.   Must not be null.
     * @return The validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule ensureComboBoxSelection( @NotNull final String fieldName,
                                                   @NotNull final JComboBox comp ) {
        final String mess = "The " + fieldName + " field must be selected.";
        final ValidationRule rule = new ComponentValidationRule(comp) {
            @Override
            public String getValidationError() {
                return
                        comp.isEnabled() &&
                        comp.getSelectedItem() == null ?
                                mess :
                                null;
            }
        };

        addRule(rule);
        return rule;
    }

    /**
     * Configure the specified text component to trigger a validation whenever its document is changed.
     * <p/>
     * This must be done after any call that changes the component's documnet
     * (for example, {@link #constrainTextFieldToNumberRange}).
     *
     * @param comp the component to trigger validation
     */
    public void validateWhenDocumentChanges(final JTextComponent comp) {
        // TODO can we validate only the field that changed, watching for the last invalid one to become valid?
        comp.getDocument().removeDocumentListener(validatingDocumentListener);
        comp.getDocument().addDocumentListener(validatingDocumentListener);
        comp.removePropertyChangeListener(validatingEnableStateListener);
        comp.addPropertyChangeListener(validatingEnableStateListener);
    }

    /**
     * Configure the specified text component so that validation will fail if the specified rule fails.
     * <p/>
     * The field will <b>not</b> be validated if it is disabled -- validation rules for disabled fields will always
     * succeed.
     * <p/>
     * If this validator has been configured to disable an Ok button when invalid, the text component will also
     * have a document change listener installed that triggers a call to validate().
     * <p/>
     * The validation rule is responsible for generating its own error message text.
     *
     * @param comp  the component to validate.   Must not be null.
     * @param constraints the constraints for this field.  Must not be null.
     * @return the validation rule that was registered, so it can be removed later if desired.  Never null.
     */
    public ValidationRule constrainTextField(final JTextComponent comp,
                                             ValidationRule constraints)
    {
        if (comp == null || constraints == null) throw new NullPointerException();
        addRuleForComponent(comp, constraints);
        return constraints;
    }

    /**
     * Install the specified validation rule, and enable validateOnChange for the specified component.
     * The component's document must not be replaced after this method is called.
     *
     * @param comp  the component to monitor.  Must not be null.
     * @param rule  the associated validation rule.  Must not be null.
     */
    private void addRuleForComponent(final JTextComponent comp, ValidationRule rule) {
        validateOnChange(comp);
        addRule(rule);
    }

    /**
     * Configure the specified component such that validate() is called whenever its document changes.
     * Takes no action if unless {@link #disableButtonWhenInvalid(javax.swing.AbstractButton)} has been called
     * on this validator instance.
     *
     * @param comp the component to monitor.  Must not be null.
     */
    private void validateOnChange(JTextComponent comp) {
        if (buttonToEnable != null) {
            validateWhenDocumentChanges(comp);
        }
    }

    /**
     * Convenience method that configures the specified button to run this validator whenever it is activated,
     * and to invoke the specified caller-provided action only if validation is successful.
     * This works by adding an ActionListener to the button.  For this to have
     * the intended effect, you must add this validation listener to the button instead of adding your own
     * action listener directly.
     *
     * @param button  the button to which an ActionListener should be added.  Must not be null.
     * @param runOnSuccess  the action to invoke if the button is pressed and validation succeeds
     */
    public void attachToButton(final AbstractButton button, final ActionListener runOnSuccess) {
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validateWithDialog()) runOnSuccess.actionPerformed(e);
            }
        });
    }

    /**
     * Disable the specified button whenever it is invalid.  This causes all future ValidationRules created
     * using this validator instance to add a hook to the watched component that revalidates whenever the
     * componenet is changed.  Thus, this will only work if all ValidationRules are created using future
     * method calls on this validator instance, or if the caller ensures that they set up their own change listeners
     * that call validate (or isValid()) on this instance whenever a watched component changes.
     * <p/>
     * Only one button can be monitored.  Multiple calls to this method with different button arguments are not permitted.
     *
     * @param button the button to be controlled by future ValidationRules created through this validator.  Must not be null.
     * @throws IllegalStateException if this method has already been called once before with a different button.
     */
    public void disableButtonWhenInvalid(final AbstractButton button) {
        if (buttonToEnable != null && buttonToEnable != button)
            throw new IllegalStateException("Already monitoring a different button");
        buttonToEnable = button;
    }

    /** @return true iff. all currently-registered validation rules are succeeding. */
    public boolean isValid() {
        return validate() == null;
    }

    /** @return all validation errors, or an empty array if no validation rule is currently failing.  Never null. */
    public String[] getAllValidationErrors() {
        feedbacks.clear();
        hiddenFeedbacks.clear();
        try {
            List<String> errors = new ArrayList<String>();
            componentToMakeVisible = null;
            for (ValidationRule rule : rules) {
                ModelessFeedback feedback = getFeedback(rule);
                String err = rule.getValidationError();
                if (err != null) {
                    errors.add(err);
                    if (componentToMakeVisible == null && rule instanceof ComponentValidationRule) {
                        componentToMakeVisible = ((ComponentValidationRule)rule).getComponent();
                    }
                    if (feedback != null) {
                        // Special rule for text components: if they have focus and are empty, don't show feedback
                        if (feedback instanceof JTextComponent) {
                            JTextComponent tc = (JTextComponent)feedback;
                            if (tc.isFocusOwner() && tc.getText().length() < 1) {
                                hiddenFeedbacks.put(feedback, err);
                                err = null;
                                monitorFocus(tc);
                            }
                        }

                        feedbacks.put(feedback, err);
                    }
                }
            }
            if (buttonToEnable != null) buttonToEnable.setEnabled(errors.isEmpty());
            return errors.toArray(new String[errors.size()]);
        } finally {
            updateAllFeedback();
        }
    }

    private void monitorFocus(Component tc) {
        if (focusListening.contains(tc))
            return;
        tc.addFocusListener(this);
        focusListening.add(tc);
    }

    /**
     * Runs all validation rules, and returns the first error message encountered.  Returns null only if
     * validation succeeds.
     * @return the first validation error message encountered, or null if all validation rules succeeded.
     */
    public String validate() {
        String[] got = getAllValidationErrors();
        return got == null || got.length < 1 ? null : got[0];
    }

    private void updateAllFeedback() {
        for (Map.Entry<ModelessFeedback, String> entry : feedbacks.entrySet())
            entry.getKey().setModelessFeedback(entry.getValue());
    }

    private ModelessFeedback getFeedback(ValidationRule rule) {
        ModelessFeedback feedback = null;
        if (rule instanceof ComponentValidationRule) {
            ComponentValidationRule cvr = (ComponentValidationRule)rule;
            Component comp;
            comp = cvr.getComponent();
            if (comp instanceof ModelessFeedback) {
                feedback = (ModelessFeedback)comp;
                if (!feedbacks.containsKey(feedback))
                    feedbacks.put(feedback, null);
            }
        }
        return feedback;
    }

    /**
     * Runs all validation rules, and displays an error dialog (and throws) if any fail.
     *
     * @return true if validation succeeded, or false if an error dialog has already been displayed.
     */
    public boolean validateWithDialog() {
        String err = validate();
        if (err != null) {
            if (componentToMakeVisible != null)
                makeComponentVisible(componentToMakeVisible);
            JOptionPane.showMessageDialog(dialogParent, err, dialogTitle, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Tries to make the specified component visible; if it is in a tabbed pane, tries to show the
     * tab it's in.
     *
     * @param component the component to attempt to make visible.
     */
    private void makeComponentVisible(Component component) {
        if (component == null)
            return;
        for (Container parent = component.getParent(); parent != null; component = parent, parent = component.getParent()) {
            if (parent instanceof JTabbedPane) {
                JTabbedPane tp = (JTabbedPane)parent;
                tp.setSelectedComponent(component);
            }
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        // If we left an empty text component showing error feedback, hide it when it gains focus
        Component comp = e.getComponent();
        if (!(comp instanceof ModelessFeedback))
            return;

        ModelessFeedback feedback = (ModelessFeedback)comp;
        String message = feedbacks.get(feedback);
        if (message == null)
            return;

        if (!(comp instanceof JTextComponent))
            return;

        JTextComponent tc = (JTextComponent)comp;
        if (tc.getText().length() < 1) {
            feedbacks.remove(feedback);
            hiddenFeedbacks.put(feedback, message);
            feedback.setModelessFeedback(null);
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (hiddenFeedbacks.isEmpty())
            return;

        // If we left an empty text component hiding error feedback, show it when it loses focus
        Component comp = e.getComponent();
        if (!(comp instanceof ModelessFeedback))
            return;

        ModelessFeedback feedback = (ModelessFeedback)comp;
        String message = hiddenFeedbacks.get(feedback);
        if (message == null)
            return;

        if (!(comp instanceof JTextComponent))
            return;

        JTextComponent tc = (JTextComponent)comp;
        if (tc.getText().length() < 1) {
            hiddenFeedbacks.remove(feedback);
            feedbacks.put(feedback, message);
            feedback.setModelessFeedback(message);
        }
    }
}
