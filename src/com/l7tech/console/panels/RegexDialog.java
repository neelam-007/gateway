package com.l7tech.console.panels;

import com.l7tech.console.event.BeanEditSupport;
import com.l7tech.policy.assertion.Regex;
import com.l7tech.common.gui.widgets.SquigglyTextArea;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.gui.util.PauseListener;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author emil
 * @version 22-Mar-2005
 */
public class RegexDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private SquigglyTextArea regexTextArea;
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
                testResultTextPane.setText("");
                Matcher matcher = pattern.matcher(testInputTextArea.getText());
                StyledDocument doc = (StyledDocument)testResultTextPane.getDocument();
                SimpleAttributeSet sas = new SimpleAttributeSet();
                sas.addAttribute(StyleConstants.ColorConstants.Background, Color.yellow);
                SimpleAttributeSet nas = new SimpleAttributeSet();

                Collection highlights = new ArrayList();
                if (matchAndReplaceRadioButton.isSelected()) {
                    String replaceText = replaceTextArea.getText();
                    StringBuffer sb = new StringBuffer();

                    while (matcher.find()) {
                        matcher.appendReplacement(sb, replaceText);
                        highlights.add(new int[] {sb.length() - replaceText.length(), replaceText.length()});
                    }
                    matcher.appendTail(sb);

                    try {
                        doc.insertString(0, sb.toString(), nas);
                    } catch (BadLocationException e1) {
                        throw new RuntimeException(e1);
                    }
                } else {
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
                for (Iterator iterator = highlights.iterator(); iterator.hasNext();) {
                    int[] pos = (int[])iterator.next();
                    doc.setCharacterAttributes(pos[0], pos[1], sas, true);
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
                    regexTextArea.setColor(Color.BLUE);
                    regexTextArea.setStraight();
                }
            }

            public void textEntryResumed(JTextComponent component) {

            }
        }, 700);


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

}
