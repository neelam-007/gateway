package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.emstrust.TrustedEsm;
import com.l7tech.gateway.common.emstrust.TrustedEsmUser;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import static com.l7tech.gui.util.TableUtil.column;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import static com.l7tech.util.Functions.propertyTransform;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog that provides the ability to view and modify the Trusted EMS and Trusted EMS User registrations
 * on this Gateway.
 */
public class TrustedEmsManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(TrustedEmsManagerWindow.class.getName());

    private JButton deleteButton;
    private JButton closeButton;
    private JPanel mainPanel;
    private JSplitPane splitPane;
    private JTable emsTable;
    private JTable usersTable;
    private JButton deleteMappingButton;

    private SimpleTableModel<TrustedEsm> emsTableModel;
    private SimpleTableModel<UserRow> usersTableModel;

    public TrustedEmsManagerWindow(Window owner) {
        super(owner, "Manage EMS User Mappings", ModalityType.DOCUMENT_MODAL);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);

        Utilities.setEscKeyStrokeDisposes(this);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteEms();
            }
        });

        deleteMappingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDeleteMapping();
            }
        });

        Utilities.deuglifySplitPane(splitPane);
        Utilities.equalizeButtonSizes(new JButton[] {
                deleteButton,
                closeButton,
                deleteMappingButton,
        });

        emsTableModel = TableUtil.configureTable(emsTable,
                column("EMS ID", 25, 110, 99999, propertyTransform(TrustedEsm.class, "name")),
                column("Trusted Cert", 25, 110, 99999, new TrustedCertFormatter()),
                column("Subject DN", 25, 100, 99999, new CertSubjectFormatter()));

        usersTableModel = TableUtil.configureTable(usersTable,
                column("EMS User ID", 25, 100, 99999, propertyTransform(UserRow.class, "emsUserId")),
                column("Local User", 25, 100, 99999, propertyTransform(UserRow.class, "ssgUserDisplayName")),
                column("Identity Provider", 25, 100, 99999, propertyTransform(UserRow.class, "idProviderDisplayName")));

        loadEmsTable();

        emsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateUsersTable();
            }
        });
    }

    private void doDeleteMapping() {
        final TrustedEsm ems = getSelectedEms();
        if (ems == null)
            return;
        final UserRow user = getSelectedUser();
        if (user == null)
            return;

        DialogDisplayer.showConfirmDialog(this,
                "Are you sure you wish to delete the mapping for user " + user.getEmsUserId() + " from EMS " + ems.getName() + "?",
                "Delete User Mapping",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.OK_OPTION)
                            return;
                        try {
                            Registry.getDefault().getClusterStatusAdmin().deleteTrustedEsmUserMapping(user.getTrustedEmsUser().getOid());
                            loadUsersTable(ems.getOid());
                        } catch (ObjectModelException e) {
                            showError("Unable to delete EMS user mapping", e);
                        }
                    }
                });
    }

    private void doDeleteEms() {
        final TrustedEsm ems = getSelectedEms();
        if (ems == null)
            return;

        DialogDisplayer.showConfirmDialog(this,
                "Are you sure you wish to remove the registration for EMS " + ems.getName() + "?\nThis will also delete all its user mappings.",
                "Remove EMS Registration And Delete User Mappings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.OK_OPTION)
                            return;
                        try {
                            Registry.getDefault().getClusterStatusAdmin().deleteTrustedEsmInstance(ems.getOid());
                            loadEmsTable();
                            updateUsersTable();
                        } catch (ObjectModelException e) {
                            showError("Unable to remove EMS registration and delete user mappings", e);
                        }
                    }
                });
    }

    private void showError(String message, Throwable e) {
        DialogDisplayer.showMessageDialog(this, message + ": " + ExceptionUtils.getMessage(e), "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private void updateUsersTable() {
        TrustedEsm ems = getSelectedEms();
        if (ems == null) {
            usersTableModel.setRows(Collections.<UserRow>emptyList());
        } else {
            loadUsersTable(ems.getOid());
        }
    }

    private TrustedEsm getSelectedEms() {
        int row = emsTable.getSelectedRow();
        if (row < 0)
            return null;
        return emsTableModel.getRowObject(row);
    }

    private UserRow getSelectedUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0)
            return null;
        return usersTableModel.getRowObject(row);
    }

    private void loadEmsTable() {
        try {
            List<TrustedEsm> emses = new ArrayList<TrustedEsm>(Registry.getDefault().getClusterStatusAdmin().getTrustedEsmInstances());
            emsTableModel.setRows(emses);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadUsersTable(long emsOid) {
        final boolean[] incompleteLoad = new boolean[] { false };

        try {
            List<TrustedEsmUser> emsUsers = new ArrayList<TrustedEsmUser>(Registry.getDefault().getClusterStatusAdmin().getTrustedEmsUserMappings(emsOid));

            final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
            usersTableModel.setRows(Functions.grepNotNull(Functions.map(emsUsers, new Functions.Unary<UserRow, TrustedEsmUser>() {
                @Override
                public UserRow call(TrustedEsmUser trustedEmsUser) {
                    try {
                        final long providerOid = trustedEmsUser.getProviderOid();
                        IdentityProviderConfig idprov = identityAdmin.findIdentityProviderConfigByID(providerOid);
                        if (idprov == null) {
                            logger.log(Level.WARNING, "ID provider not found for EMS user mapping " + trustedEmsUser);
                            incompleteLoad[0] = true;
                            return null;
                        }

                        String idProviderDisplayName = idprov.getName();
                        User user = identityAdmin.findUserByID(idprov.getOid(), trustedEmsUser.getSsgUserId());
                        if (user == null) {
                            logger.log(Level.WARNING, "User not found for EMS user mapping " + trustedEmsUser);
                            incompleteLoad[0] = true;
                            return null;
                        }

                        String username = user.getLogin();
                        if (username == null) username = user.getName();
                        if (username == null) username = user.getId();

                        return new UserRow(trustedEmsUser, username, idProviderDisplayName);
                    } catch (ObjectModelException e) {
                        logger.log(Level.WARNING, "Unable to load user row: " + ExceptionUtils.getMessage(e), e);
                        incompleteLoad[0] = true;
                        return null;
                    }
                }
            })));
        } catch (RuntimeException e) {
            showError("Unable to load users table", e);
        } catch (ObjectModelException e) {
            showError("Unable to load users table", e);
        }

        if (incompleteLoad[0])
            DialogDisplayer.showMessageDialog(this, "Warning: Not all user mappings were loaded successfully.", "Error", JOptionPane.WARNING_MESSAGE, null);
    }

    private static class CertSubjectFormatter implements Functions.Unary<Object, TrustedEsm> {
        @Override
        public String call(TrustedEsm trustedEms) {
            return trustedEms.getTrustedCert().getCertificate().getSubjectDN().getName();
        }
    }

    private static class TrustedCertFormatter implements Functions.Unary<Object, TrustedEsm> {
        @Override
        public String call(TrustedEsm trustedEms) {
            return trustedEms.getTrustedCert().getName();
        }
    }

    public static class UserRow {
        final TrustedEsmUser trustedEmsUser;
        final String ssgUserDisplayName;
        final String idProviderDisplayName;

        private UserRow(TrustedEsmUser trustedEmsUser, String ssgUserDisplayName, String identityProviderDisplayName) {
            if (trustedEmsUser == null) throw new NullPointerException();
            this.trustedEmsUser = trustedEmsUser;
            this.ssgUserDisplayName = ssgUserDisplayName;
            this.idProviderDisplayName = identityProviderDisplayName;
        }

        public TrustedEsmUser getTrustedEmsUser() {
            return trustedEmsUser;
        }

        public String getEmsUserId() {
            return trustedEmsUser.getEsmUserId();
        }

        public String getSsgUserDisplayName() {
            return ssgUserDisplayName;
        }

        public String getIdProviderDisplayName() {
            return idProviderDisplayName;
        }
    }
}

