package com.l7tech.console.panels;

import com.l7tech.console.event.BeanEditSupport;
import com.l7tech.policy.assertion.Regex;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
    private JTextArea regexTextArea;
    private JScrollPane regexTextAreaScrollPane;
    private JPanel testPanel;
    private JTextArea replaceTextArea;
    private JScrollPane replaceTextAreaScrollPane;
    private JTextArea testInputTextArea;
    private JScrollPane testInputTextAreaScrollPane;
    private JButton testButton;
    private JScrollPane testResultTextPaneScrollPane;
    private Regex regexAssertion;
    private BeanEditSupport beanEditSupport = new BeanEditSupport(this);
    private Pattern pattern = null;
    private JButton clearTestOutputButton;
    private JCheckBox caseInsensitivecheckBox;
    private JRadioButton matchRadioButton;
    private JRadioButton matchAndReplaceRadioButton;
    private JTextPane testResultTextPane;
    private JLabel replaceMentTextAreaLabel;
    private JLabel testInputTextAreaLabel;
    private JLabel testResultTextAreaLabel;

    public RegexDialog(Frame owner, Regex regexAssertion) throws HeadlessException {
        super(owner, true);
        setTitle("Regular Expression Assertion");
        if (regexAssertion == null) {
            throw new IllegalArgumentException();
        }
        this.regexAssertion = regexAssertion;
        initialize();
    }

    private void initialize() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel);
        if (regexAssertion.getRegex() != null) {
            regexTextArea.setText(regexAssertion.getRegex());
        }
        ButtonGroup group = new ButtonGroup();
        group.add(matchRadioButton);
        group.add(matchAndReplaceRadioButton);

        final ItemListener radioButtonListener = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean enable = matchAndReplaceRadioButton.isSelected();
                replaceTextArea.setEnabled(enable);
                replaceMentTextAreaLabel.setEnabled(enable);
            }
        };
        matchRadioButton.addItemListener(radioButtonListener);
        matchAndReplaceRadioButton.addItemListener(radioButtonListener);

        matchRadioButton.setSelected(!regexAssertion.isReplace());
        matchAndReplaceRadioButton.setSelected(regexAssertion.isReplace());

        if (regexAssertion.isReplace() && regexAssertion.getReplacement() != null) {
            replaceTextArea.setText(regexAssertion.getReplacement());
        }

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                beanEditSupport.fireCancelled(regexAssertion);
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                regexAssertion.setRegex(regexTextArea.getText());
                regexAssertion.setReplacement(replaceTextArea.getText());
                regexAssertion.setCaseInsensitive(caseInsensitivecheckBox.isSelected());
                regexAssertion.setReplace(matchAndReplaceRadioButton.isSelected());
                beanEditSupport.fireEditAccepted(regexAssertion);
            }
        });

        testButton.setEnabled(false);
        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Matcher matcher = pattern.matcher(testInputTextArea.getText());
                StyledDocument doc = (StyledDocument)testResultTextPane.getDocument();
                SimpleAttributeSet sas = new SimpleAttributeSet();
                sas.addAttribute(StyleConstants.ColorConstants.Background, Color.yellow);
                SimpleAttributeSet nas = new SimpleAttributeSet();
                if (matchAndReplaceRadioButton.isSelected()) {
                    String replaceText = replaceTextArea.getText();
                    StringBuffer sb = new StringBuffer();
                    while (matcher.find()) {
                        int offset = sb.length();
                        matcher.appendReplacement(sb, replaceText);
                        String s = sb.substring(offset, sb.length());
                        try {
                            String beforeMatchString = s.substring(0, matcher.start());
                            doc.insertString(offset, beforeMatchString, nas);
                            doc.insertString(offset + beforeMatchString.length(), replaceText, sas);
                        } catch (BadLocationException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                    int offset = sb.length();
                    matcher.appendTail(sb);
                    String appendString = sb.substring(offset);
                    try {
                        doc.insertString(offset, appendString, nas);
                    } catch (BadLocationException e1) {
                        throw new RuntimeException(e1);
                    }
                } else {
                    String testInputString = testInputTextArea.getText();
                    int offset = 0;
                    while (matcher.find()) {
                        String beforeMatchString = testInputString.substring(offset, matcher.start());
                        try {
                            doc.insertString(offset, beforeMatchString, nas);
                            offset+=beforeMatchString.length();
                            doc.insertString(offset, matcher.group(), sas);
                            offset +=matcher.group().length();
                        } catch (BadLocationException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                    if (offset < testInputString.length()) {
                        try {
                            doc.insertString(offset, testInputString.substring(offset), nas);
                        } catch (BadLocationException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }
            }
        });

        clearTestOutputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testResultTextPane.setText("");
            }
        });
        regexTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern != null);
                testButton.setEnabled(shouldEnableTestButton());
            }

            public void removeUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern != null);
                testButton.setEnabled(shouldEnableTestButton());
            }

            public void changedUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern != null);
                testButton.setEnabled(shouldEnableTestButton());
            }

        });

        testInputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                testButton.setEnabled(shouldEnableTestButton());
            }

            public void removeUpdate(DocumentEvent e) {
                testButton.setEnabled(shouldEnableTestButton());
            }

            public void changedUpdate(DocumentEvent e) {
                testButton.setEnabled(shouldEnableTestButton());
            }
        });
        updatePattern();
        okButton.setEnabled(pattern != null);
        testResultTextPane.setEditable(false);
        testResultTextPane.setFont(testInputTextArea.getFont());

    }

    private boolean shouldEnableTestButton() {
        return !empty(testInputTextArea) && pattern != null;
    }


    public BeanEditSupport getBeanEditSupport() {
        return beanEditSupport;
    }

    private boolean empty(JTextComponent tc) {
        return tc.getText() == null || "".equals(tc.getText());
    }


    private void updatePattern() {
        regexTextArea.setToolTipText("");
        if (!empty(regexTextArea)) {
            try {
                int flags = Pattern.DOTALL | Pattern.MULTILINE;
                if (caseInsensitivecheckBox.isSelected()) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                pattern = Pattern.compile(regexTextArea.getText(), flags);
            } catch (PatternSyntaxException e1) {
                regexTextArea.setToolTipText(e1.getDescription()+" index: "+e1.getIndex());
                pattern = null;
            }
        } else {
            pattern = null;
        }
    }

}
