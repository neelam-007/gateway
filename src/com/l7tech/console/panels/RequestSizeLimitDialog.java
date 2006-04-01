package com.l7tech.console.panels;

import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.common.gui.NumberField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.InputValidator;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 2:32:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSizeLimitDialog extends JDialog {
    private static final String TITLE = "Request Size Limit";
    private final InputValidator validator = new InputValidator(this, TITLE);
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private RequestSizeLimit sizeAssertion;
    private JTextField sizeLimit;
    private JCheckBox applyToWholeMessage;

    private boolean modified;
    private boolean confirmed = false;

    public RequestSizeLimitDialog(Frame owner, RequestSizeLimit assertion, boolean modal) throws HeadlessException {
        super(owner, TITLE, modal);
        doInit(assertion);
    }

    private void doInit(RequestSizeLimit assertion) {
        this.sizeAssertion = assertion;
        sizeLimit.setDocument(new NumberField(String.valueOf(Long.MAX_VALUE).length()));

        final DocumentListener dl = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateView(); }
            public void insertUpdate(DocumentEvent e) { updateView(); }
            public void removeUpdate(DocumentEvent e) { updateView(); }
        };


        validator.constrainTextFieldToNumberRange("size limit", sizeLimit, 1, Long.MAX_VALUE / 1024);
        Utilities.equalizeButtonSizes(new AbstractButton[]{okButton, cancelButton});

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
        applyToWholeMessage.setSelected(sizeAssertion.isEntireMessage());
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
        sizeAssertion.setEntireMessage(applyToWholeMessage.isSelected());
        modified = true;
        confirmed = true;
        setVisible(false);
    }

    private void doCancel() {
        modified = false;
        confirmed = false;
        setVisible(false);
    }

    public RequestSizeLimit getAssertion() {
        return sizeAssertion;
    }
}
