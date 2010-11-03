package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class SecurePasswordManagerWindow extends JDialog {
    private static Logger logger = Logger.getLogger(SecurePasswordManagerWindow.class.getName());

    private JPanel contentPane;
    private JButton closeButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton editButton;
    private JTable passwordTable;
    private JButton helpButton;

    private SimpleTableModel<SecurePassword> passwordTableModel;

    public SecurePasswordManagerWindow(Window owner) {
        super(owner, "Manage Stored Passwords");        
        setContentPane(contentPane);
        setModal(true);
        Utilities.setEscKeyStrokeDisposes(this);

        passwordTableModel = TableUtil.configureTable(passwordTable,
                column("Name", 25, 110, 1024, propertyTransform(SecurePassword.class, "name")),
                column("Description", 25, 220, 99999, propertyTransform(SecurePassword.class, "description")),
                column("Last Changed", 25, 210, 300, propertyTransform(SecurePassword.class, "lastUpdateAsDate")));

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        final ActionListener addEditAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean isEdit = editButton == e.getSource();

                final SecurePassword securePassword = isEdit ? getSelectedSecurePassword() : new SecurePassword();
                if (securePassword == null)
                    return;

                final SecurePasswordPropertiesDialog dlg = new SecurePasswordPropertiesDialog(SecurePasswordManagerWindow.this, securePassword);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.isConfirmed()) {
                            try {
                                long oid = Registry.getDefault().getTrustedCertManager().saveSecurePassword(securePassword);
                                securePassword.setOid(oid);

                                // Update password field, if necessary
                                char[] newpass = dlg.getEnteredPassword();
                                if (newpass != null)
                                    Registry.getDefault().getTrustedCertManager().setSecurePassword(oid, newpass);

                                loadSecurePasswords(oid);
                            } catch (UpdateException e1) {
                                showError("save stored password", e1);
                            } catch (SaveException e1) {
                                showError("save stored password", e1);
                            } catch (FindException e1) {
                                showError("save stored password", e1);
                            }
                        }
                    }
                });
            }
        };
        addButton.addActionListener(addEditAction);
        editButton.addActionListener(addEditAction);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SecurePassword securePassword = getSelectedSecurePassword();
                if (securePassword == null)
                    return;

                DialogDisplayer.showConfirmDialog(removeButton,
                        "Are you sure you want to delete the password " + securePassword.getName() + "?",
                        "Confirm Delete",
                        JOptionPane.WARNING_MESSAGE,
                        new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.OK_OPTION)
                            return;
                        try {
                            Registry.getDefault().getTrustedCertManager().deleteSecurePassword(securePassword.getOid());
                            loadSecurePasswords(null);
                        } catch (DeleteException de) {
                            showError("delete stored password", de);
                        } catch (FindException fe) {
                            showError("delete stored password", fe);
                        }
                    }
                });
            }
        });

        helpButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                Actions.invokeHelp(SecurePasswordManagerWindow.this);
            }
        } );

        Utilities.setButtonAccelerator( this, helpButton, KeyEvent.VK_F1 );
        Utilities.setDoubleClickAction(passwordTable, editButton);
        Utilities.setRowSorter(passwordTable, passwordTableModel);

        loadSecurePasswords(null);
    }

    private void loadSecurePasswords(final Long oidToSelect) {
        try {
            java.util.List<SecurePassword> passes = new ArrayList<SecurePassword>(Registry.getDefault().getTrustedCertManager().findAllSecurePasswords());
            Collections.sort(passes, new Comparator<SecurePassword>() {
                @Override
                public int compare(SecurePassword a, SecurePassword b) {                    
                    return (a.getName().compareTo(b.getName()));
                }
            });
            passwordTableModel.setRows(passes);

            if (oidToSelect != null) {
                int row = passwordTableModel.findFirstRow(new Functions.Unary<Boolean, SecurePassword>() {
                    @Override
                    public Boolean call(SecurePassword securePassword) {
                        return oidToSelect.equals(securePassword.getOid());
                    }
                });
                if (row >= 0)
                    passwordTable.getSelectionModel().setSelectionInterval(row, row);
            }
        } catch (FindException e) {
            showError("populate list of stored passwords", e);
        }
    }

    private void showError(String action, Throwable t) {
        String message = "Unable to " + action + ": " + ExceptionUtils.getMessage(t);
        //noinspection ThrowableResultOfMethodCallIgnored
        logger.log(Level.WARNING, message, ExceptionUtils.getDebugException(t));
        DialogDisplayer.showMessageDialog(this, "Error", message, null);
    }

    /** @return the selected SecurePassword, or null if no row is selected. */
    private SecurePassword getSelectedSecurePassword() {
        return passwordTableModel.getRowObject(passwordTable.getSelectedRow());                
    }
}
