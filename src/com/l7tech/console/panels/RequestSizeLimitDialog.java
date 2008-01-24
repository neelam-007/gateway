package com.l7tech.console.panels;

import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 2:32:47 PM
 */
public class RequestSizeLimitDialog extends JDialog {
    private static final String TITLE = "Request Size Limit";
    private final InputValidator validator = new InputValidator(this, TITLE);
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private RequestSizeLimit sizeAssertion;
    private JTextField sizeLimit;
    private JCheckBox exemptAttachmentCheck;

    private boolean modified;
    private boolean confirmed = false;

    public RequestSizeLimitDialog(Frame owner, RequestSizeLimit assertion, boolean modal, boolean readOnly) throws HeadlessException {
        super(owner, TITLE, modal);
        doInit(assertion, readOnly);
    }

    private void doInit(RequestSizeLimit assertion, boolean readOnly) {
        this.sizeAssertion = assertion;
        sizeLimit.setDocument(new NumberField(String.valueOf(Long.MAX_VALUE).length()));

        validator.constrainTextFieldToNumberRange("size limit", sizeLimit, 1, Long.MAX_VALUE / 1024);
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

        okButton.setEnabled( !readOnly );
        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancel();
            }
        });

        updateView();

        getContentPane().add(mainPanel);
    }

    private void updateView() {
        long size = sizeAssertion.getLimit() / 1024;
        if (size < 1) {
            size = 1;
        }
        sizeLimit.setText(String.valueOf(size));
        exemptAttachmentCheck.setSelected(!sizeAssertion.isEntireMessage());
    }

    public boolean isModified() {
        return modified;
    }

    public boolean wasConfirmed() {
        return confirmed;
    }

    private void doSave() {
        long limit = Long.parseLong(sizeLimit.getText());
        if (limit < 1) {
            limit = 1;
        }
        sizeAssertion.setLimit(limit*1024);
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
