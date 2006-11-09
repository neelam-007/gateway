package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.SsmPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

public class AuditAlertOptionsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(AuditAlertOptionsDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox enableAuditAlerts;
    private JComboBox auditLevelChoice;
    private JPanel optionsPanel;
    private JComboBox intervalChooser;

    private AuditAlertConfigBean configBean;

    public AuditAlertConfigBean getConfigBean() {
        return configBean;
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
            if (seconds > MAX_SECONDS_BEFORE_MINUTES) return String.valueOf(seconds/60) + " minutes";
            return String.valueOf(seconds) + " seconds";
        }

        public boolean equals(Object other) {
            if (other instanceof IntervalWrapper) {
                IntervalWrapper o = (IntervalWrapper) other;
                return o == this || o.getSeconds() == this.getSeconds();
            } else {
                return false;
            }
        }
    }

    public AuditAlertOptionsDialog(Frame owner) {
        super(owner, true);
        if (TopComponents.getInstance().isApplet())
            configBean = new AuditAlertConfigBean();
        else
            configBean = new AuditAlertConfigBean(TopComponents.getInstance().getPreferences());
        
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        initComponents();
        enableOptions();
    }

    private void initComponents() {

        boolean alertsEnabled = configBean.isEnabled();
        Level preferredLevel = configBean.getAuditAlertLevel();
        int preferredInterval = configBean.getAuditCheckInterval();

        auditLevelChoice.setModel(new DefaultComboBoxModel(new String[] {
                Level.WARNING.getLocalizedName(),
                Level.INFO.getLocalizedName()
        }));
        auditLevelChoice.setSelectedItem(preferredLevel.getLocalizedName());

        intervalChooser.setModel(new DefaultComboBoxModel(new IntervalWrapper[] {
                new IntervalWrapper(10),
                new IntervalWrapper(30),
                new IntervalWrapper(60),
                new IntervalWrapper(120),
                new IntervalWrapper(300),
                new IntervalWrapper(600),
        }));
        intervalChooser.setSelectedItem(preferredInterval);

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
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void enableOptions() {
        Utilities.setEnabled(optionsPanel, enableAuditAlerts.isSelected());
    }

    private void onOK() {

        try {
            configBean.savePreferences();
        } catch (IOException e) {
            logger.warning("Couldn't save preferences for audit alerts options: " + e.getMessage());
        }

        //TODO updateAlertBar();
        dispose();
    }

//    private void updateAlertBar() {
//    }

    private void onCancel() {
        dispose();
    }
}
