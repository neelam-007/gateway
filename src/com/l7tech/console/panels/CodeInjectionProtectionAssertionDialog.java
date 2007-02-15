/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

/**
 * Properties dialog for the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 * @see com.l7tech.policy.assertion.CodeInjectionProtectionAssertion
 */
public class CodeInjectionProtectionAssertionDialog extends JDialog {
    private JPanel _contentPane;
    private JCheckBox _requestUrlCheckBox;
    private JCheckBox _requestBodyCheckBox;
    private JRadioButton _requestRadioButton;
    private JRadioButton _responseRadioButton;
    private JPanel _protectionsPanel;
    private ButtonGroup _protectionButtons = new ButtonGroup();
    private JTextArea _descriptionText;
    private JButton _okButton;
    private JButton _cancelButton;

    private final CodeInjectionProtectionAssertion _assertion;
    private boolean _modified;

    public CodeInjectionProtectionAssertionDialog(Frame owner, final CodeInjectionProtectionAssertion assertion) throws HeadlessException {
        super(owner, "Code Injection Protection", true);
        _assertion = assertion;

        _requestRadioButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _requestUrlCheckBox.setEnabled(_requestRadioButton.isSelected());
                _requestBodyCheckBox.setEnabled(_requestRadioButton.isSelected());
                enableOkButton();
            }
        });
        _requestRadioButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scans request message URL or body. Effective only if this assertion is placed before routing assertion.");
                }

                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        _requestUrlCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        _requestBodyCheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        _responseRadioButton.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                for (Enumeration<AbstractButton> i = _protectionButtons.getElements(); i.hasMoreElements();) {
                    final JRadioButton button = (JRadioButton) i.nextElement();
                    final String name = button.getText();
                    final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromDisplayName(name);
                    button.setEnabled(!_responseRadioButton.isSelected() || protection.isApplicableToResponse());
                }
                enableOkButton();
            }
        });
        _responseRadioButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scans response message body. Applies only if this assertion is placed after routing assertion. Use this only if the response is not supposed to contain JavaScript.");
                }

                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        // Adds radio buttons for protection types available.
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        for (CodeInjectionProtectionType protection : CodeInjectionProtectionType.values()) {
            final JRadioButton radioButton = new JRadioButton(protection.getDisplayName());
            radioButton.setActionCommand(protection.getDisplayName());
            radioButton.setSelected(protection == _assertion.getProtection());
            radioButton.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    final String name = ((JRadioButton) e.getComponent()).getText();
                    final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromDisplayName(name);
                    _descriptionText.setText(protection.getDescription());
                }

                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });
            _protectionsPanel.add(radioButton, c);
            _protectionButtons.add(radioButton);
        }

        // Selects the first radio button as default if none selected yet.
        if (_protectionButtons.getSelection() == null) {
            _protectionButtons.getElements().nextElement().setSelected(true);
        }

        _descriptionText.setMargin(new Insets(0, 10, 0, 10));

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

        _requestRadioButton.setSelected(_assertion.isIncludeRequestUrl() || _assertion.isIncludeRequestBody());
        _requestUrlCheckBox.setSelected(_assertion.isIncludeRequestUrl());
        _requestBodyCheckBox.setSelected(_assertion.isIncludeRequestBody());
        _responseRadioButton.setSelected(_assertion.isIncludeResponseBody());
        enableOkButton();

        setContentPane(_contentPane);
        getRootPane().setDefaultButton(_okButton);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void onOK() {
        _assertion.setIncludeRequestUrl(_requestRadioButton.isSelected() && _requestUrlCheckBox.isSelected());
        _assertion.setIncludeRequestBody(_requestRadioButton.isSelected() && _requestBodyCheckBox.isSelected());
        _assertion.setIncludeResponseBody(_responseRadioButton.isSelected());

        final String name = _protectionButtons.getSelection().getActionCommand();
        final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(name);
        _assertion.setProtection(protection);
        
        _modified = true;
        dispose();
    }

    private void onCancel() {
        _modified = false;
        dispose();
    }

    /**
     * Enable/disable the OK button if all settings are OK.
     */
    private void enableOkButton() {
        boolean ok = false;
        if (_requestRadioButton.isSelected()) {
            ok = _requestUrlCheckBox.isSelected() || _requestBodyCheckBox.isSelected();
        } else { // _responseRadioButton.isSelected()
            for (Enumeration<AbstractButton> i = _protectionButtons.getElements(); i.hasMoreElements();) {
                final JRadioButton button = (JRadioButton) i.nextElement();
                if (button.isEnabled() && button.isSelected()) {
                    ok = true;
                    break;
                }
            }
        }
        _okButton.setEnabled(ok);
    }

    public boolean isAssertionModified() {
        return _modified;
    }
}
