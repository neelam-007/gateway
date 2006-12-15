/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.SheetHolder;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.MetricsChartPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
import com.l7tech.service.MetricsSummaryBin;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @author rmak
 */
public class DashboardWindow extends JFrame implements LogonListener, SheetHolder {

    private JPanel mainPanel;
    private JPanel chartPanel;
    private JLabel statusLabel;
    private JButton closeButton;
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

    private static final Logger _logger = Logger.getLogger(DashboardWindow.class.getName());

    private static final ResourceBundle _resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(_resources.getString("tabbedPane.timeFormat"));

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

    private final Timer _refreshTimer = new Timer(1500, _refreshListener);

    private long _latestDownloadedPeriodStart = -1;

    /** Whether previous attempt to connect to gateway was successful. */
    private boolean _connected = false;

    public DashboardWindow() throws HeadlessException {
        super(_resources.getString("window.title"));

        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());

        rightTabbedPane.setSelectedIndex(LATEST_TAB_INDEX);
        statusLabel.setText("");

        int fineInterval;
        try {
            fineInterval = getClusterStatusAdmin().getMetricsFineInterval();
        } catch (RemoteException e) {
            _logger.warning("Cannot get fine bin interval from gateway; defaulting to 5 seconds: " + e);
            fineInterval = 5 * 1000;
        }

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
            _clusterNodes = getClusterStatusAdmin().getClusterStatus();
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

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        getContentPane().add(mainPanel);
        pack();

