package com.l7tech.console.panels;

import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ManageKeystoresDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ManageKeystoresDialog.class.getName());

    private static final String PROVIDER_NAME_LUNA = "luna";
    private static final String PROP_PROVIDER_NAME = "security.jceProviderEngineName";

    private JPanel mainPanel;
    private JButton closeButton;
    private JLabel currentKeystoreTypeLabel;
    private JButton disableLunaButton;
    private JButton enableLunaButton;
    private JLabel lunaKeystoreAvailabilityLabel;

    private final boolean canUseLuna;
    private final boolean configuredToUseLuna;
    private final boolean currentlyUsingLuna;

    public ManageKeystoresDialog(Window owner, boolean currentlyUsingLuna) {
        super(owner, "Manage Keystore", ModalityType.DOCUMENT_MODAL);
        this.currentlyUsingLuna = currentlyUsingLuna;
        this.canUseLuna = "true".equals(Registry.getDefault().getClusterStatusAdmin().getHardwareCapability(ClusterStatusAdmin.CAPABILITY_LUNACLIENT));
        this.configuredToUseLuna = PROVIDER_NAME_LUNA.equals(getClusterProperty(PROP_PROVIDER_NAME));

        init();
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    public String getClusterProperty(String propname) {
        try {
            return ClusterPropertyCrud.getClusterProperty(propname);
        } catch (FindException e) {
            showErrorMessage("Unable to Read Cluster Property", "Unable to read cluster property " + propname + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    public void putClusterProperty(String propname, String value) {
        try {
            ClusterPropertyCrud.putClusterProperty(propname, value);
        } catch (ObjectModelException e) {
            showErrorMessage("Unable to Set Cluster Property", "Unable to set cluster property " + propname + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ManageKeystoresDialog.this.dispose();
            }
        });

        enableLunaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEnableLuna();
            }
        });

        disableLunaButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDisableLuna();
            }
        });

        if (configuredToUseLuna || currentlyUsingLuna) {
            if (configuredToUseLuna) {
                if (currentlyUsingLuna) {
                    currentKeystoreTypeLabel.setText("SafeNet HSM");
                } else {
                    currentKeystoreTypeLabel.setText("Configured for SafeNet HSM, but using system default");
                }
            } else {
                currentKeystoreTypeLabel.setText("Configured for system default, but using SafeNet HSM");
            }
        } else {
            currentKeystoreTypeLabel.setText("System default");
        }

        if (canUseLuna) {
            lunaKeystoreAvailabilityLabel.setText("Ready to use");
        } else {
            lunaKeystoreAvailabilityLabel.setText("Client software and JSP not installed or not configured");
        }

        enableLunaButton.setEnabled(canUseLuna && !currentlyUsingLuna);
        enableLunaButton.setVisible(enableLunaButton.isEnabled());
        disableLunaButton.setEnabled(currentlyUsingLuna || configuredToUseLuna);
        disableLunaButton.setVisible(disableLunaButton.isEnabled());

        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(closeButton);
    }

    private void doEnableLuna() {
        final EnableLunaDialog dlg = new EnableLunaDialog(this);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConnectionSucceeded())
                    ManageKeystoresDialog.this.dispose();
            }
        });
    }

    private void doDisableLuna() {
        DialogDisplayer.showConfirmDialog(this,
                "This will disconnect the Gateway cluster from the SafeNet HSM,\n" +
                "causing it to revert to its system default keystore on next restart.\n" +
                "\n\nAre you sure you want to disconnect from the SafeNet HSM?",
                "Disconnect from SafeNet HSM",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.YES_OPTION) {
                            putClusterProperty(PROP_PROVIDER_NAME, null);
                            DialogDisplayer.showMessageDialog(ManageKeystoresDialog.this,
                                    "Cluster disconnected from SafeNet HSM.  All cluster nodes must be restarted for this to take effect.",
                                    "Disconnected from SafeNet HSM", JOptionPane.INFORMATION_MESSAGE, new Runnable() {
                                        @Override
                                        public void run() {
                                            ManageKeystoresDialog.this.dispose();                                            
                                        }
                                    });
                        }
                    }
                });
    }
}
