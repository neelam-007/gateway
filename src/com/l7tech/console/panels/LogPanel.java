package com.l7tech.console.panels;

import com.l7tech.console.table.LogTableModel;
import com.l7tech.console.table.FilteredLogTableSorter;
import com.l7tech.console.icons.ArrowIcon;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.text.SimpleDateFormat;


/*
 * This class creates a log panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogPanel extends JPanel {

     // Titles/Labels
    public static final int MSG_FILTER_LEVEL_ALL = 4;
    public static final int MSG_FILTER_LEVEL_INFO = 3;
    public static final int MSG_FILTER_LEVEL_WARNING = 2;
    public static final int MSG_FILTER_LEVEL_SEVERE = 1;

    public static final int LOG_MSG_NUMBER_COLUMN_INDEX = 0;
    public static final int LOG_NODE_NAME_COLUMN_INDEX = 1;
    public static final int LOG_TIMESTAMP_COLUMN_INDEX = 2;
    public static final int LOG_SEVERITY_COLUMN_INDEX = 3;
    public static final int LOG_MSG_DETAILS_COLUMN_INDEX = 4;
    public static final int LOG_JAVA_CLASS_COLUMN_INDEX = 5;
    public static final int LOG_JAVA_METHOD_COLUMN_INDEX = 6;
    public static final int LOG_REQUEST_ID_COLUMN_INDEX = 7;

    public static final String MSG_TOTAL_PREFIX = "Total: ";

    private static final int LOG_REFRESH_TIMER = 3000;
    private javax.swing.Timer logsRefreshTimer = null;

    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private int msgFilterLevel = MSG_FILTER_LEVEL_WARNING;
    private JPanel selectPane = null;
    private JPanel filterPane = null;

    private JPanel controlPane = null;
    private JScrollPane msgTablePane = null;
    private JPanel statusPane = null;
    private JTable msgTable = null;
    private JTabbedPane msgDetailsPane = null;
    private JTextArea msgDetails = null;
    private JSlider slider = null;
    private JCheckBox autoRefresh = null;
    private DefaultTableModel logTableModel = null;
    private FilteredLogTableSorter logTableSorter = null;
    private JLabel msgTotal = null;
    private JLabel lastUpdateTimeLabel = null;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);

    /**
     * Constructor
     */
    public LogPanel() {
        setLayout(new BorderLayout());

        JSplitPane logSplitPane = new JSplitPane();

        logSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        logSplitPane.setTopComponent(getMsgTablePane());
        logSplitPane.setBottomComponent(getMsgDetailsPane());
        logSplitPane.setDividerLocation(0.5);

        add(logSplitPane, BorderLayout.CENTER);
        add(getSelectPane(), BorderLayout.SOUTH);

        getMsgTable().getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    /**
                     * Called whenever the value of the selection changes.
                     * @param e the event that characterizes the change.
                     */
                    public void valueChanged(ListSelectionEvent e) {
                        int row = getMsgTable().getSelectedRow();
                        String msg;

                        if (row == -1) return;

                        //msg = "The selected row is: " + row + "\n";
                        msg = "";
                        //if (getMsgTable().getModel().getValueAt(row, LOG_MSG_NUMBER_COLUMN_INDEX) != null)
                        //    msg = msg + "Message #: " + getMsgTable().getModel().getValueAt(row, LOG_MSG_NUMBER_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_NODE_NAME_COLUMN_INDEX) != null)
                            msg = msg + "Node     : " + getMsgTable().getModel().getValueAt(row, LOG_NODE_NAME_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_TIMESTAMP_COLUMN_INDEX) != null)
                            msg = msg + "Time     : " + getMsgTable().getModel().getValueAt(row, LOG_TIMESTAMP_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_SEVERITY_COLUMN_INDEX) != null)
                            msg = msg + "Severity : " + getMsgTable().getModel().getValueAt(row, LOG_SEVERITY_COLUMN_INDEX).toString() + "\n";
                        if ((getMsgTable().getModel().getValueAt(row, LOG_REQUEST_ID_COLUMN_INDEX) != null) &&
                             (getMsgTable().getModel().getValueAt(row, LOG_REQUEST_ID_COLUMN_INDEX)).toString().length() > 0)
                            msg = msg + "Request Id : " + getMsgTable().getModel().getValueAt(row, LOG_REQUEST_ID_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_MSG_DETAILS_COLUMN_INDEX) != null)
                            msg = msg + "Message    : " + getMsgTable().getModel().getValueAt(row, LOG_MSG_DETAILS_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_JAVA_CLASS_COLUMN_INDEX) != null)
                            msg = msg + "Class    : " + getMsgTable().getModel().getValueAt(row, LOG_JAVA_CLASS_COLUMN_INDEX).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, LOG_JAVA_METHOD_COLUMN_INDEX) != null)
                            msg = msg + "Method   : " + getMsgTable().getModel().getValueAt(row, LOG_JAVA_METHOD_COLUMN_INDEX).toString() + "\n";
                        getMsgDetails().setText(msg);

                    }
                });
    }

    /**
     * Return the log message filter level.
     * @return int msgFilterLevel - Message filter level.
     */
    public int getMsgFilterLevel(){
        return msgFilterLevel;
    }

    public void stopRefreshTimer(){
        getLogsRefreshTimer().stop();
    }

    public void onConnect(){
        getFilteredLogTableSorter().onConnect();
        clearMsgTable();
        getLogsRefreshTimer().start();
    }

    public void onDisconnect(){
        getLogsRefreshTimer().stop();
        getFilteredLogTableSorter().onDisconnect();
    }

    /**
     * Return SelectPane property value
     * @return JPanel
     */
    private JPanel getSelectPane(){
        if(selectPane != null) return selectPane;

        selectPane = new JPanel();
        selectPane.setMinimumSize(new Dimension((int)selectPane.getSize().getWidth(), 50));
        selectPane.setPreferredSize(new Dimension((int)selectPane.getSize().getWidth(), 50));
       // selectPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPane.setLayout(new BorderLayout());

        JPanel leftPane = new JPanel();
        leftPane.setLayout(new FlowLayout());
        leftPane.add(getFilterPane());
        leftPane.add(getControlPane());

        selectPane.add(leftPane, BorderLayout.WEST);
        selectPane.add(getStatusPane(), BorderLayout.EAST);


        return selectPane;
    }

    /**
     * Return FilterPane property value
     * @return JPanel
     */
    private JPanel getFilterPane(){
        if(filterPane != null) return filterPane;

        filterPane = new JPanel();
        filterPane.setLayout(new FlowLayout());
        filterPane.add(getFilterSlider());

        return filterPane;
    }

    /**
     * Return filterSlider property value
     * @return JSlider
     */
    private JSlider getFilterSlider(){
        if(slider != null)  return slider;

        slider = new JSlider(0, 120);
        slider.setMajorTickSpacing(40);

        Dictionary table = new Hashtable();
        JLabel aLabel = new JLabel("all");

        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(0), aLabel);

        aLabel = new JLabel("info");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(40), aLabel);

        aLabel = new JLabel("warning");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(80), aLabel);

        aLabel = new JLabel("severe");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(120), aLabel);

        slider.setPaintLabels(true);
        slider.setLabelTable(table);
        slider.setSnapToTicks(true);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                if (!source.getValueIsAdjusting()) {
                    int value = source.getValue();
                    switch (value) {
                        case 0:
                            updateMsgFilterLevel(MSG_FILTER_LEVEL_ALL);
                            break;
                        case 40:
                            updateMsgFilterLevel(MSG_FILTER_LEVEL_INFO);
                            break;
                        case 80:
                            updateMsgFilterLevel(MSG_FILTER_LEVEL_WARNING);
                            break;
                        case 120:
                            updateMsgFilterLevel(MSG_FILTER_LEVEL_SEVERE);
                            break;
                        default:
                            System.err.println("Unhandled value " + value);
                    }
                }
            }
        });

        return slider;
    }

    /**
     * Return ControlPane property value
     * @return  JPanel
     */
    private JPanel getControlPane(){
        if(controlPane != null) return controlPane;

        controlPane = new JPanel();
        controlPane.setLayout(new FlowLayout());

         if(autoRefresh == null){
            autoRefresh = new JCheckBox();
        }
        autoRefresh.setFont(new java.awt.Font("Dialog", 0, 12));
        autoRefresh.setText("Auto-refresh");
        autoRefresh.setSelected(true);
        autoRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (autoRefresh.isSelected()) {
                    getLogsRefreshTimer().start();
                } else {
                    getLogsRefreshTimer().stop();
                }
            }
        });

        controlPane.add(autoRefresh);
        controlPane.add(getMsgTotal());

        return controlPane;
    }

    private JLabel getMsgTotal(){
        if(msgTotal != null) return msgTotal;
        msgTotal = new JLabel(MSG_TOTAL_PREFIX + "0");
        msgTotal.setFont(new java.awt.Font("Dialog", 0, 12));
        return msgTotal;
    }

    /**
     * Return MsgTable property value
     * @return JTable
     */
    private JTable getMsgTable(){
        if(msgTable != null) return msgTable;

        msgTable = new JTable(getFilteredLogTableSorter(), getLogColumnModel());
        msgTable.setShowHorizontalLines(false);
        msgTable.setShowVerticalLines(false);
        msgTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        msgTable.setRowSelectionAllowed(true);

        addMouseListenerToHeaderInTable(msgTable);

        return msgTable;
    }

    /**
     * Return MsgTablePane property value
     * @return JScrollPane
     */
    private JScrollPane getMsgTablePane(){
        if(msgTablePane != null) return msgTablePane;

        msgTablePane = new JScrollPane();
        msgTablePane.setViewportView(getMsgTable());
        msgTablePane.getViewport().setBackground(getMsgTable().getBackground());
        msgTablePane.setMinimumSize(new Dimension(1000, 200));
        msgTablePane.setPreferredSize(new Dimension(1000, 300));
        return msgTablePane;
    }

    /**
     * Return MsgDetailsPane property value
     * @return JScrollPane
     */
    private JTabbedPane getMsgDetailsPane(){
        if(msgDetailsPane != null)  return msgDetailsPane;

        msgDetailsPane = new JTabbedPane();
        msgDetailsPane.setMaximumSize(new java.awt.Dimension(1000, 150));
        msgDetailsPane.setMinimumSize(new java.awt.Dimension(1000, 100));
        msgDetailsPane.setPreferredSize(new java.awt.Dimension(1000, 150));

        JScrollPane msgDetailsScrollPane = new JScrollPane();
        msgDetailsScrollPane.setViewportView(getMsgDetails());
        msgDetailsPane.addTab("Details", msgDetailsScrollPane);

        return msgDetailsPane;
    }

    /**
     * Return MsgDetails property value
     * @return JTextArea
     */
    private JTextArea getMsgDetails()
    {
        if(msgDetails != null) return msgDetails;

        msgDetails = new JTextArea();
        msgDetails.setEditable(false);
       // msgDetails.setMinimumSize(new Dimension(0, 0));

        return msgDetails;
    }

    private JPanel getStatusPane() {
        if(statusPane != null)  return statusPane;

        statusPane = new JPanel();
        statusPane.setLayout(new GridLayout());
        statusPane.add(getLastUpdateTimeLabel());

        return statusPane;
    }

    private JLabel getLastUpdateTimeLabel() {

        if(lastUpdateTimeLabel != null) return lastUpdateTimeLabel;

        lastUpdateTimeLabel = new JLabel();
//        lastUpdateTimeLabel.setVerticalTextPosition(JLabel.CENTER);
        lastUpdateTimeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        lastUpdateTimeLabel.setText("");
        return lastUpdateTimeLabel;
    }
    /**
     * Return LogColumnModel property value
     * @return  DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(LOG_MSG_NUMBER_COLUMN_INDEX, 20));
        columnModel.addColumn(new TableColumn(LOG_NODE_NAME_COLUMN_INDEX, 30));
        columnModel.addColumn(new TableColumn(LOG_TIMESTAMP_COLUMN_INDEX, 80));
        columnModel.addColumn(new TableColumn(LOG_SEVERITY_COLUMN_INDEX, 15));
        columnModel.addColumn(new TableColumn(LOG_MSG_DETAILS_COLUMN_INDEX, 400));
        columnModel.addColumn(new TableColumn(LOG_JAVA_CLASS_COLUMN_INDEX, 0));   // min width is used
        columnModel.addColumn(new TableColumn(LOG_JAVA_METHOD_COLUMN_INDEX, 0));   // min width is used
        columnModel.addColumn(new TableColumn(LOG_REQUEST_ID_COLUMN_INDEX, 0));

        for(int i = 0; i < columnModel.getColumnCount(); i++){
            columnModel.getColumn(i).setHeaderRenderer(iconHeaderRenderer);
            columnModel.getColumn(i).setHeaderValue(getLogTableModel().getColumnName(i));
        }

        String showMsgFlag = resapplication.getString("Show_Message_Number_Column");
        if ((showMsgFlag != null) && showMsgFlag.equals(new String("true"))){
            // show the message # column (mainly for debugging and testing purpose
        }
        else{
            columnModel.getColumn(LOG_MSG_NUMBER_COLUMN_INDEX).setMinWidth(0);
            columnModel.getColumn(LOG_MSG_NUMBER_COLUMN_INDEX).setMaxWidth(0);
            columnModel.getColumn(LOG_MSG_NUMBER_COLUMN_INDEX).setPreferredWidth(0);
        }

        // we don't show following columns including method, class
        // but the data is retrieved for display in the detailed pane
        columnModel.getColumn(LOG_JAVA_CLASS_COLUMN_INDEX).setMinWidth(0);
        columnModel.getColumn(LOG_JAVA_CLASS_COLUMN_INDEX).setMaxWidth(0);
        columnModel.getColumn(LOG_JAVA_CLASS_COLUMN_INDEX).setPreferredWidth(0);

        columnModel.getColumn(LOG_JAVA_METHOD_COLUMN_INDEX).setMinWidth(0);
        columnModel.getColumn(LOG_JAVA_METHOD_COLUMN_INDEX).setMaxWidth(0);
        columnModel.getColumn(LOG_JAVA_METHOD_COLUMN_INDEX).setPreferredWidth(0);

        columnModel.getColumn(LOG_REQUEST_ID_COLUMN_INDEX).setMinWidth(0);
        columnModel.getColumn(LOG_REQUEST_ID_COLUMN_INDEX).setMaxWidth(0);
        columnModel.getColumn(LOG_REQUEST_ID_COLUMN_INDEX).setPreferredWidth(0);

        return columnModel;
    }

    /**
     * Return LogTableModelFilter property value
     * @return FilteredLogTableModel
     */
    private FilteredLogTableSorter getFilteredLogTableSorter(){
        if(logTableSorter != null) return logTableSorter;

        logTableSorter = new FilteredLogTableSorter();
        logTableSorter.setRealModel(getLogTableModel());

        return logTableSorter;
    }

    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     *
     */
    private DefaultTableModel getLogTableModel() {
        if (logTableModel != null) {
            return logTableModel;
        }

        String[] cols = {"Message #", "Node", "Time", "Severity", "Message", "Class", "Method"};
        String[][] rows = new String[][]{};

        logTableModel = new LogTableModel(rows, cols);

        return logTableModel;
    }


    public void refreshLogs() {
        getLogsRefreshTimer().stop();

        // get the selected row index
        int selectedRowIndexOld = getMsgTable().getSelectedRow();
        String msgNumSelected = null;

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            msgNumSelected = getMsgTable().getValueAt(selectedRowIndexOld, 0).toString();
        }

        // retrieve the new logs
        ((FilteredLogTableSorter) getMsgTable().getModel()).refreshLogs(getMsgFilterLevel(), this, msgNumSelected, autoRefresh.isSelected(), new Vector(), true);

    }

    public void setSelectedRow(String msgNumber) {
        if (msgNumber != null) {
            // keep the current row selection
            int rowCount = getMsgTable().getRowCount();
            boolean rowFound = false;
            for (int i = 0; i < rowCount; i++) {
                if (getMsgTable().getValueAt(i, 0).toString().equals(msgNumber)) {
                    getMsgTable().setRowSelectionInterval(i, i);

                    rowFound = true;
                    break;
                }
            }

            if (!rowFound) {
                // clear the details text area
                getMsgDetails().setText("");
            }
        }
    }

    private void updateMsgFilterLevel(int newFilterLevel) {

        if (msgFilterLevel != newFilterLevel) {

            msgFilterLevel = newFilterLevel;
            // get the selected row index
            int selectedRowIndexOld = getMsgTable().getSelectedRow();
            String msgNumSelected = null;

            // save the number of selected message
            if (selectedRowIndexOld >= 0) {
                msgNumSelected = getMsgTable().getValueAt(selectedRowIndexOld, 0).toString();
            }

            ((FilteredLogTableSorter) getMsgTable().getModel()).applyNewMsgFilter(newFilterLevel);

            if (msgNumSelected != null) {
                setSelectedRow(msgNumSelected);
            }

            updateMsgTotal();
        }
    }


    public void clearMsgTable(){
       //todo: clear table
        //((FilteredLogTableSorter)getMsgTable().getModel()).clearTable();
        getMsgDetails().setText("");
        msgTotal.setText(MSG_TOTAL_PREFIX + "0");
    }


    public javax.swing.Timer getLogsRefreshTimer() {

        if (logsRefreshTimer != null) return logsRefreshTimer;

        //Create a refresh logs timer.
        logsRefreshTimer = new javax.swing.Timer(LOG_REFRESH_TIMER, new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                refreshLogs();
            }
        });

        return logsRefreshTimer;
    }

    public void updateMsgTotal(){
         msgTotal.setText(MSG_TOTAL_PREFIX + msgTable.getRowCount());
    }

    public void updateTimeStamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
        getLastUpdateTimeLabel().setText("Last updated: " + sdf.format(Calendar.getInstance().getTime()) + "      ");
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

            if (getFilteredLogTableSorter().getSortedColumn() == column) {

                if (getFilteredLogTableSorter().isAscending()) {
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

                    ((FilteredLogTableSorter)tableView.getModel()).sortData(column, true);
                    ((FilteredLogTableSorter)tableView.getModel()).fireTableDataChanged();
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