        resetData(false);
        _refreshTimer.setInitialDelay(0);   /** So that {@link #resetData} will be more snappy. */
        _refreshTimer.start();
    } /* constructor */

    private ClusterNodeInfo[] findAllClusterNodes() throws RemoteException, FindException {
        final ClusterNodeInfo[] result = getClusterStatusAdmin().getClusterStatus();
        Arrays.sort(result);
        return result;
    }

    private EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        final EntityHeader[] result = getServiceAdmin().findAllPublishedServices();
        appendUriToServiceName(result);
        Arrays.sort(result);
        return result;
    }

    private void updateClusterNodesCombo() throws RemoteException, FindException {
        final ClusterNodeInfo[] newNodes = findAllClusterNodes();
        if (!Arrays.equals(_clusterNodes, newNodes)) {
            updateComboModel(_clusterNodes, newNodes, _clusterNodeComboModel);
        }
        _clusterNodes = newNodes;
    }

    private void updatePublishedServicesCombo() throws RemoteException, FindException {
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
        TimeZone tz = null;
        try {
            tz = TimeZone.getTimeZone(getClusterStatusAdmin().getCurrentClusterTimeZone());
        } catch (RemoteException e) {
            // Falls through to use local time zone.
        }
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
            statusLabel.setText("[Disconnected] " + statusLabel.getText().trim());
        } else {
            // Was already disconnected due to some error. Clears out the exception message.
            statusLabel.setText("[Disconnected]");
        }
        clusterNodeCombo.setEnabled(false);
        publishedServiceCombo.setEnabled(false);
        resolutionCombo.setEnabled(false);
        _connected = false;
    }

    public void setVisible(boolean vis) {
        if (vis) {
            _refreshTimer.start();
            _metricsChartPanel.restoreAutoRange(); // In case chart was zoomed in when closed.
            _metricsChartPanel.resumeUpdate();     // In case chart was suspended when closed.
        } else {
            _refreshTimer.stop();

            // Let inactivity timeout start counting after this window is closed.
            TopComponents.getInstance().updateLastActivityTime();
        }
        super.setVisible(vis);
    }

    public void dispose() {
        _refreshTimer.stop();
        super.dispose();
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
        _refreshTimer.start();
    }

    private synchronized void refreshData() {
        try {
            final ClusterStatusAdmin clusterStatusAdmin = getClusterStatusAdmin();

            updateClusterNodesCombo();
            updatePublishedServicesCombo();

            String nodeId = null;
            final ClusterNodeInfo node = (ClusterNodeInfo) _clusterNodeComboModel.getSelectedItem();
            if (node != ALL_NODES) {
                nodeId = node.getNodeIdentifier();
            }

            Long serviceOid = null;
            final EntityHeader service = (EntityHeader) _publishedServiceComboModel.getSelectedItem();
            if (service != ALL_SERVICES) {
                serviceOid = service.getOid();
            }

            final Integer resolution = _currentResolution.getResolution();

            Collection<MetricsSummaryBin> newBins = null;
            if (_latestDownloadedPeriodStart == -1) {
                newBins = clusterStatusAdmin.summarizeLatestByPeriod(nodeId,
                                                                     serviceOid,
                                                                     resolution,
                                                                     _currentResolution.getChartTimeRange() +
                                                                     _currentResolution.getBinInterval());
            } else {
                newBins = clusterStatusAdmin.summarizeByPeriod(nodeId,
                                                               serviceOid,
                                                               resolution,
                                                               _latestDownloadedPeriodStart + 1,
                                                               null);
            }

            MetricsSummaryBin latestBin = null;
            if (newBins.size() > 0) {
                for (MetricsSummaryBin bin : newBins) {
                    if (_latestDownloadedPeriodStart < bin.getPeriodStart()) {
                        _latestDownloadedPeriodStart = bin.getPeriodStart();
                        latestBin = bin;
                    }
                }

                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Dowloaded " + newBins.size() + " " + MetricsBin.describeResolution(resolution) +
                                 " summary bins. Latest bin = " + new Date(latestBin.getPeriodStart()) +
                                 " - " + new Date(latestBin.getPeriodEnd()));
                }

                _metricsChartPanel.addData(newBins);
            }

            // Gets a summary of the latest resolution period.
            if (_currentResolution == _fineResolution) {
                // The latest fine resolution summary bin was already downloaded,
                // either during this call or the previous call.
            } else if (_currentResolution == _hourlyResolution) {
                // Gets a summary collated from an hour's worth of fine metrics bins.
                latestBin = clusterStatusAdmin.summarizeLatest(nodeId, serviceOid, MetricsBin.RES_FINE, 60 * 60 * 1000);
            } else if (_currentResolution == _dailyResolution) {
                // Gets a summary collated from a day's worth of hourly metrics bins.
                latestBin = clusterStatusAdmin.summarizeLatest(nodeId, serviceOid, MetricsBin.RES_HOURLY, 24 * 60 * 60 * 1000);
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
            }

            if (clusterStatusAdmin.isMetricsEnabled()) {
                statusLabel.setText(STATUS_UPDATED_FORMAT.format(new Object[] { new Date() }));
            } else {
                statusLabel.setText(METRICS_NOT_ENABLED);
            }

            if (!_connected) {  // Previously disconnected.
                _logger.log(Level.INFO, "Reconnected to SSG.");
            }
            _connected = true;
        } catch (RemoteException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Unable to get dashboard data.");
            _refreshTimer.stop();
            dispose();
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
        }

        if (bringTabToFront) {
            rightTabbedPane.setSelectedIndex(SELECTION_TAB_INDEX);
        }
    }

    private <T> void updateComboModel(T[] oldItems, T[] newItems, DefaultComboBoxModel comboModel) {
        Set<T> news = new HashSet<T>(Arrays.asList(newItems));
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
     * If a published service has a custom routing URI, append it to the name to
     * better distinguish from the first version published, when displayed.
     *
     * @param headers   published services
     * @throws RemoteException if remote communication error
     * @throws FindException if there was a problem accessing the requested information
     */
    private void appendUriToServiceName(EntityHeader[] headers)
            throws RemoteException, FindException {
        for (EntityHeader header : headers) {
            final PublishedService ps = getServiceAdmin().findServiceByID(header.getStrId());
            if (ps != null && ps.getRoutingUri() != null) {
                header.setName(ps.getName() + " [" + ps.getRoutingUri() + "]");
            }
        }
    }

    public void showSheet(JInternalFrame sheet) {
        DialogDisplayer.showSheet(this, sheet);
    }
}
