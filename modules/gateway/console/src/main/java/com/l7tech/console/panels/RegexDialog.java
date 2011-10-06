package com.l7tech.console.panels;

import com.l7tech.console.event.BeanEditSupport;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.*;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextArea;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author emil
 */
public class RegexDialog extends LegacyAssertionPropertyDialog {
    private final String dialogTitleFromMeta;

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private SquigglyTextArea regexTextArea;
    private JTextArea replaceTextArea;
    private JTextField nameTextField;
    private JTextArea testInputTextArea;
    private Regex regexAssertion;
    private BeanEditSupport beanEditSupport = new BeanEditSupport(this);
    private Pattern pattern = null;
    private JCheckBox caseInsensitivecheckBox;
    private JRadioButton proceedIfMatchRadioButton;
    private JRadioButton matchAndReplaceRadioButton;
    private JTextPane testResultTextPane;
    private JLabel replacementTextAreaLabel;
    private JRadioButton proceedIfNoMatchRadioButton;
    private JSpinner mimePartSpinner;
    private JFormattedTextField encodingField;
    private JLabel testResultLabel;
    public JTabbedPane tabbedPane1;
    public JSplitPane splitPaneTop;
    public JSplitPane splitPaneTest;
    public JSplitPane splitPaneRegex;
    private JRadioButton encodingDefaultButton;
    private JRadioButton encodingCustomButton;
    private JLabel mimePartLabel;
    private JLabel characterEncodingLabel;
    private TargetMessagePanel targetMessagePanel;
    private JPanel captureVariablePrefixPanel;
    private JCheckBox includeMatchedSubstringInCheckBox;
    private JCheckBox repeatReplacementCheckBox;
    private JTextField repeatCountField;
    private JCheckBox stopAfterSuccessfulMatchCheckBox;
    private TargetVariablePanel captureVariablePrefix;
    private final boolean postRouting;
    private final boolean readOnly;

    public RegexDialog(Frame owner, Regex regexAssertion, boolean postRouting, boolean readOnly) throws HeadlessException {
        super(owner,regexAssertion, true);
        dialogTitleFromMeta = this.getTitle();
        if (regexAssertion == null) {
            throw new IllegalArgumentException();
        }
        this.regexAssertion = regexAssertion;
        this.postRouting = postRouting;
        this.readOnly = readOnly;
        initialize();
    }

