package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Regex;
import com.l7tech.console.event.BeanEditSupport;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

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
    private JTextArea testResultTextArea;
    private JScrollPane testResultTextAreaScrollPane;
    private Regex regexAssertion;
    private BeanEditSupport beanEditSupport = new BeanEditSupport(this);
    private Pattern pattern = null;
    private JButton clearTestOutputButton;
    private JCheckBox caseInsensitivecheckBox;

    public RegexDialog(Frame owner, Regex regexAssertion) throws HeadlessException {
        super(owner, true);
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

        if (regexAssertion.getReplacement() != null) {
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
                beanEditSupport.fireEditAccepted(regexAssertion);
            }
        });

        testButton.setEnabled(!empty(testInputTextArea) && pattern != null);

        testButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updatePattern();
                Matcher matcher = pattern.matcher(testInputTextArea.getText());
                testResultTextArea.setText(matcher.replaceAll(replaceTextArea.getText()));
            }
        });

        clearTestOutputButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                testResultTextArea.setText("");
            }
        });
        regexTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern !=null);
            }

            public void removeUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern !=null);
            }

            public void changedUpdate(DocumentEvent e) {
                updatePattern();
                okButton.setEnabled(pattern !=null);
            }

        });

        testInputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                testButton.setEnabled(!empty(testInputTextArea) && pattern != null);
            }

            public void removeUpdate(DocumentEvent e) {
                testButton.setEnabled(!empty(testInputTextArea) && pattern != null);
            }

            public void changedUpdate(DocumentEvent e) {
                testButton.setEnabled(!empty(testInputTextArea) && pattern != null);
            }
        });

        updatePattern();
        okButton.setEnabled(pattern !=null);

    }


    public BeanEditSupport getBeanEditSupport() {
        return beanEditSupport;
    }

    private boolean empty(JTextComponent tc) {
        return tc.getText() == null || "".equals(tc.getText());
    }


    private void updatePattern() {
        if (!empty(regexTextArea)) {
            try {
                int flags = Pattern.DOTALL | Pattern.MULTILINE;
                if (caseInsensitivecheckBox.isSelected()) {
                    flags |= Pattern.CASE_INSENSITIVE;
                }
                pattern = Pattern.compile(regexTextArea.getText(), flags);
            } catch (PatternSyntaxException e1) {
                pattern = null;
            }
        } else {
            pattern = null;
        }
    }

}
