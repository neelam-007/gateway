package com.l7tech.console.panels;

import com.l7tech.console.table.LogTableModel;
import com.l7tech.console.table.FilteredLogTableModel;
import com.l7tech.console.util.Preferences;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.io.IOException;


/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 12, 2003
 * Time: 4:12:07 PM
 * This class implements the display for log messsgaes.
 */
public class LogPanel extends JPanel {

     // Titles/Labels
    public static final int MSG_FILTER_LEVEL_ALL = 4;
    public static final int MSG_FILTER_LEVEL_INFO = 3;
    public static final int MSG_FILTER_LEVEL_WARNING = 2;
    public static final int MSG_FILTER_LEVEL_SEVERE = 1;

    public static final String MSG_TOTAL_PREFIX = "Total: ";

    private static final int LOG_REFRESH_TIMER = 3000;
    private javax.swing.Timer logsRefreshTimer = null;

    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private int msgFilterLevel = MSG_FILTER_LEVEL_WARNING;
    private JPanel selectPane = null;
    private JPanel msgPane = null;
    private JPanel filterPane = null;

    private JPanel controlPane = null;
    private JSplitPane logPaneTop = null;
    private JScrollPane msgTablePane = null;
    private JTable msgTable = null;
    private JTabbedPane msgDetailsPane = null;
    private JTextArea msgDetails = null;
    private JSlider slider = null;
    private JCheckBox details = null;
    private JCheckBox autoRefresh = null;
    private DefaultTableModel logTableModel = null;
    private FilteredLogTableModel logTableModelFilter = null;
    private JLabel msgTotal = new JLabel(MSG_TOTAL_PREFIX + "0");

    /**
     * Constructor
     */
    public LogPanel() {
        setLayout(new BorderLayout());

        add(getMsgPane(), BorderLayout.NORTH);
        add(getMsgDetailsPane(), BorderLayout.CENTER);
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
                        //if (getMsgTable().getModel().getValueAt(row, 0) != null)
                        //    msg = msg + "Message #: " + getMsgTable().getModel().getValueAt(row, 0).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 1) != null)
                            msg = msg + "Time     : " + getMsgTable().getModel().getValueAt(row, 1).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 2) != null)
                            msg = msg + "Severity : " + getMsgTable().getModel().getValueAt(row, 2).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 3) != null)
                            msg = msg + "Message  : " + getMsgTable().getModel().getValueAt(row, 3).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 4) != null)
                            msg = msg + "Class    : " + getMsgTable().getModel().getValueAt(row, 4).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 5) != null)
                            msg = msg + "Method   : " + getMsgTable().getModel().getValueAt(row, 5).toString() + "\n";
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
        getLogTableModelFilter().onConnect();
    }

    public void onDisconnect(){
        getLogTableModelFilter().onDisconnect();
        stopRefreshTimer();
        clearMsgTable();
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
        selectPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPane.add(getFilterPane());
        selectPane.add(getControlPane());

        return selectPane;
    }

    /**
     * Return FilterPane property value
     * @return JPanel
     */
    private JPanel getFilterPane(){
        if(filterPane != null) return filterPane;

        filterPane = new JPanel();
        filterPane.setLayout(new BorderLayout());
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

        if(details == null){
            details = new JCheckBox();
        }
        details.setFont(new java.awt.Font("Dialog", 0, 11));
        details.setText("Details");
        details.setSelected(true);
/*        details.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detailsActionPerformed(evt);
            }
        });*/

         if(autoRefresh == null){
            autoRefresh = new JCheckBox();
        }
        autoRefresh.setFont(new java.awt.Font("Dialog", 0, 11));
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
        controlPane.add(details);
        controlPane.add(msgTotal);

        return controlPane;
    }

    /**
     * Return MsgTable property value
     * @return JTable
     */
    private JTable getMsgTable(){
        if(msgTable != null) return msgTable;

        msgTable = new JTable(getLogTableModelFilter(), getLogColumnModel());
        msgTable.setShowHorizontalLines(false);
        msgTable.setShowVerticalLines(false);
        msgTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        msgTable.setRowSelectionAllowed(true);

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

        return msgTablePane;
    }

    /**
     * Return LogPaneTop property value
     * @return JSplitPane
     */
