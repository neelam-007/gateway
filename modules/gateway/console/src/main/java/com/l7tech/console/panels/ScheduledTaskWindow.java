package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gateway.common.task.ScheduledTaskAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ScheduledTaskWindow extends JDialog {
    private static final Logger logger = Logger.getLogger(ScheduledTaskWindow.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle(ScheduledTaskWindow.class.getName());

    private JPanel mainPanel;
    private SimpleTableModel<ScheduledTask> scheduledPoliciesTableModel;
    private TableRowSorter<SimpleTableModel<ScheduledTask>> rowSorter;
    private JTable scheduledPoliciesTable;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JButton closeButton;
    private JButton cloneButton;

    private PolicyAdmin policyAdmin;
    private ScheduledTaskAdmin scheduledTaskAdmin;


    public ScheduledTaskWindow(Frame parent) {
        super(parent, resources.getString("dialog.title"), true);

        scheduledTaskAdmin = Registry.getDefault().getScheduledTaskAdmin();
        policyAdmin = Registry.getDefault().getPolicyAdmin();

        initComponents();
    }

    private void initComponents() {
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAdd();
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scheduledPoliciesTable.getSelectedRow() > -1) {
                    doEdit(scheduledPoliciesTableModel.getRowObject(rowSorter.convertRowIndexToModel(scheduledPoliciesTable.getSelectedRow())));
                }
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (scheduledPoliciesTable.getSelectedRow() > -1) {
                    doClone(scheduledPoliciesTableModel.getRowObject(rowSorter.convertRowIndexToModel(scheduledPoliciesTable.getSelectedRow())));
                }
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

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        scheduledPoliciesTableModel = buildResourcesTableModel();
        scheduledPoliciesTable.setModel(scheduledPoliciesTableModel);
        scheduledPoliciesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduledPoliciesTable.getSelectionModel().addListSelectionListener(enableDisableListener);
        rowSorter = new TableRowSorter<>(scheduledPoliciesTableModel);
        scheduledPoliciesTable.setRowSorter(rowSorter);
        rowSorter.toggleSortOrder(0);


        Utilities.setDoubleClickAction(scheduledPoliciesTable, editButton);
        setContentPane(mainPanel);
        pack();

        loadDocuments();
        enableDisableComponents();
    }

    private void enableDisableComponents() {
        final int[] selectedRows = scheduledPoliciesTable.getSelectedRows();
        editButton.setEnabled(selectedRows.length == 1);
        cloneButton.setEnabled(selectedRows.length == 1);
        removeButton.setEnabled(selectedRows.length > 0);
    }

    private SimpleTableModel<ScheduledTask> buildResourcesTableModel() {
        return TableUtil.configureTable(
                scheduledPoliciesTable,
                TableUtil.column(resources.getString("column.type"), 80, 100, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(final ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            switch (scheduledTask.getJobType()) {
                                case RECURRING:
                                    return resources.getString("column.type.recurring");
                                case ONE_TIME:
                                    return resources.getString("column.type.one.time");
                                default:
                                    return "";
                            }
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.name"), 80, 110,  10000, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(final ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            return scheduledTask.getName();
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.policy"), 80, 160, 10000, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            return scheduledTask.getPolicy().getName();
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.schedule"), 80, 160, 10000, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            switch (scheduledTask.getJobType()) {
                                case RECURRING:
                                    return scheduledTask.getCronExpression();
                                case ONE_TIME:
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");
                                    return dateFormat.format(scheduledTask.getExecutionDate());
                                default:
                                    return "";
                            }
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.node"), 50, 80, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            return scheduledTask.isUseOneNode() ? resources.getString("column.node.one") : resources.getString("column.node.all");
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column(resources.getString("column.status"), 80, 100, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if (scheduledTask != null) {
                            switch (scheduledTask.getJobStatus()) {
                                case SCHEDULED:
                                    return resources.getString("column.status.scheduled");
                                case DISABLED:
                                    return resources.getString("column.status.disabled");
                                case COMPLETED:
                                    return resources.getString("column.status.completed");
                                default:
                                    return "";
                            }
                        }
                        return "";
                    }
                }, String.class)
        );
    }

    private void loadDocuments() {
        try {
            List<ScheduledTask> documentHeaders = new ArrayList<ScheduledTask>();
            Collection<ScheduledTask> scheduledServiceJobEntities = scheduledTaskAdmin.getAllScheduledTasks();
            if (scheduledServiceJobEntities != null) {
                documentHeaders.addAll(scheduledServiceJobEntities);
            }
            scheduledPoliciesTableModel.setRows(documentHeaders);
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Failed to load scheduled tasks.");
        }
    }

    private void doAdd() {
        final ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setJobType(JobType.ONE_TIME);
        displayPropertiesDialog(new ScheduledTaskPropertiesDialog(this, scheduledTask));
    }

    private void doEdit(final ScheduledTask scheduledTask) {
        displayPropertiesDialog(new ScheduledTaskPropertiesDialog(this, scheduledTask));
    }

    private void doClone(final ScheduledTask scheduledTask) {
        ScheduledTask scheduledTaskCopy = new ScheduledTask();
        scheduledTaskCopy.copyFrom(scheduledTask);
        EntityUtils.updateCopy(scheduledTaskCopy);
        displayPropertiesDialog(new ScheduledTaskPropertiesDialog(this, scheduledTaskCopy));
    }

    private void displayPropertiesDialog(final ScheduledTaskPropertiesDialog dialog) {
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if (dialog.isConfirmed()) {
                    loadDocuments();
                }
            }
        });
    }

    private void doRemove() {
        if (scheduledPoliciesTable.getSelectedRowCount() == 0) {
            return;
        }


        ScheduledTask task = scheduledPoliciesTableModel.getRowObject(rowSorter.convertRowIndexToModel(scheduledPoliciesTable.getSelectedRow()));
        Object[] options = {resources.getString("option.remove"), resources.getString("option.cancel")};
        int result = JOptionPane.showOptionDialog(
                this, MessageFormat.format(resources.getString("remove.connection.confirmation"), task.getName()),
                resources.getString("remove.connection.dialog.title"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

        if (result == 0) {
            try {
                scheduledTaskAdmin.deleteScheduledTask(task);
            } catch (DeleteException de) {
                JOptionPane.showMessageDialog(
                        this,
                        MessageFormat.format(resources.getString("error.delete.message"), task.getName()),
                        resources.getString("error.dialog.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            loadDocuments();
        }
    }

}
