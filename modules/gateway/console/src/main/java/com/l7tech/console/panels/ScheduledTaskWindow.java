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
import com.l7tech.policy.Policy;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ScheduledTaskWindow extends JDialog {
    private static final Logger logger = Logger.getLogger( ScheduledTaskWindow.class.getName() );

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
        super(parent, "Scheduled Task", true);

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
                if(scheduledPoliciesTable.getSelectedRow() > -1) {
                    doEdit(scheduledPoliciesTableModel.getRowObject(rowSorter.convertRowIndexToModel(scheduledPoliciesTable.getSelectedRow())));
                }
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(scheduledPoliciesTable.getSelectedRow() > -1) {
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

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        scheduledPoliciesTableModel = buildResourcesTableModel();
        scheduledPoliciesTable.setModel(scheduledPoliciesTableModel);
        scheduledPoliciesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduledPoliciesTable.getSelectionModel().addListSelectionListener( enableDisableListener );
        rowSorter = new TableRowSorter<>(scheduledPoliciesTableModel);
        scheduledPoliciesTable.setRowSorter(rowSorter);


        Utilities.setDoubleClickAction(scheduledPoliciesTable, editButton);
        setContentPane(mainPanel);
        pack();

        loadDocuments();
        enableDisableComponents();
    }

    private void enableDisableComponents() {
        final int[] selectedRows = scheduledPoliciesTable.getSelectedRows();
        editButton.setEnabled( selectedRows.length == 1 );
        cloneButton.setEnabled( selectedRows.length == 1 );
        removeButton.setEnabled( selectedRows.length > 0 );
    }

    private SimpleTableModel<ScheduledTask> buildResourcesTableModel() {
        return TableUtil.configureTable(
                scheduledPoliciesTable,
                TableUtil.column("Job Type", 80, 110, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(final ScheduledTask scheduledTask) {
                        if(scheduledTask != null){
                            switch(scheduledTask.getJobType()) {
                            case RECURRING:
                                return "Recurring";
                            case ONE_TIME:
                                return "Once";
                            default:
                                return "";
                            }
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column("Job Name", 80, 110, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(final ScheduledTask scheduledTask) {
                        if(scheduledTask != null){
                            return scheduledTask.getName();
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column("Policy Name", 80, 260, 10000, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if(scheduledTask != null){
                            return getPolicyName(scheduledTask);
                        }
                        return "";
                    }
                }, String.class),
                TableUtil.column("Schedule", 140, 160, 180, new Functions.Unary<String, ScheduledTask>() {
                    @Override
                    public String call(ScheduledTask scheduledTask) {
                        if(scheduledTask != null){
                            switch(scheduledTask.getJobType()) {
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
                TableUtil.column("Node", 80, 260, 10000, new Functions.Unary<String, ScheduledTask>() {
                            @Override
                            public String call(ScheduledTask scheduledTask) {
                                if(scheduledTask != null){
                                    return scheduledTask.isUseOneNode()?"One":"All";
                                }
                                return "";
                            }
                        }, String.class),
                TableUtil.column("Status", 80, 100, 180, new Functions.Unary<String, ScheduledTask>() {
                        @Override
                        public String call(ScheduledTask scheduledTask) {
                            if(scheduledTask != null){
                                switch(scheduledTask.getJobStatus()){
                                    case SCHEDULED:
                                        return "Scheduled";
                                    case DISABLED:
                                        return "Disabled";
                                    case COMPLETED:
                                        return "Completed";
                                    default:
                                        return "";
                                }
                            }
                            return "";
                        }
                    }, String.class)
        );
    }

    private String getPolicyName(ScheduledTask scheduledTask) {
        try {
            Policy policy = policyAdmin.findPolicyByPrimaryKey(scheduledTask.getPolicyGoid());
            if(policy!=null){
                return policy.getName();
            }
        } catch (FindException e) {
            return "";
        }
        return "";
    }

    private void loadDocuments() {
        try {
            List<ScheduledTask> documentHeaders = new ArrayList<ScheduledTask>();
            Collection<ScheduledTask> scheduledServiceJobEntities = scheduledTaskAdmin.getAllScheduledTasks();
            if(scheduledServiceJobEntities != null){
                documentHeaders.addAll(scheduledServiceJobEntities);
            }
            scheduledPoliciesTableModel.setRows(documentHeaders);
        } catch(FindException fe) {
            logger.log(Level.WARNING, "Failed to load scheduled tasks.");
        }
    }

    private void doAdd() {
        final ScheduledTask scheduledTask = new ScheduledTask();
        scheduledTask.setJobType(JobType.ONE_TIME);
        displayPropertiesDialog(new ScheduledTaskPropertiesDialog(this, scheduledTask));
    }

    private void doEdit(final ScheduledTask scheduledTask) {
        displayPropertiesDialog( new ScheduledTaskPropertiesDialog(this,scheduledTask));
    }

    private void doClone(final ScheduledTask scheduledTask) {
        ScheduledTask scheduledTaskCopy = new ScheduledTask();
        scheduledTaskCopy.copyFrom(scheduledTask);
        EntityUtils.updateCopy(scheduledTaskCopy);
        displayPropertiesDialog( new ScheduledTaskPropertiesDialog(this,scheduledTaskCopy));
    }
    
    private void displayPropertiesDialog(final ScheduledTaskPropertiesDialog dialog){
        DialogDisplayer.display(dialog, new Runnable() {
            @Override
            public void run() {
                if(dialog.isConfirmed()) {
                        loadDocuments();
                }
            }
        });
   }

    private void doRemove() {
        if(scheduledPoliciesTable.getSelectedRowCount() == 0) {
            return;
        }

        java.util.List<ScheduledTask> entries = new ArrayList<ScheduledTask>(scheduledPoliciesTable.getSelectedRowCount());
        for(int row : scheduledPoliciesTable.getSelectedRows()) {
            entries.add(scheduledPoliciesTableModel.getRowObject(rowSorter.convertRowIndexToModel(row)));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Are you sure you want to remove the selected scheduled policy jobs?");
        for(ScheduledTask entry : entries) {
            sb.append("\n").append("Policy: " + getPolicyName(entry))
                    .append(" with schedule: " + (entry.getJobType().equals(JobType.ONE_TIME) ? entry.getExecutionDate() : entry.getCronExpression()));
        }
        if(JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                sb.toString(),
                "Confirm Scheduled Task(s) Deletion",
                JOptionPane.YES_NO_OPTION))
        {
            boolean needUpdate = false;
            for(ScheduledTask entry : entries) {
                try {
                    scheduledTaskAdmin.deleteScheduledTask(entry);
                    needUpdate = true;
                } catch(DeleteException de) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Unable to delete the following scheduled task.\n" + entry.getName(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }

            if(needUpdate) {
               loadDocuments();
            }
        }
    }

}
