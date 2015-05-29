package com.l7tech.console.panels;

import com.l7tech.console.panels.identity.finder.FindIdentitiesDialog;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.identity.finder.SearchType;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.jcalendar.JDateTimeChooser;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAll;
import com.l7tech.gateway.common.task.JobStatus;
import com.l7tech.gateway.common.task.JobType;
import com.l7tech.gateway.common.task.ScheduledTask;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.BetterComboBox;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ScheduledTaskPropertiesDialog extends JDialog {
    private final Logger logger = Logger.getLogger(ScheduledTaskPropertiesDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(ScheduledTaskPropertiesDialog.class.getName());


    private JPanel mainPanel;
    private JLabel policyIdLabel;
    private BetterComboBox policyComboBox;
    private BetterComboBox nodeComboBox;
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
    private JDateTimeChooser timeChooser;
    private SecurityZoneWidget securityZoneChooser;
    private JCheckBox userCheckBox;
    private JButton userButton;
    private JPanel userPanel;
    private JLabel userLabel;
    private JCheckBox enableCheckBox;
    private ButtonGroup jobTypeButtonGroup;
    private ButtonGroup recurringButtonGroup;

    private boolean confirmed = false;
    private DefaultComboBoxModel policyComboBoxModel;
    private PolicyAdmin policyAdmin;

    private final String All_NODES = resources.getString("node.all");
    private final String ONE_NODE = resources.getString("node.one");
    private String[] cronFragments;

    private ScheduledTask scheduledTask;
    private IdentityHeader userHeader = null;
    private boolean readOnly;
    private boolean canEditUser = false;

    public ScheduledTaskPropertiesDialog(Dialog parent, ScheduledTask scheduledTask, final boolean readOnly) {
        super(parent, resources.getString("dialog.title"));
        this.scheduledTask = new ScheduledTask();
        this.scheduledTask.copyFrom(scheduledTask);
        this.readOnly = readOnly;

        if (scheduledTask.getExecutionDate() == 0) {
            scheduledTask.setExecutionDate(new Date().getTime());
        }


        canEditUser = canEditUser();
        initComponents();
    }

    private boolean canEditUser() {
        return getSecurityProvider().hasPermission(new AttemptedCreate(EntityType.USER)) &&
                getSecurityProvider().hasPermission(new AttemptedUpdateAll(EntityType.USER));
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

        RunOnChangeListener dayOfWeekChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableDayInputs();
            }
        });

        // Day of Week checkboxes
        mondayCheckBox.addActionListener(dayOfWeekChangeListener);
        tuesdayCheckBox.addActionListener(dayOfWeekChangeListener);
        wednesdayCheckBox.addActionListener(dayOfWeekChangeListener);
        thursdayCheckBox.addActionListener(dayOfWeekChangeListener);
        fridayCheckBox.addActionListener(dayOfWeekChangeListener);
        saturdayCheckBox.addActionListener(dayOfWeekChangeListener);
        sundayCheckBox.addActionListener(dayOfWeekChangeListener);

        // name field
        nameField.setDocument(new MaxLengthDocument(128));
        nameField.getDocument().addDocumentListener(changeListener);

        // node combo box
        nodeComboBox.addItem(All_NODES);
        nodeComboBox.addItem(ONE_NODE);

        // policy combo box
        try {
            policyComboBoxModel = new DefaultComboBoxModel();
            List<Policy> policyHeaders = Functions.sort(getPolicyAdmin().findPoliciesByTypeTagAndSubTag(PolicyType.POLICY_BACKED_OPERATION, "com.l7tech.objectmodel.polback.BackgroundTask", "run"), new Comparator<Policy>() {
                @Override
                public int compare(Policy o1, Policy o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
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
        enableCheckBox.addActionListener(changeListener);

        // One time date field
        timeChooser.getJCalendar().setDecorationBackgroundVisible(true);
        timeChooser.getJCalendar().setDecorationBordersVisible(false);
        timeChooser.getJCalendar().setWeekOfYearVisible(false);
        timeChooser.getJCalendar().setMinSelectableDate(new Date());
        timeChooser.getDateEditor().addPropertyChangeListener(changeListener);

        // basic
        intervalTextField.getDocument().addDocumentListener(changeListener);
        intervalTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        }));
        intervalTextField.setText("1");

        unitComboBox.setModel(new DefaultComboBoxModel(ScheduledTaskBasicInterval.values()));
        unitComboBox.setSelectedIndex(0);
        try {
            populateTextFieldsFromCronExpression("* * * * * ?");
        } catch (NumberFormatException ex) {
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

        securityZoneChooser.configure(scheduledTask);

        //user options
        userCheckBox.addActionListener(changeListener);
        userButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectUser();
                enableDisableComponents();
            }
        });

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
        enableDisableDayInputs();
    }

    private void selectUser() {
        Frame f = TopComponents.getInstance().getTopParent();
        Options options = new Options();

        options.setSearchType(SearchType.USER);
        options.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        options.setDisposeOnSelect(true);
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
        fd.pack();
        Utilities.centerOnScreen(fd);
        FindIdentitiesDialog.FindResult result = fd.showDialog();
        if (result != null && result.entityHeaders != null && result.entityHeaders.length > 0) {
            try {
                IdentityProviderConfig idProvider = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(result.entityHeaders[0].getProviderGoid());
                userLabel.setText(MessageFormat.format(resources.getString("label.user.checkbox"), result.entityHeaders[0].getCommonName(), idProvider.getName()));
                userHeader = result.entityHeaders[0];
            } catch (FindException e) {
                logger.warning("Cannot find identity provider");
            }
        }
    }

    private void enableDisableComponents() {
        timeChooser.setEnabled(oneTimeRadioButton.isSelected());

        boolean isRecurring = recurringRadioButton.isSelected();
        Utilities.setEnabled(recurringPanel, isRecurring);
        unitComboBox.setEnabled(isRecurring && basicRadioButton.isSelected());
        intervalTextField.setEnabled(isRecurring && basicRadioButton.isSelected());
        enableCheckBox.setEnabled(isRecurring);
        Utilities.setEnabled(advancedPanel, isRecurring && advancedRadioButton.isSelected());

        Utilities.setEnabled(userPanel, canEditUser);
        userButton.setEnabled(canEditUser && userCheckBox.isSelected());

        boolean isOK;
        isOK = nameField.getText().trim().length() > 0;
        isOK = isOK && policyComboBox.getSelectedIndex() > -1;
        isOK = isOK && (!canEditUser || (!userCheckBox.isSelected() || userHeader != null));
        isOK = isOK && !readOnly;
        if (isRecurring) {
            try {
                Integer.parseInt(intervalTextField.getText());
            } catch (NumberFormatException e) {
                isOK = false;
            }
        } else {
            isOK = isOK && timeChooser.getDate() != null;
        }


        okButton.setEnabled(isOK);
    }

    private void modelToView() {
        nameField.setText(scheduledTask.getName());
        nodeComboBox.setSelectedIndex(scheduledTask.isUseOneNode() ? 1 : 0);
        policyComboBox.setSelectedItem(getPolicyByGoid(scheduledTask.getPolicyGoid()));
        timeChooser.setDate(new Date());
        if (JobType.ONE_TIME.equals(scheduledTask.getJobType())) {
            oneTimeRadioButton.setSelected(true);
            timeChooser.setDate(new Date(scheduledTask.getExecutionDate()));
        } else {
            recurringRadioButton.setSelected(true);
            selectRadioButton(scheduledTask.getCronExpression());
        }

        if (JobType.ONE_TIME.equals(scheduledTask.getJobType()) || JobStatus.SCHEDULED.equals(scheduledTask.getJobStatus())) {
            enableCheckBox.setSelected(true);
        }
        securityZoneChooser.configure(scheduledTask);

        boolean hasUser = scheduledTask.getUserId() != null && scheduledTask.getIdProviderGoid() != null;
        userCheckBox.setSelected(hasUser);
        if (hasUser) {
            try {
                IdentityProviderConfig idProvider = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByID(scheduledTask.getIdProviderGoid());
                User user = Registry.getDefault().getIdentityAdmin().findUserByID(scheduledTask.getIdProviderGoid(), scheduledTask.getUserId());
                userLabel.setText(MessageFormat.format(resources.getString("label.user.checkbox"), user.getLogin(), idProvider.getName()));
                userHeader = new IdentityHeader(scheduledTask.getIdProviderGoid(), scheduledTask.getUserId(), EntityType.USER, user.getLogin(), null, null, null);
            } catch (FindException e) {
                userLabel.setText(MessageFormat.format(resources.getString("label.user.checkbox"), scheduledTask.getUserId(), scheduledTask.getIdProviderGoid()));
                userHeader = new IdentityHeader(scheduledTask.getIdProviderGoid(), scheduledTask.getUserId(), EntityType.USER, null, null, null, null);
            }
        } else {
            userLabel.setText(null);
            userButton.setEnabled(false);
        }
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
        final ScheduledTaskEditCronIntervalDialog editCronIntervalDialog = new ScheduledTaskEditCronIntervalDialog(this, scheduledTaskBasicInterval, textField.getText());
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

    protected final SecurityProvider getSecurityProvider() {
        return Registry.getDefault().getSecurityProvider();
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
        scheduledTask.setSecurityZone(securityZoneChooser.getSelectedZone());

        if (oneTimeRadioButton.isSelected()) {
            scheduledTask.setJobType(JobType.ONE_TIME);
            scheduledTask.setExecutionDate(timeChooser.getDate().getTime());
            scheduledTask.setCronExpression(null);

            // Validate that scheduled execution date is in the future
            Date now = new Date();
            if (new Date(scheduledTask.getExecutionDate()).before(now)) {
                DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, resources.getString("error.date.message"),
                        resources.getString("error.date.title"), JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            scheduledTask.setJobStatus(JobStatus.SCHEDULED);

        } else {
            scheduledTask.setJobType(JobType.RECURRING);
            scheduledTask.setExecutionDate(0);
            if (enableCheckBox.isSelected()) {
                scheduledTask.setJobStatus(JobStatus.SCHEDULED);
            } else {
                scheduledTask.setJobStatus(JobStatus.DISABLED);
            }

            String cronExpression = scheduledTask.getCronExpression();
            if (basicRadioButton.isSelected()) {
                try {
                    cronExpression = ((ScheduledTaskBasicInterval) unitComboBox.getSelectedItem()).getCronExpression(Integer.parseInt(intervalTextField.getText()));
                } catch (NumberFormatException e) {
                    DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, MessageFormat.format(resources.getString("error.interval.message"), intervalTextField.getText()),
                            resources.getString("error.interval.title"), JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
            } else if (advancedRadioButton.isSelected() || cronExpression == null) {
                cronExpression = createAdvancedCronExpression();
            }

            if (!CronExpression.isValidExpression(cronExpression)) {
                try {
                    CronExpression.validateExpression(cronExpression);
                } catch (ParseException e) {
                    DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, MessageFormat.format(resources.getString("error.cron.message.detail"), cronExpression, e.getMessage()),
                            resources.getString("error.cron.title"), JOptionPane.ERROR_MESSAGE, null);
                    return;
                }
                DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, MessageFormat.format(resources.getString("error.cron.message"), cronExpression),
                        resources.getString("error.cron.title"), JOptionPane.ERROR_MESSAGE, null);
                return;
            }
            scheduledTask.setCronExpression(cronExpression);
        }

        scheduledTask.setIdProviderGoid(userCheckBox.isSelected() ? userHeader.getProviderGoid() : null);
        scheduledTask.setUserId(userCheckBox.isSelected() ? userHeader.getStrId() : null);

        // save the task
        try {
            Registry.getDefault().getScheduledTaskAdmin().saveScheduledTask(scheduledTask);
        } catch (UpdateException | SaveException e) {
            DialogDisplayer.showMessageDialog(ScheduledTaskPropertiesDialog.this, MessageFormat.format(resources.getString("error.save.message"), e.getMessage()),
                    resources.getString("error.save.title"), JOptionPane.ERROR_MESSAGE, null);
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
        if (!mondayCheckBox.isSelected() &&
                !tuesdayCheckBox.isSelected() &&
                !wednesdayCheckBox.isSelected() &&
                !thursdayCheckBox.isSelected() &&
                !fridayCheckBox.isSelected() &&
                !saturdayCheckBox.isSelected() &&
                !sundayCheckBox.isSelected()) {
            dayOfweekExpression = "?";
        } else {
            StringBuilder weekBuilder = new StringBuilder();
            // (1 = Sunday)
            weekBuilder.append(sundayCheckBox.isSelected() ? "1," : "");
            weekBuilder.append(mondayCheckBox.isSelected() ? "2," : "");
            weekBuilder.append(tuesdayCheckBox.isSelected() ? "3," : "");
            weekBuilder.append(wednesdayCheckBox.isSelected() ? "4," : "");
            weekBuilder.append(thursdayCheckBox.isSelected() ? "5," : "");
            weekBuilder.append(fridayCheckBox.isSelected() ? "6," : "");
            weekBuilder.append(saturdayCheckBox.isSelected() ? "7," : "");
            dayOfweekExpression = weekBuilder.toString().substring(0, weekBuilder.toString().length() - 1);

            // Automatically modify Day fragment to "?" because a Day of Week is selected and it only works with "?".
            cronFragments[ScheduledTaskBasicInterval.EVERY_DAY.ordinal()] = "?";
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
        EVERY_SECOND(resources.getString("edit.cron.second"), "{0} * * * * ?", "\\*\\/\\d+ \\* \\* \\* \\* \\?"),
        EVERY_MINUTE(resources.getString("edit.cron.minute"), "0 {0} * * * ?", "0 \\*\\/\\d+ \\* \\* \\* \\?"),
        EVERY_HOUR(resources.getString("edit.cron.hour"), "0 0 {0} * * ?", "0 0 \\*\\/\\d+ \\* \\* \\?"),
        EVERY_DAY(resources.getString("edit.cron.day"), "0 0 0 {0} * ?", "0 0 0 \\*\\/\\d+ \\* \\?"),
        EVERY_MONTH(resources.getString("edit.cron.month"), "0 0 0 1 {0} ?", "0 0 0 1 \\*\\/\\d+ \\?"),
        EVERY_WEEK(resources.getString("edit.cron.week"), "0 0 0 ? * {0}", "0 0 0 ? \\* \\*\\/\\d+");

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
            return MessageFormat.format(cronExpression, "*/" + interval);
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

    private void enableDisableDayInputs() {
        if (mondayCheckBox.isSelected() || tuesdayCheckBox.isSelected() || wednesdayCheckBox.isSelected() ||
                thursdayCheckBox.isSelected() || fridayCheckBox.isSelected() || saturdayCheckBox.isSelected() || sundayCheckBox.isSelected()) {
            dayTextField.setEnabled(false);
            dayEditButton.setEnabled(false);
        } else if (advancedRadioButton.isSelected()) {
            if ("?".equals(dayTextField.getText())) {
                dayTextField.setText("*");
            }
            dayTextField.setEnabled(true);
            dayEditButton.setEnabled(true);
        }
    }
}
