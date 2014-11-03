package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.cassandra.CassandraConnectionManagerAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/3/14
 */
public class CassandraConnectionManagerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(CassandraConnectionManagerDialog.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.CassandraConnectionManagerDialog");

    private JPanel mainPanel;
    private JTable connectionTable;
    private JButton addButton;
    private JButton cloneButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    private SimpleTableModel<CassandraConnection> cassandraConnectionsTableModel;
    private TableRowSorter<SimpleTableModel<CassandraConnection>> cassandraConnectionsRawSorter;

    private PermissionFlags flags;

    /**
     * Creates a modeless dialog with the specified {@code Frame}
     * as its owner and an empty title. If {@code owner}
     * is {@code null}, a shared, hidden frame will be set as the
     * owner of the dialog.
     * <p/>
     * This constructor sets the component's locale property to the value
     * returned by {@code JComponent.getDefaultLocale}.
     * <p/>
     * NOTE: This constructor does not allow you to create an unowned
     * {@code JDialog}. To create an unowned {@code JDialog}
     * you must use either the {@code JDialog(Window)} or
     * {@code JDialog(Dialog)} constructor with an argument of
     * {@code null}.
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *                           returns {@code true}.
     * @see java.awt.GraphicsEnvironment#isHeadless
     * @see javax.swing.JComponent#getDefaultLocale
     */
    public CassandraConnectionManagerDialog(Frame owner) {
        super(owner, resources.getString("dialog.title.manage.cassandra.connections"));
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.CASSANDRA_CONFIGURATION);

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        cassandraConnectionsTableModel = buildConnectionTableModel();
        loadCassandraConnections();

        connectionTable.setModel(cassandraConnectionsTableModel);
        connectionTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected  void run() {
                enableOrDisableButtons();
            }

        };
        connectionTable.getSelectionModel().addListSelectionListener(enableDisableListener);


        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClone();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });


        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setDoubleClickAction(connectionTable, editButton);
        enableOrDisableButtons();

    }

    private void loadCassandraConnections() {
        if(Registry.getDefault().isAdminContextPresent()) {

            CassandraConnectionManagerAdmin admin = Registry.getDefault().getCassandraConnectionAdmin();
            if (admin != null) {
                try {
                    for (CassandraConnection connection : admin.getAllCassandraConnections())
                        cassandraConnectionsTableModel.addRow(connection);
                } catch (FindException e) {
                    logger.log(Level.WARNING, resources.getString("errors.manage.cassandra.connections.loadconnections="));
                }
            }
        }
        else {
            logger.log(Level.WARNING, "No Admin Context present!");
        }

    }

    private void enableOrDisableButtons() {
        int selectedRow = connectionTable.getSelectedRow();

        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;
        boolean copyEnabled = selectedRow >= 0;


        addButton.setEnabled(flags.canCreateSome());
        editButton.setEnabled(editEnabled);  // Not using flags.canUpdateSome(), since we still allow users to view the properties.
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        cloneButton.setEnabled(flags.canCreateSome() && copyEnabled);
    }

    private void doAdd() {

    }


    private void doEdit() {

    }

    private void doClone() {

    }

    private void doRemove() {

    }

    private SimpleTableModel<CassandraConnection> buildConnectionTableModel() {
        return TableUtil.configureTable(
                connectionTable,
                TableUtil.column("Connection Name", 40, 200, 1000000, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getName();
                    }
                }, String.class),
                TableUtil.column("Contact Points", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getContactPoints();
                    }
                }, String.class),
                TableUtil.column("Keyspace", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getKeyspaceName();
                    }
                }, String.class),
                TableUtil.column("Port", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getPort();
                    }
                }, String.class),
                TableUtil.column("User Name", 40, 100, 180, new Functions.Unary<String, CassandraConnection>() {
                    @Override
                    public String call(CassandraConnection cassandraConnectionEntity) {
                        return cassandraConnectionEntity.getUsername();
                    }
                }, String.class)
        );
    }
}
