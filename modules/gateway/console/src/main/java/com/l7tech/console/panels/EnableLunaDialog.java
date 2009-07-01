package com.l7tech.console.panels;

import com.l7tech.console.util.ClusterPropertyCrud;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.KeyStoreException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class EnableLunaDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(EnableLunaDialog.class.getName());

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton connectButton;
    private JCheckBox overrideSlotNumberCheckBox;
    private JTextField slotNumberField;
    private JPasswordField clientPinField;

    private final InputValidator validator;
    private boolean connectionSucceeded = false;

    public EnableLunaDialog(Window owner) {
        super(owner, "Connect to SafeNet HSM", ModalityType.DOCUMENT_MODAL);
        validator = new InputValidator(this, "Connect to SafeNet HSM");
        init();
    }

    private void init() {
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                EnableLunaDialog.this.dispose();
            }
        });

        validator.attachToButton(connectButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doConnect();
            }
        });

        validator.constrainTextFieldToBeNonEmpty("client PIN", clientPinField, null);
        validator.constrainTextFieldToNumberRange("slot number", slotNumberField, 1, 99);
        Utilities.enableGrayOnDisabled(slotNumberField);
        overrideSlotNumberCheckBox.setSelected(false);
        overrideSlotNumberCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisable();
            }
        });

        enableDisable();

        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(connectButton);        
    }

    private void enableDisable() {
        slotNumberField.setEnabled(overrideSlotNumberCheckBox.isSelected());
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private void doConnect() {
        int slotnum = overrideSlotNumberCheckBox.isSelected() ? Integer.valueOf(slotNumberField.getText()) : -1;
        char[] clientPin = clientPinField.getPassword();

        try {
            Registry.getDefault().getClusterStatusAdmin().testHardwareTokenAvailability(ClusterStatusAdmin.CAPABILITY_LUNACLIENT, slotnum, clientPin);
        } catch (ClusterStatusAdmin.NoSuchCapabilityException e) {
            showErrorMessage("Connection Failed", "Unable to connect to SafeNet HSM: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return;
        } catch (KeyStoreException e) {
            showErrorMessage("Connection Failed", "Unable to connect to SafeNet HSM: " + ExceptionUtils.getMessage(e), e);
            return;
        }

        try {
            ClusterPropertyCrud.putClusterProperty("keyStore.luna.lunaSlotNum", slotnum >= 0 ? Integer.toString(slotnum) : null);
            Registry.getDefault().getClusterStatusAdmin().putHardwareCapabilityProperty(ClusterStatusAdmin.CAPABILITY_LUNACLIENT, ClusterStatusAdmin.CAPABILITY_PROPERTY_LUNAPIN, clientPin);
            ClusterPropertyCrud.putClusterProperty("security.jceProviderEngineName", "luna");
            connectionSucceeded = true;
            DialogDisplayer.showMessageDialog(this, "Connection to SafeNet HSM succeeded.  All Gateway nodes must be restarted for this to take effect.",
                    "Connection Succeeded",
                    JOptionPane.INFORMATION_MESSAGE,
                    new Runnable() {
                        @Override
                        public void run() {
                            EnableLunaDialog.this.dispose();
                        }
                    });
        } catch (ObjectModelException e) {
            showErrorMessage("Connection Failed", "Unable to configure SafeNet HSM support: " + ExceptionUtils.getMessage(e), e);
        } catch (ClusterStatusAdmin.NoSuchPropertyException e) {
            showErrorMessage("Connection Failed", "Unable to configure SafeNet HSM support: " + ExceptionUtils.getMessage(e), e);
        } catch (ClusterStatusAdmin.NoSuchCapabilityException e) {
            showErrorMessage("Connection Failed", "Unable to configure SafeNet HSM support: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public boolean isConnectionSucceeded() {
        return connectionSucceeded;
    }
}
