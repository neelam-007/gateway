package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AuditAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * Properties dialog for the Audit detail assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertionPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField detailTextField;
    private JComboBox levelComboBox;
    private JButton cancelButton;
    private JButton okButton;

    public AuditDetailAssertionPropertiesDialog(Frame owner) {
        super(owner, true);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Audit Detail Properties");
        levelComboBox.setModel(new DefaultComboBoxModel(AuditAssertion.ALLOWED_LEVELS));
        //levelComboBox.setSelectedItem(ass.getLevel());

        // todo, event handlers
    }

    public boolean isModified() {
        // todo
        return true;
    }
}
