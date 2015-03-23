package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.BetterComboBox;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Functions;
import org.quartz.CronExpression;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;


public class ScheduledTaskPropertiesDialog extends JDialog {
    private final Logger logger = Logger.getLogger(ScheduledTaskPropertiesDialog.class.getName());
    // todo use resource

    private JPanel mainPanel;
    private JLabel policyIdLabel;
    private BetterComboBox policyComboBox;
    private BetterComboBox nodeComboBox;
    private JPanel timePanel;
    private JRadioButton oneTimeRadioButton;
    private JRadioButton recurringRadioButton;
    private JComboBox basicComboBox;
    private JTextField minuteTextField;
    private JButton minuteEditButton;
    private SquigglyTextField cronExpressionTextField;
    private JButton hourEditButton;
    private JButton dayEditButton;
    private JButton monthEditButton;
    private JButton weekdayEditButton;
    private JTextField hourTextField;
    private JTextField dayTextField;
    private JTextField monthTextField;
    private JTextField weekdayTextField;
    private JLabel minuteLabel;
    private JLabel hourLabel;
    private JLabel dayLabel;
    private JLabel monthLabel;
    private JLabel weekdayLabel;
    private JPanel recurringPanel;
    private JPanel advancedPanel;
    private JLabel secondLabel;
    private JTextField secondTextField;
    private JButton secondEditButton;
    private JCheckBox disableCheckBox;
    private JButton cancelButton;
    private JButton okButton;
    private JTextField nameField;
    private JRadioButton basicRadioButton;
    private JRadioButton advancedRadioButton;
    private JRadioButton cronExpressionRadioButton;
    private ButtonGroup jobTypeButtonGroup;
    private ButtonGroup recurringButtonGroup;

    private boolean confirmed = false;
    private JDateTimeChooser timeChooser;
    private DefaultComboBoxModel policyComboBoxModel;
    private PolicyAdmin policyAdmin;

    private final String All_NODES = "All Nodes";
    private final String ONE_NODE = "One Node";
    private String[] cronFragments;

    private ScheduledTask scheduledTask;

    public ScheduledTaskPropertiesDialog(Dialog parent, ScheduledTask scheduledTask) {
        super(parent, "Scheduled Task Properties");
        this.scheduledTask = scheduledTask;
        scheduledTask.setJobStatus(JobStatus.SCHEDULED);

        if (scheduledTask.getExecutionDate() == 0) {
            scheduledTask.setExecutionDate(new Date().getTime());
        }

        initComponents();
    }

