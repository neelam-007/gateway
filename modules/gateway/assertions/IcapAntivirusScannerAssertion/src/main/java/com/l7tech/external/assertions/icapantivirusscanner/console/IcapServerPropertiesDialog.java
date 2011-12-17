package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAdmin;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;

/**
 * <p>The GUI for adding/modifying the server connection information.</p>
 *
 * @author Ken Diep
 */
public final class IcapServerPropertiesDialog extends JDialog {


    private JPanel contentPane;

    private JTextField icapServerUrlField;

    private JButton btnCancel;
    private JButton btnOk;

    private JLabel icapServerUrl;
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
                    final String[] referencedNames;
                    try {
                        referencedNames = Syntax.getReferencedNames(icapServerUrlField.getText().trim());
                    } catch (VariableNameSyntaxException e1) {
                        DialogDisplayer.showMessageDialog(owner,
                                "Invalid context variable reference in ICAP Server URL",
                                "WARNING",
                                JOptionPane.PLAIN_MESSAGE, null);
                        return;
                    }
                    if (referencedNames.length > 0) {
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

    private void testServerEntry() {
        try {
            IcapAntivirusScannerAdmin admin = Registry.getDefault().getExtensionInterface(IcapAntivirusScannerAdmin.class, null);
            admin.testConnection(icapServerUrlField.getText().trim());
            DialogDisplayer.showMessageDialog(this,
                    "Connection is successful.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE, null);
        } catch (Exception e) {
            String msg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            DialogDisplayer.showMessageDialog(this,
                    msg,
                    "Connection failed",
                    JOptionPane.ERROR_MESSAGE, null);
        }
    }

    public String getIcapUri() {
        return icapServerUrlField.getText().trim();
    }

    private boolean validateData() {
        String icapServerUrl = icapServerUrlField.getText().trim();
        if (icapServerUrl.isEmpty()) {
            DialogDisplayer.showMessageDialog(this,
                    "Please enter an ICAP Server URL",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        final String[] referencedNames;
        try {
            referencedNames = Syntax.getReferencedNames(icapServerUrl);
        } catch (VariableNameSyntaxException e) {
            DialogDisplayer.showMessageDialog(this,
                    "Invalid context variable reference in ICAP Server URL",
                    "Error",
                    JOptionPane.ERROR_MESSAGE, null);

            return false;
        }

        if (referencedNames.length == 0) {
            final Matcher matcher = IcapAntivirusScannerAssertion.ICAP_URI.matcher(icapServerUrl);
            if (!matcher.matches()) {
                DialogDisplayer.showMessageDialog(this,
                        "ICAP Server URL must begin with 'icap'",
                        "Error",
                        JOptionPane.ERROR_MESSAGE, null);
                return false;
            }

            final String nonSchemePart = matcher.group(1);
            //validate URL
            try {
                new URL("http" + nonSchemePart);
            } catch (MalformedURLException e) {
                DialogDisplayer.showMessageDialog(this,
                        "Invalid ICAP Server URL: " + ExceptionUtils.getMessage(e),
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

    public void setIcapServerURL(final String icapServerURL) {
        icapServerUrlField.setText(icapServerURL);
    }
}
