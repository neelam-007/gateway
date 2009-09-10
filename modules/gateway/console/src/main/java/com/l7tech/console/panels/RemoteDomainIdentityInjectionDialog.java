package com.l7tech.console.panels;

import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.util.Pair;
import com.l7tech.gui.FilterDocument;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Properties dialog for the RemoteDomainIdentityInjection assertion.
 */
public class RemoteDomainIdentityInjectionDialog extends AssertionPropertiesEditorSupport<RemoteDomainIdentityInjection> {
    private boolean confirmed = false;
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField variablePrefixField;
    private JLabel injectedUsernameLabel;
    private JLabel injectedProgramLabel;
    private JLabel injectedNamespaceLabel;

    private final Collection<Pair<JLabel, String>> variablesSet = new ArrayList<Pair<JLabel, String>>() {{
        add(new Pair<JLabel, String>(injectedUsernameLabel, ".user"));
        add(new Pair<JLabel, String>(injectedNamespaceLabel, ".domain"));
        add(new Pair<JLabel, String>(injectedProgramLabel, ".program"));
    }};

    public RemoteDomainIdentityInjectionDialog(Window owner, RemoteDomainIdentityInjection assertion) {
        super(owner, assertion);
        initialize();
        setData(assertion);
    }

    private void initialize() {
        getContentPane().add(mainPanel);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (allGood()) {
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        variablePrefixField.setDocument(new FilterDocument(40, new FilterDocument.Filter() {
            final Pattern legal = Pattern.compile("^[a-zA-Z0-9_\\-\\.]*$");
            public boolean accept(String s) {
                return legal.matcher(s).matches();
            }
        }));
        variablePrefixField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateLabels();
            }

            public void removeUpdate(DocumentEvent e) {
                updateLabels();
            }

            public void changedUpdate(DocumentEvent e) {
                updateLabels();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        pack();
    }

    @Override
    public void setParameter( final String name, Object value ) {
        super.setParameter(name, value);
        okButton.setEnabled(!isReadOnly());
    }

    private void updateLabels() {
        String prefix = variablePrefixField.getText();
        for (Pair<JLabel, String> pair : variablesSet)
            pair.left.setText(prefix + pair.right);
    }

    private boolean allGood() {
        return variablePrefixField.getText().length() > 0;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(RemoteDomainIdentityInjection assertion) {
        variablePrefixField.setText(assertion.getVariablePrefix());
    }

    public RemoteDomainIdentityInjection getData(RemoteDomainIdentityInjection assertion) {
        assertion.setVariablePrefix(variablePrefixField.getText());
        return assertion;
    }

    private void createUIComponents() {
        variablePrefixField = new SquigglyTextField();
    }
}