    private void initComponents() {

        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        });

        // name field
        nameField.setDocument(new MaxLengthDocument(128));
        nameField.getDocument().addDocumentListener(changeListener);

        // node combo box
        nodeComboBox.addItem(All_NODES);
        nodeComboBox.addItem(ONE_NODE);

        // policy combo box
        try {
            policyComboBoxModel = new DefaultComboBoxModel() {

            };
            Collection<Policy> policyHeaders = getPolicyAdmin().findPoliciesByTypeTagAndSubTag(PolicyType.POLICY_BACKED_OPERATION, "com.l7tech.objectmodel.polback.BackgroundTask", "run");
            Functions.forall(policyHeaders, new Functions.Unary<Boolean, Policy>() {
                @Override
                public Boolean call(Policy policyHeader) {
                    policyComboBoxModel.addElement(policyHeader);
                    return true;
                }
            });
            policyComboBox.setModel(policyComboBoxModel);
            policyComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null && value instanceof Policy) {
                        setText(((Policy) value).getName());
                    }
                    return this;
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        policyComboBox.addActionListener(changeListener);

        // job type
        jobTypeButtonGroup = new ButtonGroup();
        jobTypeButtonGroup.add(oneTimeRadioButton);
        jobTypeButtonGroup.add(recurringRadioButton);
        oneTimeRadioButton.addActionListener(changeListener);
        recurringRadioButton.addActionListener(changeListener);

        // One time date field
        timePanel.setLayout(new BorderLayout());
        timeChooser = new JDateTimeChooser(null, new Date(System.currentTimeMillis()), null, null);
        timeChooser.getJCalendar().setDecorationBackgroundVisible(true);
        timeChooser.getJCalendar().setDecorationBordersVisible(false);
        timeChooser.getJCalendar().setWeekOfYearVisible(false);
        timeChooser.getJCalendar().setMinSelectableDate(new Date());
        timeChooser.setPreferredSize(new Dimension(170, 20));
        timeChooser.getDateEditor().addPropertyChangeListener(changeListener);
        timePanel.add(timeChooser, BorderLayout.CENTER);

        // basic
        basicComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                populateTextFieldsFromCronExpression(((ScheduledTaskBasicInterval) basicComboBox.getSelectedItem()).getCronExpression());
            }
        });

        basicComboBox.setModel(new DefaultComboBoxModel(ScheduledTaskBasicInterval.values()));
        basicComboBox.setSelectedIndex(0);

        //advance edit buttons
        secondEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_SECOND, secondTextField);
            }
        });
        minuteEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_MINUTE, minuteTextField);
            }
        });
        hourEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_HOUR, hourTextField);
            }
        });
        dayEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_DAY, dayTextField);
            }
        });
        weekdayEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_WEEK, weekdayTextField);
            }
        });
        monthEditButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit(ScheduledTaskBasicInterval.EVERY_MONTH, monthTextField);
            }
        });

        // recurring options
        recurringButtonGroup = new ButtonGroup();
        recurringButtonGroup.add(basicRadioButton);
        recurringButtonGroup.add(advancedRadioButton);
        recurringButtonGroup.add(cronExpressionRadioButton);
        basicRadioButton.addActionListener(changeListener);
        advancedRadioButton.addActionListener(changeListener);
        cronExpressionRadioButton.addActionListener(changeListener);
        cronExpressionTextField.setColumns(225);
        TextComponentPauseListenerManager.registerPauseListener(cronExpressionTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                enableDisableComponents();
                try {
                    CronExpression.validateExpression(cronExpressionTextField.getText());
                    cronExpressionTextField.setStraight();
                } catch (ParseException e) {
                    cronExpressionTextField.setSquiggly();

                }
            }
        }, 500);

        // todo add tootips
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        modelToView();
        enableDisableComponents();

        Utilities.setEscAction(this, okButton);
        pack();
        Utilities.centerOnScreen(this);
    }

    private void enableDisableComponents() {
        timeChooser.setEnabled(oneTimeRadioButton.isSelected());

        boolean isRecurring = recurringRadioButton.isSelected();
        Utilities.setEnabled(recurringPanel, isRecurring);
        basicComboBox.setEnabled(isRecurring && basicRadioButton.isSelected());
        Utilities.setEnabled(advancedPanel, isRecurring && advancedRadioButton.isSelected());

        boolean isOK;
        isOK = nameField.getText().trim().length() > 0;
        isOK = isOK && policyComboBox.getSelectedIndex() > -1;
        isOK = isOK && timeChooser.getDate() != null;
        isOK = isOK && (!cronExpressionRadioButton.isSelected() || cronExpressionTextField.getText().trim().length() > 0);
        okButton.setEnabled(isOK);

        cronExpressionTextField.setEnabled(cronExpressionRadioButton.isSelected());
    }

    private void modelToView() {
        nameField.setText(scheduledTask.getName());
        nodeComboBox.setSelectedIndex(scheduledTask.isUseOneNode() ? 1 : 0);
        policyComboBox.setSelectedItem(getPolicyByGoid(scheduledTask.getPolicyGoid()));
        if (JobType.ONE_TIME.equals(scheduledTask.getJobType())) {
            oneTimeRadioButton.setSelected(true);
            timeChooser.setDate(new Date(scheduledTask.getExecutionDate()));
        } else {
            recurringRadioButton.setSelected(true);
            selectRadioButton(scheduledTask.getCronExpression());
        }
        disableCheckBox.setSelected(JobStatus.DISABLED.equals(scheduledTask.getJobStatus()));
    }

    private void selectRadioButton(String cronExpression) {
        cronExpressionTextField.setText(cronExpression);

        for (ScheduledTaskBasicInterval interval : ScheduledTaskBasicInterval.values()) {
            if (cronExpression.equals(interval.getCronExpression())) {
                basicRadioButton.setSelected(true);
                basicComboBox.setEnabled(true);
                basicComboBox.setSelectedItem(interval);
                return;
            }
        }
        populateTextFieldsFromCronExpression(cronExpression);
        cronExpressionTextField.setText(cronExpression);
        advancedRadioButton.setSelected(true);
    }

    private void doEdit(final ScheduledTaskBasicInterval scheduledTaskBasicInterval, final JTextField textField) {
        final ScheduledTaskEditCronIntervalDialog editCronIntervalDialog = new ScheduledTaskEditCronIntervalDialog(this, scheduledTaskBasicInterval, cronFragments[scheduledTaskBasicInterval.ordinal()]);
        DialogDisplayer.display(editCronIntervalDialog, new Runnable() {
            @Override
            public void run() {
                if (editCronIntervalDialog.isConfirmed()) {
                    textField.setText(editCronIntervalDialog.getCronExpressionFragment());
                }
            }
        });
        cronExpressionTextField.setText(createAdvancedCronExpression());
    }


    private void populateTextFieldsFromCronExpression(String cronExpression) {
        cronFragments = cronExpression.split(" ");

        secondTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_SECOND.ordinal()]);
        minuteTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_MINUTE.ordinal()]);
        hourTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_HOUR.ordinal()]);
        dayTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()]);
        monthTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_MONTH.ordinal()]);
        weekdayTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_WEEK.ordinal()]);
    }

    private PolicyAdmin getPolicyAdmin() {
        if (policyAdmin == null) {
            policyAdmin = Registry.getDefault().getPolicyAdmin();
        }
        return policyAdmin;
    }

    private Policy getPolicyByGoid(Goid goid) {
        if (goid == null || goid.equals(Goid.DEFAULT_GOID)) {
            return (Policy) policyComboBox.getItemAt(0);
        }

        for (int i = 0; i < policyComboBox.getItemCount(); i++) {
            Policy policyHeader = (Policy) policyComboBox.getItemAt(i);
            if (goid.equals(policyHeader.getGoid())) {
                return policyHeader;
            }
        }
        return null;
    }

    protected void onOK() {

        Goid policyGoid = ((Policy) policyComboBox.getSelectedItem()).getGoid();
        scheduledTask.setName(nameField.getText());
        scheduledTask.setPolicyGoid(policyGoid);
        scheduledTask.setUseOneNode(nodeComboBox.getSelectedItem().equals(ONE_NODE));

        if (oneTimeRadioButton.isSelected()) {
            scheduledTask.setJobType(JobType.ONE_TIME);
            scheduledTask.setExecutionDate(timeChooser.getDate().getTime());
            scheduledTask.setCronExpression(null);

            // Validate that scheduled execution date is in the future
            Date now = new Date();
            if (new Date(scheduledTask.getExecutionDate()).before(now)) {
                DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, "Scheduled Excecution time must be later than current time",
                        "Scheduled Execution Date Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            scheduledTask.setJobStatus(JobStatus.SCHEDULED);

        } else {
            scheduledTask.setJobType(JobType.RECURRING);
            scheduledTask.setExecutionDate(0);
            if (disableCheckBox.isSelected()) {
                scheduledTask.setJobStatus(JobStatus.DISABLED);
            } else {
                scheduledTask.setJobStatus(JobStatus.SCHEDULED);
            }

            if (basicRadioButton.isSelected()) {
                scheduledTask.setCronExpression(((ScheduledTaskBasicInterval) basicComboBox.getSelectedItem()).getCronExpression());
            } else {
                String cronExpression;
                if (advancedRadioButton.isSelected()) {
                    cronExpression = createAdvancedCronExpression();
                } else {
                    cronExpression = cronExpressionRadioButton.getText();
                }
                try {
                    CronExpression.validateExpression(cronExpression);
                    scheduledTask.setCronExpression(cronExpression);
                } catch (ParseException e) {
                    DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, e.getMessage(),
                            "Cron Expression Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
            }
        }



        // save the task
        try {
            Registry.getDefault().getScheduledTaskAdmin().saveScheduledTask(scheduledTask);
        } catch (UpdateException | SaveException e) {
            DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, "Unable to save this Scheduled Task. " + e.getMessage(),
                    "Scheduled Task Error", JOptionPane.ERROR_MESSAGE, null);
            return;
        }
        confirmed = true;
        dispose();
    }

    private String createAdvancedCronExpression() {

        cronFragments[ScheduledTaskBasicInterval.EVERY_SECOND.ordinal()] = secondTextField.getText();
        cronFragments[ScheduledTaskBasicInterval.EVERY_MINUTE.ordinal()] = minuteTextField.getText();
        cronFragments[ScheduledTaskBasicInterval.EVERY_HOUR.ordinal()] = hourTextField.getText();
        cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()] = dayTextField.getText();
        cronFragments[ScheduledTaskBasicInterval.EVERY_MONTH.ordinal()] = monthTextField.getText();
        cronFragments[ScheduledTaskBasicInterval.EVERY_WEEK.ordinal()] = weekdayTextField.getText();

        StringBuilder builder = new StringBuilder();
        builder.append(cronFragments[ScheduledTaskBasicInterval.EVERY_SECOND.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_MINUTE.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_HOUR.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_MONTH.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_WEEK.ordinal()]);

        return builder.toString();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    protected enum ScheduledTaskBasicInterval {
        EVERY_SECOND("Every Second", "* * * * * ?"),
        EVERY_MINUTE("Every Minute", "0 * * * * ?"),
        EVERY_HOUR("Every Hour", "0 0 * * * ?"),
        EVERY_DAY("Every Day", "0 0 0 * * ?"),
        EVERY_MONTH("Every Month", "0 0 0 1 * ?"),
        EVERY_WEEK("Every Week", "0 0 0 ? * 1");

        private final String name;
        private final String cronExpression;

        private ScheduledTaskBasicInterval(String name, String cronExpression) {
            this.name = name;
            this.cronExpression = cronExpression;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getCronExpression() {
            return cronExpression;
        }

    }
}
