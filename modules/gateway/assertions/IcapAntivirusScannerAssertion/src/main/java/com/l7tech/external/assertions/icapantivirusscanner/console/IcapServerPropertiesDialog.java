package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>The GUI for adding/modifying the server connection information.</p>
 *
 * @author Ken Diep
 */
public final class IcapServerPropertiesDialog extends JDialog {
    private static final Pattern STATUS_LINE = Pattern.compile("(?i)ICAP/1\\.0\\s(\\d+)\\s(.*)");

    private JPanel contentPane;

    private JTextField serverHostnameField;
    private JTextField serverPortNumberField;
    private JTextField serverServiceNameField;

    private JButton btnCancel;
    private JButton btnOk;

    private JLabel lbHost;
    private JLabel lbPort;
    private JLabel lbServiceName;
    private JButton testConnectionButton;

    private boolean confirmed;

    public IcapServerPropertiesDialog(final Window owner, final String title) {
        super(owner, title);
        initComponents(owner);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(btnOk);
    }

    private void initComponents(final Window owner) {
        Utilities.setEscKeyStrokeDisposes(this);
        btnOk.setEnabled(false);
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
        testConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (validateData()) {
                    if (isContextVariable(serverHostnameField.getText()) ||
                            isContextVariable(serverPortNumberField.getText()) ||
                            isContextVariable(serverServiceNameField.getText())) {
                        JOptionPane.showMessageDialog(owner, "Unable to test connection containing context variable(s).", "WARNING", JOptionPane.PLAIN_MESSAGE);
                        btnOk.setEnabled(true);
                    } else {
                        testServerEntry();
                    }
                }
            }
        });
    }

    private boolean isContextVariable(final String text) {
        return Syntax.getReferencedNames(text).length > 0;
    }

    private void testServerEntry() {
        Socket sock = new Socket();
        OutputStream out = null;
        BufferedReader br = null;
        try {
            //testing the server is trivial enough that we can do it by hand.
            //using the netty & icap framework is rather expensive
            sock.connect(new InetSocketAddress(serverHostnameField.getText(), Integer.parseInt(serverPortNumberField.getText().trim())), 30000);
            out = sock.getOutputStream();
            out.write(String.format("OPTIONS %1$s %2$s %3$s%3$s", getIcapUri(), "ICAP/1.0", "\r\n").getBytes());
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            String line = br.readLine();
            Matcher matcher = STATUS_LINE.matcher(line);
            if (matcher.matches() && "200".equals(matcher.group(1))) {
                btnOk.setEnabled(true);
            } else {
                //unknown error, icap says first line must be status line
                JOptionPane.showMessageDialog(this, "ICAP server error: " + line,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + ex, "Error", JOptionPane.ERROR_MESSAGE);
            btnOk.setEnabled(false);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (sock.isConnected()) {
                    sock.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                //ignore
            }
        }
    }

    public String getIcapUri() {
        return new StringBuilder("icap://").append(serverHostnameField.getText().trim()).append(":")
                .append(serverPortNumberField.getText().trim()).append("/").
                        append(serverServiceNameField.getText().trim()).toString();
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
        if (portText.isEmpty() || (!isContextVariable(portText)) && !ValidationUtils.isValidInteger(portText, false, 1, 65535)) {
            JOptionPane.showMessageDialog(this, "Please enter a port number between 1 and 65535..", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
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

    public void setHostname(final String hostname) {
        serverHostnameField.setText(hostname);
    }

    public void setServiceName(final String serviceName) {
        serverServiceNameField.setText(serviceName);
    }

    public void setPort(String port) {
        serverPortNumberField.setText(port);
    }
}
