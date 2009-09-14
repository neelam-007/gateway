package com.l7tech.console.panels;

import com.l7tech.policy.assertion.transport.PreemptiveCompression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * @author flascell<br/>
 */
public class PreemptiveCompressionDialog extends LegacyAssertionPropertyDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton OKButton;
    private JCheckBox serverSideCheck;
    private PreemptiveCompression assertion;
    private boolean readOnly = false;
    private boolean oked = false;

    public PreemptiveCompressionDialog(Frame owner, PreemptiveCompression assertion, boolean readOnly) {
        super(owner, assertion, true);
        this.assertion = assertion;
        this.readOnly = readOnly;
        initialize();
    }

    private void initialize() {
        getContentPane().add(mainPanel);

        OKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                assertion.setServerSideCheck(serverSideCheck.isSelected());
                oked = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        serverSideCheck.setSelected(assertion.isServerSideCheck());

        OKButton.setEnabled(!readOnly);
    }

    public boolean wasOKed() {
        return oked;
    }
}
