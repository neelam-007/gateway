package com.l7tech.external.assertions.stripparts.console;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.external.assertions.stripparts.StripPartsAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class StripPartsPropertiesDialog extends JDialog implements AssertionPropertiesEditor<StripPartsAssertion> {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton rbRequest;
    private JRadioButton rbReply;

    private boolean confirmed = false;

    public StripPartsPropertiesDialog(Frame parent) {
        super(parent, "Strip Parts Properties", true);
        init();
    }

    private void init() {
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(StripPartsAssertion assertion) {
        boolean isReq = assertion.isActOnRequest();
        rbRequest.setSelected(isReq);
        rbReply.setSelected(!isReq);
        confirmed = false;
    }

    public StripPartsAssertion getData(StripPartsAssertion assertion) {
        assertion.setActOnRequest(rbRequest.isSelected());
        return assertion;
    }

    public void setParameter(String name, Object value) {
    }

    public Object getParameter(String name) {
        return null;
    }
}