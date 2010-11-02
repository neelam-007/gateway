package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ghuang
 */
public class CancelSecurityContextPropertiesDialog extends AssertionPropertiesEditorSupport<CancelSecurityContext> {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox failIfNotExistCheckBox;
    private JCheckBox failIfExpiredCheckBox;

    private CancelSecurityContext assertion;
    private boolean confirmed;

    public CancelSecurityContextPropertiesDialog(Window owner, CancelSecurityContext assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CancelSecurityContext assertion) {
        this.assertion = assertion;
    }

    @Override
    public CancelSecurityContext getData(CancelSecurityContext assertion) {
        viewToModel(assertion);
        return assertion;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        modelToView();
    }

    private void modelToView() {
        failIfNotExistCheckBox.setSelected(assertion.isFailIfNotExist());
        failIfExpiredCheckBox.setSelected(assertion.isFailIfExpired());
    }

    private void viewToModel(CancelSecurityContext assertion) {
        assertion.setFailIfNotExist(failIfNotExistCheckBox.isSelected());
        assertion.setFailIfExpired(failIfExpiredCheckBox.isSelected());
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}