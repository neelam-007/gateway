/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.ImageCache;
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

    private final MetricsChartPanel metricsChartPanel = new MetricsChartPanel(chartRange);

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
    private JLabel frontMinImageLabel;
    private JLabel frontAvgImageLabel;
    private JLabel frontMaxImageLabel;
    private JLabel backMinImageLabel;
    private JLabel backAvgImageLabel;
    private JLabel backMaxImageLabel;
    private JLabel numRoutingFailImageLabel;
    private JLabel numPolicyFailImageLabel;
    private JLabel numSuccessImageLabel;

    private ClusterNodeInfo[] currentClusterNodes = null;
    private EntityHeader[] currentServiceHeaders = null;

    private final ActionListener refreshListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            refreshData();
        }
    };

    private final DefaultComboBoxModel serviceComboModel;
    private final DefaultComboBoxModel nodeComboModel;

    private final TreeMap allFinePeriods = new TreeMap();
    private final Set chartedFinePeriods = new HashSet();

    private final TreeMap hourlyEntriesByPeriodStart = new TreeMap();
    private final TreeMap dailyEntriesByPeriodStart = new TreeMap();

    private long lastPeriodSeen;

    private final Timer refreshTimer = new Timer(900, refreshListener);

    private static final EntityHeader ALL_SERVICES = new EntityHeader() {
        public String toString() {
            return "<All Services>";
        }
    };

    private static final ClusterNodeInfo ALL_NODES = new ClusterNodeInfo() {
        public String toString() {
            return "<All Nodes>";
        }
    };

    private static final int chartRange = 5 * 60 * 1000;

    public DashboardWindow() throws HeadlessException {
        super("Dashboard");

        this.clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
        this.serviceAdmin = Registry.getDefault().getServiceManager();

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
        backMinImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/BackendResponseTimeMinLegend.gif")));
        backAvgImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/BackendResponseTimeAvgLegend.gif")));
        backMaxImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/BackendResponseTimeMaxLegend.gif")));

        frontMinImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/FrontendResponseTimeMinLegend.gif")));
        frontAvgImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/FrontendResponseTimeAvgLegend.gif")));
        frontMaxImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/FrontendResponseTimeMaxLegend.gif")));

        numPolicyFailImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/MsgRatePolicyViolationLegend.gif")));
        numRoutingFailImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/MsgRateRoutingFailureLegend.gif")));
        numSuccessImageLabel.setIcon(new ImageIcon(cache.getIcon("com/l7tech/console/resources/MsgRateSuccessLegend.gif")));

        resetData();

        chartPanel.add(metricsChartPanel);

        getContentPane().add(mainPanel);
        pack();
        refreshTimer.start();
    }

    private void resetData() {
        chartedFinePeriods.clear();
        metricsChartPanel.clearData();
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

    private void refreshData() {
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

            // Find all new bins since last period
            List newBins = clusterStatusAdmin.findMetricsBins(null, new Long(lastPeriodSeen + 1), null, new Integer(MetricsBin.RES_FINE), null);
            if (newBins.size() > 0)
                logger.info("Found " + newBins.size() + " MetricsBins");

            // Add new bins to allFinePeriods etc.
            for (Iterator i = newBins.iterator(); i.hasNext();) {
                MetricsBin newBin = (MetricsBin)i.next();
                final long ps = newBin.getPeriodStart();
                if (ps > lastPeriodSeen) lastPeriodSeen = ps;
                addBin(newBin.getResolution(), ps, newBin);
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
            {
                List chartBins = new ArrayList();
                long now = System.currentTimeMillis();

                Map currentPeriods = allFinePeriods.subMap(new Long(now - chartRange), new Long(now));
                for (Iterator i = currentPeriods.keySet().iterator(); i.hasNext();) {
                    Long period = (Long)i.next();
                    if (!chartedFinePeriods.contains(period)) {
                        PeriodData data = (PeriodData)allFinePeriods.get(period);
                        MetricsBin bin = data.get(whichNode, whichService);
                        if (bin != null) chartBins.add(bin);
                        chartedFinePeriods.add(period);
                    }
                }
                metricsChartPanel.addData(chartBins);
            }

            Long lastPeriod = (Long)allFinePeriods.lastKey();
            MetricsBin lastBin = null;
            if (lastPeriod != null) {
                PeriodData data = (PeriodData)allFinePeriods.get(lastPeriod);
                lastBin = data.get(whichNode, whichService);
            }

            int numPolicyFail = 0, numRoutingFail = 0, numSuccess = 0, numTotal = 0;
            int frontMin = 0, frontMax = 0, backMin = 0, backMax = 0;
            double frontAvg = 0.0, backAvg = 0.0;

            if (lastBin != null) {
                if (lastPeriod.longValue() + (lastBin.getInterval() * 2) > System.currentTimeMillis()) {
                    // Next bin is still likely in the future
                    logger.info("New last bin: " + lastBin);
                    numPolicyFail = lastBin.getNumAttemptedRequest() - lastBin.getNumAuthorizedRequest();
                    numRoutingFail = lastBin.getNumAuthorizedRequest() - lastBin.getNumCompletedRequest();
                    numSuccess = lastBin.getNumCompletedRequest();
                    numTotal = lastBin.getNumAttemptedRequest();

                    frontMin = lastBin.getMinFrontendResponseTime();
                    frontAvg = lastBin.getAverageFrontendResponseTime();
                    frontMax = lastBin.getMaxFrontendResponseTime();

                    backMin = lastBin.getMinBackendResponseTime();
                    backAvg = lastBin.getAverageBackendResponseTime();
                    backMax = lastBin.getMaxBackendResponseTime();
                } else {
                    logger.fine("Last bin was >" + lastBin.getInterval() + "ms ago, displaying zeros for current data");
                }
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

            statusLabel.setText("Last updated: " + new Date());

        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Error getting data from SSG", e);
            statusLabel.setText("Error receiving data: " + e.toString());
        } catch (FindException e) {
            logger.log(Level.WARNING, "SSG can't get data", e);
            statusLabel.setText("SSG can't get data: " + e.toString());
        }
    }

    private void addBin(int res, long ps, MetricsBin bin) {
        TreeMap map;
        switch(res) {
            case MetricsBin.RES_FINE:
                map = allFinePeriods;
                break;
            case MetricsBin.RES_HOURLY:
                map = hourlyEntriesByPeriodStart;
                break;
            case MetricsBin.RES_DAILY:
                map = dailyEntriesByPeriodStart;
                break;
            default:
                throw new IllegalArgumentException("MetricsBin " + bin + " has unsupported resolution");
        }

        Long lps = new Long(ps);
        synchronized(map) {
            PeriodData data = (PeriodData)map.get(lps);
            if (data == null) {
                data = new PeriodData(res, ps, bin.getInterval());
                map.put(lps, data);
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

    private static class PeriodData {
        private final long periodStart;
        private final int resolution;
        private final int interval;

        private final Set allBins = new HashSet();
        private final Map binsByNodeId = new HashMap();
        private final Map binsByServiceOid = new HashMap();

        private final MetricsBin bigBin;

        public PeriodData(int resolution, long periodStart, int interval) {
            this.periodStart = periodStart;
            this.resolution = resolution;
            this.interval = interval;

            bigBin = new MetricsBin(periodStart, interval, resolution, null, -1);
        }

        synchronized void add(MetricsBin newBin) {
            if (newBin.getPeriodStart() != periodStart) throw new IllegalArgumentException();
            if (newBin.getResolution() != resolution) throw new IllegalArgumentException();

            bigBin.setMinBackendResponseTime(Math.min(bigBin.getMinBackendResponseTime(), newBin.getMinBackendResponseTime()));
            bigBin.setMaxBackendResponseTime(Math.max(bigBin.getMaxBackendResponseTime(), newBin.getMaxBackendResponseTime()));
            bigBin.setMinFrontendResponseTime(Math.min(bigBin.getMinFrontendResponseTime(), newBin.getMinFrontendResponseTime()));
            bigBin.setMaxFrontendResponseTime(Math.max(bigBin.getMaxFrontendResponseTime(), newBin.getMaxFrontendResponseTime()));

            bigBin.setStartTime(Math.min(bigBin.getStartTime(), newBin.getStartTime()));
            bigBin.setEndTime(Math.max(bigBin.getEndTime(), newBin.getEndTime()));

            bigBin.setNumAttemptedRequest(bigBin.getNumAttemptedRequest() + newBin.getNumAttemptedRequest());
            bigBin.setNumAuthorizedRequest(bigBin.getNumAuthorizedRequest() + newBin.getNumAuthorizedRequest());
            bigBin.setNumCompletedRequest(bigBin.getNumCompletedRequest() + newBin.getNumCompletedRequest());

            bigBin.setSumFrontendResponseTime(bigBin.getSumFrontendResponseTime() + newBin.getSumFrontendResponseTime());
            bigBin.setSumBackendResponseTime(bigBin.getSumBackendResponseTime() + newBin.getSumBackendResponseTime());

            allBins.add(newBin);

            {
                Set bins = (Set)binsByNodeId.get(newBin.getClusterNodeId());
                if (bins == null) {
                    bins = new HashSet();
                    binsByNodeId.put(newBin.getClusterNodeId(), bins);
                }
                bins.add(newBin);
            }

            {
                Long serviceOid = new Long(newBin.getServiceOid());
                Set bins = (Set)binsByServiceOid.get(serviceOid);
                if (bins == null) {
                    bins = new HashSet();
                    binsByServiceOid.put(serviceOid, bins);
                }
                bins.add(newBin);
            }

        }

        synchronized MetricsBin get(String nodeId, Long serviceOid) {
            Set binsToAdd;
            if (nodeId == null && serviceOid == null) {
                return bigBin;
            } else if (nodeId != null && serviceOid != null) {
                Set sbins = (Set)binsByServiceOid.get(serviceOid);
                Set nbins = (Set)binsByNodeId.get(nodeId);
                sbins.retainAll(nbins);
                if (sbins.size() != 1) logger.warning(sbins.size() + " bins for period " + periodStart);
                binsToAdd = sbins;
            } else if (nodeId != null) {
                binsToAdd = (Set)binsByNodeId.get(nodeId);
            } else {
                binsToAdd = (Set)binsByServiceOid.get(serviceOid);
            }

            Iterator bins = binsToAdd.iterator();

            if (binsToAdd.size() == 1) return (MetricsBin)bins.next();

            MetricsBin megabin;
            int numAttempted = 0, numAuthorized = 0, numCompleted = 0;
            int backTime = 0, frontTime = 0, backMin = 0, frontMin = 0;
            int backMax = 0, frontMax = 0;
            long start = 0;
            long end = 0;

            while (bins.hasNext()) {
                MetricsBin bin = (MetricsBin)bins.next();
                if (nodeId == null || bin.getClusterNodeId().equals(nodeId) ||
                    serviceOid == null || bin.getServiceOid() == serviceOid.longValue()) {
                    numAttempted += bin.getNumAttemptedRequest();
                    numAuthorized += bin.getNumAuthorizedRequest();
                    numCompleted += bin.getNumCompletedRequest();
                    backTime += bin.getSumBackendResponseTime();
                    frontTime += bin.getSumFrontendResponseTime();

                    backMin = backMin == 0 ? bin.getMinBackendResponseTime() : Math.min(backMin, bin.getMinBackendResponseTime());
                    backMax = backMax == 0 ? bin.getMaxBackendResponseTime() : Math.max(backMax, bin.getMaxBackendResponseTime());
                    frontMin = frontMin == 0 ? bin.getMinFrontendResponseTime() : Math.min(frontMin, bin.getMinFrontendResponseTime());
                    frontMax = frontMax == 0 ? bin.getMaxFrontendResponseTime() : Math.max(frontMax, bin.getMaxFrontendResponseTime());
                    start = start == 0 ? bin.getStartTime() : Math.min(start, bin.getStartTime());
                    end = end == 0 ? bin.getEndTime() : Math.max(end, bin.getEndTime());
                }
            }

            if (backMax == Integer.MAX_VALUE) backMax = 0;
            if (frontMax == Integer.MAX_VALUE) frontMax = 0;
            if (end == Integer.MAX_VALUE) end = start;

            megabin = new MetricsBin(periodStart, interval, resolution,
                    nodeId == null ? null : nodeId,
                    serviceOid == null ? -1 : serviceOid.longValue());
            megabin.setStartTime(start);
            megabin.setSumBackendResponseTime(backTime);
            megabin.setSumFrontendResponseTime(frontTime);
            megabin.setNumAttemptedRequest(numAttempted);
            megabin.setNumAuthorizedRequest(numAuthorized);
            megabin.setNumCompletedRequest(numCompleted);
            megabin.setMinBackendResponseTime(backMin);
            megabin.setMaxBackendResponseTime(backMax);
            megabin.setMinFrontendResponseTime(frontMin);
            megabin.setMaxFrontendResponseTime(frontMax);
            megabin.setEndTime(end);
            return megabin;
        }

    }


}
