package com.l7tech.console.panels.encass;

import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog used to resolve EncapsulatedAssertionConfig import conflicts.
 */
public class EncapsulatedAssertionConfigConflictDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(EncapsulatedAssertionConfigConflictDialog.class.getName());
    private JTextField encassNameTextField;
    private JTextField policyNameTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel contentPanel;
    private JLabel encassLabel;
    private JLabel policyLabel;
    private boolean confirmed;
    private String encassName;
    private String policyName;
    private InputValidator inputValidator;
    private EncapsulatedAssertionConfig conflictingConfig;
    private Policy conflictingPolicy;

    /**
     * @param owner the parent of this dialog.
     * @param toImport the EncapsulatedAssertionConfig to be imported.
     * @param conflictingConfig the EncapsulatedAssertionConfig that conflicts with the one being imported.
     *                          Can be null if a conflicting Policy is provided.
     * @param conflictingPolicy the Policy which conflicts with the one being imported.
     *                          Can be null if a conflicting EncapsulatedAssertionConfig is provided.
     */
    public EncapsulatedAssertionConfigConflictDialog(@NotNull Window owner, @NotNull final EncapsulatedAssertionConfig toImport,
                                                     @Nullable final EncapsulatedAssertionConfig conflictingConfig,
                                                     @Nullable final Policy conflictingPolicy) {
        super(owner, "Encapsulated Assertion Conflict", ModalityType.APPLICATION_MODAL);
        Validate.isTrue(conflictingConfig != null || conflictingPolicy != null, "Must have a conflicting config or policy or both.");
        Utilities.setEscKeyStrokeDisposes(this);
        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        this.conflictingConfig = conflictingConfig;
        this.conflictingPolicy = conflictingPolicy;
        encassName = toImport.getName();
        policyName = toImport.getPolicy().getName();
        encassNameTextField.setText(encassName);
        policyNameTextField.setText(policyName);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));
        enableOrDisable();
        initValidators();
    }

    private void initValidators() {
        inputValidator = new InputValidator(this, "Error");
        inputValidator.constrainTextFieldToBeNonEmpty("Encapsulated Assertion name", encassNameTextField, null);
        inputValidator.constrainTextFieldToBeNonEmpty("Policy name", policyNameTextField, null);
        if (conflictingConfig != null) {
            inputValidator.addRule(new InputValidator.ValidationRule() {
                @Override
                public String getValidationError() {
                    String error = null;
                    try {
                        final String encassName = encassNameTextField.getText().trim();
                        final EncapsulatedAssertionConfig sameEncassName = Registry.getDefault().getEncapsulatedAssertionAdmin().findByUniqueName(encassName);
                        if (sameEncassName != null) {
                            error = "Encapsulated Assertion name " + encassName + " is already in use.";
                        }
                    } catch (final FindException e) {
                        error = "Unable to validate Encapsulated Assertion name.";
                        logger.log(Level.WARNING, error + " " + e.getMessage(), ExceptionUtils.getDebugException(e));
                    }
                    return error;
                }
            });
        }
        if (conflictingPolicy != null) {
            inputValidator.addRule(new InputValidator.ValidationRule() {
                @Override
                public String getValidationError() {
                    String error = null;
                    final String policyName = policyNameTextField.getText().trim();
                    try {
                        final Policy samePolicyName = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(policyName);
                        if (samePolicyName != null) {
                            error = "Policy name " + policyName + " is already in use.";
                        }
                    } catch (final FindException e) {
                        error = "Unable to validate Policy name.";
                        logger.log(Level.WARNING, error + " " + e.getMessage(), ExceptionUtils.getDebugException(e));
                    }
                    return error;
                }
            });
        }
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                encassName = encassNameTextField.getText().trim();
                policyName = policyNameTextField.getText().trim();
                dispose();
            }
        });
    }

    /**
     * Only displays fields that are relevant to the conflicting entities.
     */
    private void enableOrDisable() {
        encassLabel.setEnabled(conflictingConfig != null);
        encassLabel.setVisible(conflictingConfig != null);
        encassNameTextField.setEnabled(conflictingConfig != null);
        encassNameTextField.setVisible(conflictingConfig != null);
        policyLabel.setEnabled(conflictingPolicy != null);
        policyLabel.setVisible(conflictingPolicy != null);
        policyNameTextField.setEnabled(conflictingPolicy != null);
        policyNameTextField.setVisible(conflictingPolicy != null);
    }

    /**
     * @return whether the dialog was OKed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return the name to use for the imported EncapsulatedAssertionConfig.
     */
    public String getEncassName() {
        return encassName;
    }

    /**
     * @return the name to use for the imported EncapsulatedAssertionConfig backing policy.
     */
    public String getPolicyName() {
        return policyName;
    }
}
