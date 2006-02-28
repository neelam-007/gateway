/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterNodeInfo;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.audit.LogonEvent;
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

    private final MetricsChartPanel metricsChartPanel = new MetricsChartPanel(5 * 60 * 1000);

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
    private ClusterNodeInfo[] currentClusterNodes = null;
    private EntityHeader[] currentServiceHeaders = null;

    private final ActionListener refreshListener = new DataRefreshListener();

    private final DefaultComboBoxModel serviceComboModel;
    private final DefaultComboBoxModel nodeComboModel;

    private long lastRefresh = 0;
    private final Timer refreshTimer = new Timer(500, refreshListener);

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

        resetData();

        chartPanel.add(metricsChartPanel);

        getContentPane().add(mainPanel);
        pack();
        refreshTimer.start();
    }

    private long oneHourAgo() {
        return System.currentTimeMillis() - (3600 * 1000);
    }

    private void resetData() {
        lastRefresh = oneHourAgo();
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

    private class DataRefreshListener implements ActionListener {
        private final TreeMap fineEntriesByPeriodStart = new TreeMap();
        private final TreeMap hourlyEntriesByPeriodStart = new TreeMap();
        private final TreeMap dailyEntriesByPeriodStart = new TreeMap();

        public void actionPerformed(ActionEvent ev) {
            try {
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
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "Error getting data from SSG", e);
                    statusLabel.setText("Error receiving data: " + e.toString());
                } catch (FindException e) {
                    logger.log(Level.WARNING, "SSG can't get data", e);
                    statusLabel.setText("SSG can't get data: " + e.toString());
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

                long start = lastRefresh;
                List bins = clusterStatusAdmin.findMetricsBins(whichNode, new Long(start), null, new Integer(MetricsBin.RES_FINE), whichService);
                if (bins.size() > 0)
                    logger.info("Found " + bins.size() + " MetricsBins");

                for (Iterator i = bins.iterator(); i.hasNext();) {
                    MetricsBin bin = (MetricsBin)i.next();
                    addBin(bin.getResolution(), bin.getPeriodStart(), bin);
                }

                Long lastPeriod = (Long)fineEntriesByPeriodStart.lastKey();
                PeriodData lastEntry = (PeriodData)fineEntriesByPeriodStart.get(lastPeriod);
                MetricsBin lastBin = lastEntry.get(whichNode, whichService);

                if (lastBin != null) {
                    lastRefresh = lastBin.getPeriodStart() + 1;
                } else {
                    lastRefresh = System.currentTimeMillis();
                }

                int numPolicyFail = 0, numRoutingFail = 0, numSuccess = 0, numTotal = 0;
                int frontMin = 0, frontMax = 0, backMin = 0, backMax = 0;
                double frontAvg = 0.0, backAvg = 0.0;

                long now = System.currentTimeMillis();
                if (lastBin != null && lastPeriod != null) {
                    logger.info("lastBin is " + (now - lastPeriod.longValue()) + "ms old");
                    if (lastPeriod.longValue() + (lastBin.getInterval()*2) > System.currentTimeMillis()) {
                        // Next bin is still likely in the future
                        logger.info("New last bin: " + lastBin);
                        numPolicyFail = lastBin.getNumAttemptedRequest() - lastBin.getNumCompletedRequest();
                        numRoutingFail = lastBin.getNumAuthorizedRequest() - lastBin.getNumCompletedRequest();
                        numSuccess = lastBin.getNumCompletedRequest();
                        numTotal = lastBin.getNumAttemptedRequest();

                        frontMin = lastBin.getMinFrontendResponseTime();
                        frontAvg = lastBin.getAverageFrontendResponseTime();
                        frontMax = lastBin.getMaxFrontendResponseTime();

                        backMin = lastBin.getMinBackendResponseTime();
                        backAvg = lastBin.getAverageBackendResponseTime();
                        backMax = lastBin.getMaxBackendResponseTime();

                        List data = new ArrayList();
                        data.add(lastBin);

                        metricsChartPanel.addData(data);
                    } else {
                        logger.fine("Last bin was >" + lastBin.getInterval() + "ms ago.");
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
                    map = fineEntriesByPeriodStart;
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
            PeriodData data = (PeriodData)map.get(lps);
            if (data == null) {
                data = new PeriodData(res, ps, bin.getInterval());
                map.put(lps, data);
            }
            data.add(bin);
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

    private static class PeriodData {
        public PeriodData(int resolution, long periodStart, int interval) {
            this.periodStart = periodStart;
            this.resolution = resolution;
            this.interval = interval;
        }

        void add(MetricsBin bin) {
            if (bin.getPeriodStart() != periodStart) throw new IllegalArgumentException();
            if (bin.getResolution() != resolution) throw new IllegalArgumentException();
            binsByNodeId.put(bin.getClusterNodeId(), bin);
            binsByServiceOid.put(new Long(bin.getServiceOid()), bin);
        }

        MetricsBin get(String nodeId, Long serviceOid) {
            if (nodeId != null) {
                return (MetricsBin)binsByNodeId.get(nodeId);
            } else if (serviceOid != null) {
                return (MetricsBin)binsByServiceOid.get(serviceOid);
            } else {
                return makeMegaBin(nodeId, serviceOid);
            }
        }

        private MetricsBin makeMegaBin(String nodeId, Long serviceOid) {
            MetricsBin megabin;
            int numAttempted = 0, numAuthorized = 0, numCompleted = 0;
            int backTime = 0, frontTime = 0, backMin = 0, frontMin = 0;
            int backMax = 0, frontMax = 0;
            long start = 0;
            long end = 0;

            for (Iterator i = binsByNodeId.values().iterator(); i.hasNext();) {
                MetricsBin bin = (MetricsBin)i.next();
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

        private final long periodStart;
        private final int resolution;
        private final int interval;
        private final Map binsByNodeId = new HashMap();
        private final Map binsByServiceOid = new HashMap();
    }


}
