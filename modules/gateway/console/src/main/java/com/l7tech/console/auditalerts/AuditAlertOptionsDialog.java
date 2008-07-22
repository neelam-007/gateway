package com.l7tech.console.auditalerts;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;

public class AuditAlertOptionsDialog extends JDialog {

    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox enableAuditAlerts;
    private JComboBox auditLevelChoice;
    private JPanel optionsPanel;
    private JComboBox intervalChooser;
    private JPanel mainPanel;

    private AuditAlertConfigBean configBean;
    private boolean wasCancelled = false;

    public AuditAlertOptionsDialog(Frame owner) {
        super(owner, "Configure Audit Alerts", true);
        configBean = new AuditAlertConfigBean(TopComponents.getInstance().getPreferences());
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(buttonOK);

        initComponents();
        enableOptions();
    }

    public AuditAlertConfigBean getConfigBean() {
        return configBean;
    }

    private void initComponents() {

        boolean alertsEnabled = configBean.isEnabled();
        Level preferredLevel = configBean.getAuditAlertLevel();
        int preferredInterval = configBean.getAuditCheckInterval();

        auditLevelChoice.setModel(new DefaultComboBoxModel(new String[] {
                Level.WARNING.getName(),
                Level.INFO.getName()
        }));
        auditLevelChoice.setSelectedItem(preferredLevel.getName());

        intervalChooser.setModel(new DefaultComboBoxModel(new IntervalWrapper[] {
                new IntervalWrapper(10),
                new IntervalWrapper(30),
                new IntervalWrapper(60),
                new IntervalWrapper(120),
                new IntervalWrapper(300),
                new IntervalWrapper(600),
        }));
        intervalChooser.getModel().setSelectedItem(new IntervalWrapper(preferredInterval));

        enableAuditAlerts.setSelected(alertsEnabled);
        initListeners();
    }

    private void initListeners() {
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        enableAuditAlerts.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                enableOptions();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        mainPanel.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void enableOptions() {
        Utilities.setEnabled(optionsPanel, enableAuditAlerts.isSelected());
    }

    private void onOK() {
        wasCancelled = false;
        configBean.setEnabled(enableAuditAlerts.isSelected());
        configBean.setAuditAlertLevel(Level.parse((String) auditLevelChoice.getSelectedItem()));

        IntervalWrapper interval = (IntervalWrapper) intervalChooser.getModel().getSelectedItem();
        configBean.setAuditCheckInterval(interval.getSeconds());

        dispose();
    }

    private void onCancel() {
        wasCancelled = true;
        dispose();
    }

    public boolean wasCancelled() {
        return wasCancelled;
    }

    private class IntervalWrapper {
        private final static int MAX_SECONDS_BEFORE_MINUTES = 60;

        private int seconds;

        public IntervalWrapper(int seconds) {
            this.seconds = seconds;
        }

        public int getSeconds() {
            return seconds;
        }

        public String toString() {
            if (seconds > MAX_SECONDS_BEFORE_MINUTES)
                return String.valueOf(seconds/60) + " minutes";

            return String.valueOf(seconds) + " seconds";
        }

        public boolean equals(Object other) {
            if (other instanceof IntervalWrapper) {
                IntervalWrapper otherWrapper = (IntervalWrapper) other;
                return otherWrapper == this || otherWrapper.getSeconds() == this.getSeconds();
            } else {
                return false;
            }
        }
    }
}
