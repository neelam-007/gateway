package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.BetterComboBox;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.Functions;
import org.quartz.CronExpression;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private JComboBox unitComboBox;
    private JTextField minuteTextField;
    private JButton minuteEditButton;
    private JButton hourEditButton;
    private JButton dayEditButton;
    private JButton monthEditButton;
    private JTextField hourTextField;
    private JTextField dayTextField;
    private JTextField monthTextField;
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
    private JCheckBox mondayCheckBox;
    private JCheckBox tuesdayCheckBox;
    private JCheckBox wednesdayCheckBox;
    private JCheckBox thursdayCheckBox;
    private JCheckBox fridayCheckBox;
    private JCheckBox saturdayCheckBox;
    private JCheckBox sundayCheckBox;
    private JTextField intervalTextField;
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
        intervalTextField.getDocument().addDocumentListener(changeListener);
        intervalTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        }));
        intervalTextField.setText("1");
        unitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int interval = Integer.parseInt(intervalTextField.getText());
                    populateTextFieldsFromCronExpression(((ScheduledTaskBasicInterval) unitComboBox.getSelectedItem()).getCronExpression(interval));
                }catch( NumberFormatException ex){
                    // do nothing
                }
            }
        });

        unitComboBox.setModel(new DefaultComboBoxModel(ScheduledTaskBasicInterval.values()));
        unitComboBox.setSelectedIndex(0);
        try {
            int interval = Integer.parseInt(intervalTextField.getText());
            populateTextFieldsFromCronExpression(((ScheduledTaskBasicInterval) unitComboBox.getSelectedItem()).getCronExpression(interval));
        }catch( NumberFormatException ex){
            // do nothing
        }


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
        basicRadioButton.setSelected(true);
        basicRadioButton.addActionListener(changeListener);
        advancedRadioButton.addActionListener(changeListener);

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
        unitComboBox.setEnabled(isRecurring && basicRadioButton.isSelected());
        Utilities.setEnabled(advancedPanel, isRecurring && advancedRadioButton.isSelected());

        boolean isOK;
        isOK = nameField.getText().trim().length() > 0;
        isOK = isOK && policyComboBox.getSelectedIndex() > -1;
        isOK = isOK && timeChooser.getDate() != null;
        try{
            Integer.parseInt(intervalTextField.getText());
        }catch(NumberFormatException e){
            isOK = false;
        }
        isOK = isOK && timeChooser.getDate() != null;
        okButton.setEnabled(isOK);
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

        for (ScheduledTaskBasicInterval interval : ScheduledTaskBasicInterval.values()) {
            if (interval.matches(cronExpression)) {
                basicRadioButton.setSelected(true);
                intervalTextField.setText(interval.getInterval(cronExpression));
                unitComboBox.setEnabled(true);
                unitComboBox.setSelectedItem(interval);

                return;
            }
        }
        populateTextFieldsFromCronExpression(cronExpression);
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
    }


    private void populateTextFieldsFromCronExpression(String cronExpression) {
        cronFragments = cronExpression.split(" ");

        secondTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_SECOND.ordinal()]);
        minuteTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_MINUTE.ordinal()]);
        hourTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_HOUR.ordinal()]);
        dayTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()]);
        monthTextField.setText(cronFragments[ScheduledTaskBasicInterval.EVERY_MONTH.ordinal()]);

        String weekday = cronFragments[ScheduledTaskBasicInterval.EVERY_WEEK.ordinal()];
        sundayCheckBox.setSelected(weekday.contains("1"));
        mondayCheckBox.setSelected(weekday.contains("2"));
        tuesdayCheckBox.setSelected(weekday.contains("3"));
        wednesdayCheckBox.setSelected(weekday.contains("4"));
        thursdayCheckBox.setSelected(weekday.contains("5"));
        fridayCheckBox.setSelected(weekday.contains("6"));
        saturdayCheckBox.setSelected(weekday.contains("7"));
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

            String cronExpression;
            if (basicRadioButton.isSelected()) {
                try {
                    cronExpression = ((ScheduledTaskBasicInterval) unitComboBox.getSelectedItem()).getCronExpression(Integer.parseInt(intervalTextField.getText()));
                }catch( NumberFormatException e){
                    DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, "Interval "+intervalTextField.getText()+"  must be an integer. ",
                            "Scheduled Interval Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
            } else {
                cronExpression = createAdvancedCronExpression();
            }

            try {
                CronExpression.validateExpression(cronExpression);
                scheduledTask.setCronExpression(cronExpression);
            } catch (ParseException e) {
                DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, "Expression: " + cronExpression + "\nError: "+ e.getMessage(),
                        "Cron Expression Error", JOptionPane.ERROR_MESSAGE, null);
                return;
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

        String dayOfweekExpression;
        if(!mondayCheckBox.isSelected() &&
                !tuesdayCheckBox.isSelected() &&
                !wednesdayCheckBox.isSelected() &&
                !thursdayCheckBox.isSelected() &&
                !fridayCheckBox.isSelected() &&
                !saturdayCheckBox.isSelected() &&
                !sundayCheckBox.isSelected()){
            dayOfweekExpression = "?";
        }else{
            StringBuilder weekBuilder = new StringBuilder();
            // (1 = Sunday)
            weekBuilder.append(sundayCheckBox.isSelected()?"1,":"");
            weekBuilder.append(mondayCheckBox.isSelected()?"2,":"");
            weekBuilder.append(tuesdayCheckBox.isSelected()?"3,":"");
            weekBuilder.append(wednesdayCheckBox.isSelected()?"4,":"");
            weekBuilder.append(thursdayCheckBox.isSelected()?"5,":"");
            weekBuilder.append(fridayCheckBox.isSelected()?"6,":"");
            weekBuilder.append(saturdayCheckBox.isSelected()?"7,":"");
            dayOfweekExpression = weekBuilder.toString().substring(0,weekBuilder.toString().length()-1);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(cronFragments[ScheduledTaskBasicInterval.EVERY_SECOND.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_MINUTE.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_HOUR.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()])
                .append(" ").append(cronFragments[ScheduledTaskBasicInterval.EVERY_MONTH.ordinal()])
                .append(" ").append(dayOfweekExpression);

        return builder.toString();
    }

    private void onCancel() {
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    protected enum ScheduledTaskBasicInterval {
        EVERY_SECOND("Second", "{0} * * * * ?", "\\*\\/\\d+ \\* \\* \\* \\* \\?"),
        EVERY_MINUTE("Minute", "0 {0} * * * ?", "0 \\*\\/\\d+ \\* \\* \\* \\?"),
        EVERY_HOUR("Hour", "0 0 {0} * * ?", "0 0 \\*\\/\\d+ \\* \\* \\?"),
        EVERY_DAY("Day", "0 0 0 {0} * ?", "0 0 0 \\*\\/\\d+ \\* \\?"),
        EVERY_MONTH("Month", "0 0 0 1 {0} ?", "0 0 0 1 \\*\\/\\d+ \\?"),
        EVERY_WEEK("Week", "0 0 0 ? * {0}", "0 0 0 ? \\* \\*\\/\\d+");

        private final String name;
        private final String cronExpression;
        private final String cronRegex;

        private ScheduledTaskBasicInterval(String name, String cronExpression, String cronRegex) {
            this.name = name;
            this.cronExpression = cronExpression;
            this.cronRegex = cronRegex;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getCronExpression(int interval) {
            return MessageFormat.format(cronExpression, "*/"+interval);
        }

        public boolean matches(String cronExpression) {
            return cronExpression.matches(cronRegex);
        }

        public String getInterval(String cronExpression) {
            String[] fragments = cronExpression.split(" ");

            Matcher matcher = Pattern.compile("\\d+").matcher(fragments[ordinal()]);
            matcher.find();
            return matcher.group();
        }
    }
}
