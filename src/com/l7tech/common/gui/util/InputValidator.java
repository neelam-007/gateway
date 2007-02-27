/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.gui.util;

import com.l7tech.common.gui.NumberField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * Runs validation rules, optionally doing so whenever an Ok button is pressed, optionally displaying a dialog
 * whenever a validation rule fails.  Also provides utility methods for easily creating common validation rules.
 * TODO i18n
 */
public class InputValidator {
    private final String dialogTitle;
    private final Component dialogParent;
    private final List rules = new ArrayList();
    private final Map<ModelessFeedback, String> feedbacks = new HashMap<ModelessFeedback, String>();
    private AbstractButton buttonToEnable = null;

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

    /** Register a custom validation rule that has already been configured to monitor a componenet for changes. */
    private void addRule(ValidationRule rule) {
        if (rule == null) throw new NullPointerException();
        rules.add(rule);
    }

    /**
     * Remove a validation rule.
     * @return true iff. a rule was removed
     */
    public boolean removeRule(ValidationRule rule) {
        return rules.remove(rule);
    }

    /**
     * Configures the specified text component to use a {@link com.l7tech.common.gui.NumberField} instance as its Document,
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
        if (comp == null) throw new NullPointerException();
        int maxlen = Math.max(Long.toString(min).length(), Long.toString(max).length());
        comp.setDocument(new NumberField(maxlen + 1));

        final String mess = "The " + fieldName + " field must be a number between " + min + " and " + max + ".";
        ValidationRule rule = new ComponentValidationRule(comp) {
            public String getValidationError() {
                if (!comp.isEnabled())
                    return null;

                String val = comp.getText();
                try {
                    long ival = Long.parseLong(val);
                    if (ival >= min && ival <= max) return null;
                } catch (Exception e) {
                    // fallthrough and return error message
                }
                return mess;
            }
        };

        addRuleForComponent(comp, rule);
        return rule;
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
                                                         final ValidationRule additionalConstraints)
    {
        if (comp == null) throw new NullPointerException();
        final String mess = "The " + fieldName + " field must not be empty.";
        ValidationRule rule = new ComponentValidationRule(comp) {
            public String getValidationError() {
                if (!getComponent().isEnabled())
                    return null;

                String val = comp.getText();
                if (val == null || val.length() < 1) return mess;
                return additionalConstraints != null ? additionalConstraints.getValidationError() : null;
            }
        };

        addRuleForComponent(comp, rule);
        return rule;
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
            // TODO can we validate only the field that changed, watching for the last invalid one to become valid?
            comp.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    isValid();
                }

                public void removeUpdate(DocumentEvent e) {
                    isValid();
                }

                public void changedUpdate(DocumentEvent e) {
                    isValid();
                }
            });

            comp.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    String prop = evt.getPropertyName();
                    if (prop == null || "enabled".equals(prop)) {
                        // Revalidate when monitored field is enabled or disabled
                        isValid();
                    }
                }
            });
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
     */
    public void attachToButton(final AbstractButton button, final ActionListener runOnSuccess) {
        button.addActionListener(new ActionListener() {
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
        try {
            List errors = new ArrayList();
            for (Iterator i = rules.iterator(); i.hasNext();) {
                ValidationRule rule = (ValidationRule)i.next();
                ModelessFeedback feedback = getFeedback(rule);
                String err = rule.getValidationError();
                if (err != null) {
                    errors.add(err);
                    if (feedback != null)
                        feedbacks.put(feedback, err);
                }
            }
            if (buttonToEnable != null) buttonToEnable.setEnabled(errors.isEmpty());
            return (String[])errors.toArray(new String[0]);
        } finally {
            updateAllFeedback();
        }
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
     * Returns true if validation succeeded, or false if an error dialog has already been displayed.
     */
    public boolean validateWithDialog() {
        String err = validate();
        if (err != null) {
            JOptionPane.showMessageDialog(dialogParent, err, dialogTitle, JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}
