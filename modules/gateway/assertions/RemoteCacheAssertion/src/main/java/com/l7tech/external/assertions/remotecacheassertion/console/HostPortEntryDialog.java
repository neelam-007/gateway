package com.l7tech.external.assertions.remotecacheassertion.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/11/11
 * Time: 4:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostPortEntryDialog extends JDialog {
    private JPanel contentPane;
    private JTextField hostField;
    private JSpinner portField;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;

    public HostPortEntryDialog(Dialog parent, String hostPort) {
        super(parent, "Host/Port Entry", true);

        portField.setModel(new SpinnerNumberModel(11211, 1, 65535, 1));

        if(hostPort != null) {
            hostField.setText(hostPort.substring(0, hostPort.indexOf(':')));
            portField.setValue(Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1)));
        }

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if input is valid
                try {
                    new InetSocketAddress(hostField.getText().trim(), ((Integer)portField.getValue()).intValue());
                } catch(IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(HostPortEntryDialog.this, "Invalid hostname and port.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch(SecurityException ex) {
                    if(JOptionPane.showConfirmDialog(HostPortEntryDialog.this, "Unable to lookup the hostname", "Error", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                        return;
                    }
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

        setContentPane(contentPane);
        pack();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        return ((Integer)portField.getValue()).intValue();
    }
}