    private void initialize() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel);

        targetMessagePanel.setTitle("Source");

        matchAndReplaceRadioButton.setToolTipText("If the pattern matches, replace the match with the replacement expression, then proceed to process the message");

        repeatCountField.setDocument(new NumberField(8));

        final ItemListener radioButtonListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateReplaceState();
                updateEnabledState();
                doTest();
            }
        };

        captureVariablePrefix = new TargetVariablePanel();
        captureVariablePrefixPanel.setLayout(new BorderLayout());
        captureVariablePrefixPanel.add(captureVariablePrefix, BorderLayout.CENTER);
        captureVariablePrefix.setAcceptEmpty(true);

        RunOnChangeListener buttonStateUpdateListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateEnabledState();
            }
        } );

        targetMessagePanel.addDocumentListener( buttonStateUpdateListener );
        proceedIfMatchRadioButton.addItemListener(radioButtonListener);
        matchAndReplaceRadioButton.addItemListener(radioButtonListener);
        proceedIfNoMatchRadioButton.addItemListener(radioButtonListener);
        captureVariablePrefix.addChangeListener(buttonStateUpdateListener);
        repeatReplacementCheckBox.addChangeListener(buttonStateUpdateListener);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                beanEditSupport.fireCancelled(regexAssertion);
            }
        });

        InputValidator inputValidator = new InputValidator(this, dialogTitleFromMeta);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                viewToModel();

                beanEditSupport.fireEditAccepted(regexAssertion);
            }
        });

        inputValidator.addRule(new InputValidator.ComponentValidationRule(targetMessagePanel) {
            @Override
            public String getValidationError() {
                return targetMessagePanel.check();
            }
        });

        regexTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { chk(); }
            @Override
            public void removeUpdate(DocumentEvent e) { chk(); }
            @Override
            public void changedUpdate(DocumentEvent e) { chk(); }

            private void chk() {
                updatePattern();
                updateEnabledState();
                doTest();
            }
        });

        DocumentListener doTestListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { doTest(); }
            @Override
            public void removeUpdate(DocumentEvent e) { doTest(); }
            @Override
            public void changedUpdate(DocumentEvent e) { doTest(); }
        };
        replaceTextArea.getDocument().addDocumentListener(doTestListener);
        testInputTextArea.getDocument().addDocumentListener(doTestListener);

        testResultTextPane.setEditable(false);
        testResultTextPane.setFont(testInputTextArea.getFont());
        TextComponentPauseListenerManager.registerPauseListener(regexTextArea, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                updatePattern();
                if (empty(regexTextArea)) {
                    regexTextArea.setNone();
                    return;
                }

                regexTextArea.setAll();
                if (pattern == null) {
                    regexTextArea.setColor(Color.RED);
                    regexTextArea.setSquiggly();
                } else {
                    regexTextArea.setNone();
                }
            }
        }, 700);

        caseInsensitivecheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updatePattern();
                doTest();
            }
        });

        mimePartSpinner.setModel(new SpinnerNumberModel(0, 0, 9999, 1));
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(mimePartSpinner, "MIME Part"));

        ItemListener encodingRadioListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateEncodingEnabledState();
            }
        };
        encodingCustomButton.addItemListener(encodingRadioListener);
        encodingDefaultButton.addItemListener(encodingRadioListener);

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        Utilities.equalizeComponentSizes(new JComponent[]  {
                mimePartLabel, characterEncodingLabel
        });
        Utilities.attachDefaultContextMenu(regexTextArea);
        Utilities.attachDefaultContextMenu(replaceTextArea);
        Utilities.attachDefaultContextMenu(testInputTextArea);
        Utilities.attachDefaultContextMenu(testResultTextPane);
        Utilities.attachDefaultContextMenu(repeatCountField);

        Utilities.enableGrayOnDisabled(replaceTextArea);
        Utilities.enableGrayOnDisabled(encodingField);
        Utilities.enableGrayOnDisabled(repeatCountField);

        Utilities.deuglifySplitPane(splitPaneTop);
        Utilities.deuglifySplitPane(splitPaneTest);
        Utilities.deuglifySplitPane(splitPaneRegex);

        updateEncodingEnabledState();
        updateReplaceState();
        updatePattern();
        updateEnabledState();
    }

    private void updateEnabledState() {
        repeatReplacementCheckBox.setEnabled(matchAndReplaceRadioButton.isSelected());
        repeatCountField.setEnabled(repeatReplacementCheckBox.isEnabled() && repeatReplacementCheckBox.isSelected());
        stopAfterSuccessfulMatchCheckBox.setEnabled(proceedIfMatchRadioButton.isSelected() || proceedIfNoMatchRadioButton.isSelected());

        okButton.setEnabled( !readOnly && pattern != null && targetMessagePanel.isValidTarget() && captureVariablePrefix.isEntryValid());
    }

    private void updateEncodingEnabledState() {
        encodingField.setEnabled(encodingCustomButton.isSelected());
    }

    private void viewToModel() {
        final String patternText = regexTextArea.getText();
        regexAssertion.setRegex(patternText);
        regexAssertion.setPatternContainsVariables(Syntax.getReferencedNames(patternText, false).length > 0);
        regexAssertion.setReplacement(replaceTextArea.getText());
        if (nameTextField.getText().trim().length() > 0)
            regexAssertion.setRegexName(nameTextField.getText().trim());
        else
            regexAssertion.setRegexName(null);
        regexAssertion.setCaseInsensitive(caseInsensitivecheckBox.isSelected());
        regexAssertion.setReplace(matchAndReplaceRadioButton.isSelected());
        regexAssertion.setProceedIfPatternMatches(!proceedIfNoMatchRadioButton.isSelected());

        Object val = mimePartSpinner.getValue();
        regexAssertion.setMimePart(val != null ? (Integer) val : 0);

        if (encodingDefaultButton.isSelected()) {
            regexAssertion.setEncoding(null);
        } else {
            regexAssertion.setEncoding(encodingField.getText());
        }

        regexAssertion.setCaptureVar(emptyToNull(captureVariablePrefix.getVariable()));

        regexAssertion.setAutoTarget(false);
        targetMessagePanel.updateModel(regexAssertion);
        if (repeatReplacementCheckBox.isSelected() && repeatCountField.getText() != null && repeatCountField.getText().trim().length() > 0) {
            regexAssertion.setReplaceRepeatCount(Integer.parseInt(repeatCountField.getText()));
        } else {
            regexAssertion.setReplaceRepeatCount(0);
        }

        regexAssertion.setIncludeEntireExpressionCapture(includeMatchedSubstringInCheckBox.isSelected());

        regexAssertion.setFindAll(!stopAfterSuccessfulMatchCheckBox.isSelected());
    }



    private static String nullToEmpty(String in) {
        return in == null || in.trim().length() < 1 ? "" : in;
    }

    private static String emptyToNull(String in) {
        return in == null || in.trim().length() < 1 ? null : in;
    }

    public void setData(Regex regexAssertion) {
        this.regexAssertion = regexAssertion;
        if (regexAssertion.getRegex() != null) {
            regexTextArea.setText(regexAssertion.getRegex());
        }
        if (regexAssertion.getEncoding() != null) {
            encodingField.setText(regexAssertion.getEncoding());
            encodingCustomButton.setSelected(true);
        }
        if (regexAssertion.getRegexName() != null) {
            nameTextField.setText(regexAssertion.getRegexName());
        }

        proceedIfMatchRadioButton.setSelected(!regexAssertion.isReplace() && regexAssertion.isProceedIfPatternMatches());
        proceedIfNoMatchRadioButton.setSelected(!regexAssertion.isReplace() && !regexAssertion.isProceedIfPatternMatches());
        matchAndReplaceRadioButton.setSelected(regexAssertion.isReplace());

        if (regexAssertion.isReplace() && regexAssertion.getReplacement() != null) {
            replaceTextArea.setText(regexAssertion.getReplacement());
        }
        caseInsensitivecheckBox.setSelected(regexAssertion.isCaseInsensitive());

        captureVariablePrefix.setVariable(nullToEmpty(regexAssertion.getCaptureVar()));
        captureVariablePrefix.setAssertion(regexAssertion,getPreviousAssertion());

        Integer mimePartIndex;
        try {
            mimePartIndex = regexAssertion.getMimePart();
        }
        catch(NumberFormatException nfe) {
            mimePartIndex = 0;
        }
        mimePartSpinner.setValue(mimePartIndex);

        if (regexAssertion.isAutoTarget())
            regexAssertion.setTarget(postRouting ? TargetMessageType.RESPONSE : TargetMessageType.REQUEST);

        targetMessagePanel.setModel(regexAssertion,getPreviousAssertion());

        includeMatchedSubstringInCheckBox.setSelected(regexAssertion.isIncludeEntireExpressionCapture());
        stopAfterSuccessfulMatchCheckBox.setSelected(!regexAssertion.isFindAll());
        int repeatCount = regexAssertion.getReplaceRepeatCount();
        if (repeatCount > 0) {
            repeatReplacementCheckBox.setSelected(true);
            repeatCountField.setText(String.valueOf(repeatCount));
        } else {
            repeatReplacementCheckBox.setSelected(false);
            repeatCountField.setText("5");
        }

        updateEnabledState();
    }

    @Override
    public void setPolicyPosition( final PolicyPosition policyPosition ) {
        super.setPolicyPosition(policyPosition);
        targetMessagePanel.setModel(regexAssertion,getPreviousAssertion());
    }

    private void doTest() {
        testResultTextPane.setText("");
        if (pattern == null)            
            return;

        Matcher matcher = pattern.matcher(testInputTextArea.getText());
        StyledDocument doc = (StyledDocument)testResultTextPane.getDocument();
        SimpleAttributeSet sas = new SimpleAttributeSet();
        sas.addAttribute(StyleConstants.ColorConstants.Background, Color.yellow);
        SimpleAttributeSet nas = new SimpleAttributeSet();

        Collection<int[]> highlights = new ArrayList<int[]>();
        if (matchAndReplaceRadioButton.isSelected()) {
            collectHighlightsForReplace(matcher, doc, nas, highlights);
            // TODO Remove the following line when collectHighlightsForReplace works correctly
            highlights.clear(); // Suppress highlights when testing replace since they are incorrect (Bug #5309)
        } else {
            collectHighlightsForMatch(matcher, doc, nas, highlights);
        }
        for (Object highlight : highlights) {
            int[] pos = (int[]) highlight;
            doc.setCharacterAttributes(pos[0], pos[1], sas, true);
        }
    }

    private void collectHighlightsForMatch(Matcher matcher, StyledDocument doc, SimpleAttributeSet nas, Collection<int[]> highlights) {
        String testInputString = testInputTextArea.getText();
        try {
            doc.insertString(0, testInputString, nas);
        } catch (BadLocationException e1) {
            throw new RuntimeException(e1);
        }

        while (matcher.find()) {
            highlights.add(new int[] {matcher.start(), matcher.end() - matcher.start()});
        }
    }

    private void collectHighlightsForReplace(Matcher matcher, StyledDocument doc, SimpleAttributeSet nas, Collection<int[]> highlights) {
        String replaceText = replaceTextArea.getText();
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            try {
                matcher.appendReplacement(sb, Syntax.regexPattern.matcher(replaceText).replaceAll(""));
                highlights.add(new int[] {sb.length() - replaceText.length(), replaceText.length()});
            } catch (RuntimeException e) {
                // Bad regex -- ignore it for now
            }
        }
        matcher.appendTail(sb);

        try {
            doc.insertString(0, sb.toString(), nas);
        } catch (BadLocationException e1) {
            throw new RuntimeException(e1);
        }
    }

    public BeanEditSupport getBeanEditSupport() {
        return beanEditSupport;
    }

    private boolean empty(JTextComponent tc) {
        return tc.getText() == null || "".equals(tc.getText());
    }

    private void updateReplaceState() {
        boolean rep = matchAndReplaceRadioButton.isSelected();
        replacementTextAreaLabel.setEnabled(rep);
        replaceTextArea.setEnabled(rep);
        testResultLabel.setText(rep
                ? "Test Result (After Replace)"
                : "Test Result (Highlighting Matches)");
    }

    private void updatePattern() {
        regexTextArea.setToolTipText(null);
        if (!empty(regexTextArea)) {
            try {
                int flags = Pattern.DOTALL | Pattern.MULTILINE;
                if (caseInsensitivecheckBox.isSelected()) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                String text = regexTextArea.getText();
                if (Syntax.getReferencedNames(text, false).length > 0)
                    text = Pattern.quote(Syntax.regexPattern.matcher(text).replaceAll("\\${$1}"));
                pattern = Pattern.compile(text, flags);
                regexTextArea.setToolTipText("OK");
            } catch (PatternSyntaxException e1) {
                regexTextArea.setToolTipText(e1.getDescription() + " index: " + e1.getIndex());
                pattern = null;
            }
        } else {
            pattern = null;
        }
    }

    private void createUIComponents() {
        targetMessagePanel = new TargetMessagePanel("Source");
        targetMessagePanel.setAllowNonMessageVariables(true);
    }
}
