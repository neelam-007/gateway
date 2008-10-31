package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import static com.l7tech.gui.util.TableUtil.column;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
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

/**
 * Dialog that provides the ability to view and modify the Trusted EMS and Trusted EMS User registrations
 * on this Gateway.
 */
public class TrustedEmsManagerWindow extends JDialog {
    private JButton deleteButton;
    private JButton closeButton;
    private JPanel mainPanel;
    private JSplitPane splitPane;
    private JTable emsTable;
    private JTable usersTable;
    private JButton deleteMappingButton;

    private SimpleTableModel<TrustedEms> emsTableModel;
    private SimpleTableModel<TrustedEmsUser> usersTableModel;

    public TrustedEmsManagerWindow(Window owner) {
        super(owner, "Manage EMS User Mappings", ModalityType.DOCUMENT_MODAL);
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);

        Utilities.setEscKeyStrokeDisposes(this);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doDeleteEms();
            }
        });

        deleteMappingButton.addActionListener(new ActionListener() {
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
                column("EMS ID", 25, 110, 99999, propertyTransform(TrustedEms.class, "name")),
                column("Trusted Cert", 25, 110, 99999, new TrustedCertFormatter()),
                column("Subject DN", 25, 100, 99999, new CertSubjectFormatter()));

        usersTableModel = TableUtil.configureTable(usersTable,
                column("EMS User ID", 25, 100, 99999, propertyTransform(TrustedEmsUser.class, "emsUserId")),
                column("Local User", 25, 100, 99999, propertyTransform(TrustedEmsUser.class, "ssgUserId")),
                column("Identity Provider", 25, 100, 99999, propertyTransform(TrustedEmsUser.class, "providerOid")));

        loadEmsTable();

        emsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        emsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateUsersTable();
            }
        });
    }

    private void doDeleteMapping() {
        // TODO
    }

    private void doDeleteEms() {
        // TODO
    }

    private void updateUsersTable() {
        TrustedEms ems = getSelectedEms();
        if (ems == null) {
            usersTableModel.setRows(Collections.<TrustedEmsUser>emptyList());
        } else {
            loadUsersTable(ems.getOid());
        }
    }

    private TrustedEms getSelectedEms() {
        int row = emsTable.getSelectedRow();
        if (row < 0)
            return null;
        return emsTableModel.getRowObject(row);
    }

    private void loadEmsTable() {
        try {
            List<TrustedEms> emses = new ArrayList<TrustedEms>(Registry.getDefault().getClusterStatusAdmin().getTrustedEmsInstances());
            emsTableModel.setRows(emses);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadUsersTable(long emsOid) {
        try {
            List<TrustedEmsUser> emsUsers = new ArrayList<TrustedEmsUser>(Registry.getDefault().getClusterStatusAdmin().getTrustedEmsUserMappings(emsOid));
            usersTableModel.setRows(emsUsers);
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    private static class CertSubjectFormatter implements Functions.Unary<Object, TrustedEms> {
        public String call(TrustedEms trustedEms) {
            return trustedEms.getTrustedCert().getCertificate().getSubjectDN().getName();
        }
    }

    private static class TrustedCertFormatter implements Functions.Unary<Object, TrustedEms> {
        public String call(TrustedEms trustedEms) {
            return trustedEms.getTrustedCert().getName();
        }
    }
}

