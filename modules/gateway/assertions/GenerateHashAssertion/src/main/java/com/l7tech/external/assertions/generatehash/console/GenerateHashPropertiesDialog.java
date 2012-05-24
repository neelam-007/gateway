package com.l7tech.external.assertions.generatehash.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.generatehash.GenerateHashAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.LineBreak;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import java.awt.*;

/**
 * The dialog to present the configuration of the {@link GenerateHashAssertion} to the user.
 */
public class GenerateHashPropertiesDialog extends AssertionPropertiesOkCancelSupport<GenerateHashAssertion> {

    private JPanel customPane;
    private JTextField keyTextField;
    private JComboBox algorithmComboBox;
    private TargetVariablePanel targetVariablePanel;
    private JTextArea dataToSign;
    private JLabel warningLabel;
    private JRadioButton rbCRLF;
    private JRadioButton rbLF;
    private JRadioButton rbCR;

    /**
     * Construct a new dialog with the given owner.
     * @param owner the dialog's owner.
     */
    public GenerateHashPropertiesDialog(final Window owner) {
        super(GenerateHashAssertion.class, owner, "Generate Security Hash Properties", true);
        initComponents();
    }

    @Override
    public void setData(GenerateHashAssertion assertion) {
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
    public GenerateHashAssertion getData(GenerateHashAssertion assertion) throws ValidationException {
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

        for(String item : GenerateHashAssertion.getSupportedAlgorithm().keySet()){
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
        boolean keyRequired = isKeyFieldValueRequired();
        this.keyTextField.setEnabled(keyRequired);
        String warningText = "";
        if(!keyRequired){
            warningText = "WARNING: the selected algorithm is a weak hashing algorithm.";
        }
        warningLabel.setText(warningText);
    }

    private boolean isKeyFieldValueRequired() {
        boolean toReturn = true;
        // key needs a value if we are using an HMAC algorithm
        if (algorithmComboBox.getSelectedItem() != null && !algorithmComboBox.getSelectedItem().toString().trim().isEmpty()) {
            toReturn = GenerateHashAssertion.HMAC_ALGORITHM.matcher(algorithmComboBox.getSelectedItem().toString()).matches();
        }
        return toReturn;
    }

    private boolean isNonEmptyRequiredTextField(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
