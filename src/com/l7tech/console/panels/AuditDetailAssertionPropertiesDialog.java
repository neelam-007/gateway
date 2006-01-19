package com.l7tech.console.panels;

import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;
import com.l7tech.console.action.Actions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

/**
 * Properties dialog for the Audit detail assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertionPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField detailTextField;
    private JComboBox levelComboBox;
    private JButton cancelButton;
    private JButton okButton;
    private AuditDetailAssertion subject;
    private boolean cancelled;

    public AuditDetailAssertionPropertiesDialog(Frame owner, AuditDetailAssertion subject) {
        super(owner, true);
        this.subject = subject;
        initialize();
    }

    private void initialize() {
        cancelled = true;
        setContentPane(mainPanel);
        setTitle("Audit Detail Properties");
        levelComboBox.setModel(new DefaultComboBoxModel(AuditAssertion.ALLOWED_LEVELS));
        levelComboBox.setSelectedItem(subject.getLevel().toString());
        detailTextField.setText(subject.getDetail());

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        Actions.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        Actions.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
    }

    private void ok() {
        cancelled = false;
        if (detailTextField.getText() == null || detailTextField.getText().length() < 1) {
            JOptionPane.showMessageDialog(okButton,
                                          "Audit detail message cannot be empty",
                                          "Invalid Audit Detail",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        subject.setDetail(detailTextField.getText());
        subject.setLevel(Level.parse((String)levelComboBox.getSelectedItem()));
        cancel();
    }

    private void cancel() {
        dispose();
    }

    public boolean isModified() {
        return !cancelled;
    }
}
