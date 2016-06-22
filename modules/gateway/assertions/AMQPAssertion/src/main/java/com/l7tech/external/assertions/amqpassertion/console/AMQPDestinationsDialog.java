package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPDestinationsDialog extends JDialog {
    private JPanel mainPanel;
    private JTable destinationsTable;
    private SimpleTableModel<AMQPDestination> destinationsTableModel;
    TableRowSorter<SimpleTableModel<AMQPDestination>> rowSorter;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;

    public AMQPDestinationsDialog(Frame owner) {
        super(owner, "AMQP Destinations", true);

        initComponents();
        loadDestinations();
    }

    public AMQPDestinationsDialog(Dialog owner) {
        super(owner, "AMQP Destinations", true);

        initComponents();
        loadDestinations();
    }

    private void initComponents() {
        destinationsTableModel = buildListenersTableModel();
        destinationsTable.setModel(destinationsTableModel);
        destinationsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Utilities.setRowSorter(destinationsTable, destinationsTableModel, new int[]{0}, new boolean[]{true}, new Comparator[]{String.CASE_INSENSITIVE_ORDER});
        rowSorter = (TableRowSorter<SimpleTableModel<AMQPDestination>>) destinationsTable.getRowSorter();
        rowSorter.setSortsOnUpdates(true);

        destinationsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableComponents();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AMQPDestinationDialog dialog = new AMQPDestinationDialog(AMQPDestinationsDialog.this);
                AMQPDestination destination = new AMQPDestination();
                dialog.updateView(destination);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    dialog.updateModel(destination);

                    try {
                        AMQPDestinationHelper.addAMQPDestination(destination);
                    } catch (SaveException ex) {
                        JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to add the new AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (UpdateException ex) {
                        JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to add the new AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                loadDestinations();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AMQPDestinationDialog dialog = new AMQPDestinationDialog(AMQPDestinationsDialog.this);
                int row = rowSorter.convertRowIndexToModel(destinationsTable.getSelectedRow());
                AMQPDestination destination = destinationsTableModel.getRowObject(row);
                dialog.updateView(destination);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    dialog.updateModel(destination);

                    try {
                        AMQPDestinationHelper.updateAMQPDestination(destination);
                    } catch (SaveException ex) {
                        JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to update AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (UpdateException ex) {
                        JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to add the new AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                loadDestinations();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indices = destinationsTable.getSelectedRows();
                AMQPDestination[] destinationsToDelete = new AMQPDestination[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    destinationsToDelete[i] = destinationsTableModel.getRowObject(rowSorter.convertRowIndexToModel(indices[i]));
                }

                for (AMQPDestination destinationToDelete : destinationsToDelete) {
                    destinationsTableModel.removeRow(destinationToDelete);
                }

                try {
                    AMQPDestinationHelper.removeAMQPDestinations(destinationsToDelete);
                } catch (SaveException ex) {
                    JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to remove the listeners.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (DeleteException ex) {
                    JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to add the new AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (FindException ex) {
                    JOptionPane.showMessageDialog(AMQPDestinationsDialog.this, "Failed to add the new AMQP destination.", "Error", JOptionPane.ERROR_MESSAGE);
                }

                loadDestinations();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setDoubleClickAction(destinationsTable, editButton);
        enableDisableComponents();

        setContentPane(mainPanel);
        pack();
    }

    private void enableDisableComponents() {
        editButton.setEnabled(destinationsTable.getSelectedRowCount() == 1);
        removeButton.setEnabled(destinationsTable.getSelectedRowCount() > 0);
    }

    private SimpleTableModel<AMQPDestination> buildListenersTableModel() {
        return TableUtil.configureTable(
                destinationsTable,
                TableUtil.column("Name", 40, 200, 100000, new Functions.Unary<String, AMQPDestination>() {
                    @Override
                    public String call(final AMQPDestination destination) {
                        return destination.getName();
                    }
                }, String.class),
                TableUtil.column("IsInbound?", 40, 80, 180, new Functions.Unary<String, AMQPDestination>() {
                    @Override
                    public String call(AMQPDestination destination) {
                        return destination.isInbound() + "";
                    }
                }, String.class)
        );
    }

    private void loadDestinations() {
        AMQPDestination[] destinations = AMQPDestinationHelper.restoreAmqpDestinations();
        if (destinations == null) {
            destinationsTableModel.setRows(new ArrayList<AMQPDestination>());
            return;
        }
        ArrayList<AMQPDestination> destinationsList = new ArrayList<AMQPDestination>(destinations.length);
        for (AMQPDestination destination : destinations) {
            destinationsList.add(destination);
        }
        destinationsTableModel.setRows(destinationsList);
    }
}
