package com.l7tech.skunkworks.gclient;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 *
 */
public class GClientDecorationDialog extends JDialog {
    private JTextArea textArea1;
    private JButton cancelButton;
    private JButton decorateButton;
    private JPanel mainPanel;

    private boolean confirmed;


    public GClientDecorationDialog(Frame owner) {
        super(owner);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        pack();

        confirmed = false;

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                setVisible(false);
            }
        });

        decorateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                setVisible(false);
            }
        });

    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setPolicyXml(String xml) {
        textArea1.setText(xml);
    }

    public String getPolicyXml() {
        return textArea1.getText();
    }
}
