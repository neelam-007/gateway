package com.l7tech.console.panels;

import com.l7tech.common.audit.AdminAuditRecord;
import com.l7tech.common.audit.AuditRecord;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.SystemAuditRecord;
import com.l7tech.common.gui.widgets.ContextMenuTextArea;
import com.l7tech.common.util.Locator;
import com.l7tech.console.table.FilteredLogTableSorter;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.logging.LogMessage;
import com.l7tech.logging.SSGLogRecord;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;


/*
 * This class creates a log panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogPanel extends JPanel {
    public static final int MSG_FILTER_LEVEL_SEVERE = 1;
    public static final int MSG_FILTER_LEVEL_WARNING = 2;
    public static final int MSG_FILTER_LEVEL_INFO = 3;
    public static final int MSG_FILTER_LEVEL_ALL = 4;

    public static final int LOG_MSG_NUMBER_COLUMN_INDEX = 0;
    public static final int LOG_NODE_NAME_COLUMN_INDEX = 1;
    public static final int LOG_TIMESTAMP_COLUMN_INDEX = 2;
    public static final int LOG_SEVERITY_COLUMN_INDEX = 3;
    public static final int LOG_MSG_DETAILS_COLUMN_INDEX = 4;
    public static final int LOG_JAVA_CLASS_COLUMN_INDEX = 5;
    public static final int LOG_JAVA_METHOD_COLUMN_INDEX = 6;
    public static final int LOG_REQUEST_ID_COLUMN_INDEX = 7;
    public static final int LOG_NODE_ID_COLUMN_INDEX = 8;

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
    private final Locator logAdminLocator;
    private JScrollPane detailsScrollPane;
    private LogMessage displayedLogMessage = null;

    /**
     * Constructor
     */
    public LogPanel(Locator logAdminLocator) {
        this.logAdminLocator = logAdminLocator;
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
                    public void valueChanged(ListSelectionEvent e) {
                        updateMsgDetails();
                    }
                });
    }

    private void updateMsgDetails() {
        int row = getMsgTable().getSelectedRow();

        if (row == -1) return;

        final TableModel model = getMsgTable().getModel();
        String msg = "";
        //if (getMsgTable().getModel().getValueAt(row, LOG_MSG_NUMBER_COLUMN_INDEX) != null)
        //    msg = msg + "Message #: " + getMsgTable().getModel().getValueAt(row, LOG_MSG_NUMBER_COLUMN_INDEX).toString() + "\n";
        if (model instanceof FilteredLogTableSorter) {
            LogMessage lm = ((FilteredLogTableSorter)model).getLogMessageAtRow(row);
            if (lm == displayedLogMessage) return;
            displayedLogMessage = lm;
            SSGLogRecord rec = lm.getSSGLogRecord();

            msg += nonull("Node       : ", lm.getNodeName());
            msg += nonull("Time       : ", lm.getTime());
            msg += nonull("Severity   : ", lm.getSeverity());
            msg += nonule("Request Id : ", lm.getReqId());
            msg += nonull("Class      : ", lm.getMsgClass());
            msg += nonull("Method     : ", lm.getMsgMethod());
            msg += nonull("Message    : ", lm.getMsgDetails());

            if (rec instanceof AuditRecord) {
                AuditRecord arec = (AuditRecord)rec;
                msg += "\n";

                if (arec instanceof AdminAuditRecord) {
                    AdminAuditRecord aarec = (AdminAuditRecord)arec;
                    msg += "Event Type : Administrator Action" + "\n";
                    msg += "Admin user : " + aarec.getAdminLogin() + "\n";
                    msg += "Admin IP   : " + arec.getIpAddress() + "\n";
                    msg += "Action     : " + fixAction(aarec.getAction()) + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                    msg += "Entity id  : " + aarec.getEntityOid() + "\n";
                    msg += "Entity type: " + fixType(aarec.getEntityClassname()) + "\n";
                } else if (arec instanceof MessageSummaryAuditRecord) {
                    MessageSummaryAuditRecord sum = (MessageSummaryAuditRecord)arec;
                    msg += "Event Type : Message Summary" + "\n";
                    msg += "Client IP  : " + arec.getIpAddress() + "\n";
                    msg += "Service    : " + sum.getName() + "\n";
                    msg += "Rqst Length: " + fixNegative(sum.getRequestContentLength(), "<Not Read>") + "\n";
                    msg += "Resp Length: " + fixNegative(sum.getResponseContentLength(), "<Not Routed>") + "\n";
                    msg += "User ID    : " + sum.getUserId() + "\n";
                    msg += "User Name  : " + sum.getUserName() + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                } else if (arec instanceof SystemAuditRecord) {
                    SystemAuditRecord sys = (SystemAuditRecord)arec;
                    msg += "Event Type : System Message" + "\n";
                    msg += "Node IP    : " + arec.getIpAddress() + "\n";
                    msg += "Action     : " + sys.getAction() + "\n";
                    msg += "Component  : " + fixComponent(sys.getComponent()) + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                } else {
                    msg += "Event Type : Unknown" + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                    msg += "IP Address : " + arec.getIpAddress() + "\n";
                }
            }
        }

        // update the msg details field only if the content has changed.
        if(!msg.equals(getMsgDetails().getText())){
            getMsgDetails().setText(msg);
            if (msg.length() > 0)
                // Scroll to top
                getMsgDetails().getCaret().setDot(1);
        }
    }

    private String nonull(String s, String n) {
        return n == null ? "" : (s + n + "\n");
    }

    private String nonule(String s, String n) {
        return n == null || n.length() < 1 ? "" : (s + n + "\n");
    }

    private String fixNegative(int num, String s) {
        return num < 0 ? s : String.valueOf(num);
    }

    private String fixComponent(String component) {
        com.l7tech.common.Component c = com.l7tech.common.Component.fromCode(component);
        if (c == null) return "Unknown Component '" + component + "'";
        StringBuffer ret = new StringBuffer(c.getName());
        while (c.getParent() != null && c.getParent() != c) {
            ret.insert(0, ": ");
            ret.insert(0, c.getParent().getName());
            c = c.getParent();
        }
        return ret.toString();
    }

    /** Strip the "com.l7tech." from the start of a class name. */
    private String fixType(String entityClassname) {
        final String coml7tech = "com.l7tech.";
        if (entityClassname == null) {
            return "<unknown>";
        } else if (entityClassname.startsWith(coml7tech))
            return entityClassname.substring(coml7tech.length());
        return entityClassname;
    }

    /** Convert a single-character action into a human-readable String. */
    private String fixAction(char action) {
        switch (action) {
            case AdminAuditRecord.ACTION_CREATED:
                return "Object Created";
            case AdminAuditRecord.ACTION_UPDATED:
                return "Object Changed";
            case AdminAuditRecord.ACTION_DELETED:
                return "Object Deleted";
            default:
                return "Unknown Action '" + action + "'";
        }
    }

    /**
     * Return the log message filter level.
     * @return int msgFilterLevel - Message filter level.
     */
    public int getMsgFilterLevel(){
        return msgFilterLevel;
    }

    /**
     * Stop the refresh timer.
     */
    public void stopRefreshTimer(){
        getLogsRefreshTimer().stop();
    }

    /**
     * Performs the necessary initialization when the connection with the cluster is established.
     */
    public void onConnect(){
        getFilteredLogTableSorter().onConnect();
        clearMsgTable();
        getLogsRefreshTimer().start();
    }

    /**
     * Performs the necessary cleanup when the connection with the cluster went down.
     */
    public void onDisconnect(){
        getLogsRefreshTimer().stop();
        getFilteredLogTableSorter().onDisconnect();

        if(!getLastUpdateTimeLabel().getText().trim().endsWith("[Disconnected]")) {
            getLastUpdateTimeLabel().setText(getLastUpdateTimeLabel().getText().trim() + " [Disconnected]   ");
        }

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
        JLabel aLabel = new JLabel("All");

        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(0), aLabel);

        aLabel = new JLabel("Info");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(40), aLabel);

        aLabel = new JLabel("Warning");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(80), aLabel);

        aLabel = new JLabel("Severe");
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
        autoRefresh.setText("Auto-Refresh");
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

    /**
     * Return the total number of the messages being displayed.
     * @return
     */
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

        JScrollPane msgDetailsScrollPane = getDetailsScrollPane();
        msgDetailsPane.addTab("Details", msgDetailsScrollPane);

        return msgDetailsPane;
    }

    private JScrollPane getDetailsScrollPane() {
        if (detailsScrollPane != null) return detailsScrollPane;
        detailsScrollPane = new JScrollPane();
        detailsScrollPane.setViewportView(getMsgDetails());
        return detailsScrollPane;
    }

    /**
     * Return MsgDetails property value
     * @return  JTextArea
     */
    private JTextArea getMsgDetails()
    {
        if(msgDetails != null) return msgDetails;

        msgDetails = new ContextMenuTextArea();
        msgDetails.setEditable(false);

        return msgDetails;
    }

    /**
     * Return statusPane property value
     * @return  JPanel
     */
    private JPanel getStatusPane() {
        if(statusPane != null)  return statusPane;

        statusPane = new JPanel();
        statusPane.setLayout(new GridLayout());
        statusPane.add(getLastUpdateTimeLabel());

        return statusPane;
    }

    /**
     * Return lastUpdateTimeLabel property value
     * @return  JLabel
     */
    private JLabel getLastUpdateTimeLabel() {

        if(lastUpdateTimeLabel != null) return lastUpdateTimeLabel;

        lastUpdateTimeLabel = new JLabel();
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
        columnModel.addColumn(new TableColumn(LOG_NODE_ID_COLUMN_INDEX, 0));

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

        columnModel.getColumn(LOG_NODE_ID_COLUMN_INDEX).setMinWidth(0);
        columnModel.getColumn(LOG_NODE_ID_COLUMN_INDEX).setMaxWidth(0);
        columnModel.getColumn(LOG_NODE_ID_COLUMN_INDEX).setPreferredWidth(0);

        return columnModel;
    }

    /**
     * Return LogTableModelFilter property value
     * @return FilteredLogTableModel
     */
    private FilteredLogTableSorter getFilteredLogTableSorter(){
        if(logTableSorter != null) return logTableSorter;

        logTableSorter = new FilteredLogTableSorter(this, getLogTableModel(), logAdminLocator);

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

        String[] cols = {"Message #", "Node", "Time", "Severity", "Message", "Class", "Method", "Request Id", "Node Id"};
        String[][] rows = new String[][]{};

        logTableModel = new DefaultTableModel(rows, cols) {
            public boolean isCellEditable(int row, int col) {
                // the table cells are not editable
                return false;
            }
        };

        return logTableModel;
    }

    /**
     * Return the message number of the selected row in the log table.
     *
     * @return  String  The message number of the selected row in the log table.
     */
    public String getSelectedMsgNumber() {
        // get the selected row index

        int selectedRowIndexOld = getMsgTable().getSelectedRow();
        String msgNumSelected = "-1";

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            msgNumSelected =
                    getMsgTable().getValueAt(selectedRowIndexOld, LOG_NODE_ID_COLUMN_INDEX).toString().trim() +
                    getMsgTable().getValueAt(selectedRowIndexOld, LOG_MSG_NUMBER_COLUMN_INDEX).toString().trim();
        }

        return msgNumSelected;
    }

    /**
     * Performs the log retrieval. This function is called when the refresh timer is expired.
     */
    public void refreshLogs() {
        getLogsRefreshTimer().stop();

        // retrieve the new logs
        ((FilteredLogTableSorter) getMsgTable().getModel()).refreshLogs(this, autoRefresh.isSelected(), new Vector(), true);

    }

    /**
     * Set the row of the log table which is currenlty selected by the user for viewing the details of the log message.
     *
     * @param msgNumber  The message number of the log being selected.
     */
    public void setSelectedRow(String msgNumber) {

        if (msgNumber != null) {

            // keep the current row selection
            int rowCount = getMsgTable().getRowCount();
            boolean rowFound = false;
            for (int i = 0; i < rowCount; i++) {
                String selctedMsgNum = getMsgTable().getValueAt(i, LOG_NODE_ID_COLUMN_INDEX).toString().trim() + getMsgTable().getValueAt(i, LOG_MSG_NUMBER_COLUMN_INDEX).toString().trim();

                if (selctedMsgNum.equals(msgNumber)) {
                    getMsgTable().setRowSelectionInterval(i, i);

                    rowFound = true;
                    break;
                }
            }

            if (!rowFound) {
                // clear the details text area
                getMsgDetails().setText("");
                displayedLogMessage = null;
            }
        }
    }

    /**
     * Update the filter level.
     *
     * @param newFilterLevel  The new filter level to be applied.
     */
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

    /**
     *  Clear the message table
     */
    public void clearMsgTable(){
        getMsgDetails().setText("Loading data...");
        msgTotal.setText(MSG_TOTAL_PREFIX + "0");
        displayedLogMessage = null;
        getFilteredLogTableSorter().clearLogCache();
    }

    /**
     * Return lgsRefreshTimer propery value
     *
     * @return javax.swing.Timer
     */
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

    /**
     * Update the message total.
     */
    public void updateMsgTotal(){
         msgTotal.setText(MSG_TOTAL_PREFIX + msgTable.getRowCount());
    }

    /**
     * Update the timestamp of the "last updated" label.
     */
    public void updateTimeStamp(java.util.Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        getLastUpdateTimeLabel().setText("Last Updated: " + sdf.format(cal.getTime()) + "   ");
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

    /**
     * Add a mouse listener to the Table to trigger a table sorting
     * when a column heading is clicked in the JTable.
     */
    public void addMouseListenerToHeaderInTable(JTable table) {

        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);

        final LogPanel logPane = this;

        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {

                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    String msgNumSelected = logPane.getSelectedMsgNumber();

                    ((FilteredLogTableSorter) tableView.getModel()).sortData(column, true);
                    ((FilteredLogTableSorter) tableView.getModel()).fireTableDataChanged();

                    setSelectedRow(msgNumSelected);
                    tableView.getTableHeader().resizeAndRepaint();
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
