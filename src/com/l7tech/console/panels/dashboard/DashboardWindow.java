/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels.dashboard;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.console.MainWindow;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
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
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class DashboardWindow extends JFrame implements LogonListener {
    private static final Logger logger = Logger.getLogger(DashboardWindow.class.getName());

    private final ClusterStatusAdmin clusterStatusAdmin;
    private final ServiceAdmin serviceAdmin;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.dashboard.resources.DashboardWindow");

    private static final int fineChartRange = 5 * 60 * 1000; // 5 minutes
    private static final int hourlyChartRange = 60 * 60 * 60 * 1000; // 60 hours
    private static final long dailyChartRange = 60 * 24 * 60 * 60 * 1000L; // 60 days

    static final Range FINE = new Range(MetricsBin.RES_FINE, fineChartRange, 5 * 1000, resources.getString("resolutionCombo.fineValue"), resources.getString("rightPanel.fineTitle"));
    static final Range HOURLY = new Range(MetricsBin.RES_HOURLY, hourlyChartRange, 60 * 60 * 1000, resources.getString("resolutionCombo.hourlyValue"), resources.getString("rightPanel.hourlyTitle"));
    static final Range DAILY = new Range(MetricsBin.RES_DAILY, dailyChartRange, 24 * 60 * 60 * 1000, resources.getString("resolutionCombo.dailyValue"), resources.getString("rightPanel.dailyTitle"));

    private static final Range[] ALL_RANGES = {FINE, HOURLY, DAILY};

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
    private JPanel rightPanel;
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

    public DashboardWindow() throws HeadlessException {
        super(resources.getString("window.title"));

        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());

        this.clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        this.serviceAdmin = Registry.getDefault().getServiceManager();

        chartPanel.setLayout(new BorderLayout());

        timeRangeCombo.setModel(new DefaultComboBoxModel(ALL_RANGES));
        timeRangeCombo.setSelectedItem(FINE);
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
                refreshTimer.stop();
                dispose();
            }
        });

        try {
            currentServiceHeaders = serviceAdmin.findAllPublishedServices();
            EntityHeader[] comboItems = new EntityHeader[currentServiceHeaders.length + 1];
            System.arraycopy(currentServiceHeaders, 0, comboItems, 1, currentServiceHeaders.length);
            comboItems[0] = ALL_SERVICES;
            serviceComboModel = new DefaultComboBoxModel(comboItems);
            publishedServiceCombo.setModel(serviceComboModel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            currentClusterNodes = clusterStatusAdmin.getClusterStatus();
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

        resetData();

        getContentPane().add(mainPanel);
        pack();
        refreshTimer.start();
    }

    public void onLogon(LogonEvent e) {
        refreshTimer.start();
    }

    public void onLogoff(LogonEvent e) {
        refreshTimer.stop();
    }

    public void setVisible(boolean vis) {
        if (vis)
            refreshTimer.start();
        else
            refreshTimer.stop();
        super.setVisible(vis);
    }

    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }

    private synchronized void resetData() {
        currentRange.clear();

        chartPanel.removeAll();
        chartPanel.add(currentRange.getMetricsChartPanel(), BorderLayout.CENTER);
        currentRange.getMetricsChartPanel().restoreAutoDomainBounds();
        chartPanel.revalidate();
    }

    private synchronized void refreshData() {
        final EntityHeader[] newServiceHeaders;
        final ClusterNodeInfo[] newNodes;
        try {
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
            {
                List chartBins = new ArrayList();
                long now = System.currentTimeMillis();

                Map currentPeriods = periodMap.subMap(new Long(now - currentRange.getChartRange()), new Long(now));
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
                        if (rightPanelBin.getPeriodStart() + (rightPanelBin.getInterval() * 2) < System.currentTimeMillis()) rightPanelBin = null;
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

                rightPanelBin = clusterStatusAdmin.getMetricsSummary(resolution, System.currentTimeMillis() - currentRange.getRightPanelRange(), (int)currentRange.getRightPanelRange(), whichNode, whichService);
            }

            int numPolicyFail = 0, numRoutingFail = 0, numSuccess = 0, numTotal = 0;
            int frontMin = 0, frontMax = 0, backMin = 0, backMax = 0;
            double frontAvg = 0.0, backAvg = 0.0;

            if (rightPanelBin != null) {
                numPolicyFail = rightPanelBin.getNumAttemptedRequest() - rightPanelBin.getNumAuthorizedRequest();
                numRoutingFail = rightPanelBin.getNumAuthorizedRequest() - rightPanelBin.getNumCompletedRequest();
                numSuccess = rightPanelBin.getNumCompletedRequest();
                numTotal = rightPanelBin.getNumAttemptedRequest();

                frontMin = rightPanelBin.getMinFrontendResponseTime();
                frontAvg = rightPanelBin.getAverageFrontendResponseTime();
                frontMax = rightPanelBin.getMaxFrontendResponseTime();

                backMin = rightPanelBin.getMinBackendResponseTime();
                backAvg = rightPanelBin.getAverageBackendResponseTime();
                backMax = rightPanelBin.getMaxBackendResponseTime();
            }

            numPolicyFailField.setText(Integer.toString(numPolicyFail));
            numSuccessField.setText(Integer.toString(numSuccess));
            numRoutingFailField.setText(Integer.toString(numRoutingFail));
            numTotalField.setText(Integer.toString(numTotal));

            frontMinField.setText(Integer.toString(frontMin));
            frontAvgField.setText(Integer.toString((int)frontAvg));
            frontMaxField.setText(Integer.toString(frontMax));

            backMinField.setText(Integer.toString(backMin));
            backAvgField.setText(Integer.toString((int)backAvg));
            backMaxField.setText(Integer.toString(backMax));

            StringBuffer sb = new StringBuffer();
            final FieldPosition positionZero = new FieldPosition(0);
            statusUpdatedFormat.format(new Object[] { new Date() }, sb, positionZero);
            statusLabel.setText(sb.toString());

        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Error getting data from SSG", e);
            statusLabel.setText("Error receiving data: " + e.toString());
        } catch (FindException e) {
            logger.log(Level.WARNING, "SSG can't get data", e);
            statusLabel.setText("SSG can't get data: " + e.toString());
        }
    }

    private void findNewBins(Range range) throws RemoteException, FindException {
        long last = range.getLastPeriodDownloaded();
        List newBins = clusterStatusAdmin.findMetricsBins(null, new Long(last + 1), null, new Integer(range.getResolution()), null);
        if (newBins.size() > 0)
            logger.info("Found " + newBins.size() + " MetricsBins for range " + range.toString());

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
        for (Iterator i = olds2.iterator(); i.hasNext();) {
            Object o = i.next();
            logger.info(o + " is new");
            comboModel.addElement(o);
        }
    }


}
