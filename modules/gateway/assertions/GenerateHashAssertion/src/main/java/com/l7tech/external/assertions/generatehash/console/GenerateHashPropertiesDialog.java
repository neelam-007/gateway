package com.l7tech.external.assertions.generatehash.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.generatehash.GenerateHashAssertion;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.EventListener;

/**
 * The dialog to present the configuration of the {@link GenerateHashAssertion} to the user.
 */
public class GenerateHashPropertiesDialog extends AssertionPropertiesOkCancelSupport<GenerateHashAssertion> {

    private JPanel customPane;
    private JTextField keyTextField;
    private JComboBox algorithmComboBox;
    private TargetVariablePanel targetVariablePanel;
    private JTextArea dataToSign;

    /**
     * Construct a new dialog with the given owner.
     * @param owner the dialog's owner.
     */
    public GenerateHashPropertiesDialog(final Window owner) {
        super(GenerateHashAssertion.class, owner, "Generate Security Hash Assertion", true);
        initComponents();
    }

    @Override
    public void setData(GenerateHashAssertion assertion) {
        keyTextField.setText(assertion.getKeyText());
        dataToSign.setText(assertion.getDataToSignText());
        algorithmComboBox.setSelectedItem(assertion.getAlgorithm());
        targetVariablePanel.setVariable(assertion.getTargetOutputVariable());
    }

    @Override
    public GenerateHashAssertion getData(GenerateHashAssertion assertion) throws ValidationException {
        assertion.setAlgorithm(algorithmComboBox.getSelectedItem().toString());
        assertion.setDataToSignText(dataToSign.getText());
        assertion.setKeyText(keyTextField.getText());
        assertion.setTargetOutputVariable(targetVariablePanel.getVariable());
        return assertion;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        final EventListener genericChangeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton(true);
            }
        });
        keyTextField.getDocument().addDocumentListener((DocumentListener) genericChangeListener);
        dataToSign.getDocument().addDocumentListener((DocumentListener) genericChangeListener);

        enableOrDisableOkButton(false);

        final EventListener algorithmComboBoxListener = new RunOnChangeListener(new Runnable() {
            public void run() {
                enableOrDisableOkButton(true);
                enableOrDisablekeyTextField();
            }
        });
        for(String item : GenerateHashAssertion.getSupportedAlgorithm()){
            algorithmComboBox.addItem(item);
        }
        algorithmComboBox.addItemListener((ItemListener) algorithmComboBoxListener);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.customPane;
    }

    @Override
    protected void updateOkButtonEnableState() {
       enableOrDisableOkButton(true);
    }

    private void enableOrDisablekeyTextField() {
        this.keyTextField.setEnabled(isKeyFieldValueRequired());
    }

    private void enableOrDisableOkButton( boolean checkForKey) {
        boolean enabled =
                isNonEmptyRequiredTextField(dataToSign.getText()) &&
                        targetVariablePanel.isEntryValid() &&
                        (algorithmComboBox.getSelectedItem() != null && !algorithmComboBox.getSelectedItem().toString().trim().isEmpty());
        if (checkForKey && isKeyFieldValueRequired()) {
            enabled = enabled && isNonEmptyRequiredTextField(keyTextField.getText());
        }
        this.getOkButton().setEnabled(enabled);
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
