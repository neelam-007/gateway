package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderLimits;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;

public class IdProviderPasswordPolicyDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.IdProviderPasswordPolicyDialog");

    private static final String DIALOG_TITLE = resources.getString("dialog.title");

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JSpinner minPasswordLengthSpinner;
    private JCheckBox upperCaseCheckBox;
    private JCheckBox lowercaseCheckBox;
    private JCheckBox numbersCheckBox;
    private JCheckBox symbolCheckBox;
    private JCheckBox nonNumericCheckBox;
    private JCheckBox characterDifferenceCheckBox;
    private JCheckBox noRepeatingCharactersCheckBox;
    private JSpinner upperCaseSpinner;
    private JSpinner lowerCaseSpinner;
    private JSpinner numberSpinner;
    private JSpinner nonNumericSpinner;
    private JSpinner symbolSpinner;
    private JSpinner charDifferenceSpinner;
    private JLabel upperCaseUnitLabel;
    private JLabel lowerCaseUnitLabel;
    private JLabel numbersUnitLabel;
    private JLabel symbolUnitLabel;
    private JLabel nonNumbericUnitLabel;
    private JLabel charDifferenceUnitLabel;
    private JSpinner repeatFrequencySpinner;
    private JCheckBox forcePwdChangeCheckBox;
    private JSpinner maxPasswordLengthSpinner;
    private JSpinner passwordExpirySpinner;
    private JLabel passwordExpiryUnit;
    private JButton resetPCIDSSButton;
    private JButton resetSTIGButton;
    private JCheckBox allowableChangesCheckBox;
    private JCheckBox maxPasswordLengthCheckBox;
    private JCheckBox passwordExpiryCheckBox;
    private JCheckBox repeatFrequencyCheckBox;
    private JLabel warningLabel;

    private IdentityProviderPasswordPolicy passwordPolicy;
    private String minimumsName;
    private Map<String, IdentityProviderPasswordPolicy> minimumsPolicies;
    private boolean confirmed = false;
    private boolean isReadOnly = false;


    public IdProviderPasswordPolicyDialog(final Window owner,
                                          final IdentityProviderPasswordPolicy passwordPolicy,
                                          final String minimumsName,
                                          final Map<String, IdentityProviderPasswordPolicy> minimumsPolicies,
                                          boolean isReadOnly) {
        super(owner, DIALOG_TITLE, IdProviderPasswordPolicyDialog.DEFAULT_MODALITY_TYPE);
        final PermissionFlags flags = PermissionFlags.get(EntityType.PASSWORD_POLICY);
        this.isReadOnly = isReadOnly || (!flags.canUpdateSome());
        this.minimumsName = minimumsName;
        this.minimumsPolicies = minimumsPolicies;
        initialize(passwordPolicy);
    }

    private void initialize(IdentityProviderPasswordPolicy passwordPolicy) {

        this.passwordPolicy = passwordPolicy;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
        initSpinner(minPasswordLengthSpinner,getResourceString("minlength.label"), IdentityProviderLimits.MIN_PASSWORD_LENGTH.getValue(), IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(maxPasswordLengthSpinner,getResourceString("maxlength.label"), IdentityProviderLimits.MIN_PASSWORD_LENGTH.getValue(), IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        maxPasswordLengthSpinner.setValue(IdentityProviderLimits.MIN_PASSWORD_LENGTH.getValue());
        initSpinner(repeatFrequencySpinner,getResourceString("repeatfrequency.label"),1,50,inputValidator);
        initSpinner(passwordExpirySpinner,getResourceString("password.expire.label"),1,365*5,inputValidator);
        initSpinner(upperCaseSpinner,getResourceString("upperchars.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(lowerCaseSpinner,getResourceString("lowerchars.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(numberSpinner,getResourceString("numbers.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(symbolSpinner,getResourceString("symbol.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(nonNumericSpinner,getResourceString("nonnumeric.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);
        initSpinner(charDifferenceSpinner,getResourceString("characterdiff.label"),1,IdentityProviderLimits.MAX_PASSWORD_LENGTH.getValue(),inputValidator);

        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if ((Integer) minPasswordLengthSpinner.getValue() < 1) {
                    return getResourceString("password.min.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (maxPasswordLengthCheckBox.isSelected() &&
                        ((Integer) maxPasswordLengthSpinner.getValue() < 1)) {
                    return getResourceString("password.max.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (maxPasswordLengthCheckBox.isSelected() &&
                        ((Integer) minPasswordLengthSpinner.getValue() > (Integer) maxPasswordLengthSpinner.getValue())) {
                    return getResourceString("password.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (repeatFrequencyCheckBox.isSelected() && (Integer) repeatFrequencySpinner.getValue() < 1) {
                    return getResourceString("repeat.frequency.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (passwordExpiryCheckBox.isSelected() && (Integer) passwordExpirySpinner.getValue() < 1) {
                    return getResourceString("expiry.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                int sum = 0;
                if (upperCaseCheckBox.isSelected())
                    sum += (Integer) upperCaseSpinner.getValue();
                if (lowercaseCheckBox.isSelected())
                    sum += (Integer) lowerCaseSpinner.getValue();
                if (numbersCheckBox.isSelected())
                    sum += (Integer) numberSpinner.getValue();
                if (symbolCheckBox.isSelected())
                    sum += (Integer) symbolSpinner.getValue();

                if (sum > (Integer) minPasswordLengthSpinner.getValue()) {
                    return getResourceString("minimum.characters.error");
                }

                int nonNumericMax = (Integer) nonNumericSpinner.getValue() + (Integer) numberSpinner.getValue();
                if (nonNumericCheckBox.isSelected() &&
                        nonNumericMax > (Integer) minPasswordLengthSpinner.getValue()) {
                    return getResourceString("minimum.characters.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                if (characterDifferenceCheckBox.isSelected() &&
                        (Integer) charDifferenceSpinner.getValue() > (Integer) minPasswordLengthSpinner.getValue()) {
                    return getResourceString("minimum.characters.error");
                }

                return null;
            }
        });
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        resetPCIDSSButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToPCIDSS();
            }
        });

        resetSTIGButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToSTIG();
            }
        });

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        });

        maxPasswordLengthCheckBox.addActionListener(enableDisableListener);
        upperCaseCheckBox.addActionListener(enableDisableListener);
        lowercaseCheckBox.addActionListener(enableDisableListener);
        numbersCheckBox.addActionListener(enableDisableListener);
        symbolCheckBox.addActionListener(enableDisableListener);
        nonNumericCheckBox.addActionListener(enableDisableListener);
        characterDifferenceCheckBox.addActionListener(enableDisableListener);
        noRepeatingCharactersCheckBox.addActionListener(enableDisableListener);
        allowableChangesCheckBox.addActionListener(enableDisableListener);
        passwordExpiryCheckBox.addActionListener(enableDisableListener);
        repeatFrequencyCheckBox.addActionListener(enableDisableListener);

        RunOnChangeListener requirementsListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateRequirementWarning();
            }
        });

        upperCaseCheckBox.addActionListener(requirementsListener);
        lowercaseCheckBox.addActionListener(requirementsListener);
        numbersCheckBox.addActionListener(requirementsListener);
        symbolCheckBox.addActionListener(requirementsListener);
        nonNumericCheckBox.addActionListener(requirementsListener);
        characterDifferenceCheckBox.addActionListener(requirementsListener);
        noRepeatingCharactersCheckBox.addActionListener(requirementsListener);
        forcePwdChangeCheckBox.addActionListener(requirementsListener);
        allowableChangesCheckBox.addActionListener(requirementsListener);
        maxPasswordLengthCheckBox.addActionListener(requirementsListener);
        passwordExpiryCheckBox.addActionListener(requirementsListener);
        repeatFrequencyCheckBox.addActionListener(requirementsListener);
        minPasswordLengthSpinner.addChangeListener(requirementsListener);
        upperCaseSpinner.addChangeListener(requirementsListener);
        lowerCaseSpinner.addChangeListener(requirementsListener);
        numberSpinner.addChangeListener(requirementsListener);
        nonNumericSpinner.addChangeListener(requirementsListener);
        symbolSpinner.addChangeListener(requirementsListener);
        charDifferenceSpinner.addChangeListener(requirementsListener);
        repeatFrequencySpinner.addChangeListener(requirementsListener);
        maxPasswordLengthSpinner.addChangeListener(requirementsListener);
        passwordExpirySpinner.addChangeListener(requirementsListener);


        // initialize main values to STIG
        resetToSTIG();
        upperCaseSpinner.setValue( 1 );
        lowerCaseSpinner.setValue( 1 );
        numberSpinner.setValue( 1 );
        symbolSpinner.setValue( 1 );
        nonNumericSpinner.setValue( 1 );
        charDifferenceSpinner.setValue( 1 );
        clearValues();

        Utilities.setEscKeyStrokeDisposes(this);

        modelToView(passwordPolicy);
    }

    private void initSpinner(JSpinner spinner, String resourceString, int min, int max, InputValidator inputValidator) {
        ((SpinnerNumberModel) spinner.getModel()).setMinimum(min);
        ((SpinnerNumberModel) spinner.getModel()).setMaximum(max);
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(spinner, resourceString));
    }

    private void updateRequirementWarning() {
        boolean noWarning = true;

        if (minimumsName != null && minimumsPolicies.get(minimumsName) != null) {
            final IdentityProviderPasswordPolicy passwordPolicy = new IdentityProviderPasswordPolicy();
            viewToModel(passwordPolicy);
            noWarning = passwordPolicy.hasStrengthOf(minimumsPolicies.get(minimumsName));
        }

        warningLabel.setText(noWarning ? null : (MessageFormat.format(getResourceString("below.warning"), minimumsName)));
    }

    private String getResourceString(String key) {
        final String value = resources.getString(key);
        if (value.endsWith(":")) {
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

    private void enableOrDisableComponents() {

        maxPasswordLengthSpinner.setEnabled(maxPasswordLengthCheckBox.isSelected());
        upperCaseSpinner.setEnabled(upperCaseCheckBox.isSelected());
        upperCaseUnitLabel.setEnabled(upperCaseCheckBox.isSelected());
        lowerCaseSpinner.setEnabled(lowercaseCheckBox.isSelected());
        lowerCaseUnitLabel.setEnabled(lowercaseCheckBox.isSelected());
        numberSpinner.setEnabled(numbersCheckBox.isSelected());
        numbersUnitLabel.setEnabled(numbersCheckBox.isSelected());
        symbolSpinner.setEnabled(symbolCheckBox.isSelected());
        symbolUnitLabel.setEnabled(symbolCheckBox.isSelected());
        nonNumericSpinner.setEnabled(nonNumericCheckBox.isSelected());
        nonNumbericUnitLabel.setEnabled(nonNumericCheckBox.isSelected());
        charDifferenceSpinner.setEnabled(characterDifferenceCheckBox.isSelected());
        charDifferenceUnitLabel.setEnabled(characterDifferenceCheckBox.isSelected());
        repeatFrequencySpinner.setEnabled(repeatFrequencyCheckBox.isSelected());
        passwordExpirySpinner.setEnabled(passwordExpiryCheckBox.isSelected());
        passwordExpiryUnit.setEnabled(passwordExpiryCheckBox.isSelected());

        okButton.setEnabled(!isReadOnly);
    }


    /**
     * Configure the GUI control states with information gathered from the passwordPolicy instance.
     */
    private void modelToView(final IdentityProviderPasswordPolicy passwordPolicy) {

        forcePwdChangeCheckBox.setSelected(passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE));

        Integer minPasswordLength = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH);
        if (minPasswordLength > 0)
            minPasswordLengthSpinner.setValue(minPasswordLength);

        Integer maxPasswordLength = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH);
        maxPasswordLengthCheckBox.setSelected(maxPasswordLength > 0);
        if (maxPasswordLength > 0)
            maxPasswordLengthSpinner.setValue(maxPasswordLength);

        Integer repeatFreq = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY);
        repeatFrequencyCheckBox.setSelected(repeatFreq > 0);
        if (repeatFreq > 0)
            repeatFrequencySpinner.setValue(repeatFreq);

        Integer passwordExpiry = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY);
        passwordExpiryCheckBox.setSelected(passwordExpiry > 0);
        if (passwordExpiry > 0)
            passwordExpirySpinner.setValue(passwordExpiry);

        allowableChangesCheckBox.setSelected(passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES));

        // Character requirements
        Integer upperMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.UPPER_MIN);
        upperCaseCheckBox.setSelected(upperMin > 0);
        if (upperMin > 0)
            upperCaseSpinner.setValue(upperMin);

        Integer lowerMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.LOWER_MIN);
        lowercaseCheckBox.setSelected(lowerMin > 0);
        if (lowerMin > 0)
            lowerCaseSpinner.setValue(lowerMin);

        Integer numMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.NUMBER_MIN);
        numbersCheckBox.setSelected(numMin > 0);
        if (numMin > 0)
            numberSpinner.setValue(numMin);

        Integer symbolMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN);
        symbolCheckBox.setSelected(symbolMin > 0);
        if (symbolMin > 0)
            symbolSpinner.setValue(symbolMin);

        Integer nonNumMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN);
        nonNumericCheckBox.setSelected(nonNumMin > 0);
        if (nonNumMin > 0)
            nonNumericSpinner.setValue(nonNumMin);

        Integer charDiffMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN);
        characterDifferenceCheckBox.setSelected(charDiffMin > 0);
        if (charDiffMin > 0)
            charDifferenceSpinner.setValue(charDiffMin);

        noRepeatingCharactersCheckBox.setSelected(passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.NO_REPEAT_CHARS));

        enableOrDisableComponents();
        updateRequirementWarning();
    }

    /**
     * Configure the passwordPolicy instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel(final IdentityProviderPasswordPolicy passwordPolicy) {
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE, forcePwdChangeCheckBox.isSelected());
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH, minPasswordLengthSpinner.getValue());
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH, maxPasswordLengthCheckBox.isSelected() ? maxPasswordLengthSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY, repeatFrequencyCheckBox.isSelected() ? (Integer) repeatFrequencySpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY, passwordExpiryCheckBox.isSelected() ? passwordExpirySpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES, allowableChangesCheckBox.isSelected());
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.UPPER_MIN, upperCaseCheckBox.isSelected() ? (Integer) upperCaseSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.LOWER_MIN, lowercaseCheckBox.isSelected() ? (Integer) lowerCaseSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NUMBER_MIN, numbersCheckBox.isSelected() ? (Integer) numberSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN, symbolCheckBox.isSelected() ? (Integer) symbolSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN, nonNumericCheckBox.isSelected() ? (Integer) nonNumericSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN, characterDifferenceCheckBox.isSelected() ? (Integer) charDifferenceSpinner.getValue() : null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NO_REPEAT_CHARS, noRepeatingCharactersCheckBox.isSelected());
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void clearValues() {
        maxPasswordLengthCheckBox.setSelected(false);

        upperCaseCheckBox.setSelected(false);
        lowercaseCheckBox.setSelected(false);
        numbersCheckBox.setSelected(false);
        symbolCheckBox.setSelected(false);
        nonNumericCheckBox.setSelected(false);
        characterDifferenceCheckBox.setSelected(false);
        noRepeatingCharactersCheckBox.setSelected(false);

        forcePwdChangeCheckBox.setSelected(false);
        allowableChangesCheckBox.setSelected(false);
        passwordExpiryCheckBox.setSelected(false);
        repeatFrequencyCheckBox.setSelected(false);
    }

    private void resetToPolicy(final IdentityProviderPasswordPolicy passwordPolicy) {
        if (passwordPolicy != null) {
            clearValues();
            modelToView(passwordPolicy);
            enableOrDisableComponents();
            updateRequirementWarning();
        }
    }

    private void resetToSTIG() {
        resetToPolicy(minimumsPolicies.get("STIG"));
    }

    private void resetToPCIDSS() {
        resetToPolicy(minimumsPolicies.get("PCI-DSS"));
    }

    private void onOk() {
        viewToModel(passwordPolicy);
        confirmed = true;
        dispose();
    }

    /**
     * @return true if the dialog has been dismissed with the ok button
     */
    public boolean isConfirmed() {
        return confirmed;
    }

}
