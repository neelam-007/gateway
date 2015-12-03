package com.l7tech.external.assertions.remotecacheassertion.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16/11/11
 * Time: 11:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class ServerUriEntryDialog extends JDialog {
    private JPanel mainPanel;
    private JTextField serverUriField;
    private JButton okButton;
    private JButton cancelButton;

    private boolean confirmed = false;

    public ServerUriEntryDialog(Dialog parent, String uri) {
        super(parent, "Server URI Entry", true);

        serverUriField.setText(uri == null ? "" : uri);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if input is valid
                if(serverUriField.getText().trim().length() == 0) {
                    JOptionPane.showMessageDialog(ServerUriEntryDialog.this, "Invalid server URI.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                try {
                    new URI(serverUriField.getText());
                } catch(URISyntaxException ex) {
                    JOptionPane.showMessageDialog(ServerUriEntryDialog.this, "Invalid server URI.", "Error", JOptionPane.ERROR_MESSAGE);
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

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getServerURI() {
        return serverUriField.getText();
    }
}
