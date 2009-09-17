/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.LineBreak;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TextUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
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
public class SetVariableAssertionDialog extends LegacyAssertionPropertyDialog {

    private static class DataTypeComboBoxItem {
        private final DataType _dataType;
        public DataTypeComboBoxItem(DataType dataType) { _dataType = dataType; }
        public DataType getDataType() { return _dataType; }
        @Override
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

    private final boolean readOnly;
    private boolean _assertionModified;
    private final Set<String> _predecessorVariables;
    private Border _expressionStatusBorder;

    public SetVariableAssertionDialog(Frame owner, final boolean readOnly, final SetVariableAssertion assertion) throws HeadlessException {
        this(owner, readOnly, assertion, null);
    }

    public SetVariableAssertionDialog(Frame owner, final boolean readOnly, final SetVariableAssertion assertion, final Assertion contextAssertion) throws HeadlessException {
        super(owner, assertion, true);
        this.readOnly = readOnly;

        _expressionStatusBorder = _expressionStatusScrollPane.getBorder();
        clearVariableNameStatus();
        clearContentTypeStatus();
        clearExpressionStatus();

        Set<String> vars = contextAssertion==null ?
                PolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet() :
                PolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(contextAssertion).keySet();
        _predecessorVariables = new TreeSet<String>(vars);

        // Populates data type combo box with supported data types.
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.STRING));
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.MESSAGE));
        _dataTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _contentTypeTextField.setEnabled(getSelectedDataType() == DataType.MESSAGE);
                validateFields();
            }
        });

        TextComponentPauseListenerManager.registerPauseListener(
                _variableNameTextField,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        clearVariableNameStatus();
                    }
                },
                500);
        TextComponentPauseListenerManager.registerPauseListener(
                _contentTypeTextField,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        clearContentTypeStatus();
                    }
                },
                500);
        TextComponentPauseListenerManager.registerPauseListener(
                _expressionTextArea,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        clearExpressionStatus();
                    }
                },
                500);

        _cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                _assertionModified = false;
                dispose();
            }
        });

        _okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assertion.setVariableToSet(_variableNameTextField.getText());

                final DataType dataType = getSelectedDataType();
                assertion.setDataType(dataType);

                if (dataType == DataType.MESSAGE) {
                    assertion.setContentType(_contentTypeTextField.getText());
                } else {
                    assertion.setContentType(null);
                }

                LineBreak lineBreak = LineBreak.CRLF;
                if (_crlfRadioButton.isSelected()) {
                    lineBreak = LineBreak.CRLF;
                } else if (_lfRadioButton.isSelected()) {
                    lineBreak = LineBreak.LF;
                } else if (_crRadioButton.isSelected()) {
                    lineBreak = LineBreak.CR;
                }
                assertion.setLineBreak(lineBreak);

                final String expression = TextUtils.convertLineBreaks(_expressionTextArea.getText(), lineBreak.getCharacters()).trim();
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
        String expression = TextUtils.convertLineBreaks(assertion.expression(), "\n");
        if (expression != null)
            expression = expression.trim();

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
    private synchronized void validateFields() {
        final String variableName = _variableNameTextField.getText();
        final String contentType = _contentTypeTextField.getText();
        final String expression = _expressionTextArea.getText();

        boolean ok = true;

        String validateNameResult;
        if (variableName.length() == 0) {
            ok = false;
        } else if ((validateNameResult = VariableMetadata.validateName(variableName)) != null) {
            ok = false;
            _variableNameStatusLabel.setIcon(WARNING_ICON);
            _variableNameStatusLabel.setText(reconstructLongStringByAddingLineBreakTags(validateNameResult, 58));
        } else {
            final VariableMetadata meta = BuiltinVariables.getMetadata(variableName);
            if (meta == null) {
                if (Syntax.getMatchingName(variableName, _predecessorVariables) == null) {
                    _variableNameStatusLabel.setText("OK");
                } else {
                    _variableNameStatusLabel.setText("OK (Overwrite)");
                }
                _variableNameStatusLabel.setIcon(OK_ICON);
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
        final String[] names;
        try {
            names = Syntax.getReferencedNames(expression);
        } catch (VariableNameSyntaxException e) {
            _expressionStatusLabel.setIcon(WARNING_ICON);
            _expressionStatusTextArea.setText(ExceptionUtils.getMessage(e));
            _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
            _okButton.setEnabled(false);
            return;
        }

        final java.util.List<String> badNames = new LinkedList<String>();
        for (String name : names) {
            if (BuiltinVariables.getMetadata(name) == null &&
                Syntax.getMatchingName(name, _predecessorVariables, true, true) == null) {
                badNames.add(name);
            }
        }
        final String expressionStatus;
        if (badNames.isEmpty()) {
            expressionStatus = "";
        } else {
            StringBuffer sb = new StringBuffer("No such variable");
            if (badNames.size() > 1) sb.append("s");
            sb.append(": ");
            for (Iterator<String> it = badNames.iterator(); it.hasNext();) {
                sb.append(it.next());
                if (it.hasNext()) sb.append(", ");
            }
            expressionStatus = sb.toString();
        }

        if (expressionStatus.length() == 0) {
            _expressionStatusLabel.setIcon(OK_ICON);
            _expressionStatusTextArea.setText("OK");
            _expressionStatusScrollPane.setBorder(null);
        } else {
            ok = false;
            _expressionStatusLabel.setIcon(WARNING_ICON);
            _expressionStatusTextArea.setText(expressionStatus);
            _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
        }

        _okButton.setEnabled(!readOnly && ok);
    }

    /**
     * Reconstruct a string by adding line break (<br>) tags into it.  The length of each row in the modified string is up to maxLength.
     *
     * @param longString: the string to reconstrctured.
     * @param maxLength: the maximum length for each row of the string.
     * @return a html string composed by the original string with line break tags.
     */
    private String reconstructLongStringByAddingLineBreakTags(String longString, int maxLength) {
        if (longString == null) return null;

        StringBuilder sb = new StringBuilder(longString.length());
        while (longString.length() > maxLength) {
            char lastChar = longString.charAt(maxLength - 1);
            char charInMaxLength = longString.charAt(maxLength);
            if (lastChar == ' ') {
                sb.append(longString.substring(0, maxLength - 1)).append("<br>");
                longString = longString.substring(maxLength);
            } else if (lastChar == ',' || lastChar == '.') {
                sb.append(longString.substring(0, maxLength)).append("<br>");
                if (longString.charAt(maxLength) == ' ')
                    longString = longString.substring(maxLength + 1);
                else
                    longString = longString.substring(maxLength);
            } else if (charInMaxLength == ' ' || charInMaxLength == ',' || charInMaxLength == '.') {
                if (charInMaxLength == ' ') {
                    sb.append(longString.substring(0, maxLength)).append("<br>");
                } else {
                    sb.append(longString.substring(0, maxLength + 1)).append("<br>");
                }
                try {
                    longString = longString.substring(maxLength + 1);
                } catch (IndexOutOfBoundsException e) {
                    longString = "";
                }
            } else {
                String tmp = longString.substring(0, maxLength);
                int lastSpaceIdx = tmp.lastIndexOf(' ');
                int lastCommaIdx = tmp.lastIndexOf(',');
                int lastPeriodIdx = tmp.lastIndexOf('.');
                int maxIdx = Math.max(Math.max(lastSpaceIdx, lastCommaIdx), lastPeriodIdx);
                if (maxIdx < 0)  maxIdx = maxLength - 1;

                char tmpChar = tmp.charAt(maxIdx);
                if (tmpChar == ' ') {
                    sb.append(longString.substring(0, maxIdx)).append("<br>");
                } else {
                    sb.append(longString.substring(0, maxIdx + 1)).append("<br>");
                }
                longString = longString.substring(maxIdx + 1);
            }
        }
        sb.append(longString);
        sb.insert(0, "<html><body>").append("</body></html>");

        return sb.toString();
    }

    public boolean isAssertionModified() {
        return _assertionModified;
    }
}
