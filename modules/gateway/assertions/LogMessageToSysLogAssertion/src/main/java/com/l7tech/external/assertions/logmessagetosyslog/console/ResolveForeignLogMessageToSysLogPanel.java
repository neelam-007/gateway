package com.l7tech.external.assertions.logmessagetosyslog.console;

import com.l7tech.console.panels.LogSinkManagerWindow;
import com.l7tech.console.panels.SinkConfigurationPropertiesDialog;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogExternalReference;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by huaal03 on 2017-06-07.
 */
public class ResolveForeignLogMessageToSysLogPanel extends WizardStepPanel {
    protected static final Logger logger = Logger.getLogger(ResolveForeignLogMessageToSysLogPanel.class.getName());

    private LogMessageToSysLogExternalReference foreignRef;
    private JPanel mainPanel;
    private JRadioButton removeAssertionsThatUseRadioButton;
    private JRadioButton changeAssertionToUseRadioButton;
    private JRadioButton importErroneousAssertionAsRadioButton;
    private JComboBox logSinkChoice;
    private JButton createANewLogButton;
    private JTextField nameField;
    private JTextField descField;
    private JTextField typeField;
    private JTextField severityField;
    private JPanel Action;

    public ResolveForeignLogMessageToSysLogPanel(WizardStepPanel next, LogMessageToSysLogExternalReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved Log Message to Syslog log sink";
    }

    @Override
    public String getDescription() {
        return "There is an unresolved log sink reference in the 'Log Message to Syslog Assertion'";
    }

    public void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        nameField.setText(foreignRef.getLogSinkName());
        descField.setText(foreignRef.getLogSinkDescription());
        typeField.setText(foreignRef.getLogSinkType().toString());
        severityField.setText(foreignRef.getLogSinkSeverity().toString());

        removeAssertionsThatUseRadioButton.setSelected(true);
        logSinkChoice.setEnabled(false);

        changeAssertionToUseRadioButton.addActionListener(e -> logSinkChoice.setEnabled(true));
        removeAssertionsThatUseRadioButton.addActionListener(e -> logSinkChoice.setEnabled(false));
        importErroneousAssertionAsRadioButton.addActionListener(e -> logSinkChoice.setEnabled(false));

        logSinkChoice.setRenderer(new TextListCellRenderer<SinkConfiguration>(sinkConfig -> getConnectorInfo(sinkConfig)));

        createANewLogButton.addActionListener(actionEvent -> createLogSink());

        populateComboBox();
        enableAndDisableComponents();
    }

    @Override
    public boolean onNextButton() {
        if (changeAssertionToUseRadioButton.isSelected()) {
            if (logSinkChoice.getSelectedIndex() < 0) return false;

            final SinkConfiguration logSink = (SinkConfiguration) logSinkChoice.getSelectedItem();
            foreignRef.setLocalizeReplace(logSink.getId());
        } else if (removeAssertionsThatUseRadioButton.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (importErroneousAssertionAsRadioButton.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = logSinkChoice.getModel().getSize() > 0;
        changeAssertionToUseRadioButton.setEnabled(enableSelection);

        if (!changeAssertionToUseRadioButton.isEnabled() && changeAssertionToUseRadioButton.isSelected()) {
            removeAssertionsThatUseRadioButton.setSelected(true);
        }
    }

    private void populateComboBox() {
        final Object selectedItem = logSinkChoice.getSelectedItem();

        LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
        Collection<SinkConfiguration> allLogSinks;
        try {
            allLogSinks = logSinkAdmin.findAllSinkConfigurations();
            Collection<SinkConfiguration> sysLogSinks = new ArrayList<>();

            for (SinkConfiguration sinkConfig : allLogSinks) {
                if (sinkConfig.getType() == SinkConfiguration.SinkType.SYSLOG
                        && sinkConfig.getName().startsWith("syslogwrite_")) {
                    sysLogSinks.add(sinkConfig);
                }
            }

            Collections.sort((List<SinkConfiguration>) sysLogSinks, (o1, o2)
                    -> o1.getName().compareToIgnoreCase(o2.getName()));

            logSinkChoice.setModel(Utilities.comboBoxModel(sysLogSinks));

            if (selectedItem != null && logSinkChoice.getModel().getSize() > 0) {
                logSinkChoice.setSelectedItem(selectedItem);
                if (logSinkChoice.getSelectedIndex() == -1) {
                    logSinkChoice.setSelectedIndex(0);
                }
            }
        } catch (FindException e) {
            e.printStackTrace();
        }
    }

    private void createLogSink() {
        editAndSave(new SinkConfiguration());
    }

    private void editAndSave(final SinkConfiguration sinkConfiguration) {
        sinkConfiguration.setName(foreignRef.getLogSinkName());
        sinkConfiguration.setDescription(foreignRef.getLogSinkDescription());
        sinkConfiguration.setEnabled(false);
        sinkConfiguration.setType(foreignRef.getLogSinkType());
        sinkConfiguration.setSeverity(foreignRef.getLogSinkSeverity());

        LogSinkManagerWindow logSinkManagerWindow = new LogSinkManagerWindow(TopComponents.getInstance().getTopParent());
        final SinkConfigurationPropertiesDialog dlg = new SinkConfigurationPropertiesDialog(logSinkManagerWindow,
                sinkConfiguration, false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            editAndSave(sinkConfiguration);
                        }
                    };
                    try {
                        LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
                        Goid oid = logSinkAdmin.saveSinkConfiguration(sinkConfiguration);
                        if (!Goid.equals(oid, sinkConfiguration.getGoid())) sinkConfiguration.setGoid(oid);

                        populateComboBox();

                        for (int i = 0; i < logSinkChoice.getModel().getSize(); i++) {
                            SinkConfiguration curConfig = (SinkConfiguration) logSinkChoice.getItemAt(i);
                            if (curConfig.getName().equals(sinkConfiguration.getName())) {
                                logSinkChoice.setSelectedItem(curConfig);
                            }
                        }
                        changeAssertionToUseRadioButton.setEnabled(true);
                        changeAssertionToUseRadioButton.setSelected(true);
                        logSinkChoice.setEnabled(true);

                    } catch (SaveException | UpdateException e) {
                        logger.log(Level.WARNING, "Log Sink save failed");
                        DialogDisplayer.showMessageDialog(logSinkManagerWindow,
                                "Cannot save Log Sink: (name) must be unique",
                                "Error Saving new Log Sink",
                                JOptionPane.ERROR_MESSAGE, reedit);
                    }
                }
            }
        });
    }

    private String getConnectorInfo(final SinkConfiguration sink) {
        final StringBuilder builder = new StringBuilder();
        builder.append(sink.getName());
        return builder.toString();
    }
}
