package com.l7tech.external.assertions.logmessagetosyslog.console;

import com.l7tech.console.panels.LogSinkManagerWindow;
import com.l7tech.console.panels.SinkConfigurationPropertiesDialog;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion;
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
import com.l7tech.policy.exporter.ExternalReference;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * ResolveForeignLogMessageToSysLogPanel represents a panel in the WizardStepPanel which is created when a user imports
 * a policy containing a Log Message to Syslog Assertion referring to an unknown log sink.
 *
 * The user has the choice to intervene and create a new log sink, change the log sink reference, remove the assertion
 * or import the erroneous assertion as-is.
 *
 * @author huaal03
 * @see WizardStepPanel
 * @see LogMessageToSysLogExternalReference
 */
public class ResolveForeignLogMessageToSysLogPanel extends WizardStepPanel<ExternalReference> {
    private static final Logger logger = Logger.getLogger(ResolveForeignLogMessageToSysLogPanel.class.getName());

    private LogMessageToSysLogExternalReference foreignRef;
    private JPanel mainPanel;
    private JRadioButton removeAssertionThatReferRadioButton;
    private JRadioButton changeAssertionToUseRadioButton;
    private JRadioButton importErroneousAssertionAsRadioButton;
    private JComboBox<SinkConfiguration> logSinkChoice;
    private JButton createANewLogButton;
    private JTextField nameField;
    private JTextField descField;
    private JTextField typeField;
    private JTextField severityField;

    public ResolveForeignLogMessageToSysLogPanel(WizardStepPanel<ExternalReference> next, LogMessageToSysLogExternalReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    /**
     * {@inheritDoc}
     *
     * The user can only finish if they are on the last step panel
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    /**
     * Provides a short message on the left step panel describing the current issue requiring user action
     *
     * @return {@inheritDoc}
     */
    @Override
    public String getStepLabel() {
        return "Unresolved Log Sink " + foreignRef.getLogSinkName();
    }

    /**
     * The text shown in the bottom right panel of the wizard
     *
     * @return a more detailed message of the issue requiring action
     */
    @Override
    public String getDescription() {
        return "There is an unresolved log sink reference in the 'Log Message to Syslog Assertion'";
    }

    /**
     * Initializes the Log Message to Syslog Assertion wizard step panel when the user imports a policy containing this
     * assertion and the log message to syslog external reference references an unknown log sink
     */
    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        nameField.setText(foreignRef.getLogSinkName());
        descField.setText(foreignRef.getLogSinkDescription());
        typeField.setText(foreignRef.getLogSinkType().toString());
        severityField.setText(foreignRef.getLogSinkSeverity().toString());

        removeAssertionThatReferRadioButton.setSelected(true);
        logSinkChoice.setEnabled(false);

        changeAssertionToUseRadioButton.addActionListener(e -> logSinkChoice.setEnabled(true));
        removeAssertionThatReferRadioButton.addActionListener(e -> logSinkChoice.setEnabled(false));
        importErroneousAssertionAsRadioButton.addActionListener(e -> logSinkChoice.setEnabled(false));

        logSinkChoice.setRenderer(new TextListCellRenderer<>(this::getConnectorInfo));

        createANewLogButton.addActionListener(actionEvent -> createLogSink());

        populateComboBox();
        enableAndDisableComponents();
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean onNextButton() {
        if (changeAssertionToUseRadioButton.isSelected()) {
            if (logSinkChoice.getSelectedIndex() < 0) return false;

            final SinkConfiguration logSink = (SinkConfiguration) logSinkChoice.getSelectedItem();
            foreignRef.setLocalizeReplace(logSink.getId());
        } else if (removeAssertionThatReferRadioButton.isSelected()) {
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
            removeAssertionThatReferRadioButton.setSelected(true);
        }
    }

    private void populateComboBox() {
        final Object selectedItem = logSinkChoice.getSelectedItem();

        LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
        Collection<SinkConfiguration> allLogSinks;
        try {
            allLogSinks = logSinkAdmin.findAllSinkConfigurations();

            Collection<SinkConfiguration> sysLogSinks = allLogSinks.stream()
                    .filter(oneLogSink -> (oneLogSink.getType() == SinkConfiguration.SinkType.SYSLOG
                            && oneLogSink.getName().startsWith(LogMessageToSysLogAssertion.SYSLOG_LOG_SINK_PREFIX)))
                    .collect(Collectors.toList());

            if (!sysLogSinks.isEmpty()) {
                ((List<SinkConfiguration>) sysLogSinks).sort((o1, o2)
                        -> o1.getName().compareToIgnoreCase(o2.getName()));

                logSinkChoice.setModel(Utilities.comboBoxModel(sysLogSinks));

                if (selectedItem != null && logSinkChoice.getModel().getSize() > 0) {
                    logSinkChoice.setSelectedItem(selectedItem);
                    if (logSinkChoice.getSelectedIndex() == -1) {
                        logSinkChoice.setSelectedIndex(0);
                    }
                }
            }
        } catch (FindException e) {
            logger.warning("Cannot find log sink");
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
        DialogDisplayer.display(dlg, () -> {
            if (dlg.isConfirmed()) {
                Runnable reedit = () -> editAndSave(sinkConfiguration);
                try {
                    LogSinkAdmin logSinkAdmin = Registry.getDefault().getLogSinkAdmin();
                    Goid oid = logSinkAdmin.saveSinkConfiguration(sinkConfiguration);
                    if (!Goid.equals(oid, sinkConfiguration.getGoid())) sinkConfiguration.setGoid(oid);

                    populateComboBox();

                    for (int i = 0; i < logSinkChoice.getModel().getSize(); i++) {
                        SinkConfiguration curConfig = logSinkChoice.getItemAt(i);
                        if (curConfig.getName().equals(sinkConfiguration.getName())) {
                            logSinkChoice.setSelectedItem(curConfig);
                            changeAssertionToUseRadioButton.setEnabled(true);
                            changeAssertionToUseRadioButton.setSelected(true);
                            logSinkChoice.setEnabled(true);
                        }
                    }
                } catch (SaveException | UpdateException e) {
                    logger.log(Level.WARNING, "Log Sink save failed");
                    DialogDisplayer.showMessageDialog(logSinkManagerWindow,
                            "Cannot save Log Sink: (name) must be unique",
                            "Error Saving new Log Sink",
                            JOptionPane.ERROR_MESSAGE, reedit);
                }
            }
        });
    }

    private String getConnectorInfo(final SinkConfiguration sink) {
        return sink.getName();
    }
}
