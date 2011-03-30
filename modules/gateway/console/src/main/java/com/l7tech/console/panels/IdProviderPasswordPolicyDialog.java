package com.l7tech.console.panels;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderPasswordPolicy;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class IdProviderPasswordPolicyDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(IdProviderPasswordPolicyDialog.class.getName());
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

    private InputValidator inputValidator;
    private IdentityProviderPasswordPolicy passwordPolicy;
    private boolean pciEnabled;
    private boolean confirmed = false;
    private boolean isReadOnly = false;

    // STIG minimum values
    private static boolean STIG_FORCE_CHANGE = true;
    private static int STIG_MIN_LENGTH = 8;
    private static int STIG_MAX_LENGTH = 32;
    private static int STIG_FREQUENCY = 10;
    private static int STIG_EXPIRY = 90;
    private static boolean STIG_ALLOW_CHANGE = true;
    private static int STIG_UPPER = 1;
    private static int STIG_LOWER = 1;
    private static int STIG_NUM = 1;
    private static int STIG_SYMBOL = 1;
    private static int STIG_DIFF = 4;
    private static boolean STIG_REPEAT = true;

    // PCI-DSS minimum values
    private static boolean PCI_FORCE_CHANGE = true;
    private static int PCI_MIN_LENGTH = 7;
    private static int PCI_FREQUENCY = 4;
    private static int PCI_EXPIRY = 90;
    private static int PCI_UPPER = 1;
    private static int PCI_LOWER = 1;
    private static int PCI_NUM = 1;


    public IdProviderPasswordPolicyDialog(Window owner, IdentityProviderPasswordPolicy passwordPolicy, boolean pciEnabled, boolean isReadOnly) {
        super(owner, DIALOG_TITLE, IdProviderPasswordPolicyDialog.DEFAULT_MODALITY_TYPE);
        this.isReadOnly = isReadOnly;
        this.pciEnabled = pciEnabled;
        initialize(passwordPolicy);
    }

    private void initialize(IdentityProviderPasswordPolicy passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        inputValidator = new InputValidator(this, DIALOG_TITLE);
       
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if ((Integer)minPasswordLengthSpinner.getValue() < 1) {
                    return getResourceString("password.min.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if (maxPasswordLengthCheckBox.isSelected() &&
                    ((Integer)maxPasswordLengthSpinner.getValue()<1)) {
                    return getResourceString("password.max.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if (maxPasswordLengthCheckBox.isSelected() &&
                    ((Integer)minPasswordLengthSpinner.getValue() > (Integer)maxPasswordLengthSpinner.getValue())) {
                    return getResourceString("password.length.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if (repeatFrequencyCheckBox.isSelected() && (Integer)repeatFrequencySpinner.getValue() < 1) {
                    return getResourceString("repeat.frequency.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if (passwordExpiryCheckBox.isSelected() && (Integer)passwordExpirySpinner.getValue() < 1) {
                    return getResourceString("expiry.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                int sum = 0;
                if(upperCaseCheckBox.isSelected())
                    sum +=(Integer)upperCaseSpinner.getValue();
                if(lowercaseCheckBox.isSelected())
                    sum +=(Integer)lowerCaseSpinner.getValue();
                if(numbersCheckBox.isSelected())
                    sum +=(Integer)numberSpinner.getValue();
                if(symbolCheckBox.isSelected())
                    sum +=(Integer)symbolSpinner.getValue();

                if(sum > (Integer)minPasswordLengthSpinner.getValue())
                {
                    return getResourceString("minimum.characters.error");
                }
                
                int nonNumericMax = (Integer) nonNumericSpinner.getValue() + (Integer)numberSpinner.getValue();
                if(nonNumericCheckBox.isSelected() &&
                   nonNumericMax > (Integer)minPasswordLengthSpinner.getValue())
                {
                    return getResourceString("minimum.characters.error");
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                if(characterDifferenceCheckBox.isSelected() && 
                   (Integer) charDifferenceSpinner.getValue() > (Integer)minPasswordLengthSpinner.getValue())
                {
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

        resetPCIDSSButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToPCIDSS();
            }
        });

        resetSTIGButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetToSTIG();
            }
        });

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run(){
                enableOrDisableComponents();
            }
        });
        ((SpinnerNumberModel)minPasswordLengthSpinner.getModel()).setMinimum(3);
        ((SpinnerNumberModel)maxPasswordLengthSpinner.getModel()).setMinimum(3);
        ((SpinnerNumberModel)maxPasswordLengthSpinner.getModel()).setMaximum(128);
        ((SpinnerNumberModel)repeatFrequencySpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)repeatFrequencySpinner.getModel()).setMaximum(100);
        ((SpinnerNumberModel)passwordExpirySpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)passwordExpirySpinner.getModel()).setMaximum(365);
        ((SpinnerNumberModel)upperCaseSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)lowerCaseSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)numberSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)symbolSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)nonNumericSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel)charDifferenceSpinner.getModel()).setMinimum(0);

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

        RunOnChangeListener requirementsListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run(){
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


        // initialize values to STIG
        resetToSTIG();
        clearValues();
        
        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
    }

    private void updateRequirementWarning() {
        boolean STIG = !pciEnabled;  // todolk;
        boolean noWarning;
        noWarning = (forcePwdChangeCheckBox.isSelected());
        noWarning = noWarning && ((Integer)minPasswordLengthSpinner.getValue() >= (STIG?STIG_MIN_LENGTH : PCI_MIN_LENGTH));
        noWarning = noWarning && (!STIG || (maxPasswordLengthCheckBox.isSelected() && (Integer) maxPasswordLengthSpinner.getValue() >= STIG_MAX_LENGTH));
        noWarning = noWarning && (repeatFrequencyCheckBox.isSelected() &&
                                   (Integer)repeatFrequencySpinner.getValue() >= (STIG?STIG_FREQUENCY : PCI_FREQUENCY));
        noWarning = noWarning && (passwordExpiryCheckBox.isSelected() &&
                                   (Integer)passwordExpirySpinner.getValue() >= (STIG?STIG_EXPIRY : PCI_EXPIRY));
        noWarning = noWarning && (!STIG || allowableChangesCheckBox.isSelected() == STIG_ALLOW_CHANGE);
        noWarning = noWarning && (upperCaseCheckBox.isSelected() &&
                                   (Integer)upperCaseSpinner.getValue() >= (STIG?STIG_UPPER : PCI_UPPER));
        noWarning = noWarning && (lowercaseCheckBox.isSelected() &&
                                   (Integer)lowerCaseSpinner.getValue() >= (STIG?STIG_LOWER : PCI_LOWER));
        noWarning = noWarning && (numbersCheckBox.isSelected() &&
                                   (Integer)numberSpinner.getValue() >= (STIG?STIG_NUM : PCI_NUM));
        noWarning = noWarning && (!STIG || (symbolCheckBox.isSelected() && (Integer) symbolSpinner.getValue() >= STIG_SYMBOL));
        noWarning = noWarning && (!STIG || (characterDifferenceCheckBox.isSelected() && (Integer) charDifferenceSpinner.getValue() >= STIG_DIFF));
        noWarning = noWarning && (!STIG || noRepeatingCharactersCheckBox.isSelected() == STIG_REPEAT);

        warningLabel.setText(noWarning? null: (STIG? getResourceString("below.stig.warning"):getResourceString("below.pcidss.warning")));
    }

    private String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }

    private void enableOrDisableComponents() {

        maxPasswordLengthSpinner.setEnabled( maxPasswordLengthCheckBox.isSelected());
        upperCaseSpinner.setEnabled( upperCaseCheckBox.isSelected());
        upperCaseUnitLabel.setEnabled( upperCaseCheckBox.isSelected());
        lowerCaseSpinner.setEnabled( lowercaseCheckBox.isSelected());
        lowerCaseUnitLabel.setEnabled( lowercaseCheckBox.isSelected());
        numberSpinner.setEnabled( numbersCheckBox.isSelected());
        numbersUnitLabel.setEnabled( numbersCheckBox.isSelected());
        symbolSpinner.setEnabled( symbolCheckBox.isSelected());
        symbolUnitLabel.setEnabled( symbolCheckBox.isSelected());
        nonNumericSpinner.setEnabled( nonNumericCheckBox.isSelected());
        nonNumbericUnitLabel.setEnabled( nonNumericCheckBox.isSelected());
        charDifferenceSpinner.setEnabled( characterDifferenceCheckBox.isSelected());
        charDifferenceUnitLabel.setEnabled( characterDifferenceCheckBox.isSelected());
        repeatFrequencySpinner.setEnabled( repeatFrequencyCheckBox.isSelected());
        passwordExpirySpinner.setEnabled( passwordExpiryCheckBox.isSelected());
        passwordExpiryUnit.setEnabled( passwordExpiryCheckBox.isSelected());

        okButton.setEnabled(!isReadOnly);
    }


    /**
     * Configure the GUI control states with information gathered from the passwordPolicy instance.
     */
    private void modelToView() {

        forcePwdChangeCheckBox.setSelected(passwordPolicy.getBooleanProperty( IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE));

        Integer minPasswordLength = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH);
        if(minPasswordLength > 0)
            minPasswordLengthSpinner.setValue(minPasswordLength);

        Integer maxPasswordLength = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH);
        maxPasswordLengthCheckBox.setSelected(maxPasswordLength>0);
        if(maxPasswordLength > 0)
            maxPasswordLengthSpinner.setValue(maxPasswordLength);

        Integer repeatFreq = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY);
        repeatFrequencyCheckBox.setSelected(repeatFreq>0);
        if(repeatFreq>0)
            repeatFrequencySpinner.setValue(repeatFreq);

        Integer passwordExpiry = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY);
        passwordExpiryCheckBox.setSelected(passwordExpiry > 0);
        if(passwordExpiry > 0)
            passwordExpirySpinner.setValue(passwordExpiry);

        allowableChangesCheckBox.setSelected(passwordPolicy.getBooleanProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES));

        // Character requirements
        Integer upperMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.UPPER_MIN);
        upperCaseCheckBox.setSelected(upperMin>0);
        if(upperMin > 0)
            upperCaseSpinner.setValue(upperMin);

        Integer lowerMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.LOWER_MIN);
        lowercaseCheckBox.setSelected(lowerMin>0);
        if(lowerMin > 0)
            lowerCaseSpinner.setValue(lowerMin);

        Integer numMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.NUMBER_MIN);
        numbersCheckBox.setSelected(numMin>0);
        if(numMin > 0)
            numberSpinner.setValue(numMin);

        Integer symbolMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN);
        symbolCheckBox.setSelected(symbolMin>0);
        if(symbolMin > 0)
            symbolSpinner.setValue(symbolMin);

        Integer nonNumMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN);
        nonNumericCheckBox.setSelected(nonNumMin>0);
        if(nonNumMin>0)
            nonNumericSpinner.setValue(nonNumMin);

        Integer charDiffMin = passwordPolicy.getIntegerProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN);
        characterDifferenceCheckBox.setSelected(charDiffMin>0);
        if(charDiffMin>0)
            charDifferenceSpinner.setValue(charDiffMin);

        noRepeatingCharactersCheckBox.setSelected(passwordPolicy.getBooleanProperty( IdentityProviderPasswordPolicy.NO_REPEAT_CHARS));

        enableOrDisableComponents();
        updateRequirementWarning();
    }

    /**
     * Configure the passwordPolicy instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {

        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.FORCE_PWD_CHANGE , forcePwdChangeCheckBox.isSelected());
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.MIN_PASSWORD_LENGTH,minPasswordLengthSpinner.getValue());
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.MAX_PASSWORD_LENGTH,maxPasswordLengthCheckBox.isSelected()? maxPasswordLengthSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.REPEAT_FREQUENCY,repeatFrequencyCheckBox.isSelected()?(Integer)repeatFrequencySpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.PASSWORD_EXPIRY,passwordExpiryCheckBox.isSelected()?passwordExpirySpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.ALLOWABLE_CHANGES,allowableChangesCheckBox.isSelected());

        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.UPPER_MIN,upperCaseCheckBox.isSelected()?(Integer)upperCaseSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.LOWER_MIN,lowercaseCheckBox.isSelected()?(Integer)lowerCaseSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NUMBER_MIN,numbersCheckBox.isSelected()?(Integer)numberSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.SYMBOL_MIN,symbolCheckBox.isSelected()?(Integer)symbolSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NON_NUMERIC_MIN,nonNumericCheckBox.isSelected()?(Integer)nonNumericSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.CHARACTER_DIFF_MIN,characterDifferenceCheckBox.isSelected()?(Integer)charDifferenceSpinner.getValue():null);
        passwordPolicy.setProperty(IdentityProviderPasswordPolicy.NO_REPEAT_CHARS,noRepeatingCharactersCheckBox.isSelected());

    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void clearValues(){
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

    private void resetToSTIG(){
        clearValues();
        minPasswordLengthSpinner.setValue(STIG_MIN_LENGTH);
        maxPasswordLengthCheckBox.setSelected(true);
        maxPasswordLengthSpinner.setValue(STIG_MAX_LENGTH);

        upperCaseCheckBox.setSelected(true);
        upperCaseSpinner.setValue(STIG_UPPER);
        lowercaseCheckBox.setSelected(true);
        lowerCaseSpinner.setValue(STIG_LOWER);
        numbersCheckBox.setSelected(true);
        numberSpinner.setValue(STIG_NUM);
        symbolCheckBox.setSelected(true);
        symbolSpinner.setValue(STIG_SYMBOL);
        characterDifferenceCheckBox.setSelected(true);
        charDifferenceSpinner.setValue(STIG_DIFF);
        noRepeatingCharactersCheckBox.setSelected(true);

        passwordExpiryCheckBox.setSelected(true);
        passwordExpirySpinner.setValue(STIG_EXPIRY);
        repeatFrequencyCheckBox.setSelected(true);
        repeatFrequencySpinner.setValue(STIG_FREQUENCY);
        forcePwdChangeCheckBox.setSelected(STIG_FORCE_CHANGE);
        allowableChangesCheckBox.setSelected(STIG_ALLOW_CHANGE);

        enableOrDisableComponents();
        updateRequirementWarning();
    }
    
    private void resetToPCIDSS(){
        clearValues();
        
        minPasswordLengthSpinner.setValue(PCI_MIN_LENGTH);
        upperCaseCheckBox.setSelected(true);
        upperCaseSpinner.setValue(PCI_UPPER);
        lowercaseCheckBox.setSelected(true);
        lowerCaseSpinner.setValue(PCI_LOWER);
        numbersCheckBox.setSelected(true);
        numberSpinner.setValue(PCI_NUM);

        repeatFrequencyCheckBox.setSelected(true);
        repeatFrequencySpinner.setValue(PCI_FREQUENCY);
        forcePwdChangeCheckBox.setSelected(true);
        passwordExpirySpinner.setValue(PCI_EXPIRY); 
        passwordExpiryCheckBox.setSelected(true);

        enableOrDisableComponents();
        updateRequirementWarning();
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

}
