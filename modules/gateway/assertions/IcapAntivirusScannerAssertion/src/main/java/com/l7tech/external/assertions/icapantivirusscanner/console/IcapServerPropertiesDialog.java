package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import static com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion.getServiceName;

/**
 * <p>The GUI for adding/modifying the server connection information.</p>
 *
 * @author Ken Diep
 */
public final class IcapServerPropertiesDialog extends JDialog {


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
        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (validateData()) {
                    confirmed = true;
                    dispose();
                }
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            @Override
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
                        DialogDisplayer.showMessageDialog(owner,
                                "Unable to test connection containing context variable(s).",
                                "WARNING",
                                JOptionPane.PLAIN_MESSAGE, null);
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
        boolean enableSave = false;
        try {
            IcapAntivirusScannerAdmin admin = Registry.getDefault().getExtensionInterface(IcapAntivirusScannerAdmin.class, null);
            admin.testConnection(serverHostnameField.getText().trim(), Integer.parseInt(serverPortNumberField.getText().trim()), getServiceName(serverServiceNameField.getText()));
            DialogDisplayer.showMessageDialog(this,
                    "Connection is successful.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE, null);
            enableSave = true;
        } catch (Exception e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            DialogDisplayer.showMessageDialog(this,
                    msg,
                    "Connection failed",
                    JOptionPane.ERROR_MESSAGE, null);
        }
        btnOk.setEnabled(enableSave);
    }

    public String getIcapUri() {
        final String path = getServiceName(serverServiceNameField.getText());
        final String hostName = InetAddressUtil.getHostForUrl(serverHostnameField.getText().trim());

        return new StringBuilder("icap://").append(hostName).append(":")
                .append(serverPortNumberField.getText().trim()).append("/").
                        append(path).toString();
    }

    private boolean validateData() {
        String hostname = serverHostnameField.getText().trim();
        if (hostname.isEmpty()) {
            DialogDisplayer.showMessageDialog(this,
                    "Please enter valid Hostname.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (Syntax.getReferencedNames(hostname).length != 0) {
            final boolean onlyVarRef = Syntax.validateStringOnlyReferencesVariables(hostname);
            if (!onlyVarRef) {
                DialogDisplayer.showMessageDialog(this,
                        "Hostname may not reference a variable and text.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
        } else {
            final String maybeUpdatedHostname = InetAddressUtil.getHostForUrl(hostname);
            //Make a URL with this hostname
            try {
                new URL("http://" + maybeUpdatedHostname);
            } catch (MalformedURLException e) {
                DialogDisplayer.showMessageDialog(this,
                        "Invalid Hostname: " + ExceptionUtils.getMessage(e),
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
        }

        String serviceName = serverServiceNameField.getText().trim();
        if (serviceName.isEmpty()) {
            DialogDisplayer.showMessageDialog(this,
                    "Please enter valid service name.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (Syntax.getReferencedNames(serviceName).length != 0) {
            final boolean onlyVarRef = Syntax.validateStringOnlyReferencesVariables(serviceName);
            if (!onlyVarRef) {
                DialogDisplayer.showMessageDialog(this,
                        "Service Name may not reference a variable and text.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
        }

        String portText = serverPortNumberField.getText().trim();
        if (portText.isEmpty() || (!isContextVariable(portText)) && !ValidationUtils.isValidInteger(portText, false, 1, 65535)) {
            DialogDisplayer.showMessageDialog(this,
                    "Please enter a port number between 1 and 65535.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (Syntax.getReferencedNames(portText).length != 0) {
            final boolean onlyVarRef = Syntax.validateStringOnlyReferencesVariables(portText);
            if (!onlyVarRef) {
                DialogDisplayer.showMessageDialog(this,
                        "Port Number may not reference a variable and text.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return false;
            }
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

    public void setServiceName(@NotNull final String serviceName) {
        serverServiceNameField.setText(getServiceName(serviceName));
    }

    public void setPort(String port) {
        serverPortNumberField.setText(port);
    }
}
