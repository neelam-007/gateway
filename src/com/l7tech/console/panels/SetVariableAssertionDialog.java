/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.TextUtils;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.LineBreak;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialog for {@link com.l7tech.policy.assertion.SetVariableAssertion}.
 *
 * <p>Related function specifications:
 * <ul>
 *  <li><a href="http://sarek.l7tech.com/mediawiki/index.php?title=XML_Variables">XML Variables</a> (4.3)
 * </ul>
 */
public class SetVariableAssertionDialog extends JDialog {

    private static class DataTypeComboBoxItem {
        private final DataType _dataType;
        public DataTypeComboBoxItem(DataType dataType) { _dataType = dataType; }
        public DataType getDataType() { return _dataType; }
        public String toString() { return _dataType.getName(); }
    }

    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    private JPanel _mainPanel;
    private JTextField _variableNameTextField;
    private JLabel _variableNameStatusLabel;
    private JComboBox _dataTypeComboBox;
    private JTextField _contentTypeTextField;
    private JLabel _contentTypeStatusLabel;
    private JTextArea _expressionTextArea;
    private JRadioButton _crlfRadioButton;
    private JRadioButton _crRadioButton;
    private JRadioButton _lfRadioButton;
    private JLabel _expressionStatusLabel;
    private JTextArea _expressionStatusTextArea;
    private JScrollPane _expressionStatusScrollPane;
    private JButton _cancelButton;
    private JButton _okButton;

    private boolean _assertionModified;
    private final Set<String> _predecessorVariables = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    private Border _expressionStatusBorder;

    public SetVariableAssertionDialog(Frame owner, final SetVariableAssertion assertion) throws HeadlessException {
        this(owner, assertion, null);
    }

