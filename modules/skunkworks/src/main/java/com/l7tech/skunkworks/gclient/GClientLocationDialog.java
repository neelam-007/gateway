package com.l7tech.skunkworks.gclient;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 * @author $Author$
 * @version $Revision$
 */
public class GClientLocationDialog extends JDialog {

    private JTextField locationTextField;
    private JButton cancelButton;
    private JButton okButton;
    private boolean wasOk;
    private JPanel mainPanel;

    public GClientLocationDialog(Frame parent) {
        super(parent, "Open Location ...", true);
        this.setContentPane(mainPanel);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.wasOk = false;
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wasOk = true;
                GClientLocationDialog.this.dispose();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GClientLocationDialog.this.dispose();
            }
        });
    }

    public boolean wasOk() {
        return wasOk;
    }

    public String getOpenLocation() {
        return locationTextField.getText();
    }
}
