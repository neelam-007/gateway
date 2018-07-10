package com.l7tech.external.assertions.gatewaymetrics.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.gatewaymetrics.GatewayMetricsAssertion;
import com.l7tech.external.assertions.gatewaymetrics.IntervalTimeUnit;
import com.l7tech.external.assertions.gatewaymetrics.IntervalType;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gui.widgets.BetterComboBox;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateConsumer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GatewayMetricsConfigDialog extends AssertionPropertiesOkCancelSupport<GatewayMetricsAssertion> {
    private static final Logger _logger = Logger.getLogger(GatewayMetricsConfigDialog.class.getName());

    private ServiceAdmin serviceAdmin;
    private ClusterStatusAdmin clusterStatusAdmin;

    private JPanel contentPane;
    private BetterComboBox clusterNodeCombo;
    private BetterComboBox publishedServiceCombo;
    private BetterComboBox resolutionCombo;
    private TargetVariablePanel variablePrefixPanel;
    private JPanel dropServicePanel;
    private JPanel variableServicePanel;
    private JPanel servicePanel;
    private JCheckBox useVariablesCheckbox;
    private JTextField clusterNodeTextField;
    private JTextField publishedServiceTextField;
    private JTextField resolutionTextField;
    private JRadioButton mostRecentIntervalRadioButton;
    private JRadioButton recentNumberOfIntervalsRadioButton;
    private JTextField recentNumberOfIntervalsTextField;
    private JRadioButton recentIntervalsWithinTimePeriodRadioButton;
    private JTextField recentIntervalsWithinTimePeriodTextField;
    private JComboBox intervalTimeUnitComboBox;

    public GatewayMetricsConfigDialog(Window owner, GatewayMetricsAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        init();

    }

    private void init() {
        this.initComponents();

        useVariablesCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePanelsForVariableCheckbox();
            }
        });

        try {
            _clusterNodesUpdateConsumer.update(_clusterNodes, _clusterNodesComboModel);
            clusterNodeCombo.setModel(_clusterNodesComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            _publishedServicesUpdateConsumer.update(_publishedServices, _publishedServicesComboModel);
            publishedServiceCombo.setModel(_publishedServicesComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int fineInterval = getClusterStatusAdmin().getMetricsFineInterval();
        GatewayMetricResolution fineResolution = new GatewayMetricResolution(MetricsBin.RES_FINE, fineInterval);
        GatewayMetricResolution hourlyResolution = new GatewayMetricResolution(MetricsBin.RES_HOURLY, 60 * 60 * 1000);
        GatewayMetricResolution dailyResolution = new GatewayMetricResolution(MetricsBin.RES_DAILY, 24 * 60 * 60 * 1000);
        resolutionCombo.setModel(new DefaultComboBoxModel(new GatewayMetricResolution[]{fineResolution, hourlyResolution, dailyResolution}));

        variablePrefixPanel.setAcceptEmpty(true);

        mostRecentIntervalRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableIntervalComponents();
            }
        });
        recentNumberOfIntervalsRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableIntervalComponents();
            }
        });
        recentIntervalsWithinTimePeriodRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableIntervalComponents();
            }
        });

        intervalTimeUnitComboBox.addItem(IntervalTimeUnit.SECONDS);
        intervalTimeUnitComboBox.addItem(IntervalTimeUnit.MINUTES);
        intervalTimeUnitComboBox.addItem(IntervalTimeUnit.HOURS);
        intervalTimeUnitComboBox.addItem(IntervalTimeUnit.DAYS);
    }

    private static final ClusterNodeInfo ALL_NODES = new ClusterNodeInfo() {
        @Override
        public String toString() {
            return "<All Nodes>";
        }
    };

    private Collection<ClusterNodeInfo> _clusterNodes = new ArrayList<ClusterNodeInfo>();

    private CollectionUpdateConsumer<ClusterNodeInfo, FindException> _clusterNodesUpdateConsumer =
        new CollectionUpdateConsumer<ClusterNodeInfo, FindException>(null) {
            @Override
            protected CollectionUpdate<ClusterNodeInfo> getUpdate(final int oldVersionID) throws FindException {
                return getClusterStatusAdmin().getClusterNodesUpdate(oldVersionID);
            }
        };

    private final DefaultComboBoxModel _clusterNodesComboModel =
        new DefaultComboBoxModel() {
            {
                // First combo box element is permanently the "all node".
                super.addElement(ALL_NODES);
            }

            /**
             * Adds an element in alphabetical order (from the second element on).
             * @param o     a {@link ClusterNodeInfo} object to add
             */
            @Override
            public void addElement(Object o) {
                final ClusterNodeInfo newElement = (ClusterNodeInfo) o;
                int i = 1;
                while (i < getSize() && newElement.compareTo((ClusterNodeInfo) getElementAt(i)) >= 0) {
                    ++i;
                }
                insertElementAt(newElement, i);
            }
        };

    /**
     * List of published services fetched from gateway.
     */
    private Collection<ServiceHeader> _publishedServices = new ArrayList<ServiceHeader>();

    private CollectionUpdateConsumer<ServiceHeader, FindException> _publishedServicesUpdateConsumer =
        new CollectionUpdateConsumer<ServiceHeader, FindException>(new ServiceHeaderDifferentiator()) {
            @Override
            protected CollectionUpdate<ServiceHeader> getUpdate(final int oldVersionID) throws FindException {
                return getServiceAdmin().getPublishedServicesUpdate(oldVersionID);
            }
        };

    /**
     * Combobox item to represent all published services selected.
     */
    private static final ServiceHeader ALL_SERVICES = new ServiceHeader(false, false, "<All Services>", null, null, null, null, null, -1L, -1, null, false, false, null, null);

    private final DefaultComboBoxModel _publishedServicesComboModel =
        new DefaultComboBoxModel() {
            {
                // First combo box element is permanently the "all services".
                super.addElement(ALL_SERVICES);
            }

            /**
             * Adds an element in alphabetical order (excluding the first
             * element since it is the "all services").
             * @param o     a {@link ServiceHeader} object to add
             */
            @Override
            public void addElement(Object o) {
                final ServiceHeader newElement = (ServiceHeader) o;
                int i = 1;
                while (i < getSize() && newElement.getDisplayName().compareToIgnoreCase(((ServiceHeader) getElementAt(i)).getDisplayName()) >= 0) {
                    ++i;
                }
                insertElementAt(newElement, i);
                if (_logger.isLoggable(Level.FINE))
                    _logger.fine("Added published service \"" + newElement.getDisplayName() + "\" to combo box.");
            }

            @Override
            public void removeElement(Object o) {
                super.removeElement(o);
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Removed published service \"" + ((ServiceHeader) o).getDisplayName() + "\" from combo box.");
                }
            }
        };

    private ServiceAdmin getServiceAdmin() {
        if (serviceAdmin == null) {
            serviceAdmin = Registry.getDefault().getServiceManager();
        }
        return serviceAdmin;
    }

    private ClusterStatusAdmin getClusterStatusAdmin() {
        if (clusterStatusAdmin == null) {
            clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        }
        return clusterStatusAdmin;
    }

    private ClusterNodeInfo getClusterNodeInfoById(String id) {
        if (id == null) {
            return (ClusterNodeInfo) clusterNodeCombo.getItemAt(0);
        }

        for (int i = 0; i < clusterNodeCombo.getItemCount(); i++) {
            ClusterNodeInfo clusterNodeInfo = (ClusterNodeInfo) clusterNodeCombo.getItemAt(i);
            if (id.equals(clusterNodeInfo.getId())) {
                return clusterNodeInfo;
            }
        }
        return null;
    }

    private ServiceHeader getPublishedServiceByGoid(Goid goid) {
        if (goid.equals(PublishedService.DEFAULT_GOID)) {
            return (ServiceHeader) publishedServiceCombo.getItemAt(0);
        }

        for (int i = 0; i < publishedServiceCombo.getItemCount(); i++) {
            ServiceHeader serviceHeader = (ServiceHeader) publishedServiceCombo.getItemAt(i);
            if (goid.equals(serviceHeader.getGoid())) {
                return serviceHeader;
            }
        }
        return null;
    }

    private GatewayMetricResolution getResolutionByInt(int resolution) {
        for (int i = 0; i < resolutionCombo.getItemCount(); i++) {
            GatewayMetricResolution gatewayMetricResolution = (GatewayMetricResolution) resolutionCombo.getItemAt(i);
            if (resolution == gatewayMetricResolution.getResolution()) {
                return gatewayMetricResolution;
            }
        }
        return null;
    }

    @Override
    public GatewayMetricsAssertion getData(GatewayMetricsAssertion assertion) throws ValidationException {
        // Check that fields are valid.
        //
        String error = getValidationError();
        if (error != null && !error.trim().isEmpty()) {
            throw new ValidationException(error);
        }

        assertion.setUseVariables(useVariablesCheckbox.isSelected());

        assertion.setClusterNodeId(((ClusterNodeInfo) clusterNodeCombo.getSelectedItem()).getId());
        assertion.setPublishedServiceGoid(((ServiceHeader) publishedServiceCombo.getSelectedItem()).getGoid());
        assertion.setResolution(((GatewayMetricResolution) resolutionCombo.getSelectedItem()).getResolution());

        assertion.setClusterNodeVariable(clusterNodeTextField.getText().trim());
        assertion.setPublishedServiceVariable(publishedServiceTextField.getText().trim());
        assertion.setResolutionVariable(resolutionTextField.getText().trim());

        IntervalType intervalType = IntervalType.MOST_RECENT;
        if (mostRecentIntervalRadioButton.isSelected()) {
            intervalType = IntervalType.MOST_RECENT;
        } else if (recentNumberOfIntervalsRadioButton.isSelected()) {
            intervalType = IntervalType.RECENT_NUMBER_OF_INTERVALS;
        } else if (recentIntervalsWithinTimePeriodRadioButton.isSelected()) {
            intervalType = IntervalType.RECENT_INTERVALS_WITHIN_TIME_PERIOD;
        } else {
            throw new RuntimeException("Interval type not selected.");
        }
        assertion.setIntervalType(intervalType);
        assertion.setNumberOfRecentIntervals(recentNumberOfIntervalsTextField.getText().trim());
        assertion.setNumberOfRecentIntervalsWithinTimePeriod(recentIntervalsWithinTimePeriodTextField.getText().trim());
        assertion.setIntervalTimeUnit((IntervalTimeUnit) intervalTimeUnitComboBox.getSelectedItem());

        assertion.setVariablePrefix(variablePrefixPanel.getVariable());

        return assertion;
    }

    @Override
    public void setData(GatewayMetricsAssertion assertion) {
        useVariablesCheckbox.setSelected(assertion.getUseVariables());

        clusterNodeCombo.setSelectedItem(getClusterNodeInfoById(assertion.getClusterNodeId()));
        publishedServiceCombo.setSelectedItem(getPublishedServiceByGoid(assertion.getPublishedServiceGoid()));
        resolutionCombo.setSelectedItem(getResolutionByInt(assertion.getResolution()));

        clusterNodeTextField.setText(assertion.getClusterNodeVariable());
        publishedServiceTextField.setText(assertion.getPublishedServiceVariable());
        resolutionTextField.setText(assertion.getResolutionVariable());

        switch (assertion.getIntervalType()) {
            case MOST_RECENT:
                mostRecentIntervalRadioButton.setSelected(true);
                break;

            case RECENT_NUMBER_OF_INTERVALS:
                recentNumberOfIntervalsRadioButton.setSelected(true);
                break;

            case RECENT_INTERVALS_WITHIN_TIME_PERIOD:
                recentIntervalsWithinTimePeriodRadioButton.setSelected(true);
                break;

            default:
                throw new RuntimeException("Unexpected interval type.");
        }
        recentNumberOfIntervalsTextField.setText(assertion.getNumberOfRecentIntervals());
        recentIntervalsWithinTimePeriodTextField.setText(assertion.getNumberOfRecentIntervalsWithinTimePeriod());
        intervalTimeUnitComboBox.setSelectedItem(assertion.getIntervalTimeUnit());

        variablePrefixPanel.setSuffixes(assertion.getVariableSuffix());
        variablePrefixPanel.setVariable(assertion.getVariablePrefix());
        variablePrefixPanel.setAssertion(assertion, getPreviousAssertion());

        this.updatePanelsForVariableCheckbox();
        this.enableDisableIntervalComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void updatePanelsForVariableCheckbox() {
        CardLayout c1 = (CardLayout)servicePanel.getLayout();
        // Show the variable panel or the dropbox panel based on the context variables checkbox.
        c1.show(servicePanel, (useVariablesCheckbox.isSelected() ? "variableServicePanel" : "dropServicePanel"));
    }

    private String getValidationError() {
        if (useVariablesCheckbox.isSelected()) {
            if (clusterNodeTextField.getText().trim().isEmpty()) {
                return "The Gateway Node Variable field must not be empty.";
            }
            if (publishedServiceTextField.getText().trim().isEmpty()) {
                return "The Published Service Variable field must not be empty.";
            }
            if (resolutionTextField.getText().trim().isEmpty()) {
                return "The Resolution Variable field must not be empty.";
            }
        } else {
            if (clusterNodeCombo.getSelectedItem() == null) {
                return "The Gateway Node field must be selected.";
            }
            if (publishedServiceCombo.getSelectedItem() == null) {
                return "The Published Service field must be selected.";
            }
            if (resolutionCombo.getSelectedItem() == null) {
                return "The Resolution field must be selected.";
            }
        }

        if (mostRecentIntervalRadioButton.isSelected()) {
            // No additional fields to check.
        } else if (recentNumberOfIntervalsRadioButton.isSelected()) {
            return getIntegerOrContextVariableTextFieldValidationError(recentNumberOfIntervalsTextField, "Recent Interval");
        } else if (recentIntervalsWithinTimePeriodRadioButton.isSelected()) {
            // No need to check interval time unit combo box. One is always selected.
            return getIntegerOrContextVariableTextFieldValidationError(recentIntervalsWithinTimePeriodTextField, "Recent Interval Within");
        } else {
            return "The Interval field must be selected.";
        }

        if (!variablePrefixPanel.isEntryValid()) {
            return "The Variable Prefix field contains invalid syntax.";
        }

        return null;
    }

    // Checks that the specified text field contains a context variable or an integer.
    private String getIntegerOrContextVariableTextFieldValidationError (JTextField textField, String fieldName) {
        String value = textField.getText().trim();
        if (value.isEmpty()) {
            return "The "+ fieldName + " field must not be empty.";
        } else {
            if (VariablePrefixUtil.hasValidDollarCurlyOpenStart(value) &&
                VariablePrefixUtil.hasValidCurlyCloseEnd(value) &&
                VariablePrefixUtil.fixVariableName(value).length() > 0) {
                // The field is a context variable.
                //
                return null;
            } else {
                // Check that it's an int.
                //
                try {
                    Long.parseLong(value);
                    return null;
                } catch (NumberFormatException e) {
                    return "The "+ fieldName + " field must be an integer value.";
                }
            }
        }
    }

    private void enableDisableIntervalComponents() {
        recentNumberOfIntervalsTextField.setEnabled(recentNumberOfIntervalsRadioButton.isSelected());
        recentIntervalsWithinTimePeriodTextField.setEnabled(recentIntervalsWithinTimePeriodRadioButton.isSelected());
        intervalTimeUnitComboBox.setEnabled(recentIntervalsWithinTimePeriodRadioButton.isSelected());
    }
}