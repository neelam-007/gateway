/**
 * Copyright (C) 2007 Layer Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.Set;

/**
 * Dialog to configure a single JMS message property in the {@link JmsRoutingAssertionDialog}.
 *
 * @since SecureSpan 4.0
 * @author rmak
 */
public class JmsMessagePropertyDialog extends JDialog {
    private JPanel _contentPane;
    private JTextField _propertyNameTextField;
    private JRadioButton _passThruRadioButton;
    private JRadioButton _customizeRadioButton;
    private JTextField _customPatternTextField;
    private JButton _okButton;
    private JButton _cancelButton;

    private final Set<String> _existingNames;
    private JmsMessagePropertyRule _rule;
    private boolean _exitedWithOK = false;
    private JmsAdmin jmsAdmin;

    /**
     * @param owner the non-null parent dialog from which this dialog is displayed
     * @param existingNames     names of existing properties; used for checking duplicates
     * @param rule  supply a non-null object for editing an existing property
     *              which will be modified upon exit through OK button;
     *              or null if adding a new property
     */
    public JmsMessagePropertyDialog(final JDialog owner, final Set<String> existingNames, final JmsMessagePropertyRule rule) {
        super(owner, true);
        jmsAdmin = Registry.getDefault().getJmsManager();
        _existingNames = existingNames;
        _rule = rule;
        if (_rule != null) {
            // Editing an existing property. Its name is not considered duplicate.
            _existingNames.remove(_rule.getName());
        }
        initComponents();
        initFormData();
    }

    /**
     * @return true if the dialog was exited via the Cancel button.
     */
    public boolean isCanceled() {
        return !_exitedWithOK;
    }

    /**
     * @return true if the dialog was exited via the OK button.
     */
    public boolean isOKed() {
        return _exitedWithOK;
    }

    /**
     * Call this method to obtain the result after exit through OK button if a
     * null rule object was passed in. Otherwise, there is no need to call this
     * because the rule object is modified.
     *
     * @return the resulting rule
     */
    public JmsMessagePropertyRule getData() {
        return _rule;
    }

    private void initComponents() {
        setTitle("JMS Message Property Setting");
        setContentPane(_contentPane);
        setModal(true);
        getRootPane().setDefaultButton(_okButton);

        _propertyNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        final ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };
        _passThruRadioButton.addActionListener(l);
        _customizeRadioButton.addActionListener(l);

        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        _contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void initFormData() {
        if (_rule == null) {
            _passThruRadioButton.doClick();
        } else {
            _propertyNameTextField.setText(_rule.getName());
            if (_rule.isPassThru()) {
                _passThruRadioButton.doClick();
            } else {
                _customizeRadioButton.doClick();
            }
            _customPatternTextField.setText(_rule.getCustomPattern());
        }
    }

    private void onOK() {
        final String name = _propertyNameTextField.getText().trim();
        final boolean passThru = _passThruRadioButton.isSelected();
        String customPattern = null;
        if (_customizeRadioButton.isSelected()) {
            customPattern = _customPatternTextField.getText();
        }

        if (_existingNames != null) {
            if (_existingNames.contains(name)) {
                JOptionPane.showMessageDialog(this,
                                              "Duplicate property name: " + name,
                                              "JMS Message Property Error",
                                              JOptionPane.ERROR_MESSAGE);
                return;     // Don't exit the dialog. Let the user fix the error.
            }
        }

        if (JmsMsgPropertiesPanel.PASS_THROUGH.equals(name)) {
            JOptionPane.showMessageDialog(this,
                                          "Reserved name not allowed.",
                                          "JMS Message Property Error",
                                          JOptionPane.ERROR_MESSAGE);
            return;     // Don't exit the dialog. Let the user fix the error.
        }

        JmsMessagePropertyRule rule = new JmsMessagePropertyRule(name, passThru, customPattern);
        if (!jmsAdmin.isValidProperty(rule)) {
            JOptionPane.showMessageDialog(this,
                    "Invalid JMS Defined Property Data Type.",
                    "JMS Message Property Error",
                    JOptionPane.ERROR_MESSAGE);
            return;     // Don't exit the dialog. Let the user fix the error.
        }

        // All OK.
        if (_rule == null) {
            _rule = new JmsMessagePropertyRule(name, passThru, customPattern);
        } else {
            _rule.setName(name);
            _rule.setPassThru(passThru);
            _rule.setCustomPattern(customPattern);
        }
        _exitedWithOK = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void enableOrDisableComponents() {
        _customPatternTextField.setEnabled(_customizeRadioButton.isSelected());
        _okButton.setEnabled(_propertyNameTextField.getText().length() != 0);
    }
}
