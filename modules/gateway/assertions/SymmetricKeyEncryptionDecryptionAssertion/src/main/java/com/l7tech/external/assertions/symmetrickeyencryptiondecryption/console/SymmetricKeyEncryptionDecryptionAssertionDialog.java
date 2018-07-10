package com.l7tech.external.assertions.symmetrickeyencryptiondecryption.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.symmetrickeyencryptiondecryption.SymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.EventListener;

public class SymmetricKeyEncryptionDecryptionAssertionDialog extends AssertionPropertiesOkCancelSupport<SymmetricKeyEncryptionDecryptionAssertion> {

    private JPanel contentPane;
    private JRadioButton encryptRadioButton;
    private JRadioButton decryptRadioButton;
    private JTextField textField;
    private JTextField keyTextField;
    private JTextField variableNametextField;
    private JComboBox<String> algorithmComboBox;
    private JTextField pgpPassPhrase;
    private JTextField ivTextField;
    private JComboBox<String> pgpEncryptionComboBox;
    private JCheckBox asciiArmourCheckBox;


    public SymmetricKeyEncryptionDecryptionAssertionDialog(Window owner, SymmetricKeyEncryptionDecryptionAssertion assertion) {
        super(SymmetricKeyEncryptionDecryptionAssertion.class, owner, "Symmetric Key Encryption / Decryption Assertion", true);
        initComponents();
    }

    @Override
    public void setData(SymmetricKeyEncryptionDecryptionAssertion assertion) {
        encryptRadioButton.setSelected(assertion.getIsEncrypt());
        decryptRadioButton.setSelected(!(assertion.getIsEncrypt()));

        textField.setText(assertion.getText());
        keyTextField.setText(assertion.getKey());
        ivTextField.setText(assertion.getIv());
        variableNametextField.setText(assertion.getOutputVariableName());
        pgpPassPhrase.setText(assertion.getPgpPassPhrase());

        asciiArmourCheckBox.setSelected(assertion.isAsciiArmourEnabled());

        loadAlgorithmComboBox(assertion.getAlgorithm());
        loadPgpEncryptionTypeComboBox(assertion.getIsPgpKeyEncryption());
    }

    @Override
    public SymmetricKeyEncryptionDecryptionAssertion getData(SymmetricKeyEncryptionDecryptionAssertion assertion) throws ValidationException {

        assertion.setIsEncrypt(encryptRadioButton.isSelected() && !decryptRadioButton.isSelected());
        assertion.setText(textField.getText());
        assertion.setKey(keyTextField.getText());
        assertion.setIv(ivTextField.getText());
        assertion.setAlgorithm(algorithmComboBox.getSelectedItem().toString());
        assertion.setOutputVariableName(variableNametextField.getText());
        assertion.setPgpPassPhrase(pgpPassPhrase.getText());
        assertion.setIsPgpKeyEncryption(this.isPgpPublicKeyEncryptionSelected());
        assertion.setAsciiArmourEnabled(this.asciiArmourCheckBox.isSelected());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.contentPane;
    }

