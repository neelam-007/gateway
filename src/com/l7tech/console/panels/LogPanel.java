package com.l7tech.console.panels;

import com.l7tech.common.audit.*;
import com.l7tech.common.gui.widgets.ContextMenuTextArea;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.RequestId;
import com.l7tech.console.table.AssociatedLogsTable;
import com.l7tech.console.table.FilteredLogTableSorter;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.FileDropTransferHandler;
import com.l7tech.logging.LogMessage;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.logging.GenericLogAdmin;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.BadLocationException;
import javax.swing.text.DateFormatter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.OutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.beans.XMLEncoder;
import java.net.URL;
import java.net.URI;

import net.sf.nachocalendar.CalendarFactory;
import net.sf.nachocalendar.components.DateField;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.XStream;

/*
 * This class creates a log panel.
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(LogPanel.class.getName());

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
    public static final int LOG_SERVICE_COLUMN_INDEX = 9;

    public static final String MSG_TOTAL_PREFIX = "Total: ";

    private static final int LOG_REFRESH_TIMER = 3000;
    private int logsRefreshInterval;
    private javax.swing.Timer logsRefreshTimer;

    private static final byte[] FILE_TYPE = new byte[]{0xC, 0xA, 0xF, 0xE, 0xD, 0x0, 0x0, 0xD};

    private static final long MILLIS_IN_HOUR = 1000L * 60L * 60L;
    private static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24L;
    private static final long MILLIS_IN_WEEK = MILLIS_IN_DAY * 7L;
    private static final long[] UNIT_FACTOR = {MILLIS_IN_HOUR, MILLIS_IN_DAY, MILLIS_IN_WEEK};

    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private int msgFilterLevel = MSG_FILTER_LEVEL_WARNING;
    private String msgFilterService = "";
    private String msgFilterMessage = "";
    private boolean isAuditType;
    private String nodeId;
    private JPanel selectPane;
    private JPanel filterPane;
    private JPanel controlPane;
    private JPanel msgTablePane;
    private JPanel statusPane;
    private JTable msgTable;
    private JTabbedPane msgDetailsPane;
    private JScrollPane associatedLogsScrollPane;
    private JTextArea msgDetails;
    private JSlider slider;
    private JCheckBox autoRefresh;
    private DefaultTableModel logTableModel;
    private FilteredLogTableSorter logTableSorter;
    private JLabel msgTotal;
    private JLabel lastUpdateTimeLabel;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);
    private JScrollPane detailsScrollPane;
    private LogMessage displayedLogMessage;
    private AssociatedLogsTable associatedLogsTable;
    private JScrollPane requestXmlScrollPane;
    private JTextArea requestXmlTextArea;
    private JPanel requestXmlPanel;
    private JCheckBox requestReformatCheckbox;
    private JPanel responseXmlPanel;
    private JScrollPane responseXmlScrollPane;
    private JCheckBox responseReformatCheckbox;
    private JTextArea responseXmlTextArea;
    private String unformattedRequestXml;
    private String unformattedResponseXml;
    private DateField startDateField;
    private JRadioButton viewCurrentRadioButton;
    private JRadioButton viewHistoricRadioButton;
    private JComboBox startDateComboBox;
    private JPanel startDatePane;
    private JPanel viewHistoricPane;
    private JPanel viewCurrentPane;
    private JSpinner rangeSpinner;
    private JButton updateSelectionButton;
    private JComboBox limitUnitComboBox;
    private boolean connected = false;

    /**
     * Constructor
     */
    public LogPanel() {
        this(true, true, null);
    }

    /**
     * Constructor
     */
    public LogPanel(boolean includeDetailPane, final boolean isAuditType, String nodeId) {
        setLayout(new BorderLayout());
        init();

        this.logsRefreshInterval = LOG_REFRESH_TIMER;
        this.nodeId = nodeId;
        this.isAuditType = isAuditType;

        if(includeDetailPane) {
            JSplitPane logSplitPane = new JSplitPane();
            logSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
            logSplitPane.setTopComponent(getMsgTablePane());
            logSplitPane.setBottomComponent(getMsgDetailsPane());
            logSplitPane.setDividerLocation(0.5);
            add(logSplitPane, BorderLayout.CENTER);
        }
        else {
            add(getMsgTablePane(), BorderLayout.CENTER);
        }

        add(getSelectPane(), BorderLayout.SOUTH);

        getMsgTable().getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        updateMsgDetails();
                    }
                });

        // create import transfer handler
        TransferHandler fdth = getFileDropTransferHandler();
        getMsgTable().setTransferHandler(fdth);
        getMsgTable().getTableHeader().setTransferHandler(fdth);
    }

    private void init() {
        ButtonGroup viewButtonGroup = new ButtonGroup();
        viewButtonGroup.add(viewCurrentRadioButton);
        viewButtonGroup.add(viewHistoricRadioButton);

        viewCurrentRadioButton.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                if(viewCurrentRadioButton.isSelected()) {
                    Utilities.setEnabled(viewCurrentPane, true);
                    Utilities.setEnabled(autoRefresh, true); // for logs this is not in the panel
                    Utilities.setEnabled(viewHistoricPane, false);
                }
                else {
                    Utilities.setEnabled(viewCurrentPane, false);
                    Utilities.setEnabled(autoRefresh, false);
                    Utilities.setEnabled(viewHistoricPane, true);
                }
                updateLogAutoRefresh();
                if(viewCurrentRadioButton.isSelected()) refreshLogs();
            }
        });

        Utilities.setFont(controlPane, new java.awt.Font("Dialog", 0, 11));

        autoRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateLogAutoRefresh();
            }
        });

        limitUnitComboBox.setModel(new DefaultComboBoxModel(new String[]{"hrs.", "days", "wks."}));

        startDatePane.setLayout(new BorderLayout());
        startDateField = CalendarFactory.createDateField();
        startDatePane.add(startDateField, BorderLayout.CENTER);

        startDateComboBox.setModel(new DefaultComboBoxModel(
                new String[]{"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"}));

        rangeSpinner.setValue(new Integer(24));

        updateSelectionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateViewSelection();
            }
        });

        // select after adding date field to ensure correct initial enabled state
        viewCurrentRadioButton.setSelected(true);
    }

    public boolean isAutoRefresh() {
        Window pWin = SwingUtilities.getWindowAncestor(this);
        return pWin!=null && pWin.isVisible()
                && autoRefresh.isEnabled()
                && autoRefresh.isSelected();
    }

    private void updateLogAutoRefresh() {
        if (isAutoRefresh()) {
            getLogsRefreshTimer().start();
        } else {
            getLogsRefreshTimer().stop();
        }
    }

    private void updateViewSelection() {

        // get selection dates
        Date selectedDate = (Date) startDateField.getValue();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, startDateComboBox.getSelectedIndex() * 3);

        long range = ((Integer)rangeSpinner.getValue()).intValue();

        Date startDate = null;
        Date endDate = null;

        if(range>=0) {
            startDate = calendar.getTime();
            endDate = new Date(startDate.getTime() + (range * UNIT_FACTOR[limitUnitComboBox.getSelectedIndex()]));
        }
        else {
            endDate = calendar.getTime();
            startDate = new Date(endDate.getTime() + (range * UNIT_FACTOR[limitUnitComboBox.getSelectedIndex()]));
        }

        refreshLogs(startDate, endDate);
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

                boolean reqXmlVisible = false;
                boolean respXmlVisible = false;
                String reqXmlDisplayed = "";
                String respXmlDisplayed = "";
                if (arec instanceof AdminAuditRecord) {
                    AdminAuditRecord aarec = (AdminAuditRecord)arec;
                    msg += "Event Type : Administrator Action" + "\n";
                    msg += "Admin user : " + aarec.getUserName() + "\n";
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
                    msg += "Operation  : " + sum.getOperationName() + "\n";
                    msg += "Rqst Length: " + fixNegative(sum.getRequestContentLength(), "<Not Saved>") + "\n";
                    msg += "Resp Length: " + fixNegative(sum.getResponseContentLength(), "<Not Saved>") + "\n";
                    msg += "Resp Status: " + sum.getResponseHttpStatus() + "\n";
                    msg += "Resp Time  : " + sum.getRoutingLatency() + "ms\n";
                    msg += "User ID    : " + sum.getUserId() + "\n";
                    msg += "User Name  : " + sum.getUserName() + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";

                    if (sum.getRequestXml() != null) {
                        reqXmlVisible = true;
                        reqXmlDisplayed = sum.getRequestXml();
                    }

                    if (sum.getResponseXml() != null) {
                        respXmlVisible = true;
                        respXmlDisplayed = sum.getResponseXml();
                    }
                } else if (arec instanceof SystemAuditRecord) {
                    SystemAuditRecord sys = (SystemAuditRecord)arec;
                    msg += "Event Type : System Message" + "\n";
                    if(arec.getUserId()==null) {
                        msg += "Node IP    : " + arec.getIpAddress() + "\n";
                    }
                    else {
                        msg += "Client IP  : " + arec.getIpAddress() + "\n";
                    }
                    msg += "Action     : " + sys.getAction() + "\n";
                    msg += "Component  : " + fixComponent(sys.getComponentId()) + "\n";
                    if(arec.getUserId()!=null) {
                        msg += "User ID    : " + arec.getUserId() + "\n";
                        msg += "User Name  : " + arec.getUserName() + "\n";
                    }
                    msg += "Entity name: " + arec.getName() + "\n";
                } else {
                    msg += "Event Type : Unknown" + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                    msg += "IP Address : " + arec.getIpAddress() + "\n";
                }

                unformattedRequestXml = reqXmlDisplayed;
                if (reqXmlVisible && reqXmlDisplayed != null && reqXmlDisplayed.length() > 0 &&
                            getRequestReformatCheckbox().isSelected()) {
                    reqXmlDisplayed = reformat(reqXmlDisplayed);
                }

                unformattedResponseXml = respXmlDisplayed;
                if (respXmlVisible && respXmlDisplayed != null && respXmlDisplayed.length() > 0 &&
                            getResponseReformatCheckbox().isSelected()) {
                    respXmlDisplayed = reformat(respXmlDisplayed);
                }

                JTextArea requestXmlTextArea = getRequestXmlTextArea();
                requestXmlTextArea.setText(reqXmlDisplayed);
                requestXmlTextArea.getCaret().setDot(0);
                requestXmlTextArea.setEnabled(reqXmlVisible);
                getRequestReformatCheckbox().setEnabled(reqXmlVisible);
                getRequestXmlScrollPane().setEnabled(reqXmlVisible);
                getMsgDetailsPane().setEnabledAt(2, reqXmlVisible);

                JTextArea responseXmlTextArea = getResponseXmlTextArea();
                responseXmlTextArea.setText(respXmlDisplayed);
                responseXmlTextArea.getCaret().setDot(0);
                responseXmlTextArea.setEnabled(respXmlVisible);
                getResponseReformatCheckbox().setEnabled(respXmlVisible);
                getResponseXmlScrollPane().setEnabled(respXmlVisible);
                getMsgDetailsPane().setEnabledAt(3, respXmlVisible);

                // clear the associated log table
                getAssociatedLogsTable().getTableSorter().clear();

                // populate the associated logs
                Iterator associatedLogsItr = arec.getDetails().iterator();

                Vector associatedLogs = new Vector();
                while(associatedLogsItr.hasNext()) {
                    AuditDetail ad = (AuditDetail) associatedLogsItr.next();


                    int id = ad.getMessageId();
                    // TODO get the CellRenderer to display the user messages differently when id < 0 (add field to AssociatedLog class?)
                    String associatedLogMessage = Messages.getMessageById(id);
                    String associatedLogLevel = Messages.getSeverityLevelNameById(id);

                    StringBuffer result = new StringBuffer();
                    if (associatedLogMessage != null) {
                        MessageFormat mf = new MessageFormat(associatedLogMessage);
                        mf.format(ad.getParams(), result, new FieldPosition(0));
                    }
                    AssociatedLog al = new AssociatedLog(ad.getTime(), associatedLogLevel, result.toString(), ad.getOrdinal());
                    associatedLogs.add(al);
                }
                getAssociatedLogsTable().getTableSorter().setData(associatedLogs);
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

    private String reformat(String xml) {
        try {
            xml = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(xml));
        } catch (Exception e) {
            xml = "Can't reformat XML: " + e.toString();
        }
        return xml;
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

    private String fixComponent(int componentId) {
        com.l7tech.common.Component c = com.l7tech.common.Component.fromId(componentId);
        if (c == null) return "Unknown Component #" + componentId;
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
            case AdminAuditRecord.ACTION_LOGIN:
                return "Admin Login";
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

    public String getMsgFilterService() {
        return msgFilterService;
    }

    public String getMsgFilterMessage() {
        return msgFilterMessage;
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
        connected = true;
        getFilteredLogTableSorter().onConnect();
        updateLogsRefreshTimerDelay();
        clearMsgTable();
        getLogsRefreshTimer().start();

        Utilities.setEnabled(viewCurrentRadioButton, true);
        Utilities.setEnabled(viewHistoricRadioButton, true);
        Utilities.setEnabled(updateSelectionButton, true);
    }

    /**
     * Performs the necessary cleanup when the connection with the cluster went down.
     */
    public void onDisconnect(){
        connected = false;

        logTableSorter.clearLogCache();
        getLogsRefreshTimer().stop();
        getFilteredLogTableSorter().onDisconnect();

        if(!getLastUpdateTimeLabel().getText().trim().endsWith("[Disconnected]")) {
            getLastUpdateTimeLabel().setText(getLastUpdateTimeLabel().getText().trim() + " [Disconnected]   ");
        }

        Utilities.setEnabled(viewCurrentRadioButton, false);
        Utilities.setEnabled(viewHistoricRadioButton, false);
        Utilities.setEnabled(updateSelectionButton, false);
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
        leftPane.add(new JPanel());

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
        if(isAuditType) filterPane.add(getFilterServicePane());
        filterPane.add(getFilterMessagePane());
        if(!isAuditType) filterPane.add(getMicroControlPane());

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

    private JPanel getFilterServicePane() {
        JPanel filterServicePane = new JPanel();

        JTextField serviceTextField = new JTextField(8);
        serviceTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        serviceTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e) {
                updateMsgFilterService(e.getDocument());
            }
            public void removeUpdate(DocumentEvent e) {
                updateMsgFilterService(e.getDocument());
            }
            public void changedUpdate(DocumentEvent e) {
                updateMsgFilterService(e.getDocument());
            }
        });

        JLabel label = new JLabel("Service:");
        label.setFont(new java.awt.Font("Dialog", 0, 11));

        filterServicePane.setLayout(new BorderLayout());
        filterServicePane.add(label, BorderLayout.NORTH);
        filterServicePane.add(serviceTextField, BorderLayout.CENTER);

        return filterServicePane;
    }

    private JPanel getFilterMessagePane() {
        JPanel filterMessagePane = new JPanel();

        JTextField messageTextField = new JTextField(8);
        messageTextField.setFont(new java.awt.Font("Dialog", 0, 11));
        messageTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e) {
                updateMsgFilterMessage(e.getDocument());
            }
            public void removeUpdate(DocumentEvent e) {
                updateMsgFilterMessage(e.getDocument());
            }
            public void changedUpdate(DocumentEvent e) {
                updateMsgFilterMessage(e.getDocument());
            }
        });

        JLabel label = new JLabel("Message:");
        label.setFont(new java.awt.Font("Dialog", 0, 11));

        filterMessagePane.setLayout(new BorderLayout());
        filterMessagePane.add(label, BorderLayout.NORTH);
        filterMessagePane.add(messageTextField, BorderLayout.CENTER);

        return filterMessagePane;
    }


    /**
     * Get the small control panel used for log viewing
     *
     * @return
     */
    private JPanel getMicroControlPane() {
        JPanel microControlPane = new JPanel();
        microControlPane.setLayout(new FlowLayout());

        autoRefresh.setSelected(true);
        microControlPane.add(autoRefresh);

        return microControlPane;
    }

    /**
     * Return ControlPane property value
     * @return  JPanel
     */
    private JPanel getControlPane(){
        return controlPane;
    }

    /**
     * Return the total number of the messages being displayed.
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
    private JComponent getMsgTablePane(){
        if(msgTablePane != null) return msgTablePane;

        msgTablePane = new JPanel();
        msgTablePane.setLayout(new BorderLayout());
        if(isAuditType) msgTablePane.add(getControlPane(), BorderLayout.NORTH);

        JScrollPane tablePane = new JScrollPane();
        tablePane.setViewportView(getMsgTable());
        tablePane.getViewport().setBackground(getMsgTable().getBackground());
        tablePane.setMinimumSize(new Dimension(1000, 200));
        tablePane.setPreferredSize(new Dimension(1000, 300));
        msgTablePane.add(tablePane, BorderLayout.CENTER);

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

        JScrollPane associatedLogsScrollPane = getAssociatedLogsScrollPane();
        msgDetailsPane.addTab("Associated Logs", associatedLogsScrollPane);

        msgDetailsPane.addTab("Request XML", getRequestXmlPanel());

        msgDetailsPane.addTab("Response XML", getResponseXmlPanel());

        return msgDetailsPane;
    }

    private JScrollPane getAssociatedLogsScrollPane() {
        if (associatedLogsScrollPane != null) return associatedLogsScrollPane;
        associatedLogsScrollPane = new JScrollPane();
        associatedLogsScrollPane.setViewportView(getAssociatedLogsTable());
        associatedLogsScrollPane.getViewport().setBackground(getAssociatedLogsTable().getBackground());

        return associatedLogsScrollPane;
    }

    private AssociatedLogsTable getAssociatedLogsTable() {
        if(associatedLogsTable != null) return associatedLogsTable;

        associatedLogsTable = new AssociatedLogsTable();

        associatedLogsTable.setShowHorizontalLines(false);
        associatedLogsTable.setShowVerticalLines(false);
        associatedLogsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        associatedLogsTable.setRowSelectionAllowed(true);

        return associatedLogsTable;
    }

    private JScrollPane getDetailsScrollPane() {
        if (detailsScrollPane != null) return detailsScrollPane;
        detailsScrollPane = new JScrollPane();
        detailsScrollPane.setViewportView(getMsgDetails());
        return detailsScrollPane;
    }

    private JScrollPane getRequestXmlScrollPane() {
        if (requestXmlScrollPane != null) return requestXmlScrollPane;
        requestXmlScrollPane = new JScrollPane();
        requestXmlScrollPane.setViewportView(getRequestXmlTextArea());
        return requestXmlScrollPane;
    }

    private JScrollPane getResponseXmlScrollPane() {
        if (responseXmlScrollPane != null) return responseXmlScrollPane;
        responseXmlScrollPane = new JScrollPane();
        responseXmlScrollPane.setViewportView(getResponseXmlTextArea());
        return responseXmlScrollPane;
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

    private JPanel getRequestXmlPanel() {
        if (requestXmlPanel != null) return requestXmlPanel;
        requestXmlPanel = new JPanel();
        requestXmlPanel.setLayout(new BorderLayout());
        requestXmlPanel.add(getRequestXmlScrollPane(), BorderLayout.CENTER);
        requestXmlPanel.add(getRequestReformatCheckbox(), BorderLayout.SOUTH);
        return requestXmlPanel;
    }

    private JCheckBox getRequestReformatCheckbox() {
        if (requestReformatCheckbox != null) return requestReformatCheckbox;
        requestReformatCheckbox = new JCheckBox("Reformat Request XML");
        requestReformatCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextArea requestTextArea = getRequestXmlTextArea();
                String text = requestTextArea.getText();
                if (requestTextArea.isEnabled() && requestTextArea.isShowing() &&
                            text != null && text.length() > 0) {
                    if (requestReformatCheckbox.isSelected()) {
                        unformattedRequestXml = text;
                        requestTextArea.setText(reformat(unformattedRequestXml));
                    } else {
                        requestTextArea.setText(unformattedRequestXml);
                    }
                    requestTextArea.getCaret().setDot(0);
                }
            }
        });
        return requestReformatCheckbox;
    }

    private JTextArea getRequestXmlTextArea() {
        if (requestXmlTextArea != null) return requestXmlTextArea;
        requestXmlTextArea = new ContextMenuTextArea();
        requestXmlTextArea.setEditable(false);
        return requestXmlTextArea;
    }

    private JPanel getResponseXmlPanel() {
        if (responseXmlPanel != null) return responseXmlPanel;
        responseXmlPanel = new JPanel();
        responseXmlPanel.setLayout(new BorderLayout());
        responseXmlPanel.add(getResponseXmlScrollPane(), BorderLayout.CENTER);
        responseXmlPanel.add(getResponseReformatCheckbox(), BorderLayout.SOUTH);
        return responseXmlPanel;
    }

    private JCheckBox getResponseReformatCheckbox() {
        if (responseReformatCheckbox != null) return responseReformatCheckbox;
        responseReformatCheckbox = new JCheckBox("Reformat Response XML");
        responseReformatCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JTextArea responseTextArea = getResponseXmlTextArea();
                String text = responseTextArea.getText();
                if (responseTextArea.isEnabled() && responseTextArea.isShowing() &&
                            text != null && text.length() > 0) {
                    if (responseReformatCheckbox.isSelected()) {
                        unformattedResponseXml = text;
                        responseTextArea.setText(reformat(unformattedResponseXml));
                    } else {
                        responseTextArea.setText(unformattedResponseXml);
                    }
                    responseTextArea.getCaret().setDot(0);
                }
            }
        });
        return responseReformatCheckbox;
    }

    private JTextArea getResponseXmlTextArea() {
        if (responseXmlTextArea != null) return responseXmlTextArea;
        responseXmlTextArea = new ContextMenuTextArea();
        responseXmlTextArea.setEditable(false);
        return responseXmlTextArea;
    }

    /**
     * Return statusPane property value
     * @return  JPanel
     */
    private JPanel getStatusPane() {
        if(statusPane != null)  return statusPane;

        statusPane = new JPanel();
        statusPane.setLayout(new FlowLayout());
        statusPane.add(getMsgTotal());
        JPanel spacer = new JPanel();
        spacer.setSize(40,20);
        statusPane.add(spacer);
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

        // Add columns according to configuration
        String showMsgFlag = resapplication.getString("Show_Message_Number_Column");
        if ((showMsgFlag != null) && showMsgFlag.equals("true")){
            columnModel.addColumn(new TableColumn(LOG_MSG_NUMBER_COLUMN_INDEX, 20));
        }
        columnModel.addColumn(new TableColumn(LOG_NODE_NAME_COLUMN_INDEX, 30));
        columnModel.addColumn(new TableColumn(LOG_TIMESTAMP_COLUMN_INDEX, 80));
        columnModel.addColumn(new TableColumn(LOG_SEVERITY_COLUMN_INDEX, 15));
        if(isAuditType) { // show the service name if we are displaying audit messages
            columnModel.addColumn(new TableColumn(LOG_SERVICE_COLUMN_INDEX, 20));
        }
        columnModel.addColumn(new TableColumn(LOG_MSG_DETAILS_COLUMN_INDEX, 400));

        // Set headers
        for(int i = 0; i < columnModel.getColumnCount(); i++){
            TableColumn tc = columnModel.getColumn(i);
            tc.setHeaderRenderer(iconHeaderRenderer);
            tc.setHeaderValue(getLogTableModel().getColumnName(tc.getModelIndex()));
        }

        return columnModel;
    }

    /**
     * Return LogTableModelFilter property value
     * @return FilteredLogTableModel
     */
    private FilteredLogTableSorter getFilteredLogTableSorter(){
        if(logTableSorter != null) return logTableSorter;

        logTableSorter = new FilteredLogTableSorter(this, getLogTableModel(),
                isAuditType ? GenericLogAdmin.TYPE_AUDIT : GenericLogAdmin.TYPE_LOG);

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

        String[] cols = {"Message #", "Node", "Time", "Severity", "Message", "Class", "Method", "Request Id", "Node Id", "Service"};
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
            Object nodeId = getMsgTable().getModel().getValueAt(selectedRowIndexOld, LOG_NODE_ID_COLUMN_INDEX);
            Object mesNum = getMsgTable().getModel().getValueAt(selectedRowIndexOld, LOG_MSG_NUMBER_COLUMN_INDEX);

            if(nodeId!=null && mesNum!=null) {
                msgNumSelected =
                        nodeId.toString().trim() +
                        mesNum.toString().trim();
            }
        }

        return msgNumSelected;
    }

    /**
     * Performs the log retrieval. This function is called when the refresh timer is expired.
     */
    public void refreshLogs() {
        getLogsRefreshTimer().stop();

        // retrieve the new logs
        Window window = SwingUtilities.getWindowAncestor(this);
        if(window!=null && window.isVisible())
            ((FilteredLogTableSorter) getMsgTable().getModel()).refreshLogs(this, isAutoRefresh(), nodeId);
    }

    /**
     * Performs the log retrieval. This function is called when the refresh timer is expired.
     */
    public void refreshLogs(Date first, Date last) {
        getLogsRefreshTimer().stop();

        // retrieve the new logs
        Window window = SwingUtilities.getWindowAncestor(this);
        if(window!=null && window.isVisible())
            ((FilteredLogTableSorter) getMsgTable().getModel()).refreshLogs(this, first, last, nodeId);
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
                Object nodeId = getMsgTable().getModel().getValueAt(i, LOG_NODE_ID_COLUMN_INDEX);
                Object mesNum = getMsgTable().getModel().getValueAt(i, LOG_MSG_NUMBER_COLUMN_INDEX);

                if(nodeId!=null && mesNum!=null) {
                    String selctedMsgNum = nodeId.toString().trim() + mesNum.toString().trim();

                    if (selctedMsgNum.equals(msgNumber)) {
                        getMsgTable().setRowSelectionInterval(i, i);

                        rowFound = true;
                        break;
                    }
                }
            }

            if (!rowFound) {
                // clear the details text area
                getMsgDetails().setText("");
                getAssociatedLogsTable().getTableSorter().clear();
                displayedLogMessage = null;
            }
        }
    }

    private void updateMsgFilter() {
        // get the selected row index
        int selectedRowIndexOld = getMsgTable().getSelectedRow();
        String msgNumSelected = null;

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            msgNumSelected = getMsgTable().getModel().getValueAt(selectedRowIndexOld, LOG_MSG_NUMBER_COLUMN_INDEX).toString();
        }

        ((FilteredLogTableSorter) getMsgTable().getModel()).applyNewMsgFilter(msgFilterLevel, msgFilterService, msgFilterMessage);

        if (msgNumSelected != null) {
            setSelectedRow(msgNumSelected);
        }

        updateMsgTotal();
    }

    /**
     * Update the filter level.
     *
     * @param newFilterLevel  The new filter level to be applied.
     */
    private void updateMsgFilterLevel(int newFilterLevel) {
        if (msgFilterLevel != newFilterLevel) {
            msgFilterLevel = newFilterLevel;
            updateMsgFilter();
        }
    }

    private void updateMsgFilterService(Document document) {
        updateMsgFilterService(toString(document));
    }

    private void updateMsgFilterService(String serviceMatch) {
        if (serviceMatch!=null && !serviceMatch.equals(msgFilterService)) {
            msgFilterService = serviceMatch;
            updateMsgFilter();
        }
    }

    private void updateMsgFilterMessage(Document document) {
        updateMsgFilterMessage(toString(document));
    }

    private void updateMsgFilterMessage(String messageMatch) {
        if (messageMatch!=null && !messageMatch.equals(msgFilterMessage)) {
            msgFilterMessage = messageMatch;
            updateMsgFilter();
        }
    }

    private String toString(Document document) {
        String text = null;

        if(document!=null) {
            try {
                text = document.getText(0, document.getLength()).trim();
            }
            catch(BadLocationException ble) {
                text = "";
            }
        }

        return text;
    }

    private void updateLogsRefreshTimerDelay() {
        int refreshSeconds = getFilteredLogTableSorter().getDelay();

        if(refreshSeconds >= 0 && refreshSeconds < 300) logsRefreshInterval = 1000 * refreshSeconds;
        else logsRefreshInterval = LOG_REFRESH_TIMER;

        if(logsRefreshInterval==0) logsRefreshInterval = Integer.MAX_VALUE; // disable refresh

        getLogsRefreshTimer().setInitialDelay(logsRefreshInterval);
        getLogsRefreshTimer().setDelay(logsRefreshInterval);
    }

    private TransferHandler getFileDropTransferHandler() {
        return new FileDropTransferHandler(new FileDropTransferHandler.FileDropListener(){
            public boolean acceptFiles(File[] files) {
                boolean accepted = false;

                if(files.length==1) {
                    try {
                        importView(files[0]);
                        accepted = true;
                    }
                    catch(Exception e) {
                        logger.log(Level.WARNING, "Unable to import data", e);
                    }
                }
                else {
                    logger.info("Multiples file not supported.");
                }

                return accepted;
            }

            public boolean isDropEnabled() {
                return !connected;
            }
        }, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                return name!=null &&
                        ((isAuditType && name.endsWith(".ssga")) || (!isAuditType && name.endsWith(".ssgl")));
            }
        });
    }

    /**
     *  Clear the message table
     */
    public void clearMsgTable(){
        getMsgDetails().setText("Loading data...");
        getMsgTotal().setText(MSG_TOTAL_PREFIX + "0");
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
        logsRefreshTimer = new javax.swing.Timer(logsRefreshInterval, new ActionListener() {
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
         getMsgTotal().setText(MSG_TOTAL_PREFIX + msgTable.getRowCount());
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

            if (getFilteredLogTableSorter().getSortedColumn() == table.convertColumnIndexToModel(column)) {

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

                int viewColumn = tableView.columnAtPoint(e.getPoint());
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

    /**
     * Export the currently displayed log/audit data.
     *
     * @param out the stream to output to.
     */
    public void exportView(File file) throws IOException {
        // process
        JTable table = getMsgTable();
        int rows = table.getRowCount();
        List data = new ArrayList(rows);
        for(int i=0; i<rows; i++) {
            LogMessage rowMessage = logTableSorter.getLogMessageAtRow(i);
            data.add(new WriteableLogMessage(rowMessage));
        }
        Collections.sort(data);

        // write
        XStream xstream = new XStream(new DomDriver());
        ObjectOutputStream oos = null;
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(FILE_TYPE);
            oos = xstream.createObjectOutputStream(new OutputStreamWriter(new GZIPOutputStream(out), "UTF-8"));
            oos.writeObject(data);
        }
        finally {
            // necessary to get the closing object tag
            ResourceUtils.closeQuietly(oos);
            ResourceUtils.closeQuietly(out);
        }
    }

    public void importView(File file) throws IOException {
        viewHistoricRadioButton.setSelected(true);

        XStream xstream = new XStream(new DomDriver());
        InputStream in = null;
        ObjectInputStream ois = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            byte[] header = HexUtils.slurpStream(in, FILE_TYPE.length);
            if(header.length < FILE_TYPE.length ||
               !HexUtils.compareArrays(FILE_TYPE, 0, header, 0, FILE_TYPE.length)) {
                throw new IOException("Unknown file type.");
            }
            ois = xstream.createObjectInputStream(new InputStreamReader(new GZIPInputStream(in), "UTF-8"));
            Object read = ois.readObject();
            if(read instanceof List) {
                List data = (List) read;
                if(data.isEmpty()) {
                    logger.info("No data in file! '"+file.getAbsolutePath()+"'.");
                }
                Hashtable loadedLogs = new Hashtable();
                for (Iterator iterator = data.iterator(); iterator.hasNext();) {
                    Object o = iterator.next();
                    if(o instanceof WriteableLogMessage) {
                        WriteableLogMessage message = (WriteableLogMessage) o;
                        Vector logsForNode = (Vector) loadedLogs.get(message.ssgLogRecord.getNodeId());
                        if(logsForNode==null) {
                            logsForNode = new Vector();
                            loadedLogs.put(message.ssgLogRecord.getNodeId(), logsForNode);
                        }
                        LogMessage lm = new LogMessage(message.ssgLogRecord);
                        lm.setNodeName(message.nodeName);
                        logsForNode.add(lm);
                    }
                }
                logTableSorter.setLogs(this, loadedLogs);
            }
            else {
                logger.warning("File '"+file.getAbsolutePath()+"' contains invalid data! '"+
                        (read==null ? "null" : read.getClass().getName())+"'.");
            }
        }
        catch(ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Error reading data file '"+file.getAbsolutePath()+"'.", cnfe);
        }
        finally {
            ResourceUtils.closeQuietly(ois);
            ResourceUtils.closeQuietly(in);
        }
    }

    public static class WriteableLogMessage implements Comparable {
        private String nodeName;
        private SSGLogRecord ssgLogRecord;

        public WriteableLogMessage(LogMessage lm) {
            nodeName = lm.getNodeName();
            ssgLogRecord = lm.getSSGLogRecord();
        }

        public int compareTo(Object o) {
            return new Long(ssgLogRecord.getMillis()).compareTo(new Long(((WriteableLogMessage)o).ssgLogRecord.getMillis()));
        }
    }
}
