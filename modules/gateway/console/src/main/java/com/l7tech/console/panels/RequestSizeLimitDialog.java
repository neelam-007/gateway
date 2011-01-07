package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author megery
 */
public class RequestSizeLimitDialog extends LegacyAssertionPropertyDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private RequestSizeLimit sizeAssertion;
    private JTextField sizeLimit;
    private JCheckBox exemptAttachmentCheck;
    private TargetMessagePanel targetMessagePanel;
    private JPanel panel;

    private boolean modified;
    private boolean confirmed = false;

    public RequestSizeLimitDialog( final Frame owner,
                                   final RequestSizeLimit assertion,
                                   final boolean modal,
                                   final boolean readOnly ) throws HeadlessException {
        super(owner, assertion, modal);
        doInit(assertion, readOnly);
    }

    private void doInit( final RequestSizeLimit assertion,
                         final boolean readOnly ) {
        this.sizeAssertion = assertion;

        targetMessagePanel.setTitle(null);
        targetMessagePanel.setBorder(null);
        targetMessagePanel.setModel(assertion);
        targetMessagePanel.setAllowNonMessageVariables(false);

        Utilities.setMaxLength( sizeLimit.getDocument(), 1024 );
        Utilities.equalizeButtonSizes( okButton, cancelButton );

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        updateView();

        getContentPane().add(mainPanel);
    }

    private void updateView() {
        sizeLimit.setText(sizeAssertion.getLimit());
        exemptAttachmentCheck.setSelected(!sizeAssertion.isEntireMessage());
    }

    public boolean isModified() {
        return modified;
    }

    public boolean wasConfirmed() {
        return confirmed;
    }

    private void ok() {
        String limit = sizeLimit.getText();

        String error = RequestSizeLimit.validateSizeLimit(limit);
        if (error != null) {
            JOptionPane.showMessageDialog(okButton,
                                          error,
                                          "Invalid value", JOptionPane.ERROR_MESSAGE);
            return;
        }
        targetMessagePanel.updateModel(sizeAssertion);
        sizeAssertion.setLimit(limit);
        sizeAssertion.setEntireMessage(!exemptAttachmentCheck.isSelected());
        modified = true;
        confirmed = true;
        dispose();
    }

    private void doCancel() {
        modified = false;
        confirmed = false;
        dispose();
    }

    public RequestSizeLimit getAssertion() {
        return sizeAssertion;
    }
}
