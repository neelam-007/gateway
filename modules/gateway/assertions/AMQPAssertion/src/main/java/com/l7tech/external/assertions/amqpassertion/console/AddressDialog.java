package com.l7tech.external.assertions.amqpassertion.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddressDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField hostnameField;
    private JSpinner portField;
    private JButton okButton;
    private JButton cancelButton;

    private InetSocketAddress address;
    private boolean confirmed = false;

    public AddressDialog(Dialog owner) {
        super(owner, "Address", true);
        initComponents();
    }

    private void initComponents() {
        portField.setModel(new SpinnerNumberModel(5672, 1, 65535, 1));

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    new InetSocketAddress(hostnameField.getText().trim(), ((Number) portField.getValue()).intValue());
                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(AddressDialog.this, "Invalid hostname or port number.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                confirmed = true;
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        setContentPane(mainPanel);
        pack();
    }

    public String getData() {
        return hostnameField.getText().trim() + ":" + portField.getValue();
    }

    public void setData(String data) {

        String[] temp = data.split(":");
        hostnameField.setText(temp[0]);
        portField.setValue(Integer.parseInt(temp[1]));
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
