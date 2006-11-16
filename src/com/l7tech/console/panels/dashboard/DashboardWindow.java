/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.MainWindow;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.MetricsBin;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class DashboardWindow extends JFrame implements LogonListener {
    private static final Logger logger = Logger.getLogger(DashboardWindow.class.getName());

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(resources.getString("rightPanel.timeFormat"));

    private ClusterStatusAdmin clusterStatusAdmin;
    private ServiceAdmin serviceAdmin;

    private static final int fineChartRange = 10 * 60 * 1000; // 10 minutes
    private static final int hourlyChartRange = 60 * 60 * 60 * 1000; // 60 hours
    private static final long dailyChartRange = 60 * 24 * 60 * 60 * 1000L; // 60 days

    private final Range FINE = new Range(MetricsBin.RES_FINE, fineChartRange, 5 * 1000, resources.getString("resolutionCombo.fineValue"), resources.getString("rightPanel.fineTitle"), this);
    private final Range HOURLY = new Range(MetricsBin.RES_HOURLY, hourlyChartRange, 60 * 60 * 1000, resources.getString("resolutionCombo.hourlyValue"), resources.getString("rightPanel.hourlyTitle"), this);
    private final Range DAILY = new Range(MetricsBin.RES_DAILY, dailyChartRange, 24 * 60 * 60 * 1000, resources.getString("resolutionCombo.dailyValue"), resources.getString("rightPanel.dailyTitle"), this);

    private final Range[] ALL_RANGES = {FINE, HOURLY, DAILY};

    private Range currentRange = FINE;

    private JPanel mainPanel;
    private JPanel chartPanel;
    private JLabel statusLabel;
    private JButton closeButton;
    private JComboBox clusterNodeCombo;
    private JComboBox publishedServiceCombo;
    private JTextField frontMinField;
    private JTextField frontAvgField;
    private JTextField frontMaxField;
    private JTextField backMinField;
    private JTextField backAvgField;
    private JTextField backMaxField;
    private JTextField numRoutingFailField;
    private JTextField numPolicyFailField;
    private JTextField numSuccessField;
    private JTextField numTotalField;
    private JTabbedPane rightTabbedPane;
    private JPanel rightUpperPanel;
    private JPanel rightLowerPanel;
    private JPanel separatorPanel1;
    private JPanel separatorPanel2;
    private JLabel fromTimeLabel;
    private JLabel toTimeLabel;
    private JLabel frontMinImageLabel;
    private JLabel frontAvgImageLabel;
    private JLabel frontMaxImageLabel;
    private JLabel backMinImageLabel;
    private JLabel backAvgImageLabel;
    private JLabel backMaxImageLabel;
    private JLabel numRoutingFailImageLabel;
    private JLabel numPolicyFailImageLabel;
    private JLabel numSuccessImageLabel;
    private JComboBox timeRangeCombo;

    private ClusterNodeInfo[] currentClusterNodes = null;
    private EntityHeader[] currentServiceHeaders = null;

    private final ActionListener refreshListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            refreshData();
        }
    };

    private final DefaultComboBoxModel serviceComboModel;
    private final DefaultComboBoxModel nodeComboModel;

    private final Timer refreshTimer = new Timer(900, refreshListener);

    private static final EntityHeader ALL_SERVICES = new EntityHeader() {
        public String toString() {
            return resources.getString("publishedServiceCombo.allValue");
        }
    };

    private static final ClusterNodeInfo ALL_NODES = new ClusterNodeInfo() {
        public String toString() {
            return resources.getString("clusterNodeCombo.allValue");
        }
    };

    private final MessageFormat statusUpdatedFormat = new MessageFormat(resources.getString("status.updated"));

    /** Whether previous attempt to connect to gateway was successful. */
    private boolean connected = false;

    public DashboardWindow() throws HeadlessException {
        super(resources.getString("window.title"));

        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        statusLabel.setText("");

        chartPanel.setLayout(new BorderLayout());

        timeRangeCombo.setModel(new DefaultComboBoxModel(ALL_RANGES));
        timeRangeCombo.setSelectedItem(currentRange);
        timeRangeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Range range = (Range)timeRangeCombo.getSelectedItem();
                if (range == null) throw new IllegalStateException();
                rightTabbedPane.setTitleAt(0, range.getRightPanelTitle());
                currentRange = range;
                resetData();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        try {
            currentServiceHeaders = getServiceAdmin().findAllPublishedServices();
            EntityHeader[] comboItems = new EntityHeader[currentServiceHeaders.length + 1];
            System.arraycopy(currentServiceHeaders, 0, comboItems, 1, currentServiceHeaders.length);
            comboItems[0] = ALL_SERVICES;
            serviceComboModel = new DefaultComboBoxModel(comboItems);
            publishedServiceCombo.setModel(serviceComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            currentClusterNodes = getClusterStatusAdmin().getClusterStatus();
            ClusterNodeInfo[] comboItems = new ClusterNodeInfo[currentClusterNodes.length + 1];
            System.arraycopy(currentClusterNodes, 0, comboItems, 1, currentClusterNodes.length);
            comboItems[0] = ALL_NODES;
            nodeComboModel = new DefaultComboBoxModel(comboItems);
            clusterNodeCombo.setModel(nodeComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        clusterNodeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetData();
            }
        });

        publishedServiceCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                resetData();
            }
        });

        rightTabbedPane.setTitleAt(0, currentRange.getRightPanelTitle());

        separatorPanel1.setLayout(new BoxLayout(separatorPanel1, BoxLayout.Y_AXIS));
        separatorPanel1.add(new JSeparator(SwingConstants.HORIZONTAL));
        separatorPanel2.setLayout(new BoxLayout(separatorPanel2, BoxLayout.Y_AXIS));
        separatorPanel2.add(new JSeparator(SwingConstants.HORIZONTAL));

        rightUpperPanel.setBorder(null);    // ? Can't disable border in GUI Designer.
        ((com.intellij.uiDesigner.core.GridLayoutManager)rightUpperPanel.getLayout()).setHGap(6);
        ((com.intellij.uiDesigner.core.GridLayoutManager)rightUpperPanel.getLayout()).setVGap(3);
        ((com.intellij.uiDesigner.core.GridLayoutManager)rightLowerPanel.getLayout()).setHGap(6);
        ((com.intellij.uiDesigner.core.GridLayoutManager)rightLowerPanel.getLayout()).setVGap(3);

        ImageCache cache = ImageCache.getInstance();
        backMinImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("backMinImageLabel.icon"))));
        backAvgImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("backAvgImageLabel.icon"))));
        backMaxImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("backMaxImageLabel.icon"))));

        frontMinImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("frontMinImageLabel.icon"))));
        frontAvgImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("frontAvgImageLabel.icon"))));
        frontMaxImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("frontMaxImageLabel.icon"))));

        numPolicyFailImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("numPolicyFailImageLabel.icon"))));
        numRoutingFailImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("numRoutingFailImageLabel.icon"))));
        numSuccessImageLabel.setIcon(new ImageIcon(cache.getIcon(resources.getString("numSuccessImageLabel.icon"))));

        backMinField.setBackground(Color.WHITE);
        backAvgField.setBackground(Color.WHITE);
        backMaxField.setBackground(Color.WHITE);
        frontMinField.setBackground(Color.WHITE);
        frontAvgField.setBackground(Color.WHITE);
        frontMaxField.setBackground(Color.WHITE);

        numRoutingFailField.setBackground(Color.WHITE);
        numPolicyFailField.setBackground(Color.WHITE);
        numSuccessField.setBackground(Color.WHITE);
        numTotalField.setBackground(Color.WHITE);

        resetData();

        getContentPane().add(mainPanel);
        pack();
        refreshTimer.start();
    }

    public ClusterNodeInfo getClusterNodeSelected() {
        ClusterNodeInfo result = (ClusterNodeInfo)nodeComboModel.getSelectedItem();
        if (result == ALL_NODES)
            result = null;
        return result;
    }

    public EntityHeader getPublishedServiceSelected() {
        EntityHeader result = (EntityHeader)serviceComboModel.getSelectedItem();
        if (result == ALL_SERVICES)
            result = null;
        return result;
    }

    public void onLogon(LogonEvent e) {
        clusterStatusAdmin = null;
        serviceAdmin = null;
        refreshTimer.start();
        clusterNodeCombo.setEnabled(true);
        publishedServiceCombo.setEnabled(true);
        timeRangeCombo.setEnabled(true);
        connected = true;
    }

    public void onLogoff(LogonEvent e) {
        refreshTimer.stop();
        if (connected) {
            statusLabel.setText("[Disconnected] " + statusLabel.getText().trim());
        } else {
            // Was already disconnected due to some error. Clears out the exception message.
            statusLabel.setText("[Disconnected]");
        }
        clusterNodeCombo.setEnabled(false);
        publishedServiceCombo.setEnabled(false);
        timeRangeCombo.setEnabled(false);
        connected = false;
    }

    public void setVisible(boolean vis) {
        if (vis) {
            refreshTimer.start();
            currentRange.getMetricsChartPanel().restoreAutoRange(); // In case chart was zoomed in when closed.
            currentRange.getMetricsChartPanel().resumeUpdate();     // In case chart was suspended when closed.
        } else {
            refreshTimer.stop();

            // Let inactivity timeout start counting after this window is closed.
            TopComponents.getInstance().updateLastActivityTime();
        }
        super.setVisible(vis);
    }

    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }

    private synchronized void resetData() {
        if (connected) {
            currentRange.clear();
        } // else keep displayed data around when disconnected

        chartPanel.removeAll();
        chartPanel.add(currentRange.getMetricsChartPanel(), BorderLayout.CENTER);
        chartPanel.repaint();
    }

    private synchronized void refreshData() {
        final EntityHeader[] newServiceHeaders;
        final ClusterNodeInfo[] newNodes;
        try {
            ServiceAdmin serviceAdmin = getServiceAdmin();
            ClusterStatusAdmin clusterStatusAdmin = getClusterStatusAdmin();

            newServiceHeaders = serviceAdmin.findAllPublishedServices();
            if (!Arrays.equals(currentServiceHeaders, newServiceHeaders)) {
                updateComboModel(currentServiceHeaders, newServiceHeaders, serviceComboModel);
            }
            currentServiceHeaders = newServiceHeaders;

            newNodes = clusterStatusAdmin.getClusterStatus();
            if (!Arrays.equals(currentClusterNodes, newNodes)) {
                updateComboModel(currentClusterNodes, newNodes, nodeComboModel);
            }
            currentClusterNodes = newNodes;

            // Find all new bins since we last looked
            for (int i = 0; i < ALL_RANGES.length; i++) {
                findNewBins(ALL_RANGES[i]);
            }

            EntityHeader currentService = (EntityHeader)serviceComboModel.getSelectedItem();

            Long whichService;
            if (currentService == null || currentService == ALL_SERVICES)
                whichService = null;
            else
                whichService = new Long(currentService.getOid());

            ClusterNodeInfo currentNode = (ClusterNodeInfo)nodeComboModel.getSelectedItem();
            String whichNode;
            if (currentNode == null || currentNode == ALL_NODES)
                whichNode = null;
            else
                whichNode = currentNode.getMac();

            // Find all current data that hasn't been charted yet, and add it to the chart
            SortedMap periodMap = currentRange.getAllPeriods();
            if (! periodMap.isEmpty()) {
                List chartBins = new ArrayList();

                Long lastestPeriodStart = (Long)periodMap.lastKey();
                Map currentPeriods = periodMap.tailMap(new Long(lastestPeriodStart.longValue() - currentRange.getChartRange()));
                final Set chartedPeriods = currentRange.getChartedPeriods();
                for (Iterator i = currentPeriods.keySet().iterator(); i.hasNext();) {
                    Long period = (Long)i.next();
                    if (!chartedPeriods.contains(period)) {
                        PeriodData data = (PeriodData)periodMap.get(period);
                        MetricsBin bin = data.get(whichNode, whichService);
                        if (bin != null) chartBins.add(bin);
                        chartedPeriods.add(period);
                    }
                }
                currentRange.getMetricsChartPanel().addData(chartBins);
            }

            MetricsBin rightPanelBin = null;
            if (currentRange == FINE) {
                // RHS stuff is the latest FINE bin
                if (!FINE.getAllPeriods().isEmpty()) {
                    Long lastPeriod = (Long)FINE.getAllPeriods().lastKey();
                    if (lastPeriod != null) {
                        PeriodData data = (PeriodData)periodMap.get(lastPeriod);
                        rightPanelBin = data.get(whichNode, whichService);
                    }
                }
            } else {
                int resolution;
                if (currentRange == HOURLY) {
                    resolution = MetricsBin.RES_FINE;
                } else if (currentRange == DAILY) {
                    resolution = MetricsBin.RES_HOURLY;
                } else
                    throw new IllegalArgumentException("currentRange is neither FINE, HOURLY nor DAILY");

                rightPanelBin = clusterStatusAdmin.getLastestMetricsSummary(whichNode, whichService, resolution, (int)currentRange.getRightPanelRange());
            }

            if (rightPanelBin != null) {
                final int numPolicyFail = rightPanelBin.getNumAttemptedRequest() - rightPanelBin.getNumAuthorizedRequest();
                final int numRoutingFail = rightPanelBin.getNumAuthorizedRequest() - rightPanelBin.getNumCompletedRequest();
                final int numSuccess = rightPanelBin.getNumCompletedRequest();
                final int numTotal = rightPanelBin.getNumAttemptedRequest();

                final int frontMin = rightPanelBin.getMinFrontendResponseTime();
                final double frontAvg = rightPanelBin.getAverageFrontendResponseTime();
                final int frontMax = rightPanelBin.getMaxFrontendResponseTime();

                final int backMin = rightPanelBin.getMinBackendResponseTime();
                final double backAvg = rightPanelBin.getAverageBackendResponseTime();
                final int backMax = rightPanelBin.getMaxBackendResponseTime();

                numPolicyFailField.setText(Integer.toString(numPolicyFail));
                numSuccessField.setText(Integer.toString(numSuccess));
                numRoutingFailField.setText(Integer.toString(numRoutingFail));
                numTotalField.setText(Integer.toString(numTotal));

                frontMinField.setText(Integer.toString(frontMin));
                frontAvgField.setText(Long.toString(Math.round(frontAvg)));
                frontMaxField.setText(Integer.toString(frontMax));

                backMinField.setText(Integer.toString(backMin));
                backAvgField.setText(Long.toString(Math.round(backAvg)));
                backMaxField.setText(Integer.toString(backMax));

                fromTimeLabel.setText(TIME_FORMAT.format(new Date(rightPanelBin.getPeriodStart())));
                toTimeLabel.setText(TIME_FORMAT.format(new Date(rightPanelBin.getPeriodEnd())));

                statusLabel.setText(statusUpdatedFormat.format(new Object[] { new Date() }));
            }

            if (! connected) {
                logger.log(Level.INFO, "Reconnected to SSG.");
            }
            connected = true;
        } catch (RemoteException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Unable to get dashboard data.");
            refreshTimer.stop();
            dispose();
        } catch (RuntimeException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Unable to get dashboard data.");
            refreshTimer.stop();
            dispose();
        } catch (FindException e) {
            logger.log(Level.WARNING, "SSG can't get data", e);
            statusLabel.setText("[Problem on Gateway] " + e.getMessage() == null ? "" : e.getMessage());
            refreshTimer.stop();
            dispose();
        }
    }

    private void findNewBins(Range range) throws RemoteException, FindException {
        List newBins = null;
        long last = range.getLastPeriodDownloaded();
        if (last == -1) {
            newBins = getClusterStatusAdmin().findLatestMetricsBins(null,
                                                                    new Long(range.getChartRange()),
                                                                    new Integer(range.getResolution()),
                                                                    null);
        } else {
            newBins = getClusterStatusAdmin().findMetricsBins(null,
                                                              new Long(last + 1),
                                                              null,
                                                              new Integer(range.getResolution()),
                                                              null);
        }
        if (newBins.size() > 0) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("Found " + newBins.size() + " MetricsBins for range " + range.toString());
        }

        // Add new bins to allPeriods etc.
        for (Iterator i = newBins.iterator(); i.hasNext();) {
            MetricsBin newBin = (MetricsBin)i.next();
            final long ps = newBin.getPeriodStart();
            if (ps > last) last = ps;
            addBin(range, newBin);
        }
        range.setLastPeriodDownloaded(last);
    }

    private void addBin(Range range, MetricsBin bin) {
        Long ps = new Long(bin.getPeriodStart());
        synchronized(range) {
            TreeMap allPeriods = range.getAllPeriods();
            PeriodData data = (PeriodData)allPeriods.get(ps);
            if (data == null) {
                data = new PeriodData(bin.getResolution(), bin.getPeriodStart(), bin.getInterval());
                allPeriods.put(ps, data);
            }
            data.add(bin);
        }
    }

    private void updateComboModel(Object[] oldObjs, Object[] newObjs, DefaultComboBoxModel comboModel) {
        Set news = new HashSet(Arrays.asList(newObjs));
        Set olds = new HashSet(Arrays.asList(oldObjs));
        Set olds2 = new HashSet(Arrays.asList(oldObjs));

        // Remove deleted stuff from model
        olds.removeAll(news);
        for (Iterator i = olds.iterator(); i.hasNext();) {
            Object o = i.next();
            logger.info(o + " has been removed");
            comboModel.removeElement(o);
        }

        // Add new stuff to model
        news.removeAll(olds2);
        for (Iterator i = news.iterator(); i.hasNext();) {
            Object o = i.next();
            logger.info(o + " is new");
            comboModel.addElement(o);
        }
    }

    private ClusterStatusAdmin getClusterStatusAdmin() {
        ClusterStatusAdmin csa = this.clusterStatusAdmin;
        if (csa == null) {
            csa = Registry.getDefault().getClusterStatusAdmin();
            this.clusterStatusAdmin = csa;
        }
        return csa;
    }

    private ServiceAdmin getServiceAdmin() {
        ServiceAdmin sa = this.serviceAdmin;
        if (sa == null) {
            sa = Registry.getDefault().getServiceManager();
            this.serviceAdmin = sa;
        }
        return sa;
    }

}
