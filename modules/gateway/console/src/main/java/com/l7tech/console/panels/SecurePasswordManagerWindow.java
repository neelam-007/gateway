package com.l7tech.console.panels;

import com.l7tech.console.action.Actions;
import static com.l7tech.console.util.AdminGuiUtils.doAsyncAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.password.SecurePassword.SecurePasswordType;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class SecurePasswordManagerWindow extends JDialog {
    private static Logger logger = Logger.getLogger(SecurePasswordManagerWindow.class.getName());
    private static ResourceBundle resources = ResourceBundle.getBundle( SecurePasswordManagerWindow.class.getName() );

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
                column("Type", 25, 100, 300, Functions.<SecurePasswordType,SecurePassword>propertyTransform( SecurePassword.class, "type" ), SecurePasswordType.class),
                column("Description", 25, 220, 99999, propertyTransform(SecurePassword.class, "description")),
                column("Last Changed", 25, 210, 300, propertyTransform( SecurePassword.class, "lastUpdateAsDate" )));

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

                boolean readOnly = isEdit ? !Registry.getDefault().getSecurityProvider().hasPermission(new AttemptedUpdate(EntityType.SECURE_PASSWORD, securePassword)) : false;

                final SecurePasswordPropertiesDialog dlg = new SecurePasswordPropertiesDialog(SecurePasswordManagerWindow.this, securePassword, readOnly);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        if (dlg.isConfirmed()) {
                            Long oid = null;
                            try {
                                final TrustedCertAdmin admin = Registry.getDefault().getTrustedCertManager();
                                oid = admin.saveSecurePassword( securePassword );
                                securePassword.setOid(oid);

                                // Update password field, if necessary
                                char[] newpass = dlg.getEnteredPassword();
                                if (newpass != null)
                                    admin.setSecurePassword( oid, newpass );

                                int keybits = dlg.getGenerateKeybits();
                                if ( keybits > 0 ) {
                                    doAsyncAdmin(
                                            admin,
                                            SecurePasswordManagerWindow.this,
                                            "Generating Key",
                                            "Generating PEM private key ...",
                                            admin.setGeneratedSecurePassword( oid, keybits )
                                    );
                                }
                            } catch (UpdateException e1) {
                                showError("save stored password", e1);
                            } catch (SaveException e1) {
                                showError("save stored password", e1);
                            } catch (FindException e1) {
                                showError("save stored password", e1);
                            } catch ( InterruptedException e1 ) {
                                showError( "Generate stored password key", e1 );
                            } catch ( InvocationTargetException e1 ) {
                                showError( "Generate stored password key", e1.getCause() );
                            } finally {
                                loadSecurePasswords(oid);
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
                        } catch (ConstraintViolationException e1) {
                            DialogDisplayer.showMessageDialog(removeButton, "Error", "Unable to delete this stored password: it is currently in use", null);
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

        passwordTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });
        passwordTable.setDefaultRenderer(
                SecurePasswordType.class,
                new TextListCellRenderer<SecurePasswordType>( new Functions.Unary<String, SecurePasswordType>() {
                    @Override
                    public String call( final SecurePasswordType securePasswordType ) {
                        return resources.getString( "securepassword.type." + securePasswordType.name() );
                    }
                } ).asTableCellRenderer() );

        enableOrDisableButtons();

        Utilities.setButtonAccelerator( this, helpButton, KeyEvent.VK_F1 );
        Utilities.setDoubleClickAction(passwordTable, editButton);
        Utilities.setRowSorter(passwordTable, passwordTableModel, new int[]{0,1,2,3}, new boolean[]{true,true,true,true}, null);
        Utilities.setMinimumSize(this);

        loadSecurePasswords( null );
    }

    private void loadSecurePasswords( @Nullable final Long oidToSelect ) {
        try {
            java.util.List<SecurePassword> passes = new ArrayList<SecurePassword>(Registry.getDefault().getTrustedCertManager().findAllSecurePasswords());
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
        return passwordTableModel.getRowObject(passwordTable.getRowSorter().convertRowIndexToModel(passwordTable.getSelectedRow()));
    }

    private void enableOrDisableButtons() {
        boolean enabled = passwordTable.getSelectedRow() >= 0;

        removeButton.setEnabled(enabled);
        editButton.setEnabled(enabled);
        // Other buttons are always enabled.
    }
}
