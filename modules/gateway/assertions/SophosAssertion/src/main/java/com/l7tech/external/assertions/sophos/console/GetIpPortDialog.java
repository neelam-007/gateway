package com.l7tech.external.assertions.sophos.console;

import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class GetIpPortDialog extends JDialog {
    private JPanel contentPane;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;
    public static final int DEFAULT_PORT_SOPHOS = 4000;
    private static final ResourceBundle resources = ResourceBundle.getBundle( GetIpPortDialog.class.getName() );

    public GetIpPortDialog(Frame owner, String title, String host, int port) {
        super(owner, title, true);
        initComponents(host, port);
    }

    public GetIpPortDialog(Dialog owner, String title, String host, int port) {
        super(owner, title, true);
        initComponents(host, port);
    }

    private void initComponents(String host, int port) {
        setContentPane(contentPane);
        setModal(true);

        hostField.setText((host == null) ? "" : host);

        portSpinner.setModel(new SpinnerNumberModel(port, 0, 32767, 1));

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(hostField.getText().trim().length() == 0) {
                    JOptionPane.showMessageDialog(GetIpPortDialog.this, getResourceString("hostNameError"), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (!isPortValid() ){
                    JOptionPane.showMessageDialog(GetIpPortDialog.this, MessageFormat.format(getResourceString("portError"), DEFAULT_PORT_SOPHOS), "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                confirmed = true;
                setVisible(false);
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                confirmed = false;
                setVisible(false);
            }
        });

        getRootPane().setDefaultButton(okButton);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getHost() {
        return hostField.getText().trim();
    }

    public int getPort() {
        return ((Number)portSpinner.getValue()).intValue();
    }

    public static void main(String[] args) {
        GetIpPortDialog dialog = new GetIpPortDialog((Frame)null, "Add IP/Port", null, DEFAULT_PORT_SOPHOS);
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
    /**
     * @return Return true iff the port number is between 1 and 65535 or references a context variable.
     */
    private boolean isPortValid() {
        boolean isValid;
        String portStr = portSpinner.getValue().toString();
        try {
            int port = Integer.parseInt(portStr);
            isValid = port > 0 && port < 65535;
        } catch (NumberFormatException e) {
            // must be using context variable
            isValid = Syntax.getReferencedNames(portStr).length > 0;
        }
        return isValid;
    }

    private String getResourceString(String key){
        final String value = resources.getString(key);
        if(value.endsWith(":")){
            return value.substring(0, value.lastIndexOf(":"));
        }
        return value;
    }
}
