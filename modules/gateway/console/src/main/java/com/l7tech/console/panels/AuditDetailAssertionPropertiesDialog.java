package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AuditDetailAssertion;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

/**
 * Properties dialog for the Audit detail assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertionPropertiesDialog extends LegacyAssertionPropertyDialog {
    private JPanel mainPanel;
    private JTextArea detailTextArea;
    private JComboBox levelComboBox;
    private JButton cancelButton;
    private JButton okButton;
    private final boolean readOnly;
    private final AuditDetailAssertion assertion;
    private boolean modified;

    private static abstract class ComboAction extends AbstractAction implements ActionListener { }

    private final ComboAction okAction = new ComboAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ok();
        }
    };

    private final ComboAction cancelAction = new ComboAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    public AuditDetailAssertionPropertiesDialog(Frame owner, AuditDetailAssertion assertion, boolean readOnly) {
        super(owner, assertion, true);
        this.readOnly = readOnly;
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        modified = false;
        setContentPane(mainPanel);
        setTitle("Audit Detail Properties");

        String[] levels = {
            Level.INFO.getName(),
            Level.WARNING.getName(),
        };

        levelComboBox.setModel(new DefaultComboBoxModel(levels));
        levelComboBox.setSelectedItem(assertion.getLevel());
        detailTextArea.setText(assertion.getDetail());
        detailTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableButtons(); }
        });

        okButton.addActionListener(okAction);
        cancelButton.addActionListener(cancelAction);

        enableButtons();

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        Utilities.setEnterAction(this, okAction);
        Utilities.setEscAction(this, cancelAction);
    }

    private void enableButtons() {
        okButton.setEnabled(!readOnly && detailTextArea.getText().length() > 0);
    }

    private void ok() {
        modified = true;
        assertion.setDetail(detailTextArea.getText());
        assertion.setLevel((String)levelComboBox.getSelectedItem());
        dispose();
    }

    private void cancel() {
        modified = false;
        dispose();
    }

    public boolean isModified() {
        return modified;
    }
}
