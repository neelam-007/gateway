/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.console.MainWindow;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.MetricsChartPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.MetricsSummaryBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * For display of cluster service metrics.
 *
 * <p> This was refactored out of {@link DashboardWindow}.
 * Up to SecureSpan 4.1, service metrics was the only content of DashboardWindow.
 * From SecureSpan 4.2 on, it is displayed in a tab inside.
 *
 * @since SecureSpan 4.2
 * @author rmak
 */
public class ServiceMetricsPanel extends JPanel {

    private JPanel mainPanel;
    private JPanel chartPanel;
    private JLabel statusLabel;
    private JComboBox clusterNodeCombo;
    private JComboBox publishedServiceCombo;
    private JComboBox resolutionCombo;
    private JTabbedPane rightTabbedPane;

    private JLabel selectionFromTimeLabel;
    private JLabel selectionToTimeLabel;
    private JTextField selectionFrontMaxText;
    private JTextField selectionFrontAvgText;
    private JTextField selectionFrontMinText;
    private JTextField selectionBackMaxText;
    private JTextField selectionBackAvgText;
    private JTextField selectionBackMinText;
    private JTextField selectionNumRoutingFailureText;
    private JTextField selectionNumPolicyViolationText;
    private JTextField selectionNumSuccessText;
    private JTextField selectionNumTotalText;
    private JList selectionServicesWithProblemList;

    private JLabel latestFromTimeLabel;
    private JLabel latestToTimeLabel;
    private JTextField latestFrontMaxText;
    private JTextField latestFrontAvgText;
    private JTextField latestFrontMinText;
    private JTextField latestBackMaxText;
    private JTextField latestBackAvgText;
    private JTextField latestBackMinText;
    private JTextField latestNumRoutingFailureText;
    private JTextField latestNumPolicyViolationText;
    private JTextField latestNumSuccessText;
    private JTextField latestNumTotalText;
    private JList latestServicesWithProblemList;

    private static final Logger _logger = Logger.getLogger(ServiceMetricsPanel.class.getName());

    private static final ResourceBundle _resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.ServiceMetricsPanel");

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(_resources.getString("fromTo.timeFormat"));

    private static final MessageFormat STATUS_UPDATED_FORMAT = new MessageFormat(_resources.getString("status.updated"));
    private static final String METRICS_NOT_ENABLED = _resources.getString("status.notEnabled");

    private static final long FINE_CHART_TIME_RANGE = 10 * 60 * 1000; // 10 minutes
    private static final long HOURLY_CHART_TIME_RANGE = 60 * 60 * 60 * 1000; // 60 hours
    private static final long DAILY_CHART_TIME_RANGE = 60 * 24 * 60 * 60 * 1000L; // 60 days

    /** Index of tab panel for currently selected period. */
    private static final int SELECTION_TAB_INDEX = 0;

    /** Index of tab panel for latest period summary. */
    private static final int LATEST_TAB_INDEX = 1;

    private final Resolution _fineResolution;
    private final Resolution _hourlyResolution;
    private final Resolution _dailyResolution;
    private Resolution _currentResolution;
    private final MetricsChartPanel _metricsChartPanel;

    private final DefaultComboBoxModel _clusterNodeComboModel;
    private final DefaultComboBoxModel _publishedServiceComboModel;

    /** List of cluster nodes last fetched from gateway; sorted. */
    private ClusterNodeInfo[] _clusterNodes;

    /** Stands for cluster nodes selected. */
    private static final ClusterNodeInfo ALL_NODES = new ClusterNodeInfo() {
        public String toString() {
            return _resources.getString("clusterNodeCombo.allValue");
        }
    };

    /** List of published services last fetched from gateway; sorted. */
    private EntityHeader[] _publishedServices;

    /** Stands for all services selected. */
    private static final EntityHeader ALL_SERVICES = new EntityHeader() {
        public String toString() {
            return _resources.getString("publishedServiceCombo.allValue");
        }
    };

    private ClusterStatusAdmin _clusterStatusAdmin;
    private ServiceAdmin _serviceAdmin;

