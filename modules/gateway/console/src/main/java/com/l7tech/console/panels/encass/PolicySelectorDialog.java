package com.l7tech.console.panels.encass;

import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.PolicyHeader;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog which allows the user to select a PolicyHeader and indicate if they want to auto-populate Encapsulated Assertion
 * inputs and outputs based on the selected policy.
 */
public class PolicySelectorDialog extends JDialog {
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox policyComboBox;
    private JCheckBox autoPopulateCheckBox;
    private JPanel contentPanel;
    private boolean confirmed = false;
    private InputValidator inputValidator;
    private PolicyHeader selected;
    private boolean autoPopulate;
    private boolean selectionChanged;

    /**
     * @param owner        the Window which owns this Dialog.
     * @param options      the PolicyHeaders to display as selectable options. Must contain at least one PolicyHeader.
     * @param initialValue the optional initial PolicyHeader to show as selected.
     */
    public PolicySelectorDialog(@NotNull Window owner, @NotNull final PolicyHeader[] options, @Nullable final PolicyHeader initialValue) {
        super(owner, "Select Implementation Policy", Dialog.ModalityType.APPLICATION_MODAL);
        Validate.isTrue(options.length > 0, "Must have at least one PolicyHeader option");
        Utilities.setEscKeyStrokeDisposes(this);
        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        policyComboBox.setModel(new DefaultComboBoxModel(options));
        selected = initialValue;
        policyComboBox.setSelectedItem(selected != null ? selected : options[0]);
        cancelButton.addActionListener(Utilities.createDisposeAction(this));
        inputValidator = new InputValidator(this, "Error");
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmed = true;
                selected = (PolicyHeader) policyComboBox.getSelectedItem();
                selectionChanged = initialValue == null || !selected.equals(initialValue);
                autoPopulate = autoPopulateCheckBox.isSelected();
                dispose();
            }
        });
        inputValidator.ensureComboBoxSelection("Policy", policyComboBox);
    }

    /**
     * @return whether the dialog was OKed.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return whether the auto populate inputs and outputs checkbox was checked.
     */
    public boolean isAutoPopulate() {
        return autoPopulate;
    }

    /**
     * @return the selected PolicyHeader. Can be null if dialog as not OKed and there was no initial value.
     */
    @Nullable
    public PolicyHeader getSelected() {
        return selected;
    }

    /**
     * @return whether the selection has changed from the initial value.
     */
    public boolean isSelectionChanged() {
        return selectionChanged;
    }
}
