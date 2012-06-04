package com.l7tech.external.assertions.generatesecurityhash.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.generatesecurityhash.GenerateSecurityHashAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.LineBreak;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The dialog to present the configuration of the {@link com.l7tech.external.assertions.generatesecurityhash.GenerateSecurityHashAssertion} to the user.
 */
public class GenerateSecurityHashPropertiesDialog extends AssertionPropertiesOkCancelSupport<GenerateSecurityHashAssertion> {

    private JPanel customPane;
    private JTextField keyTextField;
    private JComboBox algorithmComboBox;
    private TargetVariablePanel targetVariablePanel;
    private JTextArea dataToSign;
    private JRadioButton rbCRLF;
    private JRadioButton rbLF;
    private JRadioButton rbCR;

    private JButton okButton;

    /**
     * Construct a new dialog with the given owner.
     * @param owner the dialog's owner.
     */
    public GenerateSecurityHashPropertiesDialog(final Window owner) {
        super(GenerateSecurityHashAssertion.class, owner, "Generate Security Hash Properties", true);
        initComponents();
    }

    @Override
    public void setData(GenerateSecurityHashAssertion assertion) {
        keyTextField.setText(assertion.getKeyText());
        keyTextField.setCaretPosition(0);
        dataToSign.setText(TextUtils.convertLineBreaks(assertion.dataToSignText(), LineBreak.LF.getCharacters()));
        dataToSign.setCaretPosition(0);
        algorithmComboBox.setSelectedItem(assertion.getAlgorithm());
        targetVariablePanel.setVariable(assertion.getTargetOutputVariable());
        LineBreak lineBreak = assertion.getLineBreak();
        if(lineBreak == LineBreak.CR){
            rbCR.setSelected(true);
        }
        else if(lineBreak == LineBreak.LF){
            rbLF.setSelected(true);
        }
        else {
            //default to CRLF
            rbCRLF.setSelected(true);
        }
    }

    @Override
    public GenerateSecurityHashAssertion getData(GenerateSecurityHashAssertion assertion) throws ValidationException {
        assertion.setAlgorithm(algorithmComboBox.getSelectedItem().toString());
        assertion.setKeyText(keyTextField.isEnabled() ? keyTextField.getText() : null);
        assertion.setTargetOutputVariable(targetVariablePanel.getVariable());

        LineBreak lineBreak = LineBreak.CRLF;
        if(rbCR.isSelected()){
            lineBreak = LineBreak.CR;
        }
        else if(rbLF.isSelected()){
            lineBreak = LineBreak.LF;
        }
        final String text = TextUtils.convertLineBreaks(dataToSign.getText(), lineBreak.getCharacters());
        assertion.setDataToSignText(text.trim());
        assertion.setLineBreak(lineBreak);
        return assertion;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        final RunOnChangeListener genericChangeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                updateOkButtonEnableState();
            }
        });
        keyTextField.getDocument().addDocumentListener(genericChangeListener);
        dataToSign.getDocument().addDocumentListener(genericChangeListener);
        targetVariablePanel.addChangeListener(genericChangeListener);

        final RunOnChangeListener algorithmComboBoxListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisablekeyTextField();
                updateOkButtonEnableState();
            }
        });

        for(String item : GenerateSecurityHashAssertion.getSupportedAlgorithm().keySet()){
            algorithmComboBox.addItem(item);
        }
        algorithmComboBox.addItemListener(algorithmComboBoxListener);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.customPane;
    }

    @Override
    protected void updateOkButtonEnableState() {
        boolean enabled =
                isNonEmptyRequiredTextField(dataToSign.getText()) &&
                        targetVariablePanel.isEntryValid() &&
                        (algorithmComboBox.getSelectedItem() != null && !algorithmComboBox.getSelectedItem().toString().trim().isEmpty());
        if (isKeyFieldValueRequired()) {
            enabled = enabled && isNonEmptyRequiredTextField(keyTextField.getText());
        }
        this.getOkButton().setEnabled(enabled);
    }

    private void enableOrDisablekeyTextField() {
        this.keyTextField.setEnabled(isKeyFieldValueRequired());
    }

    private boolean isKeyFieldValueRequired() {
        boolean toReturn = true;
        // key needs a value if we are using an HMAC algorithm
        if (algorithmComboBox.getSelectedItem() != null && !algorithmComboBox.getSelectedItem().toString().trim().isEmpty()) {
            toReturn = GenerateSecurityHashAssertion.HMAC_ALGORITHM.matcher(algorithmComboBox.getSelectedItem().toString()).matches();
        }
        return toReturn;
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }

    @Override
    protected JButton getOkButton() {
        if (okButton == null){
            okButton = new JButton("OK");
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if(!isKeyFieldValueRequired()){
                        showNonHmacConfirmation();
                    }
                    else {
                        setConfirmed(true);
                        dispose();
                    }
                }
            });
        }
        return okButton;
    }

    private void showNonHmacConfirmation() {
        DialogDisplayer.showSafeConfirmDialog(
                this,
                "<html><center><p>Warning: The selected algorithm is non-HMAC and is not recommended as it is a weak algorithm and is exploitable.</p>" +
                        "<p>Do you wish to continue?</p></center></html>",
                "Confirm Use of non-HMAC Algorithm",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.CANCEL_OPTION) {
                            setConfirmed(false);
                            return;
                        }
                        setConfirmed(true);
                        dispose();
                    }
                }
        );
    }
}
