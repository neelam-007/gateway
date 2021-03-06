package com.l7tech.console.panels;

import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.console.table.StatisticsTableSorter;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.ColumnHeaderTooltips;
import com.l7tech.gateway.common.logging.StatisticsRecord;
import com.l7tech.objectmodel.Goid;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.logging.Logger;

/**
 * This class creates a cluster statistics panel.
 */
public class StatisticsPanel extends JPanel {

    private Vector<StatisticsRecord> statsList = new Vector<StatisticsRecord>();

    // IMPORTANT NOTE:
    // 1. need to make sure that NUMBER_OF_SAMPLE_PER_MINUTE has no fraction when REFRESH_INTERVAL is changed
    // 2. REFRESH_INTERVAL must be <= 60
    private static final int REFRESH_INTERVAL = 5;
    private static final int NUMBER_OF_SAMPLE_PER_MINUTE = 60 / REFRESH_INTERVAL;

    private JTable statTable = null;
    private JTable statTotalTable = null;
    private JScrollPane statTablePane = null;
    private StatisticsTableSorter statTableSorter = null;
    private DefaultTableColumnModel columnModel = null;
    private DefaultTableColumnModel totalColumnModel = null;
    private DefaultTableModel statTotalTableModel = null;
    private long totalNumRoutingFailure = 0;
    private long totalNumPolicyViolation = 0;
    private long totalNumSuccess = 0;
    private long totalNumSuccessLastMinute = 0;
    private HashMap lastMinuteCompletedCountsCache;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);
    static Logger logger = Logger.getLogger(StatisticsPanel.class.getName());

    /** Snapshot of services usage at counter reset time. Can be null if counting since cluster startup. */
    private Map<Goid, ServiceUsage> serviceUsageAtCounterStart;

    public StatisticsPanel() {
        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(getStatTablePane());
        add(getStatTotalTable());
        setMinimumSize(new java.awt.Dimension(400, 120));
        setPreferredSize(new java.awt.Dimension(400, 200));
        lastMinuteCompletedCountsCache = new HashMap();
    }


    private JScrollPane getStatTablePane() {
        if (statTablePane != null) return statTablePane;

        statTablePane = new JScrollPane();

        statTablePane.setViewportView(getStatTable());
        statTablePane.getViewport().setBackground(getStatTable().getBackground());
        statTablePane.getVerticalScrollBar().addComponentListener(new ComponentListener(){
            public void componentHidden(ComponentEvent e){
                getStatTotalColumnModel().getColumn(5).setMinWidth(0);
                getStatTotalColumnModel().getColumn(5).setMaxWidth(0);
                getStatTotalColumnModel().getColumn(5).setPreferredWidth(0);
            }
            public void componentResized(ComponentEvent e){
                // not used - the scroll bar width is fixed
            }
            public void componentMoved(ComponentEvent e){
                // not used
            }
            public void componentShown(ComponentEvent e){
                getStatTotalColumnModel().getColumn(5).setMinWidth(((JScrollBar) e.getSource()).getWidth() + 1);
                getStatTotalColumnModel().getColumn(5).setMaxWidth(((JScrollBar) e.getSource()).getWidth() + 1);
                getStatTotalColumnModel().getColumn(5).setPreferredWidth(((JScrollBar) e.getSource()).getWidth() + 1);
            }
        });


        ColumnHeaderTooltips htt = new ColumnHeaderTooltips();
        htt.setToolTip(getStatColumnModel().getColumn(1), "Number of routing failures during the counting period");
        htt.setToolTip(getStatColumnModel().getColumn(2), "Number of policy violations during the counting period");
        htt.setToolTip(getStatColumnModel().getColumn(3), "Number of success during the counting period");
        htt.setToolTip(getStatColumnModel().getColumn(4), "Number of success in the past 60 seconds, updated every " + StatisticsPanel.REFRESH_INTERVAL + " seconds");

        getStatTable().getTableHeader().addMouseMotionListener(htt);

        return statTablePane;
    }

    private JTable getStatTotalTable(){
        if(statTotalTable != null) return statTotalTable;

        statTotalTable = new JTable(getStatTotalTableModel(), getStatTotalColumnModel());
        statTotalTable.setRowSelectionAllowed(false);

        return statTotalTable;
    }

    private JTable getStatTable() {
        if (statTable != null) return statTable;

        statTable = new JTable(getStatTableModel(), getStatColumnModel());

        addMouseListenerToHeaderInTable(statTable);

        statTable.setShowHorizontalLines(false);
        statTable.setShowVerticalLines(false);
        statTable.setRowSelectionAllowed(true);
        statTable.getTableHeader().setReorderingAllowed(false);

        return statTable;
    }

    private DefaultTableColumnModel getStatColumnModel() {
        if(columnModel != null) return columnModel;

        columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 300));
        columnModel.addColumn(new TableColumn(1, 80));
        columnModel.addColumn(new TableColumn(2, 50));
        columnModel.addColumn(new TableColumn(3, 50));
        columnModel.addColumn(new TableColumn(4, 80));

        for(int i = 0; i < columnModel.getColumnCount(); i++){
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getStatTableModel().getColumnName(i));
        }

        columnModel.addColumnModelListener(new TableColumnModelListener(){
              public void columnMarginChanged(ChangeEvent e){
                  updateStatTotalTableColumnModel(e);
              }
              public void columnMoved(TableColumnModelEvent e){
                  // not used
              }
              public void columnAdded(TableColumnModelEvent e){
                  // not used
              }
               public void columnRemoved(TableColumnModelEvent e){
                   // not used
               }
            public void columnSelectionChanged(ListSelectionEvent e){
                // not used
            }
        });
        return columnModel;
    }


    private DefaultTableColumnModel getStatTotalColumnModel(){
        if(totalColumnModel != null ) return totalColumnModel;

        totalColumnModel = new DefaultTableColumnModel();

        totalColumnModel.addColumn(new TableColumn(0, 300));
        totalColumnModel.addColumn(new TableColumn(1, 80));
        totalColumnModel.addColumn(new TableColumn(2, 50));
        totalColumnModel.addColumn(new TableColumn(3, 50));
//        totalColumnModel.addColumn(new TableColumn(4, 100));
        totalColumnModel.addColumn(new TableColumn(4, 80));
        totalColumnModel.addColumn(new TableColumn(5, 15));
        return totalColumnModel;
    }

    private StatisticsTableSorter getStatTableModel() {
        if (statTableSorter != null) {
            return statTableSorter;
        }

        String[] cols = {"Service Name", "Routing Failure", "Policy Violation", "Success", "Success (last min.)"};
        String[][] rows = new String[][]{};

        DefaultTableModel tableModel = new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };

        statTableSorter = new StatisticsTableSorter(tableModel) {
            public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }
        };

        return statTableSorter;
    }

    DefaultTableModel getStatTotalTableModel() {
        if (statTotalTableModel != null) {
            return statTotalTableModel;
        }

        String[] cols = {"Total", "Routing Failure", "Policy Violation", "Success", "Success (last min.)", "dummy"};
        String[][] rows = new String[][]{
            {"TOTAL", null, null, null},
        };

        statTotalTableModel = new DefaultTableModel(rows, cols) {
            public Class getColumnClass(int columnIndex) {
                Class dataType = java.lang.String.class;
                // Only the value of the first column is a string, other columns contains numbers which should be aligned to the right
                if (columnIndex > 0) {
                    dataType = java.lang.Number.class;
                }
                return dataType;
            }

            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };
        return statTotalTableModel;
    }


    /** Names of services selected. Used by {@link #updateStatisticsTable} only.
        Declared as member field instead of local variable to avoid reinstantiation. */
    private Set selected = new HashSet();

    public void updateStatisticsTable(Vector<ServiceUsage> rawStatsList) {
        statsList = new Vector<StatisticsRecord>();
        totalNumRoutingFailure = 0;
        totalNumPolicyViolation = 0;
        totalNumSuccess = 0;
        totalNumSuccessLastMinute = 0;

        for (int i = 0; i < rawStatsList.size(); i++) {

            ServiceUsage stats = rawStatsList.get(i);
            long completedCount = stats.getCompleted();

            updateStatCache(stats.getServiceid(), completedCount);
            long lastMinuteCompletedCount = getLastMinuteCompletedCount(stats.getServiceid());

            long requests = stats.getRequests();
            long authorized = stats.getAuthorized();
            long completed = stats.getCompleted();
            if (serviceUsageAtCounterStart != null) {
                ServiceUsage usageAtCounterStart = serviceUsageAtCounterStart.get(stats.getServiceid());
                if (usageAtCounterStart != null) {
                    requests -= usageAtCounterStart.getRequests();
                    authorized -= usageAtCounterStart.getAuthorized();
                    completed -= usageAtCounterStart.getCompleted();
                }
            }
            StatisticsRecord statsRec = new StatisticsRecord(stats.getName(), requests, authorized, completed, lastMinuteCompletedCount);

            statsList.add(statsRec);
            totalNumRoutingFailure += statsRec.getNumRoutingFailure();
            totalNumPolicyViolation += statsRec.getNumPolicyViolation();
            totalNumSuccess += statsRec.getNumSuccess();
            totalNumSuccessLastMinute += lastMinuteCompletedCount;
        }

        /** Saves row selections before changing data. */
        selected.clear();
        for (int row = 0; row < getStatTable().getRowCount(); ++ row) {
            if (getStatTable().getSelectionModel().isSelectedIndex(row)) {
                selected.add(getStatTableModel().getValueAt(row, 0));
            }
        }

        getStatTableModel().setData(statsList);
        getStatTableModel().getRealModel().setRowCount(statsList.size());
        getStatTableModel().fireTableDataChanged();

        /** Re-applies row selections after changing data. */
        for (int row = 0; row < getStatTable().getRowCount(); ++ row) {
            if (selected.contains(getStatTable().getValueAt(row, 0))) {
                getStatTable().addRowSelectionInterval(row, row);
            }
        }

        updateRequestsTotal();
    }

    private void updateRequestsTotal() {
       getStatTotalTable().setValueAt(new Long(totalNumRoutingFailure), 0, 1);
       getStatTotalTable().setValueAt(new Long(totalNumPolicyViolation), 0, 2);
       getStatTotalTable().setValueAt(new Long(totalNumSuccess), 0, 3);
       getStatTotalTable().setValueAt(new Long(totalNumSuccessLastMinute), 0, 4);
       getStatTotalTableModel().fireTableDataChanged();
    }

    public void clearStatistics() {
        while (getStatTableModel().getRowCount() > 0) {
            getStatTableModel().getRealModel().removeRow(0);
        }
        getStatTableModel().fireTableDataChanged();

        // clear the totals
        getStatTotalTable().setValueAt(null, 0, 1);
        getStatTotalTable().setValueAt(null, 0, 2);
        getStatTotalTable().setValueAt(null, 0, 3);
        getStatTotalTable().setValueAt(null, 0, 4);
        getStatTotalTableModel().fireTableDataChanged();
    }

    public void updateStatTotalTableColumnModel(ChangeEvent e) {

        getStatTotalColumnModel().getColumn(0).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(0).getWidth());
        getStatTotalColumnModel().getColumn(1).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(1).getWidth());
        getStatTotalColumnModel().getColumn(2).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(2).getWidth());
        getStatTotalColumnModel().getColumn(3).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(3).getWidth());
        getStatTotalColumnModel().getColumn(4).setPreferredWidth(((DefaultTableColumnModel) e.getSource()).getColumn(4).getWidth());

        getStatTotalColumnModel().setColumnMargin(((DefaultTableColumnModel) e.getSource()).getColumnMargin());
    }

    public void updateStatCache(Goid serviceid, long completedCount) {

        Vector lastMinuteCounts = null;
        lastMinuteCounts = (Vector) lastMinuteCompletedCountsCache.get(serviceid);

        if (lastMinuteCounts != null) {
            if(lastMinuteCounts.size() <= NUMBER_OF_SAMPLE_PER_MINUTE){
                lastMinuteCounts.add(new Long(completedCount));
            }
            else{
               lastMinuteCounts.remove(0);
               lastMinuteCounts.add(new Long(completedCount));
            }

            if (lastMinuteCounts.size() > 2) {
                // check if the new count is smaller than the previous count
                if (((Long) lastMinuteCounts.get(lastMinuteCounts.size() - 1)).longValue()
                        - ((Long) lastMinuteCounts.get(lastMinuteCounts.size() - 2)).longValue() < 0) {

                    // this could happen when a node is removed from the cluster
                    logger.info("New total completed count is less than the previous count. Service Id: " + serviceid);

                    // should clean up the cache
                    lastMinuteCounts.removeAllElements();
                }
            }
        } else {
            Vector newList = new Vector();
            newList.add(new Long(completedCount));
            lastMinuteCompletedCountsCache.put(serviceid, newList);
        }
    }

    private long getLastMinuteCompletedCount(Goid serviceid){

        long lastMinuteCompletedCount = 0;
        Vector lastMinuteCounts = null;

        lastMinuteCounts = (Vector) lastMinuteCompletedCountsCache.get(serviceid);

        if (lastMinuteCounts != null) {
            int index = lastMinuteCounts.size() - 1;

            for(int i = 0; i < lastMinuteCounts.size() - 1 ; i++, index--){

                lastMinuteCompletedCount += ((Long) lastMinuteCounts.get(index)).longValue() - ((Long) lastMinuteCounts.get(index-1)).longValue();
            }
        }

        //for debug purpose
/*        if (lastMinuteCompletedCount < 0) {
            // dump the counter
            System.out.println("Service Name: " + serviceName +  ", count = " + lastMinuteCompletedCount);
            if (lastMinuteCounts != null) {

                for (int index = 0; index < lastMinuteCounts.size(); index++) {

                    System.out.println("couter " + index + ": " +  ((Long) lastMinuteCounts.get(index)).longValue());
                }
            }

        }*/

        return lastMinuteCompletedCount;
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Inherit the colors and font from the header component
            if (table != null) {
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setHorizontalTextPosition(SwingConstants.LEFT);
                }
            }

            setText((String) value);

            if (getStatTableModel().getSortedColumn() == column) {

                if (getStatTableModel().isAscending()) {
                    setIcon(upArrowIcon);
                } else {
                    setIcon(downArrowIcon);
                }
            }
            else{
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    };

    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    public void addMouseListenerToHeaderInTable(JTable table) {

        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    ((StatisticsTableSorter)tableView.getModel()).sortData(column, true);
                    ((StatisticsTableSorter)tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    /**
     * Sets the service name filter.
     *
     * @param pattern   a regular expression pattern to search within service names;
     *                  empty or null for no filtering
     * @throws java.util.regex.PatternSyntaxException if the pattern's syntax is invalid
     */
    public void setServiceFilter(String pattern) {
        getStatTableModel().setFilter(pattern);
        getStatTableModel().fireTableDataChanged();
    }

    /**
     * Sets counters to count from the given service usage snapshot.
     *
     * @param snapshot  service usage snapshot
     */
    public void setCounterStart(Vector<ServiceUsage> snapshot) {
        // For a snappier UI experience, instead of waiting for the next timer
        // round to refresh the display, we manually zero the displayed cell values.
        Vector<StatisticsRecord> zeroStats = new Vector<StatisticsRecord>();
        for (StatisticsRecord sr : statsList) {
            zeroStats.add(new StatisticsRecord(sr.getServiceName(), 0, 0, 0, sr.getCompletedCountLastMinute()));
        }
        getStatTableModel().setData(zeroStats);
        getStatTableModel().fireTableDataChanged();

        getStatTotalTable().setValueAt(0, 0, 1);
        getStatTotalTable().setValueAt(0, 0, 2);
        getStatTotalTable().setValueAt(0, 0, 3);
        getStatTotalTableModel().fireTableDataChanged();

        if (serviceUsageAtCounterStart == null) {
            serviceUsageAtCounterStart = new HashMap<Goid, ServiceUsage>();
        } else {
            serviceUsageAtCounterStart.clear();
        }

        for (ServiceUsage su : snapshot) {
            serviceUsageAtCounterStart.put(su.getServiceid(), su);
        }

    }

    /**
     * Sets counters to count from cluster startup.
     */
    public void clearCounterStart() {
        serviceUsageAtCounterStart = null;
    }
}
