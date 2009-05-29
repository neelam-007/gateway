package com.l7tech.console.panels;

import com.l7tech.policy.assertion.IdentityTagable;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.regex.Pattern;
import java.util.*;

/**
 * Dialog for configuration of an identity tag.
 */
public class AssertionIdentityTagSelector extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;

    private IdentityTagable identityTagable;
    private JTextField _identityTag;
    private boolean readonly;
    private boolean wasOKed;

    private static final Pattern ALLOWED_PATTERN = Pattern.compile("[a-zA-Z0-9 _\\-,.]*");
    private static final ResourceBundle bundle = ResourceBundle.getBundle(AssertionIdentityTagSelector.class.getName());

    public AssertionIdentityTagSelector( final Window owner,
                                         final IdentityTagable identityTagable,
                                         final boolean readonly ) {
        super(owner, AssertionIdentityTagSelector.DEFAULT_MODALITY_TYPE);
        this.identityTagable = identityTagable;
        this.readonly = readonly;
        initialize();
    }

    private void initialize() {
        setContentPane(contentPane);
        setTitle(bundle.getString("dialog.title"));
        getRootPane().setDefaultButton(buttonOK);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

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

        Utilities.setEscKeyStrokeDisposes(this);
        updateView();
    }

    private void updateView() {
        _identityTag.setText(identityTagable.getIdentityTag());
    }

    private void onOK() {
        String text = _identityTag.getText();
        if (text != null && ! ALLOWED_PATTERN.matcher(text).matches() ) {
            DialogDisplayer.showMessageDialog(this,
                    bundle.getString("error.characters"),
                    bundle.getString("error.title"),
                    JOptionPane.INFORMATION_MESSAGE,
                    new Runnable(){
                        @Override
                        public void run() {
                            _identityTag.grabFocus();
                        }
                    });
        } else {
            identityTagable.setIdentityTag(_identityTag.getText().trim());
            wasOKed = true;
            dispose();
        }
    }

    public boolean hasAssertionChanged() {
        return wasOKed;
    }

    private void onCancel() {
        dispose();
    }
}