    public SetVariableAssertionDialog(Frame owner, final SetVariableAssertion assertion, final Assertion contextAssertion) throws HeadlessException {
        super(owner, "Set Variable", true);

        _expressionStatusBorder = _expressionStatusScrollPane.getBorder();
        clearVariableNameStatus();
        clearContentTypeStatus();
        clearExpressionStatus();

        _predecessorVariables.addAll(
                contextAssertion==null ?
                PolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet() :
                PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(contextAssertion).keySet());

        // Populates data type combo box with supported data types.
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.STRING));
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.MESSAGE));
        _dataTypeComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _contentTypeTextField.setEnabled(getSelectedDataType() == DataType.MESSAGE);
                validateFields();
            }
        });

        TextComponentPauseListenerManager.registerPauseListener(
                _variableNameTextField,
                new PauseListener() {
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    public void textEntryResumed(JTextComponent component) {
                        clearVariableNameStatus();
                    }
                },
                500);
        TextComponentPauseListenerManager.registerPauseListener(
                _contentTypeTextField,
                new PauseListener() {
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    public void textEntryResumed(JTextComponent component) {
                        clearContentTypeStatus();
                    }
                },
                500);
        TextComponentPauseListenerManager.registerPauseListener(
                _expressionTextArea,
                new PauseListener() {
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    public void textEntryResumed(JTextComponent component) {
                        clearExpressionStatus();
                    }
                },
                500);

        _cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _assertionModified = false;
                dispose();
            }
        });

        _okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assertion.setVariableToSet(_variableNameTextField.getText());

                final DataType dataType = getSelectedDataType();
                assertion.setDataType(dataType);

                if (dataType == DataType.MESSAGE) {
                    assertion.setContentType(_contentTypeTextField.getText());
                } else {
                    assertion.setContentType(null);
                }

                LineBreak lineBreak = null;
                if (_crlfRadioButton.isSelected()) {
                    lineBreak = LineBreak.CRLF;
                } else if (_lfRadioButton.isSelected()) {
                    lineBreak = LineBreak.LF;
                } else if (_crRadioButton.isSelected()) {
                    lineBreak = LineBreak.CR;
                }
                assertion.setLineBreak(lineBreak);

                final String expression = TextUtils.convertLineBreaks(_expressionTextArea.getText(), lineBreak.getCharacters());
                    // Conversion necessary? Can a CR be pasted into a JTextArea and returned by getText()?
                assertion.setExpression(expression);

                _assertionModified = true;
                dispose();
            }
        });

        //
        // Sets dialog to assertion data.
        //

        _variableNameTextField.setText(assertion.getVariableToSet());
        selectDataType(assertion.getDataType());
        _contentTypeTextField.setText(assertion.getContentType());

        // JTextArea likes all line break to be LF.
        final String expression = TextUtils.convertLineBreaks(assertion.expression(), "\n");
        _expressionTextArea.setText(expression);

        if (assertion.getLineBreak() == LineBreak.LF) {
            _lfRadioButton.doClick();
        } else if (assertion.getLineBreak() == LineBreak.CR) {
            _crRadioButton.doClick();
        } else {
             // Defaults to CR-LF.
            _crlfRadioButton.doClick();
        }

        validateFields();

        add(_mainPanel);
    }

    /**
     * Sets which data type is selected in {@link #_dataTypeComboBox}.
     * @param dataType  the data type to select
     * @throws RuntimeException if <code>dataType</code> is not avaiable in the combo box
     */
    private void selectDataType(final DataType dataType) {
        if (dataType == null) return;

        for (int i = 0; i < _dataTypeComboBox.getItemCount(); ++i) {
            if (((DataTypeComboBoxItem) _dataTypeComboBox.getItemAt(i)).getDataType() == dataType) {
                _dataTypeComboBox.setSelectedIndex(i);
                return;
            }
        }

        throw new RuntimeException("Data type \"" + dataType.getName() + "\" not available in combobox.");
    }

    /**
     * @return the data type currently selected in the data type combobox
     */
    private DataType getSelectedDataType() {
        return ((DataTypeComboBoxItem) _dataTypeComboBox.getSelectedItem()).getDataType();
    }

    private void clearVariableNameStatus() {
        _variableNameStatusLabel.setIcon(BLANK_ICON);
        _variableNameStatusLabel.setText(null);
    }

    private void clearContentTypeStatus() {
        _contentTypeStatusLabel.setIcon(BLANK_ICON);
        _contentTypeStatusLabel.setText(null);
    }

    private void clearExpressionStatus() {
        _expressionStatusLabel.setIcon(BLANK_ICON);
        _expressionStatusTextArea.setText(null);
        _expressionStatusScrollPane.setBorder(null);
    }

    /**
     * Validates values in various fields and sets the status labels as appropriate.
     */
    private void validateFields() {
        final String variableName = _variableNameTextField.getText();
        final String contentType = _contentTypeTextField.getText();
        final String expression = _expressionTextArea.getText();

        boolean ok = true;

        String validateNameResult = null;
        if (variableName.length() == 0) {
            ok = false;
        } else if ((validateNameResult = VariableMetadata.validateName(variableName)) != null) {
            ok = false;
            _variableNameStatusLabel.setIcon(WARNING_ICON);
            _variableNameStatusLabel.setText(validateNameResult);
        } else {
            final VariableMetadata meta = BuiltinVariables.getMetadata(variableName);
            if (meta == null) {
                _variableNameStatusLabel.setIcon(OK_ICON);
                _variableNameStatusLabel.setText("OK");
                _dataTypeComboBox.setEnabled(true);
            } else {
                if (meta.isSettable()) {
                    _variableNameStatusLabel.setIcon(OK_ICON);
                    _variableNameStatusLabel.setText("OK (Built-in, settable)");
                } else {
                    ok = false;
                    _variableNameStatusLabel.setIcon(WARNING_ICON);
                    _variableNameStatusLabel.setText("Built-in, not settable");
                }
                selectDataType(meta.getType());
                _dataTypeComboBox.setEnabled(false);
            }
        }

        if (getSelectedDataType() == DataType.MESSAGE) {
            _contentTypeTextField.setEnabled(true);
            if (contentType.length() == 0) {
                ok = false;
            } else {
                try {
                    ContentTypeHeader.parseValue(_contentTypeTextField.getText());
                    _contentTypeStatusLabel.setIcon(OK_ICON);
                    _contentTypeStatusLabel.setText("OK");
                } catch (IOException e) {
                    ok = false;
                    _contentTypeStatusLabel.setIcon(WARNING_ICON);
                    _contentTypeStatusLabel.setText("Incorrect syntax");
                    _okButton.setEnabled(false);
                }
            }
        } else {
            _contentTypeTextField.setEnabled(false);
            clearContentTypeStatus();
        }

        // Expression. Blank is OK.
        final StringBuilder expressionStatus = new StringBuilder();
        final String[] names = Syntax.getReferencedNames(expression);
        for (String name : names) {
            if (BuiltinVariables.getMetadata(name) == null &&
                Syntax.getMatchingName(name, _predecessorVariables) == null) {
                if (expressionStatus.length() > 0) expressionStatus.append("\n");
                expressionStatus.append(name).append(": No such variable");
            }
        }

        if (expressionStatus.length() == 0) {
            _expressionStatusLabel.setIcon(OK_ICON);
            _expressionStatusTextArea.setText("OK");
            _expressionStatusScrollPane.setBorder(null);
        } else {
            ok = false;
            _expressionStatusLabel.setIcon(WARNING_ICON);
            _expressionStatusTextArea.setText(expressionStatus.toString());
            _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
        }

        _okButton.setEnabled(ok);
    }

    public boolean isAssertionModified() {
        return _assertionModified;
    }
}
