package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;

/**
 * <p>The GUI for adding/modifying the server connection information.</p>
 *
 * @author Ken Diep
 */
public final class IcapServerPropertiesDialog extends JDialog {
    private static final int MAX_PORT = 65535;
    private JPanel contentPane;

    private JTextField serverHostnameField;
    private JTextField serverPortNumberField;
    private JTextField serverServiceNameField;
    private JTextField serverTimeoutField;

    private JButton btnCancel;
    private JButton btnOk;

    private JLabel lbHost;
    private JLabel lbPort;
    private JLabel lbServiceName;
    private JLabel serverTimeoutLabel;

    private IcapConnectionDetail connectionDetail;
    private boolean confirmed;

    private static final Pattern NUMERIC_VALUE = Pattern.compile("\\d+");

    public IcapServerPropertiesDialog(Frame owner, String title) {
        super(owner, title, true);
        initComponents();
    }

    private void initComponents() {
        btnOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (validateData()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmed = false;
                dispose();
            }
        });
        getRootPane().setDefaultButton(btnOk);
        setContentPane(createPropertyPanel());
    }

    private boolean validateData() {
        String hostname = serverHostnameField.getText().trim();
        if (hostname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid hostname.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String serviceName = serverServiceNameField.getText().trim();
        if (serviceName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid service name.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String portText = serverPortNumberField.getText().trim();
        if (!NUMERIC_VALUE.matcher(portText).matches()) {
            JOptionPane.showMessageDialog(this, "Please specify a port number", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        int port = Integer.parseInt(portText);
        if (port < 1 || port > MAX_PORT) {
            JOptionPane.showMessageDialog(this, "Port number must be between 1 and 65535.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        String timeoutText = serverTimeoutField.getText().trim();
        if (!NUMERIC_VALUE.matcher(timeoutText).matches()) {
            JOptionPane.showMessageDialog(this, "Please specify a timeout value", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        int timeout = Integer.parseInt(timeoutText);
        if (timeout < 1) {
            JOptionPane.showMessageDialog(this, "Timeout value must be greater than zero (0)", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        connectionDetail = new IcapConnectionDetail();
        connectionDetail.setHostname(hostname);
        connectionDetail.setServiceName(serviceName);
        connectionDetail.setPort(port);
        connectionDetail.setTimeout(timeout);
        return true;
    }


    private JPanel createPropertyPanel() {
        return contentPane;
    }

    /**
     * @return true if the OK button was clicked, false otherwise.
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * @return the {@link com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail} which contains
     *         the configured server information.
     */
    public IcapConnectionDetail getConnectionDetail() {
        return connectionDetail;
    }

    /**
     * @param connectionDetail the server information to restore to the GUI.
     */
    public void setViewData(IcapConnectionDetail connectionDetail) {
        serverHostnameField.setText(connectionDetail.getHostname());
        serverPortNumberField.setText(String.valueOf(connectionDetail.getPort()));
        serverServiceNameField.setText(connectionDetail.getServiceName());
        serverTimeoutField.setText(String.valueOf(connectionDetail.getTimeout()));
    }


}
