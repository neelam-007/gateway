package com.l7tech.console.panels;

import com.l7tech.adminws.logging.Log;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogMessage;
import com.l7tech.console.table.LogTableModel;
import com.ibm.xml.policy.xacl.builtIn.provisional_action.log;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Vector;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.rmi.RemoteException;


/**
 * Created by IntelliJ IDEA.
 * User: fpang
 * Date: Sep 12, 2003
 * Time: 4:12:07 PM
 * To change this template use Options | File Templates.
 */
public class LogPanel extends JPanel {

     // Titles/Labels
//    private static final String LOG_LABEL = "Log";

    private static final int MAX_MESSAGE_BLOCK_SIZE = 100;
    private JPanel selectPane = null;
    private JPanel msgPane = null;
    private JPanel filterPane = null;

    private JPanel controlPane = null;
    private JSplitPane logPaneTop = null;
    private JScrollPane msgTablePane = null;
    private JTable msgTable = null;
    private JScrollPane msgDetailsPane = null;
    private JTextArea msgDetails = null;
 //   private JTabbedPane tabbedLogPane = null;
    private JSlider slider = null;
    private JCheckBox details = null;
    private AbstractTableModel logTableModel = null;


    public LogPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0)));
        setMinimumSize(new Dimension(100, 150));

        add(getMsgPane());
        add(getSelectPane());

        // the logPane is not shown default
        setVisible(false);

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

                        if (getMsgTable().getModel().getValueAt(row, 0) != null)
                            msg = msg + "Time    : " + getMsgTable().getModel().getValueAt(row, 0).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 1) != null)
                            msg = msg + "Severity: " + getMsgTable().getModel().getValueAt(row, 1).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 2) != null)
                            msg = msg + "Message : " + getMsgTable().getModel().getValueAt(row, 2).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 3) != null)
                            msg = msg + "Class   : " + getMsgTable().getModel().getValueAt(row, 3).toString() + "\n";
                        if (getMsgTable().getModel().getValueAt(row, 4) != null)
                            msg = msg + "Method  :" + getMsgTable().getModel().getValueAt(row, 4).toString() + "\n";
                        getMsgDetails().setText(msg);

                    }
                });
    }

    private JPanel getSelectPane(){
        if(selectPane == null){
            selectPane = new JPanel();
        }

        selectPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        selectPane.add(getFilterPane());
        selectPane.add(getControlPane());

        return selectPane;
    }

    private JPanel getFilterPane(){
        if(filterPane == null){
            filterPane = new JPanel();
        }

        filterPane.setLayout(new BorderLayout());
        filterPane.add(getSlider());

        return filterPane;
    }

    private JSlider getSlider(){
        if(slider == null){
            slider = new JSlider(0, 160);
        }

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

        aLabel = new JLabel("off");
        aLabel.setFont(new java.awt.Font("Dialog", 0, 11));
        table.put(new Integer(160), aLabel);

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
                            //adjustHandlerLevel(Level.ALL);
                            simpleTest("all");
                            break;
                        case 40:
                            //adjustHandlerLevel(Level.INFO);
                            simpleTest("Info");
                            break;
                        case 80:
                            //adjustHandlerLevel(Level.WARNING);
                            simpleTest("Warning");
                            break;
                        case 120:
                            //adjustHandlerLevel(Level.SEVERE);
                            simpleTest("Severe");
                            break;
                        case 160:
                            //adjustHandlerLevel(Level.OFF);
                            simpleTest("Off");
                            break;
                        default:
                            System.err.println("Unhandled value " + value);
                    }
                }
            }
        });

        return slider;
    }

    private JPanel getControlPane(){
        if(controlPane == null){
             controlPane = new JPanel();
        }

        controlPane.setLayout(new FlowLayout());

        if(details == null){
            details = new JCheckBox();
        }
        details.setFont(new java.awt.Font("Dialog", 0, 11));
        details.setText("Details");
        details.setSelected(true);
        details.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                detailsActionPerformed(evt);
            }
        });
        controlPane.add(details);

        JButton Refresh = new JButton();
        Refresh.setFont(new java.awt.Font("Dialog", 0, 11));
        Refresh.setText("Refresh");
        Refresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RefreshActionPerformed(evt);
            }
        });
        controlPane.add(Refresh);

        return controlPane;
    }

    private JTable getMsgTable(){
        if(msgTable == null){
            msgTable = new JTable(getLogTableModel(), getLogColumnModel());
        }

        msgTable.setShowHorizontalLines(false);
        msgTable.setShowVerticalLines(false);
        msgTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return msgTable;
    }

    private JScrollPane getMsgTablePane(){
        if(msgTablePane == null){
            msgTablePane = new JScrollPane();
        }
        msgTablePane.setViewportView(getMsgTable());

        return msgTablePane;
    }

    private JSplitPane getLogPaneTop(){
        if(logPaneTop == null){
            logPaneTop = new JSplitPane();
        }

        logPaneTop.setLeftComponent(getMsgTablePane());
        logPaneTop.setRightComponent(getMsgDetailsPane());
        logPaneTop.setResizeWeight(0.5);

        return logPaneTop;
    }

    private JScrollPane getMsgDetailsPane(){
        if(msgDetailsPane == null){
            msgDetailsPane = new JScrollPane();
        }
        msgDetailsPane.setViewportView(getMsgDetails());
        msgDetailsPane.setMinimumSize(new Dimension(0, 0));

        return msgDetailsPane;
    }

    private JTextArea getMsgDetails()
    {
        if(msgDetails == null){
            msgDetails = new JTextArea();
        }

        msgDetails.setEditable(false);
        msgDetails.setMinimumSize(new Dimension(0, 0));

        return msgDetails;
    }

    private JPanel getMsgPane(){
        if(msgPane == null){
            msgPane = new JPanel();
        }

        msgPane.setLayout(new BorderLayout());

        msgPane.add(getLogPaneTop(), BorderLayout.CENTER);

        return msgPane;
    }



    private DefaultTableColumnModel getLogColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        columnModel.addColumn(new TableColumn(0, 60));
        columnModel.addColumn(new TableColumn(1, 15));
        columnModel.addColumn(new TableColumn(2, 200));
        columnModel.addColumn(new TableColumn(3, 100));
        columnModel.addColumn(new TableColumn(4, 30));
        columnModel.getColumn(0).setHeaderValue(getLogTableModel().getColumnName(0));
        columnModel.getColumn(1).setHeaderValue(getLogTableModel().getColumnName(1));
        columnModel.getColumn(2).setHeaderValue(getLogTableModel().getColumnName(2));
        columnModel.getColumn(3).setHeaderValue(getLogTableModel().getColumnName(3));
        columnModel.getColumn(4).setHeaderValue(getLogTableModel().getColumnName(4));

        return columnModel;
    }

    /**
     * create the table model with log fields
     *
     * @return the <code>AbstractTableModel</code> for the
     * log pane
     */
    private AbstractTableModel getLogTableModel() {
        if (logTableModel != null) {
            return logTableModel;
        }

        String[] cols = {"Time", "Severity", "Message", "Class", "Method"};
        String[][] rows = new String[][]{};

        logTableModel = new LogTableModel(rows, cols);

        return logTableModel;
    }

    public void getLogs() {

        Log log = (Log) Locator.getDefault().lookup(Log.class);
        if (log == null) throw new IllegalStateException("cannot obtain log remote reference");

        String[] rawLogs = new String[]{};
        int numOfMsgReceived = 0;
        boolean cleanUp = true;
        do {
            try {
                rawLogs = log.getSystemLog(numOfMsgReceived, MAX_MESSAGE_BLOCK_SIZE);

                numOfMsgReceived += rawLogs.length;

                if (cleanUp) {
                    while (getMsgTable().getRowCount() > 0) {
                        //System.out.println("Number of row left is: " + msgTable.getRowCount() + "\n");
                        ((DefaultTableModel) (getMsgTable().getModel())).removeRow(0);
                    }
                    cleanUp = false;
                }
                //           ((DefaultTableModel)msgTable.getModel()).rowsRemoved(new TableModelEvent(msgTable.getModel()));
                for (int i = 0; i < rawLogs.length; i++) {
                    //msgTable.setValueAt(rawLogs[i], i, 0);
                    Vector newRow = new Vector();

                    LogMessage logMsg = new LogMessage(rawLogs[i]);
                    newRow.add(logMsg.getTime());
                    newRow.add(logMsg.getSeverity());
                    newRow.add(logMsg.getMessageDetail());
                    newRow.add(logMsg.getMessageClass());
                    newRow.add(logMsg.getMessageMethod());
                    ((DefaultTableModel) getMsgTable().getModel()).addRow(newRow);
                    //System.out.println("adding a new row (" + i + ") .....\n");
                    //System.out.println(rawLogs[i]);
                }

                ((AbstractTableModel) (getMsgTable().getModel())).fireTableDataChanged();

            } catch (RemoteException e) {
                System.err.println("Unable to retrieve logs from server");
            }
        } while (rawLogs.length == MAX_MESSAGE_BLOCK_SIZE);    // may be more messages for retrieval
    }

    private void RefreshActionPerformed(java.awt.event.ActionEvent evt) {
        getLogs();
    }

    private void detailsActionPerformed(java.awt.event.ActionEvent evt) {
        if (details.isSelected()) {
            getLogPaneTop().setDividerLocation(0.7);
            getMsgDetails().setVisible(true);
        } else {
            hideMsgDetails();
        }
    }

    private void allActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void infoActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void warningActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void severeActionPerformed(java.awt.event.ActionEvent evt) {
        // Add your handling code here:
    }

    private void hideMsgDetails() {

        getLogPaneTop().setDividerLocation(getLogPaneTop().getMaximumDividerLocation());
        getMsgDetails().setVisible(false);
    }

    private void simpleTest(String severity) {

        System.out.println("Severity is: " + severity);
    }

/*
     private JTabbedPane getLogTabbedPane() {
        // If tabbed pane not already created
        if (tabbedLogPane == null) {
            // Create tabbed pane
            tabbedLogPane = new JTabbedPane();

            // Add all tabs
            tabbedLogPane.add(getLogPaneTop(), LOG_LABEL);
        }

        // Return tabbed pane
        return tabbedLogPane;
     }
 */

    /*
        private Action getLogRefreshAction() {
           if (logRefreshAction != null) return logRefreshAction;
           String atext = resapplication.getString("Refresh_MenuItem_text");
           Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Refresh16.gif"));
           logRefreshAction =
             new AbstractAction(atext, icon) {

                 public void actionPerformed(ActionEvent event) {
                     // todo: retrieve the logs from ssg
                 }
             };
           logRefreshAction.setEnabled(false);
           logRefreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
           return logRefreshAction;
       }

   */
}
