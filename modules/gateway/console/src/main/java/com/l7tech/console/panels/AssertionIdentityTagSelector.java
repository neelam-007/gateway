package com.l7tech.console.panels;

import com.l7tech.policy.assertion.IdentityTagable;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.regex.Pattern;

public class AssertionIdentityTagSelector extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;

    private IdentityTagable assertion;
    private JTextField _identityTag;
    private boolean readonly;
    private boolean wasOKed;

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("[a-zA-Z0-9 _\\-,.]*");


    public AssertionIdentityTagSelector(Frame owner, IdentityTagable assertion, boolean readonly) {
        super(owner, true);
        this.assertion = assertion;
        this.readonly = readonly;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setTitle("Change Identity Tag");
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        buttonOK.setEnabled(!readonly);

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        updateView();
    }

    private void updateView() {
        _identityTag.setText(assertion.getIdentityTag());
    }

    private void onOK() {
        String text = _identityTag.getText();
        if (text != null && ! ALLOWED_PATTERN.matcher(text).matches() ) {
            JOptionPane.showMessageDialog(this, "Only alphanumeric, space, underline, dash, dot and comma characters are allowed!");
            _identityTag.grabFocus();
            return;
        }
        assertion.setIdentityTag(_identityTag.getText());
        wasOKed = true;
        dispose();
    }

    public boolean hasAssertionChanged() {
        return wasOKed;
    }

    private void onCancel() {
        dispose();
    }
}
