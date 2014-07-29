/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.*;
import com.l7tech.util.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.LineBreak;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.*;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static com.l7tech.policy.assertion.SetVariableAssertion.CalendarFields;

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
        private DataTypeComboBoxItem(DataType dataType) { _dataType = dataType; }
        public DataType getDataType() { return _dataType; }
        @Override
        public String toString() { return _dataType.getName(); }
    }

    private final static String DATE_PREVIEW_DEFAULT = "<No preview available>";
    private final static String AUTO_STRING = "<auto>";

    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));
    private final DateTimeConfigUtils dateParser = new DateTimeConfigUtils();

    private JPanel _mainPanel;
    private JComboBox<DataTypeComboBoxItem> _dataTypeComboBox;
    private JLabel nonExpressionInputStatusLabel;
    private JRadioButton _crlfRadioButton;
    private JRadioButton _crRadioButton;
    private JRadioButton _lfRadioButton;
    private JLabel _expressionStatusLabel;
    private JTextArea _expressionStatusTextArea;
    private JScrollPane _expressionStatusScrollPane;
    private JButton _cancelButton;
    private JButton _okButton;
    private JComboBox<String> _contentTypeComboBox;
    private TargetVariablePanel _variableNameVarPanel;
    private JPanel saveLineBreaksPanel;
    private JLabel contentTypeLabel;
    private JLabel formatLabel;
    private JTextField dateOffsetTextField;
    private JComboBox<CalendarFields> dateOffsetComboBox;
    private JLabel offsetLabel;
    private JComboBox<String> dateFormatComboBox;
    private JTextField datePreviewTextField;
    private JLabel datePreviewLabel;
    private JLabel dateOffsetStatusLabel;
    private JScrollPane expressionScrollPane;

    // custom create
    private JTextArea _expressionTextArea;
    private void createUIComponents() {
        _expressionTextArea = new RSyntaxTextArea();

        Utilities.attachClipboardKeyboardShortcuts( _expressionTextArea );
        ActionMap am = _expressionTextArea.getActionMap();
        am.put( "paste-from-clipboard", ClipboardActions.getPasteAction() );
        am.put( "copy-to-clipboard", ClipboardActions.getCopyAction() );
        am.put( "cut-to-clipboard", ClipboardActions.getCutAction() );
        am.put( "paste", ClipboardActions.getPasteAction() );
        am.put( "copy", ClipboardActions.getCopyAction() );
        am.put( "cut", ClipboardActions.getCutAction() );
    }

    private final boolean readOnly;
    private boolean _assertionModified;
    private final Set<String> _predecessorVariables;
    private Border _expressionStatusBorder;
    private DataType previousType;
    private DataType currentType;

    public SetVariableAssertionDialog(Frame owner, final boolean readOnly, final SetVariableAssertion assertion) throws HeadlessException {
        this(owner, readOnly, assertion, null);
    }

    public SetVariableAssertionDialog(Frame owner, final boolean readOnly, final SetVariableAssertion assertion, final Assertion contextAssertion) throws HeadlessException {
        super(owner, assertion, true);
        this.readOnly = readOnly;

        _expressionStatusBorder = _expressionStatusScrollPane.getBorder();
        clearContentTypeStatus();
        clearExpressionStatus();

        Set<String> vars = contextAssertion==null ?
                SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet() :
                SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf(contextAssertion).keySet();
        // convert all vars to lower
         _predecessorVariables = new TreeSet<String>();
        for(String var : vars) {
            _predecessorVariables.add(var.toLowerCase());
        }

        _variableNameVarPanel.setAssertion(assertion,contextAssertion);
        _variableNameVarPanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                validateFields();
            }
        });

        // Populates data type combo box with supported data types.
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.STRING));
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.MESSAGE));
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.DATE_TIME));
        _dataTypeComboBox.addItem(new DataTypeComboBoxItem(DataType.INTEGER));
        _dataTypeComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final DataType selectedItem = getSelectedDataType();
                previousType = currentType;
                currentType = selectedItem;

                if (previousType == DataType.DATE_TIME && currentType != DataType.DATE_TIME) {
                    // clear any auto string
                    configureExpressionFieldForDate(true);
                } else if (previousType != DataType.DATE_TIME && currentType == DataType.DATE_TIME) {
                    configureExpressionFieldForDate(false);
                }

                enableDisableComponents();
                validateFields();
            }
        });

        _contentTypeComboBox.addItem( ContentTypeHeader.XML_DEFAULT.getFullValue() );
        _contentTypeComboBox.addItem( ContentTypeHeader.TEXT_DEFAULT.getFullValue() );
        _contentTypeComboBox.addItem( ContentTypeHeader.SOAP_1_2_DEFAULT.getFullValue() );
        _contentTypeComboBox.addItem( ContentTypeHeader.APPLICATION_JSON.getFullValue() );

        _contentTypeComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                enableDisableComponents();
            }
        } );

        TextComponentPauseListenerManager.registerPauseListener(
                (JTextComponent)_contentTypeComboBox.getEditor().getEditorComponent(),
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
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(
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
                500, new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                final boolean isDateSelected = getSelectedDataType() == DataType.DATE_TIME;
                if (isDateSelected) {
                    configureExpressionFieldForDate(true);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                final boolean isDateSelected = getSelectedDataType() == DataType.DATE_TIME;
                if (isDateSelected) {
                    configureExpressionFieldForDate(false);
                }
            }
        }
        );

        TextComponentPauseListenerManager.registerPauseListener(
                dateOffsetTextField,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        clearDateOffsetStatus();
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
                assertion.setVariableToSet(_variableNameVarPanel.getVariable());

                final DataType dataType = getSelectedDataType();
                assertion.setDataType(dataType);

                if (dataType == DataType.MESSAGE) {
                    assertion.setContentType((String)_contentTypeComboBox.getSelectedItem());
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
                assertion.setExpression((AUTO_STRING.equals(expression)) ? "" : expression);

                if (dataType == DataType.DATE_TIME) {
                    final String dateFormat = getDateFormat();
                    assertion.setDateFormat((AUTO_STRING.equals(dateFormat) ? null : dateFormat));
                    assertion.setDateOffsetField(((CalendarFields) dateOffsetComboBox.getSelectedItem()).getCalendarField());
                    assertion.setDateOffsetExpression(dateOffsetTextField.getText().trim());
                } else {
                    // reset to defaults
                    assertion.setDateFormat(null);
                    assertion.setDateOffsetField(CalendarFields.SECONDS.getCalendarField());
                    assertion.setDateOffsetExpression(null);
                }

                _assertionModified = true;
                dispose();
            }
        });

        if (Registry.getDefault().isAdminContextPresent()) {
            final ClusterStatusAdmin clusterAdmin = Registry.getDefault().getClusterStatusAdmin();
            final List<String> configuredDateFormats = new ArrayList<String>(clusterAdmin.getConfiguredDateFormats());
            configuredDateFormats.add(0, AUTO_STRING);

            dateParser.setCustomDateFormats(configuredDateFormats);
            dateFormatComboBox.setModel(new DefaultComboBoxModel<String>(configuredDateFormats.toArray(new String[configuredDateFormats.size()])));

            dateParser.setAutoDateFormats(clusterAdmin.getAutoDateFormats());
        }

        final JTextComponent dateFormatField = (JTextComponent) dateFormatComboBox.getEditor().getEditorComponent();
        final RunOnChangeListener dateFormatListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                validateFields();
            }
        });
        dateFormatField.getDocument().addDocumentListener(dateFormatListener);
        dateFormatField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                final String item = getDateFormat();
                if (AUTO_STRING.equals(item)) {
                    //clear selection
                    dateFormatComboBox.getEditor().setItem("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                final String item = getDateFormat();
                if ("".equals(item)) {
                    //clear selection
                    dateFormatComboBox.getEditor().setItem(AUTO_STRING);
                }
            }
        });

        dateOffsetComboBox.setModel(new DefaultComboBoxModel<CalendarFields>(CalendarFields.values()));
        dateOffsetComboBox.setRenderer(new TextListCellRenderer<CalendarFields>(new Functions.Unary<String, CalendarFields>() {
            @Override
            public String call(CalendarFields calendarFields) {
                return calendarFields.getDisplayName();
            }
        }));
        dateOffsetComboBox.addActionListener(dateFormatListener);

        //
        // Sets dialog to assertion data.
        //

        _variableNameVarPanel.setVariable(assertion.getVariableToSet());
        selectDataType(assertion.getDataType(), true);
        previousType = assertion.getDataType();
        currentType = previousType;
        if ( assertion.getContentType() != null ) {
            _contentTypeComboBox.setSelectedItem( assertion.getContentType() );
        } else {
            _contentTypeComboBox.setSelectedIndex( 0 );
        }

        // JTextArea likes all line break to be LF.
        String expression = TextUtils.convertLineBreaks(assertion.expression(), "\n");
        if (expression != null)
            expression = expression.trim();

        _expressionTextArea.setText(expression);
        _expressionTextArea.setCaretPosition(0);
        if (assertion.getLineBreak() == LineBreak.LF) {
            _lfRadioButton.doClick();
        } else if (assertion.getLineBreak() == LineBreak.CR) {
            _crRadioButton.doClick();
        } else {
             // Defaults to CR-LF.
            _crlfRadioButton.doClick();
        }

        final String dateFormat = assertion.getDateFormat();
        final String formatToUse = (dateFormat == null)? AUTO_STRING : dateFormat;
        dateFormatComboBox.setSelectedItem(formatToUse);
        dateOffsetComboBox.setSelectedItem(CalendarFields.getCalendarField(assertion.getDateOffsetField()));
        dateOffsetTextField.setText(assertion.getDateOffsetExpression());

        validateFields();

        enableDisableComponents();

        add(_mainPanel);
    }

    /**
     * Configure the expression field by adding &lt;auto&gt; or removing when needed.
     * Only call when the data type is Date/Time
     * @param isRemoveAuto true if auto string should be removed, false when should be added
     */
    private void configureExpressionFieldForDate(boolean isRemoveAuto) {
        if (isRemoveAuto) {
            final String expText = _expressionTextArea.getText().trim();
            if (AUTO_STRING.equals(expText)) {
                _expressionTextArea.setText("");
            }
        } else {
            final String expText = _expressionTextArea.getText().trim();
            if (expText.isEmpty()) {
                _expressionTextArea.setText(AUTO_STRING);
            }
        }
    }

    private String getDateFormat() {
        return ((String) dateFormatComboBox.getEditor().getItem()).trim();
    }

    private void enableDisableComponents() {
        final DataType dataType = getSelectedDataType();
        final boolean isMessageSelected = dataType == DataType.MESSAGE;
        _contentTypeComboBox.setEnabled(isMessageSelected);

        final boolean isDateSelected = dataType == DataType.DATE_TIME;
        final boolean isStringMessageSelected = dataType == DataType.STRING ||dataType == DataType.MESSAGE;

        _contentTypeComboBox.setVisible(isStringMessageSelected);
        contentTypeLabel.setVisible(isStringMessageSelected);
        _contentTypeComboBox.setVisible(isStringMessageSelected);
        nonExpressionInputStatusLabel.setVisible(isDateSelected || isMessageSelected);
        saveLineBreaksPanel.setVisible(isStringMessageSelected);

        formatLabel.setVisible(isDateSelected);
        dateFormatComboBox.setVisible(isDateSelected);
        offsetLabel.setVisible(isDateSelected);
        dateOffsetTextField.setVisible(isDateSelected);
        dateOffsetComboBox.setVisible(isDateSelected);
        datePreviewLabel.setVisible(isDateSelected);
        datePreviewTextField.setVisible(isDateSelected);
        dateOffsetStatusLabel.setVisible(isDateSelected);

        if ( _expressionTextArea instanceof RSyntaxTextArea ) {
            RSyntaxTextArea rsTextArea = (RSyntaxTextArea) _expressionTextArea;
            String syntax = SyntaxConstants.SYNTAX_STYLE_NONE;
            if ( _contentTypeComboBox.isVisible() ) {

                Object ctypeObj = _contentTypeComboBox.getSelectedItem();
                if ( ctypeObj != null ) {
                    ContentTypeHeader ctype = ContentTypeHeader.create( ctypeObj.toString() );
                    if ( ctype.isXml() ) {
                        syntax = SyntaxConstants.SYNTAX_STYLE_XML;
                    } else if ( ctype.isJson() ) {
                        syntax = SyntaxConstants.SYNTAX_STYLE_JSON;
                    }
                }
            }
            rsTextArea.setSyntaxEditingStyle( syntax );
        }
    }

    /**
     * Sets which data type is selected in {@link #_dataTypeComboBox}.
     * @param dataType  the data type to select
     * @param requireSelection True if it is an error if the given type cannot be selected
     * @throws RuntimeException if <code>dataType</code> is not avaiable in the combo box
     */
    private void selectDataType(final DataType dataType, final boolean requireSelection ) {
        if (dataType == null) return;

        for (int i = 0; i < _dataTypeComboBox.getItemCount(); ++i) {
            if (((DataTypeComboBoxItem) _dataTypeComboBox.getItemAt(i)).getDataType() == dataType) {
                _dataTypeComboBox.setSelectedIndex(i);
                return;
            }
        }

        if ( !requireSelection ) return;

        throw new RuntimeException("Data type \"" + dataType.getName() + "\" not available in combobox.");
    }

    /**
     * @return the data type currently selected in the data type combobox
     */
    private DataType getSelectedDataType() {
        return ((DataTypeComboBoxItem) _dataTypeComboBox.getSelectedItem()).getDataType();
    }

    private void clearContentTypeStatus() {
        nonExpressionInputStatusLabel.setIcon(BLANK_ICON);
        nonExpressionInputStatusLabel.setText(null);
    }

    private void clearExpressionStatus() {
        _expressionStatusLabel.setIcon(BLANK_ICON);
        _expressionStatusTextArea.setText(null);
        _expressionStatusScrollPane.setBorder(null);
    }

    private void clearDateOffsetStatus() {
        dateOffsetStatusLabel.setIcon(BLANK_ICON);
        dateOffsetStatusLabel.setText(null);
    }

    private void generateDatePreview(@Nullable Integer offset) {
        if (getSelectedDataType() != DataType.DATE_TIME) {
            return;
        }

        final String dateInput = _expressionTextArea.getText().trim();
        final String dateFormat = getDateFormat();
        String message;
        if (AUTO_STRING.equals(dateInput) || dateInput.isEmpty()) {
            message = "<Gateway time>";
        } else if (!Syntax.isAnyVariableReferenced(dateInput) && !Syntax.isAnyVariableReferenced(dateFormat)) {
            try {
                final Date date;
                if (!AUTO_STRING.equals(dateFormat) && !dateFormat.isEmpty() && !DateTimeConfigUtils.isTimestampFormat(dateFormat)) {
                    SimpleDateFormat format = new SimpleDateFormat(dateFormat);
                    format.setTimeZone(DateUtils.getZuluTimeZone());
                    format.setLenient(ConfigFactory.getBooleanProperty("com.l7tech.util.lenientDateFormat", false));
                    date = format.parse(dateInput);
                } else if (DateTimeConfigUtils.isTimestampFormat(dateFormat)) {
                    date = DateTimeConfigUtils.parseForTimestamp(dateFormat, dateInput);
                } else {
                    date = dateParser.parseDateFromString(dateInput);
                }

                final Date dateToUse;
                if (offset != null) {
                    final int calendarField = ((CalendarFields) dateOffsetComboBox.getSelectedItem()).getCalendarField();
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTime(date);
                    calendar.add(calendarField, offset);
                    dateToUse = calendar.getTime();
                } else {
                    dateToUse = date;
                }

                final SimpleDateFormat format = new SimpleDateFormat(DateUtils.ISO8601_PATTERN);
                format.setTimeZone(DateUtils.getZuluTimeZone());
                message = format.format(dateToUse);

            } catch (ParseException e) {
                message = ExceptionUtils.getMessage(e);
            } catch (DateTimeConfigUtils.UnknownDateFormatException e) {
                message = e.getMessage();
            } catch (DateTimeConfigUtils.InvalidDateFormatException e) {
                message = e.getMessage();
            } catch (IllegalArgumentException e) {
                //Can be throw by SimpleDateFormat - translate message
                message = "Invalid format: " + dateFormat;
            }
        } else {
            message = DATE_PREVIEW_DEFAULT;
        }

        datePreviewTextField.setText(message);
    }

    private String getExpressionTrimmed() {
        final String exp = _expressionTextArea.getText().trim();
        if (AUTO_STRING.equals(exp)) {
            return "";
        } else {
            return exp;
        }
    }

    /**
     * Validates values in various fields and sets the status labels as appropriate.
     */
    private void validateFields() {
        final String variableName = _variableNameVarPanel.getVariable();
        final String contentType = ((JTextComponent)_contentTypeComboBox.getEditor().getEditorComponent()).getText();
        final String expression = getExpressionTrimmed();

        boolean ok = _variableNameVarPanel.isEntryValid();

        final VariableMetadata meta = BuiltinVariables.getMetadata(variableName);
        if (meta == null) {
            _dataTypeComboBox.setEnabled(true);
        } else {
            selectDataType(meta.getType(), false);
            _dataTypeComboBox.setEnabled(false);
        }


        if (getSelectedDataType() == DataType.MESSAGE) {
            _contentTypeComboBox.setEnabled(true);
            if (contentType.length() == 0) {
                ok = false;
            } else {
                try {
                    ContentTypeHeader.parseValue( contentType );
                    nonExpressionInputStatusLabel.setIcon(OK_ICON);
                    nonExpressionInputStatusLabel.setText("OK");
                } catch (IOException e) {
                    ok = false;
                    nonExpressionInputStatusLabel.setIcon(WARNING_ICON);
                    nonExpressionInputStatusLabel.setText("Incorrect syntax");
                    _okButton.setEnabled(false);
                }
            }
        } else {
            _contentTypeComboBox.setEnabled(false);
            clearContentTypeStatus();
        }

        if (getSelectedDataType() == DataType.INTEGER) {
            String text = _expressionTextArea.getText();

            if(Syntax.isAnyVariableReferenced(text)){
                _expressionStatusLabel.setIcon(OK_ICON);
                _expressionStatusTextArea.setText("OK");
                _expressionStatusScrollPane.setBorder(null);
                _okButton.setEnabled(!readOnly);
                return;
            }

            try{
                Integer val = Integer.parseInt(text);
                if(val <= Integer.MAX_VALUE && val >= Integer.MIN_VALUE)
                {
                    _expressionStatusLabel.setIcon(OK_ICON);
                    _expressionStatusTextArea.setText("OK");
                    _expressionStatusScrollPane.setBorder(null);
                    _okButton.setEnabled(!readOnly);
                }
                else {
                    _expressionStatusLabel.setIcon(WARNING_ICON);
                    _expressionStatusTextArea.setText("Number out of range");
                    _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
                    _okButton.setEnabled(false);
                }
            } catch (NumberFormatException e) {
                ok = false;
                _expressionStatusLabel.setIcon(WARNING_ICON);
                _expressionStatusTextArea.setText("Incorrect syntax");
                _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
                _okButton.setEnabled(false);
            }
            return;
        }

        if (getSelectedDataType() == DataType.DATE_TIME) {
            // Validate any custom format selected
            try {
                final String pattern = getDateFormat();
                if (!Syntax.isAnyVariableReferenced(pattern) && !AUTO_STRING.equals(pattern) && !pattern.isEmpty() && !DateTimeConfigUtils.isTimestampFormat(pattern)) {
                    new SimpleDateFormat(pattern);
                }
                nonExpressionInputStatusLabel.setIcon(OK_ICON);
                nonExpressionInputStatusLabel.setText("OK");
            } catch (Exception e) {
                ok = false;
                nonExpressionInputStatusLabel.setIcon(WARNING_ICON);
                nonExpressionInputStatusLabel.setText("Invalid date format");
            }

            // validate offset
            final String offsetExp = dateOffsetTextField.getText().trim();
            final Either<String, Option<Integer>> timeOffsetEither = getTimeOffset(offsetExp);

            if (timeOffsetEither.isRight()) {
                dateOffsetStatusLabel.setIcon(OK_ICON);
                dateOffsetStatusLabel.setText("OK");
            } else {
                dateOffsetStatusLabel.setIcon(WARNING_ICON);
                dateOffsetStatusLabel.setText(timeOffsetEither.left());
                ok = false;
            }
            generateDatePreview(timeOffsetEither.isRight()? timeOffsetEither.right().toNull(): null);
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
                Syntax.getMatchingName(name, _predecessorVariables, false, true) == null) {
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
            _expressionStatusLabel.setIcon(WARNING_ICON);
            _expressionStatusTextArea.setText(expressionStatus);
            _expressionStatusScrollPane.setBorder(_expressionStatusBorder);
        }

        _okButton.setEnabled(!readOnly && ok);
    }

    public boolean isAssertionModified() {
        return _assertionModified;
    }

    // - PRIVATE

    /**
     * Get the value of the time offset.
     *
     * @param offsetExp expression to validate.
     * @return Either. When left it's not null if no variable was referenced. When right it's the validation error message.
     */
    private Either<String, Option<Integer>> getTimeOffset(String offsetExp) {
        boolean isValid = true;
        String errorMsg = null;

        Integer value = null;
        try {
            if (!Syntax.isAnyVariableReferenced(offsetExp) && !offsetExp.isEmpty()) {
                try {
                    value = Integer.valueOf(offsetExp);
                } catch (NumberFormatException e) {
                    isValid = false;
                    errorMsg = "Invalid integer";
                }
            }
        } catch (VariableNameSyntaxException e) {
            errorMsg = "Invalid variable reference";
            isValid = false;
        }

        if (!isValid) {
            return Either.left(errorMsg);
        } else {
            return Either.rightOption(value);
        }
    }
}
