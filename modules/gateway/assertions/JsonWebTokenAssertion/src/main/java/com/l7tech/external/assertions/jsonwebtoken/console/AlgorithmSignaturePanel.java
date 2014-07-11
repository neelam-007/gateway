package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TreeSet;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;

/**
 * User: rseminoff
 * Date: 28/11/12
 */
public class AlgorithmSignaturePanel extends JPanel {
    private JRadioButton signatureListRadioButton;
    private JRadioButton signatureVariableRadioButton;
    private JComboBox signatureListDrop;
    private TargetVariablePanel signatureVariableField;
    private JPanel sigPanel;
    private JwtEncodePanel parent;  // Kludgy, but required for events to affect items in another panel.
    private Assertion myAssertion;
    private int signatureSelection;


    public AlgorithmSignaturePanel() {
        super();
        initComponents();

        signatureListRadioButton.setSelected(true);
        signatureVariableRadioButton.setSelected(false);
        variableSelected(false);
        listSelected(false);
        signatureListDrop.setSelectedIndex(0);         // Defaults to None if there is no other selection made.
        signatureVariableField.setVariable("");
        signatureVariableField.setValueWillBeRead(true);
        signatureVariableField.setValueWillBeWritten(false);

        signatureSelection = SELECTED_SIGNATURE_NONE;

    }

    public void initComponents() {

        signatureListRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // From List Radio Button was selected.
                listSelected(true);
                variableSelected(false);
                updateParentPanel();
            }
        });

        signatureListDrop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateParentPanel();
            }
        });

        signatureVariableRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // From Variable Radio Button was selected.
                variableSelected(true);
                listSelected(false);
                updateParentPanel();
            }
        });

        // Fill the List Combo drop down with the available signatures.
        // Gets the list from the signature registry.
        TreeSet<JsonWebSignature> supportedSignatures = JsonWebSignature.getRegisteredSignatureTypes();

        for (JsonWebSignature currentSig : supportedSignatures) {
            signatureListDrop.addItem(currentSig);
        }

    }

    protected void setPanelWithValue(String value, int selection) {
        if (value != null) {
            value = value.trim();
            signatureSelection = selection;
            switch (selection) {
                case SELECTED_SIGNATURE_LIST: {
                    listSelected(true);
                    try {
                        signatureListDrop.setSelectedItem(JsonWebSignature.getAlgorithm(value));
                    } catch (IllegalJwtSignatureException e) {
                        signatureListDrop.setSelectedItem(0);
                    }
                    variableSelected(false);
                    updateParentPanel();  // This updates the panel based on the zero selection.
                    return;
                }
                case SELECTED_SIGNATURE_VARIABLE: {
                    variableSelected(true);
                    signatureVariableField.setVariable(value);
                    listSelected(false);
                    updateParentPanel();  // This updates the panel based on the zero selection.
                    return;
                }
                default: {
                } // Includes SELECTED_SIGNATURE_NONE .. falls through.
            }
        }

        signatureListDrop.setSelectedItem(0);  // Set to None...we know it's always first now.
        signatureVariableField.setVariable("");
        listSelected(true); // We set this by default
        variableSelected(false);
        updateParentPanel();
    }

    protected String getValueFromPanel() {
        // Which radio button is selected?
        if (signatureListRadioButton.isSelected() && signatureListRadioButton.isEnabled()) {
            signatureSelection = SELECTED_SIGNATURE_LIST;
            JsonWebSignature selectedSignature = (JsonWebSignature) signatureListDrop.getSelectedItem();
            if (selectedSignature != null) {
                String signatureName = selectedSignature.getAlgorithmName();
                return signatureName.trim();
            }
            return "";
        }

        if (signatureVariableRadioButton.isSelected() && signatureVariableRadioButton.isEnabled()) {
            signatureSelection = SELECTED_SIGNATURE_VARIABLE;
            String returnVar = signatureVariableField.getVariable();
            if (returnVar.startsWith(Syntax.SYNTAX_PREFIX)) {
                String[] names = Syntax.getReferencedNames(signatureVariableField.getVariable().trim());
                if (names.length >= 1) returnVar = names[0];    // Only the first variable is used.
            }
            return returnVar;
        }

        signatureSelection = SELECTED_SIGNATURE_NONE;
        return "";
    }

    protected int getSignatureSelection() {
        return this.signatureSelection;
    }

    protected boolean isVariableSelected() {
        // Passes back whether the variable radio is selected
        return signatureVariableRadioButton.isSelected();
    }

    private void listSelected(boolean enable) {
        signatureListRadioButton.setSelected(enable);    // For building the panel the first time with existing selection.
        signatureListDrop.setEnabled(enable);
    }

    private void variableSelected(boolean enable) {
        signatureVariableRadioButton.setSelected(enable);
        signatureVariableField.setEnabled(enable);
    }

    protected void setParent(JwtEncodePanel panel) {
        parent = panel;
    }

    private void updateParentPanel() {
        if (parent != null) {
            parent.updatePanels();
        }
    }

    public void setAssertion(Assertion assertion, Assertion previousAssertion) {
        this.myAssertion = assertion;

        if (assertion != null) {
            signatureVariableField.setAssertion(this.myAssertion, previousAssertion);
        }
    }

    public Assertion getAssertion() {
        return myAssertion;
    }

    public void enableSignatureSelections(boolean enableSelections) {

        signatureListRadioButton.setEnabled(enableSelections);
        signatureVariableRadioButton.setEnabled(enableSelections);

        if (enableSelections) {
            if (isVariableSelected()) {
                this.variableSelected(true);
                this.listSelected(false);
            } else {
                this.listSelected(true);
                this.variableSelected(false);
            }
        } else {
            this.variableSelected(false);
            this.listSelected(false);
        }
    }




}
