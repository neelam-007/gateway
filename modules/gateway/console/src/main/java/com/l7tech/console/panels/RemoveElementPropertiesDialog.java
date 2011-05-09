package com.l7tech.console.panels;

import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xml.RemoveElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Assertion property dialog for RemoveElement assertion.
 */
public class RemoveElementPropertiesDialog extends AssertionPropertiesEditorSupport<RemoveElement> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.RemoveElementPropertiesDialog");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel existingElementPanel;
    private TargetVariablePanel existingElementField;
    private JRadioButton rbRemoveElement;
    private JRadioButton rbInsertElement;
    private JComboBox elementLocationCombo;
    private JPanel newElementPanel;
    private TargetVariablePanel newElementField;

    private RemoveElement assertion;
    private boolean confirmed = false;
    private final InputValidator inputValidator;

    public RemoveElementPropertiesDialog( final Window owner, final RemoveElement assertion ) {
        super(owner, assertion);
        inputValidator = new InputValidator(this, resources.getString("validator.title"));
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(buttonOK);

        final RunOnChangeListener enabler = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateEnabledState();
            }
        });
        existingElementField = new TargetVariablePanel();
        Utilities.setSingleChild(existingElementPanel, existingElementField);
        existingElementField.setValueWillBeRead(true);
        existingElementField.setValueWillBeWritten(false);
        existingElementField.addChangeListener(enabler);
        newElementField = new TargetVariablePanel();
        Utilities.setSingleChild(newElementPanel, newElementField);
        newElementField.setValueWillBeRead(true);
        newElementField.setValueWillBeWritten(false);
        newElementField.addChangeListener(enabler);
        rbInsertElement.addActionListener(enabler);
        rbRemoveElement.addActionListener(enabler);

        inputValidator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        inputValidator.addRule(new InputValidator.ComponentValidationRule(existingElementField) {
            @Override
            public String getValidationError() {
                String var = existingElementField.getVariable();
                if (var != null && var.trim().length() > 0)
                    return null;
                return "Existing element variable name must be provided.";
            }
        });

        inputValidator.addRule(new InputValidator.ComponentValidationRule(newElementField) {
            @Override
            public String getValidationError() {
                if (!newElementField.isEnabled())
                    return null;
                String var = newElementField.getVariable();
                if (var != null && var.trim().length() > 0)
                    return null;
                return "A new element variable name must be provided when adding an element.";
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        updateView();
        updateEnabledState();
    }

    private void updateView() {
        RemoveElement.ElementLocation loc = assertion.getInsertedElementLocation();
        boolean inserting = loc != null;
        existingElementField.setAssertion(assertion, getPreviousAssertion());
        existingElementField.setVariable(assertion.getElementFromVariable());
        newElementField.setAssertion(assertion, getPreviousAssertion());
        newElementField.setVariable(assertion.getElementToInsertVariable());
        elementLocationCombo.setModel(new DefaultComboBoxModel(RemoveElement.ElementLocation.values()));
        if (inserting)
            elementLocationCombo.setSelectedItem(loc);
        else
            elementLocationCombo.setSelectedIndex(0);
        rbInsertElement.setSelected(inserting);
        rbRemoveElement.setSelected(!inserting);
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData( final RemoveElement assertion ) {
        this.assertion = assertion;
        updateView();
    }

    @Override
    public RemoveElement getData( final RemoveElement assertion ) {
        return this.assertion;
    }

    private void onOK() {
        confirmed = true;
        assertion.setElementFromVariable(VariablePrefixUtil.fixVariableName(existingElementField.getVariable()));
        assertion.setElementToInsertVariable(VariablePrefixUtil.fixVariableName(newElementField.getVariable()));
        assertion.setInsertedElementLocation(rbInsertElement.isSelected() ? (RemoveElement.ElementLocation)elementLocationCombo.getSelectedItem() : null);
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    private void updateEnabledState() {
        boolean inserting = rbInsertElement.isSelected();
        elementLocationCombo.setEnabled(inserting);
        newElementField.setEnabled(inserting);

        buttonOK.setEnabled( !isReadOnly() );
    }
}
