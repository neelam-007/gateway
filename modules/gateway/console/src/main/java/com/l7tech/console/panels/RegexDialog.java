package com.l7tech.console.panels;

import com.l7tech.console.event.BeanEditSupport;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
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
 * @version 22-Mar-2005
 */
public class RegexDialog extends JDialog {
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
    private JTextField captureVariablePrefix;
    private JRadioButton encodingDefaultButton;
    private JRadioButton encodingCustomButton;
    private JLabel mimePartLabel;
    private JLabel characterEncodingLabel;
    private TargetMessagePanel targetMessagePanel;
    private final boolean postRouting;
    private final boolean readOnly;

    public RegexDialog(Frame owner, Regex regexAssertion, boolean postRouting, boolean readOnly) throws HeadlessException {
        super(owner, true);
        setTitle("Regular Expression Assertion");
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

        matchAndReplaceRadioButton.setToolTipText("If the pattern matches, replace the match with the replacement expression, then proceed to process the message");

        final ItemListener radioButtonListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateReplaceState();
                doTest();
            }
        };

        proceedIfMatchRadioButton.addItemListener(radioButtonListener);
        matchAndReplaceRadioButton.addItemListener(radioButtonListener);
        proceedIfNoMatchRadioButton.addItemListener(radioButtonListener);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                beanEditSupport.fireCancelled(regexAssertion);
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                viewToModel();

                beanEditSupport.fireEditAccepted(regexAssertion);
            }
        });

        regexTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { chk(); }
            public void removeUpdate(DocumentEvent e) { chk(); }
            public void changedUpdate(DocumentEvent e) { chk(); }

            private void chk() {
                updatePattern();
                okButton.setEnabled(!readOnly && pattern != null);
                doTest();
            }
        });

        DocumentListener doTestListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doTest(); }
            public void removeUpdate(DocumentEvent e) { doTest(); }
            public void changedUpdate(DocumentEvent e) { doTest(); }
        };
        replaceTextArea.getDocument().addDocumentListener(doTestListener);
        testInputTextArea.getDocument().addDocumentListener(doTestListener);

        testResultTextPane.setEditable(false);
        testResultTextPane.setFont(testInputTextArea.getFont());
        TextComponentPauseListenerManager.registerPauseListener(regexTextArea, new PauseListener() {
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

            public void textEntryResumed(JTextComponent component) {

            }
        }, 700);

        caseInsensitivecheckBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updatePattern();
                doTest();
            }
        });

        Integer mimePartIndex;
        try {
            mimePartIndex = regexAssertion.getMimePart();
        }
        catch(NumberFormatException nfe) {
            mimePartIndex = 0;
        }
        mimePartSpinner.setModel(new SpinnerNumberModel(0, 0, 9999, 1));
        mimePartSpinner.setValue(mimePartIndex);

        ItemListener encodingRadioListener = new ItemListener() {
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
        Utilities.attachDefaultContextMenu(captureVariablePrefix);

        Utilities.enableGrayOnDisabled(replaceTextArea);
        Utilities.enableGrayOnDisabled(encodingField);

        Utilities.deuglifySplitPane(splitPaneTop);
        Utilities.deuglifySplitPane(splitPaneTest);
        Utilities.deuglifySplitPane(splitPaneRegex);

        modelToView();
        updateEncodingEnabledState();
        updateReplaceState();
        updatePattern();
        okButton.setEnabled(!readOnly && pattern != null);
    }

    private void updateEncodingEnabledState() {
        encodingField.setEnabled(encodingCustomButton.isSelected());
    }

    private void viewToModel() {
        regexAssertion.setRegex(regexTextArea.getText());
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

        regexAssertion.setCaptureVar(emptyToNull(captureVariablePrefix.getText()));

        regexAssertion.setAutoTarget(false);
        targetMessagePanel.updateModel(regexAssertion);
    }



    private static String nullToEmpty(String in) {
        return in == null || in.trim().length() < 1 ? "" : in;
    }

    private static String emptyToNull(String in) {
        return in == null || in.trim().length() < 1 ? null : in;
    }

    private void modelToView() {
        if (regexAssertion.getRegex() != null) {
            regexTextArea.setText(regexAssertion.getRegex());
        }
        if (regexAssertion.getEncoding() != null) {
            encodingField.setText(regexAssertion.getEncoding());
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

        captureVariablePrefix.setText(nullToEmpty(regexAssertion.getCaptureVar()));

        if (regexAssertion.isAutoTarget())
            regexAssertion.setTarget(postRouting ? TargetMessageType.RESPONSE : TargetMessageType.REQUEST);
        targetMessagePanel.setModel(regexAssertion);
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
                : "Test Result (Showing Matches)");
    }

    private void updatePattern() {
        regexTextArea.setToolTipText(null);
        if (!empty(regexTextArea)) {
            try {
                int flags = Pattern.DOTALL | Pattern.MULTILINE;
                if (caseInsensitivecheckBox.isSelected()) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                pattern = Pattern.compile(regexTextArea.getText(), flags);
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
