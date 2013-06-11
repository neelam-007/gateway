package com.l7tech.console.panels;

import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.policy.variable.Syntax;

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
    private JTextField customLoggerTextField;
    private JRadioButton auditRadioButton;
    private JRadioButton logRadioButton;
    private JCheckBox customLoggerNameCheckBox;
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
        detailTextArea.setCaretPosition(0);
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

        final ActionListener enableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableButtons();
                if (customLoggerNameCheckBox == e.getSource() && customLoggerTextField.isEnabled())
                    customLoggerTextField.requestFocusInWindow();
            }
        };
        logRadioButton.setSelected(assertion.isLoggingOnly());
        logRadioButton.addActionListener(enableListener);
        auditRadioButton.setSelected(!logRadioButton.isSelected());
        auditRadioButton.addActionListener(enableListener);

        final String cust = assertion.getCustomLoggerSuffix();
        customLoggerNameCheckBox.setSelected(cust != null);
        customLoggerNameCheckBox.addActionListener(enableListener);
        customLoggerTextField.setDocument(new MaxLengthDocument(128));
        customLoggerTextField.setText(cust == null ? "" : cust);
        customLoggerTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void removeUpdate(DocumentEvent e) { enableButtons(); }
            @Override
            public void changedUpdate(DocumentEvent e) { enableButtons(); }
        });
        
        Utilities.enableGrayOnDisabled(customLoggerTextField);

        enableButtons();

        Utilities.equalizeButtonSizes(okButton, cancelButton);
        Utilities.setEnterAction(this, okAction);
        Utilities.setEscAction(this, cancelAction);
    }

    private void enableButtons() {
        customLoggerTextField.setEnabled(!readOnly && customLoggerNameCheckBox.isSelected());
        okButton.setEnabled(!readOnly && detailTextArea.getText().length() > 0 &&
                (!customLoggerNameCheckBox.isSelected() || customLoggerTextField.getText().trim().length() > 0));
    }

    private void ok() {
        if (!validData())
            return;

        modified = true;
        assertion.setDetail(detailTextArea.getText());
        assertion.setLevel((String)levelComboBox.getSelectedItem());
        assertion.setLoggingOnly(logRadioButton.isSelected());
        assertion.setCustomLoggerSuffix(customLoggerNameCheckBox.isSelected() ? customLoggerTextField.getText() : null);

        dispose();
    }

    private boolean validData() {
        String err = null;
        final boolean contextVariablesUsed = Syntax.getReferencedNames(customLoggerTextField.getText()).length > 0;

        if (customLoggerTextField.isEnabled() && !contextVariablesUsed && !validLoggerName(customLoggerTextField.getText()))
            err = "A custom logger name suffix must consist of letters and numbers separated by periods.  For example, \"example.widgetwatcher\"";

        if (err == null)
            return true;

        DialogDisplayer.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE, null);
        return false;
    }

    private boolean validLoggerName(String name) {
        return !(name == null || name.trim().length() < 1) && name.matches("^[A-Za-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*$");
    }

    private void cancel() {
        modified = false;
        dispose();
    }

    public boolean isModified() {
        return modified;
    }
}