package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import static com.l7tech.gui.util.TableUtil.column;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.ObjectNotFoundException;
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
 * Dialog that provides the ability to view and modify the Trusted ESM and Trusted ESM User registrations
 * on this Gateway.
 */
public class TrustedEsmManagerWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(TrustedEsmManagerWindow.class.getName());

    private JButton deleteButton;
    private JButton closeButton;
    private JPanel mainPanel;
    private JSplitPane splitPane;
    private JTable esmTable;
    private JTable usersTable;
    private JButton deleteMappingButton;

    private SimpleTableModel<TrustedEsm> esmTableModel;
    private SimpleTableModel<UserRow> usersTableModel;

    public TrustedEsmManagerWindow(Window owner) {
        super(owner, "Manage ESM User Mappings", ModalityType.DOCUMENT_MODAL);
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
                doDeleteEsm();
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

        esmTableModel = TableUtil.configureTable(esmTable,
                column("ESM ID", 25, 110, 99999, propertyTransform(TrustedEsm.class, "name")),
                column("Trusted Cert", 25, 110, 99999, new TrustedCertFormatter()),
                column("Subject DN", 25, 100, 99999, new CertSubjectFormatter()));

        usersTableModel = TableUtil.configureTable(usersTable,
                column("ESM User Name", 25, 100, 99999, propertyTransform(UserRow.class, "esmUserDisplayName")),
                column("ESM User ID", 25, 100, 99999, propertyTransform(UserRow.class, "esmUserId")),
                column("Local User", 25, 100, 99999, propertyTransform(UserRow.class, "ssgUserDisplayName")),
                column("Identity Provider", 25, 100, 99999, propertyTransform(UserRow.class, "idProviderDisplayName")));

        loadEsmTable();

        esmTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        esmTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateUsersTable();
            }
        });
    }

    private void doDeleteMapping() {
        final TrustedEsm esm = getSelectedEsm();
        if (esm == null)
            return;
        final UserRow user = getSelectedUser();
        if (user == null)
            return;

        DialogDisplayer.showConfirmDialog(this,
                "Are you sure you wish to delete the mapping for user " + user.getEsmUserDisplayName()
                        + " (" + user.getEsmUserId() + ") \nfrom ESM " + esm.getName()
                        + " (" + esm.getTrustedCert().getCertificate().getSubjectDN().getName() + ") ?",
                "Delete User Mapping",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.OK_OPTION)
                            return;

                        boolean reloadUserMappingDisplay = false;
                        try {
                            Registry.getDefault().getClusterStatusAdmin().deleteTrustedEsmUserMapping(user.getTrustedEsmUser().getOid());
                            reloadUserMappingDisplay = true;
                        } catch (ObjectNotFoundException e) {
                            reloadUserMappingDisplay = true;
                        } catch (ObjectModelException e) {
                            showError("Unable to delete ESM user mapping", e);
                        }

                        if ( reloadUserMappingDisplay ) {
                            loadUsersTable(esm.getOid());
                        }
                    }
                });
    }

    private void doDeleteEsm() {
        final TrustedEsm esm = getSelectedEsm();
        if (esm == null)
            return;

        DialogDisplayer.showConfirmDialog(this,
                "Are you sure you wish to remove the registration for ESM " + esm.getName()
                        + "\n (" + esm.getTrustedCert().getCertificate().getSubjectDN().getName()
                        + ") ?\nThis will also delete all its user mappings.",
                "Remove ESM Registration And Delete User Mappings",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.OK_OPTION)
                            return;
                        try {
                            Registry.getDefault().getClusterStatusAdmin().deleteTrustedEsmInstance(esm.getOid());
                            loadEsmTable();
                            updateUsersTable();
                        } catch (ObjectModelException e) {
                            showError("Unable to remove ESM registration and delete user mappings", e);
                        }
                    }
                });
    }

    private void showError(String message, Throwable e) {
        DialogDisplayer.showMessageDialog(this, message + ": " + ExceptionUtils.getMessage(e), "Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private void updateUsersTable() {
        TrustedEsm esm = getSelectedEsm();
        if (esm == null) {
            usersTableModel.setRows(Collections.<UserRow>emptyList());
        } else {
            loadUsersTable(esm.getOid());
        }
    }

    private TrustedEsm getSelectedEsm() {
        int row = esmTable.getSelectedRow();
        if (row < 0)
            return null;
        return esmTableModel.getRowObject(row);
    }

    private UserRow getSelectedUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0)
            return null;
        return usersTableModel.getRowObject(row);
    }

    private void loadEsmTable() {
        try {
            List<TrustedEsm> esms = new ArrayList<TrustedEsm>(Registry.getDefault().getClusterStatusAdmin().getTrustedEsmInstances());
            esmTableModel.setRows(esms);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadUsersTable(long esmOid) {
        final boolean[] incompleteLoad = new boolean[] { false };

        try {
            List<TrustedEsmUser> esmUsers = new ArrayList<TrustedEsmUser>(Registry.getDefault().getClusterStatusAdmin().getTrustedEsmUserMappings(esmOid));

            final IdentityAdmin identityAdmin = Registry.getDefault().getIdentityAdmin();
            usersTableModel.setRows(Functions.grepNotNull(Functions.map(esmUsers, new Functions.Unary<UserRow, TrustedEsmUser>() {
                @Override
                public UserRow call(TrustedEsmUser trustedEsmUser) {
                    try {
                        final Goid providerOid = trustedEsmUser.getProviderGoid();
                        IdentityProviderConfig idprov = identityAdmin.findIdentityProviderConfigByID(providerOid);
                        if (idprov == null) {
                            logger.log(Level.WARNING, "ID provider not found for ESM user mapping " + trustedEsmUser);
                            incompleteLoad[0] = true;
                            return null;
                        }

                        String idProviderDisplayName = idprov.getName();
                        User user = identityAdmin.findUserByID(idprov.getGoid(), trustedEsmUser.getSsgUserId());
                        if (user == null) {
                            logger.log(Level.WARNING, "User not found for ESM user mapping " + trustedEsmUser);
                            incompleteLoad[0] = true;
                            return null;
                        }

                        String username = user.getLogin();
                        if (username == null) username = user.getName();
                        if (username == null) username = user.getId();

                        return new UserRow(trustedEsmUser, username, idProviderDisplayName);
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
        public String call(TrustedEsm trustedEsm) {
            return trustedEsm.getTrustedCert().getCertificate().getSubjectDN().getName();
        }
    }

    private static class TrustedCertFormatter implements Functions.Unary<Object, TrustedEsm> {
        @Override
        public String call(TrustedEsm trustedEsm) {
            return trustedEsm.getTrustedCert().getName();
        }
    }

    public static class UserRow {
        final TrustedEsmUser trustedEsmUser;
        final String ssgUserDisplayName;
        final String idProviderDisplayName;
        final String esmUserDisplayName;

        private UserRow(TrustedEsmUser trustedEsmUser, String ssgUserDisplayName, String identityProviderDisplayName) {
            if (trustedEsmUser == null) throw new NullPointerException();
            this.trustedEsmUser = trustedEsmUser;
            this.ssgUserDisplayName = ssgUserDisplayName;
            this.idProviderDisplayName = identityProviderDisplayName;
            String desc = trustedEsmUser.getEsmUserDisplayName();
            this.esmUserDisplayName = desc == null ? "" : desc;
        }

        public TrustedEsmUser getTrustedEsmUser() {
            return trustedEsmUser;
        }

        public String getEsmUserId() {
            return trustedEsmUser.getEsmUserId();
        }

        public String getSsgUserDisplayName() {
            return ssgUserDisplayName;
        }

        public String getIdProviderDisplayName() {
            return idProviderDisplayName;
        }

        public String getEsmUserDisplayName() {
            return esmUserDisplayName;
        }
    }
}