    @Override
    // Have to override this field to ensure the "OK" button is enabled/disabled propertly when the dialog is first opened
    protected void updateOkButtonEnableState() {
        // call local enable or disable button method.  Also check for Key Text Field
        enableOrDisableOkButton(true);
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        /* ---------------- Ok button enablement ---------*/
        // Action Listener called everytime any of the input fields change
        final EventListener genericChangeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton(true);
                enableOrDisableProperTextBoxesForPGP();
            }
        });

        // attaching the Event Listener to each field
        keyTextField.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        ivTextField.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        pgpPassPhrase.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        textField.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        variableNametextField.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        algorithmComboBox.addItemListener((ItemListener) genericChangeListener);
        encryptRadioButton.addItemListener((ItemListener) genericChangeListener);
        decryptRadioButton.addItemListener((ItemListener) genericChangeListener);
        pgpEncryptionComboBox.addItemListener((ItemListener) genericChangeListener);

        // initially set the okay button too
        enableOrDisableOkButton(false);
    }

    //proper PGP setup
    /*                                                    ;
    1) if Encrypt, key disables, Passphrase enabled.
    2) If Decrypt, passphrase enabled and key enabled.
     -> If key empty, a key is adhocly created.
     */
    private void enableOrDisableProperTextBoxesForPGP()
    {
        if (SymmetricKeyEncryptionDecryptionAssertion.TRANS_PGP.equals(algorithmComboBox.getSelectedItem()))
        {
            this.ivTextField.setEnabled(false);
            if (encryptRadioButton.isSelected())
            {
                this.pgpEncryptionComboBox.setEnabled(true); //enable combobox
                if (isPgpPublicKeyEncryptionSelected()){
                    this.pgpPassPhrase.setEnabled(false);
                    this.keyTextField.setEnabled(true);
                    this.asciiArmourCheckBox.setEnabled(true);
                } else {
                    this.pgpPassPhrase.setEnabled(true);
                    this.keyTextField.setEnabled(false);
                    this.asciiArmourCheckBox.setEnabled(false);
                }
            }
            else
            {
                this.pgpEncryptionComboBox.setEnabled(false);
                this.pgpPassPhrase.setEnabled(true);
                this.keyTextField.setEnabled(true);
                this.asciiArmourCheckBox.setEnabled(false);
            }
        }
        else
        {
            this.pgpEncryptionComboBox.setEnabled(false);
            this.pgpPassPhrase.setEnabled(false);
            this.asciiArmourCheckBox.setEnabled(false);
            this.keyTextField.setEnabled(true);
            boolean cbcSelected = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_CBC_PKCS5Padding.equals(algorithmComboBox.getSelectedItem());
            boolean gcmSelected = SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NoPadding.equals(algorithmComboBox.getSelectedItem());
            // IV Is only Used for Decrypt, not Encrypt.
            this.ivTextField.setEnabled(decryptRadioButton.isSelected() && (cbcSelected || gcmSelected));
        }

    }

    private void enableOrDisableOkButton(boolean checkForKey) {
        boolean enabled =
                isNonEmptyRequiredTextField(textField.getText()) &&
                        //isNonEmptyRequiredTextField(keyTextField.getText()) &&
                        isNonEmptyRequiredTextField(variableNametextField.getText()) &&
                        (algorithmComboBox.getSelectedItem() != null && !algorithmComboBox.getSelectedItem().toString().trim().isEmpty());

        if (SymmetricKeyEncryptionDecryptionAssertion.TRANS_PGP.equals(algorithmComboBox.getSelectedItem()))
        {
            if (isPgpPublicKeyEncryptionSelected()){
                enabled = enabled && isNonEmptyRequiredTextField(keyTextField.getText());
            } else {
                enabled = enabled && isNonEmptyRequiredTextField(pgpPassPhrase.getText());
            }
        }
        else
        {
            enabled = enabled && isNonEmptyRequiredTextField(keyTextField.getText());
        }

        this.getOkButton().setEnabled(enabled);
    }

    private void loadAlgorithmComboBox(String selectedAlgorithmDisplay) {

        this.algorithmComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_CBC_PKCS5Padding);
        this.algorithmComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.TRANS_AES_GCM_NoPadding);
        this.algorithmComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.TRANS_DES_CBC_PKCS5Padding);
        this.algorithmComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.TRANS_DESede_CBC_PKCS5Padding);
        this.algorithmComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.TRANS_PGP);

        if (selectedAlgorithmDisplay != null && !selectedAlgorithmDisplay.equals("")) {
            this.algorithmComboBox.setSelectedItem(selectedAlgorithmDisplay);
        }
    }

    private void loadPgpEncryptionTypeComboBox(boolean isPgpPublicKeySelected){
        this.pgpEncryptionComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.PGP_PASS_ENCRYPT);
        this.pgpEncryptionComboBox.addItem(SymmetricKeyEncryptionDecryptionAssertion.PGP_PUBLIC_KEY_ENCRYPT);

        if(isPgpPublicKeySelected){
            this.pgpEncryptionComboBox.setSelectedItem(SymmetricKeyEncryptionDecryptionAssertion.PGP_PUBLIC_KEY_ENCRYPT);
        } else {
            this.pgpEncryptionComboBox.setSelectedItem(SymmetricKeyEncryptionDecryptionAssertion.PGP_PASS_ENCRYPT);
        }

    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean isPgpPublicKeyEncryptionSelected(){
        return encryptRadioButton.isSelected() && SymmetricKeyEncryptionDecryptionAssertion.PGP_PUBLIC_KEY_ENCRYPT.equals(pgpEncryptionComboBox.getSelectedItem());
    }

}
