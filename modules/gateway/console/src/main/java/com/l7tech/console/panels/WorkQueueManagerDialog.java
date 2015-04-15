package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.gateway.common.workqueue.WorkQueueManagerAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorkQueueManagerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(WorkQueueManagerDialog.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.resources.WorkQueueManagerDialog");

    private JPanel mainPanel;
    private JTable queueTable;
    private JButton addButton;
    private JButton cloneButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private boolean closed;

    private SimpleTableModel<WorkQueue> workQueueTableModel;

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
    public WorkQueueManagerDialog(Frame owner) {
        super(owner, resources.getString("dialog.title.manage.work.queues"));
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.WORK_QUEUE);

        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);
        Utilities.setEscKeyStrokeDisposes(this);

        workQueueTableModel = buildWorkQueueTableModel();
        loadWorkQueues();

        queueTable.setModel(workQueueTableModel);
        queueTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableOrDisableButtons();
            }

        };
        queueTable.getSelectionModel().addListSelectionListener(enableDisableListener);

        final RowSorter.SortKey sortKey = new RowSorter.SortKey(0, SortOrder.ASCENDING);
        final TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(workQueueTableModel);
        sorter.setSortKeys(java.util.Arrays.asList(sortKey));
        sorter.setSortsOnUpdates(true);
        queueTable.setRowSorter(sorter);

        // Make integer value columns (Max Size, Max Threads) left-aligned
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        queueTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        queueTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

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
                closed = true;
                dispose();
            }
        });

        Utilities.setDoubleClickAction(queueTable, editButton);
        enableOrDisableButtons();

    }

    private WorkQueueManagerAdmin getWorkQueueManagerAdmin() {
        WorkQueueManagerAdmin admin = null;
        if (Registry.getDefault().isAdminContextPresent()) {
            admin = Registry.getDefault().getWorkQueueManagerAdmin();
        } else {
            logger.log(Level.WARNING, "No Admin Context present!");
        }
        return admin;
    }

    private void loadWorkQueues() {
        WorkQueueManagerAdmin admin = getWorkQueueManagerAdmin();
        if (admin != null) {
            try {
                // Clear the table first
                final int totalCount = workQueueTableModel.getRowCount();
                for (int i = 0; i < totalCount; i++) {
                    workQueueTableModel.removeRowAt(0);
                }

                for (WorkQueue wq : admin.getAllWorkQueues()) {
                    workQueueTableModel.addRow(wq);
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, resources.getString("errors.load.work.queues"));
            }
        }
    }

    private void enableOrDisableButtons() {
        int selectedRow = queueTable.getSelectedRow();

        boolean editEnabled = selectedRow >= 0;
        boolean removeEnabled = selectedRow >= 0;
        boolean copyEnabled = selectedRow >= 0;


        addButton.setEnabled(flags.canCreateSome());
        editButton.setEnabled(editEnabled);  // Not using flags.canUpdateSome(), since we still allow users to view the properties.
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        cloneButton.setEnabled(flags.canCreateSome() && copyEnabled);
    }

    private void doAdd() {
        WorkQueue wq = new WorkQueue();
        editAndSave(wq, true);
    }

    private void doEdit() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) return;

        editAndSave(workQueueTableModel.getRowObject(queueTable.convertRowIndexToModel(selectedRow)), false);
    }

    private void doClone() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow < 0) return;

        WorkQueue newWq = new WorkQueue();
        newWq.copyFrom(workQueueTableModel.getRowObject(queueTable.convertRowIndexToModel(selectedRow)));
        EntityUtils.updateCopy(newWq);
        editAndSave(newWq, true);
    }

    private void doRemove() {
        int currentRow = queueTable.getSelectedRow();
        if (currentRow < 0) return;

        final int currentModelRow = queueTable.convertRowIndexToModel(currentRow);
        WorkQueue wq = workQueueTableModel.getRowObject(currentModelRow);
        Object[] options = {resources.getString("button.remove"), resources.getString("button.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("confirmation.remove.work.queue"), wq.getName()),
                resources.getString("dialog.title.remove.work.queue"), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            workQueueTableModel.removeRowAt(currentModelRow);

            WorkQueueManagerAdmin admin = getWorkQueueManagerAdmin();
            if (admin == null) return;
            try {
                admin.deleteWorkQueue(wq);
            } catch (DeleteException e) {
                logger.warning("Cannot delete the work queue " + wq.getName());
                return;
            }

            // Refresh the table
            workQueueTableModel.fireTableDataChanged();

            // Refresh the selection highlight
            if (currentRow == workQueueTableModel.getRowCount()) currentRow--; // If the previous deleted row was the last row
            if (currentRow >= 0) queueTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
        }
    }

    private void editAndSave(final WorkQueue workQueue, final boolean selectName) {
        final WorkQueuePropertiesDialog dlg =
                new WorkQueuePropertiesDialog(WorkQueueManagerDialog.this, workQueue);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        if (selectName)
            dlg.selectName();
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            loadWorkQueues();
                            editAndSave(workQueue, selectName);
                        }
                    };

                    // Save the work queue
                    WorkQueueManagerAdmin admin = getWorkQueueManagerAdmin();
                    if (admin == null) return;
                    try {
                        admin.saveWorkQueue(workQueue);
                    } catch (UpdateException | SaveException | FindException e) {
                        showErrorMessage(resources.getString("dialog.title.save.failed"),
                                resources.getString("errors.save.failed") + " " + ExceptionUtils.getMessage(e),
                                e,
                                reedit);
                        return;
                    }

                    // Refresh the list
                    loadWorkQueues();

                    // Refresh the table
                    workQueueTableModel.fireTableDataChanged();

                    // Refresh the selection highlight
                    int currentRow = 0;
                    for (int i = 0; i < workQueueTableModel.getRowCount(); i++) {
                        WorkQueue wq = workQueueTableModel.getRowObject(i);
                        if (wq.getName().equals(workQueue.getName())) {
                            break;
                        }
                        currentRow++;
                    }
                    queueTable.getSelectionModel().setSelectionInterval(currentRow, currentRow);
                }
            }
        });
    }

    private void showErrorMessage(String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private SimpleTableModel<WorkQueue> buildWorkQueueTableModel() {
        return TableUtil.configureTable(
                queueTable,
                TableUtil.column("Name", 40, 200, 1000000, new Functions.Unary<String, WorkQueue>() {
                    @Override
                    public String call(WorkQueue workQueueEntity) {
                        return workQueueEntity.getName();
                    }
                }, String.class),
                TableUtil.column("Max Size", 40, 150, 180, new Functions.Unary<Integer, WorkQueue>() {
                    @Override
                    public Integer call(WorkQueue workQueueEntity) {
                        return workQueueEntity.getMaxQueueSize();
                    }
                }, Integer.class),
                TableUtil.column("Max Threads", 40, 150, 180, new Functions.Unary<Integer, WorkQueue>() {
                    @Override
                    public Integer call(WorkQueue workQueueEntity) {
                        return workQueueEntity.getThreadPoolMax();
                    }
                }, Integer.class)
        );
    }

    public boolean isClosed() {
        return closed;
    }

    public WorkQueue getSelectedWorkQueue() {
        if (queueTable.getSelectedRow() < 0) {
            return null;
        }
        return workQueueTableModel.getRowObject(queueTable.convertRowIndexToModel(queueTable.getSelectedRow()));
    }
}