    private final ActionListener _refreshListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            refreshData();
        }
    };

    private final Timer _refreshTimer = new Timer(2500, _refreshListener);

    private long _latestDownloadedPeriodStart = -1;

    /** Whether previous attempt to connect to gateway was successful. */
    private boolean _connected = false;

    private static final String SERVICES_WITH_PROBLEM_TOOLTIP = _resources.getString("servicesWithProblem.tooltip");

    // Icons for use in the "services with problem" listbox.
    // Declared non-static only to avoid problem with class dependency check.
    private final ImageIcon ROUTING_FAILURE_ICON = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/ServicesWithRoutingFailure.gif"));
    private final ImageIcon POLICY_VIOLATION_ICON = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/ServicesWithPolicyViolation.gif"));
    private final ImageIcon BOTH_PROBLEMS_ICON = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/ServicesWithBothProblems.gif"));

    private DefaultListModel _selectionServicesWithProblemListModel = new DefaultListModel();
    private DefaultListModel _latestServicesWithProblemListModel = new DefaultListModel();

    /** An element in the "services with problem" listbox. */
    private static class ProblemListElement {
        private final ImageIcon _icon;
        private final EntityHeader _publishedService;
        public ProblemListElement(ImageIcon icon, EntityHeader publishedService) {
            _icon = icon;
            _publishedService = publishedService;
        }
        public ImageIcon getIcon() {return _icon;}
        public EntityHeader getPublishedService() {return _publishedService;}
    }

    /**
     * Renderer to paint a problem indicator icon next to the service name in
     * the "services with problem" listbox. The icon indicates either routing
     * failure only, policy violation only, or both.
     */
    private class ProblemListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            ProblemListElement e = (ProblemListElement)value;
            super.getListCellRendererComponent(list, e.getPublishedService().getName(), index, isSelected, cellHasFocus);
            setIcon(e.getIcon());
            return this;
        }
    }

    public ServiceMetricsPanel() {
        final ProblemListCellRenderer problemListCellRenderer = new ProblemListCellRenderer();
        final Cursor handCursor = new Cursor(Cursor.HAND_CURSOR);
        selectionServicesWithProblemList.setModel(_selectionServicesWithProblemListModel);
        selectionServicesWithProblemList.setCellRenderer(problemListCellRenderer);
        selectionServicesWithProblemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionServicesWithProblemList.setCursor(handCursor);
        selectionServicesWithProblemList.setToolTipText(SERVICES_WITH_PROBLEM_TOOLTIP);
        selectionServicesWithProblemList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                final ProblemListElement selected = (ProblemListElement)selectionServicesWithProblemList.getSelectedValue();
                if (selected != null) {
                    publishedServiceCombo.setSelectedItem(selected.getPublishedService());
                }
            }
        });
        latestServicesWithProblemList.setModel(_latestServicesWithProblemListModel);
        latestServicesWithProblemList.setCellRenderer(problemListCellRenderer);
        latestServicesWithProblemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        latestServicesWithProblemList.setCursor(handCursor);
        latestServicesWithProblemList.setToolTipText(SERVICES_WITH_PROBLEM_TOOLTIP);
        latestServicesWithProblemList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                final ProblemListElement selected = (ProblemListElement)latestServicesWithProblemList.getSelectedValue();
                if (selected != null)
                    publishedServiceCombo.setSelectedItem(selected.getPublishedService());
            }
        });

        rightTabbedPane.setSelectedIndex(LATEST_TAB_INDEX);
        statusLabel.setText(" ");

        int fineInterval = getClusterStatusAdmin().getMetricsFineInterval();;

        _fineResolution = new Resolution(MetricsBin.RES_FINE, fineInterval, FINE_CHART_TIME_RANGE);
        _hourlyResolution = new Resolution(MetricsBin.RES_HOURLY, 60 * 60 * 1000, HOURLY_CHART_TIME_RANGE);
        _dailyResolution = new Resolution(MetricsBin.RES_DAILY, 24 * 60 * 60 * 1000, DAILY_CHART_TIME_RANGE);
        _currentResolution = _fineResolution;

        _metricsChartPanel = new MetricsChartPanel(_currentResolution.getResolution(),
                                                   _currentResolution.getBinInterval(),
                                                   _currentResolution.getChartTimeRange(),
                                                   this);
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(_metricsChartPanel, BorderLayout.CENTER);

        rightTabbedPane.setTitleAt(LATEST_TAB_INDEX, _currentResolution.getLatestTabTitle());

        resolutionCombo.setModel(new DefaultComboBoxModel(new Resolution[]{_fineResolution, _hourlyResolution, _dailyResolution}));
        resolutionCombo.setSelectedItem(_currentResolution);
        resolutionCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _currentResolution = (Resolution)resolutionCombo.getSelectedItem();
                rightTabbedPane.setTitleAt(LATEST_TAB_INDEX, _currentResolution.getLatestTabTitle());
                rightTabbedPane.setSelectedIndex(LATEST_TAB_INDEX);
                resetData(false);   // Cannot keep selected period when changing resolution.
            }
        });

        try {
            _clusterNodes = findAllClusterNodes();
            ClusterNodeInfo[] comboItems = new ClusterNodeInfo[_clusterNodes.length + 1];
            System.arraycopy(_clusterNodes, 0, comboItems, 1, _clusterNodes.length);
            comboItems[0] = ALL_NODES;
            _clusterNodeComboModel = new DefaultComboBoxModel(comboItems);
            clusterNodeCombo.setModel(_clusterNodeComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        publishedServiceCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetData(true);    // Keep selected period to aid user.
            }
        });

        try {
            _publishedServices = findAllPublishedServices();
            final EntityHeader[] comboItems = new EntityHeader[_publishedServices.length + 1];
            System.arraycopy(_publishedServices, 0, comboItems, 1, _publishedServices.length);
            comboItems[0] = ALL_SERVICES;
            _publishedServiceComboModel = new DefaultComboBoxModel(comboItems);
            publishedServiceCombo.setModel(_publishedServiceComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        clusterNodeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetData(true);    // Keep selected period to aid user.
            }
        });

        resetData(false);
        _refreshTimer.setInitialDelay(0);   /** So that {@link #resetData} will be more snappy. */
        _refreshTimer.start();
    } /* constructor */

    private ClusterNodeInfo[] findAllClusterNodes() throws FindException {
        final ClusterNodeInfo[] result = getClusterStatusAdmin().getClusterStatus();
        Arrays.sort(result);
        return result;
    }

    private EntityHeader[] findAllPublishedServices() throws FindException {
        final EntityHeader[] result = getServiceAdmin().findAllPublishedServices();
        updateServiceNames(result);
        Arrays.sort(result, new Comparator<EntityHeader>() {
            public int compare(EntityHeader eh1, EntityHeader eh2) {
                String name1 = eh1.getName();
                String name2 = eh2.getName();

                if (name1 == null) name1 = "";
                if (name2 == null) name2 = "";

                return name1.toLowerCase().compareTo(name2.toLowerCase());
            }
        });
        return result;
    }

    private void updateClusterNodesCombo() throws FindException {
        final ClusterNodeInfo[] newNodes = findAllClusterNodes();
        if (!Arrays.equals(_clusterNodes, newNodes)) {
            updateComboModel(_clusterNodes, newNodes, _clusterNodeComboModel);
        }
        _clusterNodes = newNodes;
    }

    private void updatePublishedServicesCombo() throws FindException {
            final EntityHeader[] newServices = findAllPublishedServices();
            if (!Arrays.equals(_publishedServices, newServices)) {
                updateComboModel(_publishedServices, newServices, _publishedServiceComboModel);
            }
            _publishedServices = newServices;
    }

    public ClusterNodeInfo getClusterNodeSelected() {
        ClusterNodeInfo result = (ClusterNodeInfo) _clusterNodeComboModel.getSelectedItem();
        if (result == ALL_NODES)
            result = null;
        return result;
    }

    public EntityHeader getPublishedServiceSelected() {
        EntityHeader result = (EntityHeader) _publishedServiceComboModel.getSelectedItem();
        if (result == ALL_SERVICES)
            result = null;
        return result;
    }

    public void updateTimeZone() {
        // Gets time zone on gateway.
        TimeZone tz = TimeZone.getTimeZone(getClusterStatusAdmin().getCurrentClusterTimeZone());
        if (tz == null) {
            _logger.warning("Failed to get time zone from gateway. Falling back to use local time zone for display.");
            tz = TimeZone.getDefault();
        }
        TIME_FORMAT.setTimeZone(tz);
    }

    public void onLogon(LogonEvent e) {
        _clusterStatusAdmin = null;
        _serviceAdmin = null;
        _refreshTimer.start();
        clusterNodeCombo.setEnabled(true);
        publishedServiceCombo.setEnabled(true);
        resolutionCombo.setEnabled(true);
        setSelectedBin(null, -1, -1, false);
        updateTimeZone();
        _connected = true;
    }

    public void onLogoff(LogonEvent e) {
        _refreshTimer.stop();
        if (_connected) {
            statusLabel.setText(statusLabel.getText().trim() + " [Disconnected]");
        } else {
            // Was already disconnected due to some error. Clears out the exception message.
            statusLabel.setText("[Disconnected]");
        }
        clusterNodeCombo.setEnabled(false);
        publishedServiceCombo.setEnabled(false);
        resolutionCombo.setEnabled(false);
        _connected = false;
    }

    /**
     * Enables/disables periodic refresh.
     *
     * @param enabled   true if enabled
     */
    public void setRefreshEnabled(boolean enabled) {
        if (enabled) {
            _refreshTimer.start();
            _metricsChartPanel.restoreAutoRange(); // In case chart was zoomed in when closed.
            _metricsChartPanel.resumeUpdate();     // In case chart was suspended when closed.
        } else {
            _refreshTimer.stop();
        }
    }

    public void dispose() {
        _refreshTimer.stop();
    }

    /**
     * Clears all data and updates the chart.
     *
     * @param saveSelectedPeriod    whether to keep period selected (if any) in the chart
     *                              around when data is available again when refreshed
     */
    private synchronized void resetData(final boolean saveSelectedPeriod) {
        _refreshTimer.stop();

        _latestDownloadedPeriodStart = -1;
        _metricsChartPanel.clearData(saveSelectedPeriod);
        _metricsChartPanel.setResolution(_currentResolution.getResolution());
        _metricsChartPanel.setBinInterval(_currentResolution.getBinInterval());
        _metricsChartPanel.setMaxTimeRange(_currentResolution.getChartTimeRange());

        setSelectedBin(null, -1, -1, false);

        latestFromTimeLabel.setText("");
        latestToTimeLabel.setText("");
        latestFrontMinText.setText("");
        latestFrontAvgText.setText("");
        latestFrontMaxText.setText("");
        latestBackMinText.setText("");
        latestBackAvgText.setText("");
        latestBackMaxText.setText("");
        latestNumPolicyViolationText.setText("");
        latestNumRoutingFailureText.setText("");
        latestNumSuccessText.setText("");
        latestNumTotalText.setText("");

        _refreshTimer.start();
    }

    private synchronized void refreshData() {
        try {
            final ClusterStatusAdmin clusterStatusAdmin = getClusterStatusAdmin();

            if (!clusterStatusAdmin.isMetricsEnabled()) {
                statusLabel.setText(METRICS_NOT_ENABLED);
                // Skips refresh if service metrics collection is disabled on server. (Bug 4053)
                return;
            }

            updateClusterNodesCombo();
            updatePublishedServicesCombo();

            String nodeId = null;
            final ClusterNodeInfo node = (ClusterNodeInfo) _clusterNodeComboModel.getSelectedItem();
            if (node != ALL_NODES) {
                nodeId = node.getNodeIdentifier();
            }

            long[] serviceOids = null;
            final EntityHeader service = (EntityHeader) _publishedServiceComboModel.getSelectedItem();
            if (service != ALL_SERVICES) {
                serviceOids = new long[]{service.getOid()};
            }

            final Integer resolution = _currentResolution.getResolution();

            // -----------------------------------------------------------------
            // Updates the chart.
            // -----------------------------------------------------------------

            Collection<MetricsSummaryBin> newBins = null;
            if (_latestDownloadedPeriodStart == -1) {
                newBins = clusterStatusAdmin.summarizeLatestByPeriod(nodeId,
                                                                     serviceOids,
                                                                     resolution,
                                                                     _currentResolution.getChartTimeRange() +
                                                                     _currentResolution.getBinInterval(),
                                                                     true); // (Bug 3855) Need to include empty uptime bins in order for moving chart to advance when there are no request message.
            } else {
                newBins = clusterStatusAdmin.summarizeByPeriod(nodeId,
                                                               serviceOids,
                                                               resolution,
                                                               _latestDownloadedPeriodStart + 1,
                                                               null,
                                                               true);       // (Bug 3855) Need to include empty uptime bins in order for moving chart to advance when there are no request message.
            }

            MetricsSummaryBin latestBin = null;
            if (newBins.size() > 0) {
                for (MetricsSummaryBin bin : newBins) {
                    if (_latestDownloadedPeriodStart < bin.getPeriodStart()) {
                        _latestDownloadedPeriodStart = bin.getPeriodStart();
                        latestBin = bin;    // For fine resolution, use this in "Latest" tab.
                    }
                }

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Dowloaded " + newBins.size() + " " + MetricsBin.describeResolution(resolution) +
                                 " summary bins. Latest bin = " + new Date(latestBin.getPeriodStart()) +
                                 " - " + new Date(latestBin.getPeriodEnd()));
                }

                _metricsChartPanel.addData(newBins);
            }

            // -----------------------------------------------------------------
            // Updates the "Latest" tab panel.
            // -----------------------------------------------------------------

            // Gets a summary of the latest resolution period.
            if (_currentResolution == _fineResolution) {
                // The latest fine resolution summary bin was already downloaded,
                // either during this call or the previous call.
            } else if (_currentResolution == _hourlyResolution) {
                // Gets a summary collated from an hour's worth of fine metrics bins.
                latestBin = clusterStatusAdmin.summarizeLatest(nodeId, serviceOids, MetricsBin.RES_FINE, 60 * 60 * 1000, true);
            } else if (_currentResolution == _dailyResolution) {
                // Gets a summary collated from a day's worth of hourly metrics bins.
                latestBin = clusterStatusAdmin.summarizeLatest(nodeId, serviceOids, MetricsBin.RES_HOURLY, 24 * 60 * 60 * 1000, false /* only FINE resolution has empty uptime bins */);
            }

            if (latestBin != null) {
                latestFromTimeLabel.setText(TIME_FORMAT.format(new Date(latestBin.getPeriodStart())));
                latestToTimeLabel.setText(TIME_FORMAT.format(new Date(latestBin.getPeriodEnd())));

                latestFrontMaxText.setText(Integer.toString(latestBin.getMaxFrontendResponseTime()));
                latestFrontAvgText.setText(Long.toString(Math.round(latestBin.getAverageFrontendResponseTime())));
                latestFrontMinText.setText(Integer.toString(latestBin.getMinFrontendResponseTime()));

                latestBackMaxText.setText(Integer.toString(latestBin.getMaxBackendResponseTime()));
                latestBackAvgText.setText(Long.toString(Math.round(latestBin.getAverageBackendResponseTime())));
                latestBackMinText.setText(Integer.toString(latestBin.getMinBackendResponseTime()));

                latestNumRoutingFailureText.setText(Integer.toString(latestBin.getNumRoutingFailure()));
                latestNumPolicyViolationText.setText(Integer.toString(latestBin.getNumPolicyViolation()));
                latestNumSuccessText.setText(Integer.toString(latestBin.getNumSuccess()));
                latestNumTotalText.setText(Integer.toString(latestBin.getNumTotal()));

                _latestServicesWithProblemListModel.clear();
                for (EntityHeader svc : _publishedServices) {   // Loop over sorted set so that the list box will be sorted too.
                    final boolean hasRF = latestBin.getServicesWithRoutingFailure().contains(svc.getOid());
                    final boolean hasPV = latestBin.getServicesWithPolicyViolation().contains(svc.getOid());
                    if (hasRF || hasPV) {
                        ImageIcon icon = null;
                        if (hasRF && hasPV) {
                            icon = BOTH_PROBLEMS_ICON;
                        } else if (hasRF) {
                            icon = ROUTING_FAILURE_ICON;
                        } else {
                            icon = POLICY_VIOLATION_ICON;
                        }
                        _latestServicesWithProblemListModel.addElement(new ProblemListElement(icon, svc));
                    }
                }
            }

            // -----------------------------------------------------------------
            // Updates the status label.
            // -----------------------------------------------------------------

            statusLabel.setText(STATUS_UPDATED_FORMAT.format(new Object[] { new Date() }));

            if (!_connected) {  // Previously disconnected.
                _logger.log(Level.INFO, "Reconnected to SSG.");
            }
            _connected = true;
        } catch (RuntimeException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Unable to get dashboard data.");
            _refreshTimer.stop();
            dispose();
        } catch (FindException e) {
            _logger.log(Level.WARNING, "SSG can't get data", e);
            statusLabel.setText("[Problem on Gateway] " + e.getMessage() == null ? "" : e.getMessage());
            _refreshTimer.stop();
            dispose();
        }
    }

    /**
     * Populates the "Selection" tab with data in the given bin.
     *
     * @param bin               null for no bin, then fields will be cleared
     * @param periodStart       -1 for not available, then time field will be cleared
     * @param periodEnd         -1 for not available, then time field will be cleared
     * @param bringTabToFront   whether to bring the selection tab to front
     */
    public void setSelectedBin(final MetricsSummaryBin bin, final long periodStart, final long periodEnd, final boolean bringTabToFront) {
        if (bin == null) {
            if (periodStart == -1) {
                selectionFromTimeLabel.setText("");
            } else {
                selectionFromTimeLabel.setText(TIME_FORMAT.format(new Date(periodStart)));
            }
            if (periodEnd == -1) {
                selectionToTimeLabel.setText("");
            } else {
                selectionToTimeLabel.setText(TIME_FORMAT.format(new Date(periodEnd)));
            }

            selectionFrontMaxText.setText("");
            selectionFrontAvgText.setText("");
            selectionFrontMinText.setText("");
            selectionBackMaxText.setText("");
            selectionBackAvgText.setText("");
            selectionBackMinText.setText("");
            selectionNumRoutingFailureText.setText("");
            selectionNumPolicyViolationText.setText("");
            selectionNumSuccessText.setText("");
            selectionNumTotalText.setText("");

            _selectionServicesWithProblemListModel.clear();
        } else {
            selectionFromTimeLabel.setText(TIME_FORMAT.format(new Date(bin.getPeriodStart())));
            selectionToTimeLabel.setText(TIME_FORMAT.format(new Date(bin.getPeriodEnd())));
            selectionFrontMaxText.setText(Integer.toString(bin.getMaxFrontendResponseTime()));
            selectionFrontAvgText.setText(Long.toString(Math.round(bin.getAverageFrontendResponseTime())));
            selectionFrontMinText.setText(Integer.toString(bin.getMinFrontendResponseTime()));
            selectionBackMaxText.setText(Integer.toString(bin.getMaxBackendResponseTime()));
            selectionBackAvgText.setText(Long.toString(Math.round(bin.getAverageBackendResponseTime())));
            selectionBackMinText.setText(Integer.toString(bin.getMinBackendResponseTime()));
            selectionNumRoutingFailureText.setText(Integer.toString(bin.getNumRoutingFailure()));
            selectionNumPolicyViolationText.setText(Integer.toString(bin.getNumPolicyViolation()));
            selectionNumSuccessText.setText(Integer.toString(bin.getNumSuccess()));
            selectionNumTotalText.setText(Integer.toString(bin.getNumTotal()));

            _selectionServicesWithProblemListModel.clear();
            for (EntityHeader publishedService : _publishedServices) {   // Loop over sorted set so that the list box will be sorted too.
                final boolean hasRF = bin.getServicesWithRoutingFailure().contains(publishedService.getOid());
                final boolean hasPV = bin.getServicesWithPolicyViolation().contains(publishedService.getOid());
                if (hasRF || hasPV) {
                    ImageIcon icon = null;
                    if (hasRF && hasPV) {
                        icon = BOTH_PROBLEMS_ICON;
                    } else if (hasRF) {
                        icon = ROUTING_FAILURE_ICON;
                    } else {
                        icon = POLICY_VIOLATION_ICON;
                    }
                    _selectionServicesWithProblemListModel.addElement(new ProblemListElement(icon, publishedService));
                }
            }
        }

        if (bringTabToFront) {
            rightTabbedPane.setSelectedIndex(SELECTION_TAB_INDEX);
        }
    }

    private <T> void updateComboModel(T[] oldItems, T[] newItems, DefaultComboBoxModel comboModel) {
        Set<T> news = new LinkedHashSet<T>(Arrays.asList(newItems));
        Set<T> olds = new HashSet<T>(Arrays.asList(oldItems));
        Set<T> olds2 = new HashSet<T>(Arrays.asList(oldItems));

        // Remove deleted stuff from model
        olds.removeAll(news);
        for (T o : olds) {
            comboModel.removeElement(o);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Removed from ComboBox model:" + o);
            }
        }

        // Add new stuff to model
        news.removeAll(olds2);
        for (T o : news) {
            comboModel.addElement(o);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Added to ComboBox model: " + o);
            }
        }
    }

    private ClusterStatusAdmin getClusterStatusAdmin() {
        if (_clusterStatusAdmin == null) {
            _clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        }
        return _clusterStatusAdmin;
    }

    private ServiceAdmin getServiceAdmin() {
        if (_serviceAdmin == null) {
            _serviceAdmin = Registry.getDefault().getServiceManager();
        }
        return _serviceAdmin;
    }

    /**
     * Use service display name (includes distinguishing information).
     *
     * @param headers   published services
     * @throws FindException if there was a problem accessing the requested information
     */
    private void updateServiceNames(EntityHeader[] headers)
            throws FindException {
        for (EntityHeader header : headers) {
            final PublishedService ps = getServiceAdmin().findServiceByID(header.getStrId());
            if (ps != null) {
                header.setName(ps.displayName());
            }
        }
    }

}
