/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.policy.assertion.CodeInjectionProtectionAssertion;
import com.l7tech.policy.assertion.CodeInjectionProtectionType;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Properties dialog for the Code Injection Protection Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 * @see com.l7tech.policy.assertion.CodeInjectionProtectionAssertion
 */
public class CodeInjectionProtectionAssertionDialog extends AssertionPropertiesEditorSupport<CodeInjectionProtectionAssertion> {
    private JPanel _contentPane;
    
    private JCheckBox _requestUrlCheckBox;
    private JCheckBox _requestBodyCheckBox;
    private JRadioButton _requestRadioButton;
    private JRadioButton _responseRadioButton;
    private JRadioButton _contextVariableRadioButton;
    private JTextField _contextVarName;

    private JPanel _protectionsPanel;
    private List<JCheckBox> _protectionCheckBoxes = new ArrayList<JCheckBox>();
    private JTextArea _descriptionText;
    private JButton _okButton;
    private JButton _cancelButton;

    private CodeInjectionProtectionAssertion _assertion;
    private boolean _confirmed;

    public CodeInjectionProtectionAssertionDialog(final Window owner, final CodeInjectionProtectionAssertion assertion) throws HeadlessException {
        super(owner, assertion);
        _assertion = assertion;

        _requestRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                _requestUrlCheckBox.setEnabled(_requestRadioButton.isSelected());
                _requestBodyCheckBox.setEnabled(_requestRadioButton.isSelected());
                enableProtectionRadioButtons();
                enableOkButton();
            }
        });
        _requestRadioButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scan request message URL or body. Effective only if this assertion is placed before routing assertion.");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        _requestUrlCheckBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scan parameter values in URL query string.");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        _requestBodyCheckBox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scan (1) parameter values if Form POST, or (2) attribute values and character content if XML, or (3) entire MIME body otherwise.");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        _requestUrlCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        _requestBodyCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOkButton();
            }
        });

        _responseRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableProtectionRadioButtons();
                enableOkButton();
            }
        });
        _responseRadioButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scan response message body. Applies only if this assertion is placed after routing assertion. Use this only if the response is not supposed to contain keywords being screened for.");
                }

                @Override                
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        _contextVariableRadioButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                _contextVarName.setEnabled(_contextVariableRadioButton.isSelected());
                enableProtectionRadioButtons();
                enableOkButton();
            }
        });
        _contextVariableRadioButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    _descriptionText.setText("Scan the contents of the selected context variable.");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });

        // Adds check boxes for protection types available.
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        final List<CodeInjectionProtectionType> protectionsToApply = Arrays.asList(_assertion.getProtections());
        for (CodeInjectionProtectionType protection : CodeInjectionProtectionType.values()) {
            final JCheckBox checkbox = new JCheckBox(protection.getDisplayName());
            checkbox.setActionCommand(protection.getWspName());
            checkbox.setSelected(protectionsToApply.contains(protection));
            checkbox.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    final String action = ((JCheckBox) e.getComponent()).getActionCommand();
                    final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(action);
                    _descriptionText.setText(protection.getDescription());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    _descriptionText.setText("");
                }
            });
            checkbox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    enableOkButton();
                }
            });
            _protectionsPanel.add(checkbox, c);
            _protectionCheckBoxes.add(checkbox);
        }

        _descriptionText.setMargin(new Insets(0, 10, 0, 10));

        _okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        _cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        _requestRadioButton.setSelected(TargetMessageType.REQUEST == _assertion.getTarget());
        _requestUrlCheckBox.setSelected(_assertion.isIncludeRequestUrl());
        _requestBodyCheckBox.setSelected(_assertion.isIncludeRequestBody());
        _responseRadioButton.setSelected(TargetMessageType.RESPONSE == _assertion.getTarget());
        _contextVariableRadioButton.setSelected(TargetMessageType.OTHER == _assertion.getTarget());
        _contextVarName.setText(_assertion.getOtherTargetMessageVariable());
        _contextVarName.setEnabled(_contextVariableRadioButton.isSelected());
        enableOkButton();

        setContentPane(_contentPane);
        getRootPane().setDefaultButton(_okButton);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
    }

    private void onOK() {
        _assertion.setTarget( _requestRadioButton.isSelected() ? TargetMessageType.REQUEST :
                              _responseRadioButton.isSelected() ? TargetMessageType.RESPONSE : TargetMessageType.OTHER);
        if ( _contextVariableRadioButton.isSelected() ) {
            _assertion.setOtherTargetMessageVariable(VariablePrefixUtil.fixVariableName(_contextVarName.getText()));
        } else {
            _assertion.setOtherTargetMessageVariable(null);            
        }
        _assertion.setIncludeRequestUrl(_requestUrlCheckBox.isSelected());
        _assertion.setIncludeRequestBody(_requestBodyCheckBox.isSelected());

        final List<CodeInjectionProtectionType> protectionsToApply = new ArrayList<CodeInjectionProtectionType>();
        for (JCheckBox checkBox : _protectionCheckBoxes) {
            if (checkBox.isEnabled() && checkBox.isSelected()) {
                final String name = checkBox.getActionCommand();
                final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(name);
                protectionsToApply.add(protection);
            }
        }
        _assertion.setProtections(protectionsToApply.toArray(new CodeInjectionProtectionType[protectionsToApply.size()]));

        _confirmed = true;
        dispose();
    }

    private void onCancel() {
        _confirmed = false;
        dispose();
    }

    private void enableProtectionRadioButtons() {
        for (JCheckBox checkBox : _protectionCheckBoxes) {
            final String action = checkBox.getActionCommand();
            final CodeInjectionProtectionType protection = CodeInjectionProtectionType.fromWspName(action);
            checkBox.setEnabled( (_requestRadioButton.isSelected()  && protection.isApplicableToRequest()) ||
                                 (_responseRadioButton.isSelected() && protection.isApplicableToResponse()) ||
                                 _contextVariableRadioButton.isSelected());
        }
    }

    /**
     * Enable/disable the OK button if all settings are OK.
     */
    private void enableOkButton() {
        boolean ok = false;

        // Ensures at least one protection type has been selected.
        for (JCheckBox checkBox : _protectionCheckBoxes) {
            if (checkBox.isEnabled() && checkBox.isSelected()) {
                ok = true;
                break;
            }
        }

        // If applying to request messages, further ensures either URL or body is selected.
        if (_requestRadioButton.isSelected()) {
            ok &= _requestUrlCheckBox.isSelected() || _requestBodyCheckBox.isSelected();
        }

        _okButton.setEnabled(ok && !isReadOnly());
    }

    @Override
    public boolean isConfirmed() {
        return _confirmed;
    }

    @Override
    public void setData(CodeInjectionProtectionAssertion assertion) {
        _assertion = assertion;
    }

    @Override
    public CodeInjectionProtectionAssertion getData(CodeInjectionProtectionAssertion assertion) {
        return _assertion;
    }
}