/*    private JSplitPane getLogPaneTop(){
        if(logPaneTop != null)  return logPaneTop;

        logPaneTop = new JSplitPane();
        logPaneTop.setLeftComponent(getMsgTablePane());
        logPaneTop.setRightComponent(getMsgDetailsPane());
        logPaneTop.setResizeWeight(0.5);
        return logPaneTop;
    }*/

    /**
     * Return MsgDetailsPane property value
     * @return JScrollPane
     */
    private JTabbedPane getMsgDetailsPane(){
        if(msgDetailsPane != null)  return msgDetailsPane;

        msgDetailsPane = new JTabbedPane();
        msgDetailsPane.setMaximumSize(new java.awt.Dimension(1000, 150));
        msgDetailsPane.setMinimumSize(new java.awt.Dimension(400, 150));
        msgDetailsPane.setPreferredSize(new java.awt.Dimension(400, 150));
        msgDetailsPane.addTab("Details", getMsgDetails());

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

    /**
     * Return MsgPane property value
     * @return JPanel
     */
    private JPanel getMsgPane(){
        if(msgPane != null) return msgPane;

        msgPane = new JPanel();
        msgPane.setLayout(new BorderLayout());

        msgPane.add(getMsgTablePane(), BorderLayout.CENTER);

        return msgPane;
    }


    /**
     * Return LogColumnModel property value
     * @return  DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 20));
        columnModel.addColumn(new TableColumn(1, 60));
        columnModel.addColumn(new TableColumn(2, 15));
        columnModel.addColumn(new TableColumn(3, 400));
        columnModel.addColumn(new TableColumn(4, 0));   // min width is used
        columnModel.addColumn(new TableColumn(5, 0));   // min width is used
        columnModel.getColumn(0).setHeaderValue(getLogTableModel().getColumnName(0));
        columnModel.getColumn(1).setHeaderValue(getLogTableModel().getColumnName(1));
        columnModel.getColumn(2).setHeaderValue(getLogTableModel().getColumnName(2));
        columnModel.getColumn(3).setHeaderValue(getLogTableModel().getColumnName(3));
        columnModel.getColumn(4).setHeaderValue(getLogTableModel().getColumnName(4));
        columnModel.getColumn(5).setHeaderValue(getLogTableModel().getColumnName(5));

        String showMsgFlag = resapplication.getString("Show_Message_Number_Column");
        if ((showMsgFlag != null) && showMsgFlag.equals(new String("true"))){
            // show the message # column (mainly for debugging and testing purpose
        }
        else{
            columnModel.getColumn(0).setMinWidth(0);
            columnModel.getColumn(0).setMaxWidth(0);
            columnModel.getColumn(0).setPreferredWidth(0);
        }

        // we don't show following columns including method, class
        // but the data is retrieved for display in the detailed pane
        columnModel.getColumn(4).setMinWidth(0);
        columnModel.getColumn(4).setMaxWidth(0);
        columnModel.getColumn(4).setPreferredWidth(0);
        columnModel.getColumn(5).setMinWidth(0);
        columnModel.getColumn(5).setMaxWidth(0);
        columnModel.getColumn(5).setPreferredWidth(0);


        return columnModel;
    }

    /**
     * Return LogTableModelFilter property value
     * @return FilteredLogTableModel
     */
    private FilteredLogTableModel getLogTableModelFilter(){
        if(logTableModelFilter != null) return logTableModelFilter;

        logTableModelFilter = new FilteredLogTableModel();
        logTableModelFilter.setRealModel(getLogTableModel());

        return logTableModelFilter;
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

        String[] cols = {"Message #", "Time", "Severity", "Message", "Class", "Method"};
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
        if(selectedRowIndexOld >= 0) {
            msgNumSelected = getMsgTable().getValueAt(selectedRowIndexOld, 0).toString();
        }

        // retrieve the new logs
        ((FilteredLogTableModel) getMsgTable().getModel()).refreshLogs(getMsgFilterLevel(), this, msgNumSelected, autoRefresh.isSelected());

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
/*

    private void detailsActionPerformed(java.awt.event.ActionEvent evt) {
        if (details.isSelected()) {
            setLogPaneDividerLocation();
            getMsgDetails().setVisible(true);
        } else {
            hideMsgDetails();
        }
    }

    private void hideMsgDetails() {

        saveLogDetailsDividerLocation();
        getLogPaneTop().setDividerLocation(1.0);
        getMsgDetails().setVisible(false);
    }
*/

    private void updateMsgFilterLevel(int newFilterLevel) {

        if (msgFilterLevel != newFilterLevel) {

            // get the selected row index
            int selectedRowIndexOld = getMsgTable().getSelectedRow();
            String msgNumSelected = null;

            // save the number of selected message
            if (selectedRowIndexOld >= 0) {
                msgNumSelected = getMsgTable().getValueAt(selectedRowIndexOld, 0).toString();
            }

            msgFilterLevel = newFilterLevel;
            ((FilteredLogTableModel) getMsgTable().getModel()).applyNewMsgFilter(newFilterLevel);

            if (msgNumSelected != null) {
                setSelectedRow(msgNumSelected);
            }

            updateMsgTotal();
        }
    }

    public void clearMsgTable(){
        ((FilteredLogTableModel)getMsgTable().getModel()).clearTable();
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

}
