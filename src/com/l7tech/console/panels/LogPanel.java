package com.l7tech.console.panels;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.common.BuildInfo;
import static com.l7tech.common.Component.fromId;
import com.l7tech.common.audit.*;
import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.JTableColumnResizeMouseListener;
import com.l7tech.common.gui.util.RunOnChangeListener;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.widgets.ContextMenuTextArea;
import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.table.AssociatedLogsTable;
import com.l7tech.console.table.FilteredLogTableSorter;
import com.l7tech.console.util.ArrowIcon;
import com.l7tech.console.util.Registry;
import com.l7tech.logging.GenericLogAdmin;
import com.l7tech.logging.LogMessage;
import com.l7tech.logging.SSGLogRecord;
import com.l7tech.objectmodel.FindException;
import net.sf.nachocalendar.CalendarFactory;
import net.sf.nachocalendar.components.DateField;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.io.*;
import java.rmi.RemoteException;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
    public static final int LOG_THREAD_COLUMN_INDEX = 10;

    public static final String MSG_TOTAL_PREFIX = "Total: ";

    private static final int LOG_REFRESH_TIMER = 3000;
    private int logsRefreshInterval;
    private javax.swing.Timer logsRefreshTimer;

    private static final byte[] FILE_TYPE = new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xD0, (byte)0x0D};

    private static final long MILLIS_IN_HOUR = 1000L * 60L * 60L;
    private static final long MILLIS_IN_DAY = MILLIS_IN_HOUR * 24L;
    private static final long MILLIS_IN_WEEK = MILLIS_IN_DAY * 7L;
    private static final long[] UNIT_FACTOR = {MILLIS_IN_HOUR, MILLIS_IN_DAY, MILLIS_IN_WEEK};

    private static ResourceBundle resapplication = java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private LogPanelBoundComponents lpbc = new LogPanelBoundComponents();
    private int[] tableColumnWidths = new int[20];
    private int msgFilterLevel = MSG_FILTER_LEVEL_WARNING;
    private String msgFilterNode = "";
    private String msgFilterService = "";
    private String msgFilterThreadId = "";
    private String msgFilterMessage = "";
    private boolean isAuditType;
    private String nodeId;
    private JPanel selectPane;
    private JPanel filterPane;
    private JLabel filterLabel;
    private final JTextField filterMessageTextField = new JTextField(16);
    private final JTextField filterNodeTextField = new JTextField(16);
    private final JTextField filterServiceTextField = new JTextField(16);
    private final JTextField filterThreadIdTextField = new JTextField(16);
    private JScrollPane msgTablePane;
    private JPanel statusPane;
    private JTable msgTable;
    private JTabbedPane msgDetailsPane;
    private JScrollPane associatedLogsScrollPane;
    private JTextArea msgDetails;
    private JSlider slider;
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
    private final StringBuffer unformattedRequestXml;
    private final StringBuffer unformattedResponseXml;
    private DateField startDateField;
    private JSplitPane logSplitPane;
    private JSplitPane selectionSplitPane;
    private boolean connected = false;

    private final HashMap<Integer, String> cachedAuditMessages = new HashMap<Integer, String>();

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

        this.logsRefreshInterval = LOG_REFRESH_TIMER;
        this.nodeId = nodeId;
        this.isAuditType = isAuditType;

        this.unformattedRequestXml = new StringBuffer();
        this.unformattedResponseXml = new StringBuffer();

        init();

        if(includeDetailPane) {
            logSplitPane = new JSplitPane();
            logSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            logSplitPane.setTopComponent(getMsgTablePane());
            logSplitPane.setBottomComponent(getMsgDetailsPane());
            logSplitPane.setOneTouchExpandable(true);
            logSplitPane.setDividerLocation(300);
            logSplitPane.setResizeWeight(1.0);

            selectionSplitPane = new JSplitPane();
            selectionSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
            selectionSplitPane.setTopComponent(getControlPane());

            JPanel bottomSplitPanel = new JPanel();
            bottomSplitPanel.setLayout(new BorderLayout());
            bottomSplitPanel.add(logSplitPane, BorderLayout.CENTER);

            selectionSplitPane.setBottomComponent(bottomSplitPanel);
            selectionSplitPane.setOneTouchExpandable(true);
            selectionSplitPane.setDividerLocation((int)getControlPane().getPreferredSize().getHeight()); // init last pos
            selectionSplitPane.setDividerLocation(0);
            selectionSplitPane.setResizeWeight(0);

            // this listener ensures that the control pane cannot be maximized
            // and hides itself when the divider is moved up
            getControlPane().setMinimumSize(new Dimension(0,0));
            getControlPane().setMaximumSize(getControlPane().getPreferredSize());
            getControlPane().addComponentListener(new ComponentAdapter(){
                public void componentResized(ComponentEvent e) {
                    if(getControlPane().getSize().getHeight() != 20 &&
                       getControlPane().getSize().getHeight() < (getControlPane().getPreferredSize().getHeight()-10)) {
                        setControlsExpanded(false);
                    }
                    else {
                        setControlsExpanded(true);
                    }
                }

                public void componentShown(ComponentEvent e) {
                    componentResized(e);
                }
            });

            // this listener ensures that the display pane hides itself when the moved below a
            // certain size.
            getMsgDetailsPane().setMinimumSize(new Dimension(0,0));
            getMsgDetailsPane().addComponentListener(new ComponentAdapter(){
                public void componentResized(ComponentEvent e) {
                    if(getMsgDetailsPane().getSize().getHeight() < 40) {
                        logSplitPane.setDividerLocation(0.69); // See note for selectionSplitPane above
                        logSplitPane.setDividerLocation(1.0);
                    }
                }

                public void componentShown(ComponentEvent e) {
                    componentResized(e);
                }
            });

            add(selectionSplitPane, BorderLayout.CENTER);
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

        // disable copy/paste
        getMsgTable().setTransferHandler(getCopyPasteDisablingTransferHandler());
    }

    private void init() {
        ButtonGroup viewButtonGroup = new ButtonGroup();
        viewButtonGroup.add(lpbc.viewCurrentRadioButton);
        viewButtonGroup.add(lpbc.viewHistoricRadioButton);

        lpbc.viewCurrentRadioButton.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent e) {
                updateControlState();
            }
        });

        Utilities.setFont(lpbc.controlPane, new java.awt.Font("Dialog", 0, 11));

        lpbc.autoRefresh.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateLogAutoRefresh();
            }
        });

        lpbc.limitUnitComboBox.setModel(new DefaultComboBoxModel(new String[]{"hrs.", "days", "wks."}));

        lpbc.startDatePane.setLayout(new BorderLayout());
        startDateField = CalendarFactory.createDateField();
        lpbc.startDatePane.add(startDateField, BorderLayout.CENTER);

        lpbc.startDateComboBox.setModel(new DefaultComboBoxModel(
                new String[]{"00:00", "03:00", "06:00", "09:00", "12:00", "15:00", "18:00", "21:00"}));

        lpbc.rangeSpinner.setValue(new Integer(24));

        lpbc.updateSelectionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateViewSelection();
            }
        });

        // select after adding date field to ensure correct initial enabled state
        lpbc.viewCurrentRadioButton.setSelected(true);
    }

    private void updateControlState() {
        clearLogCache();
        if(connected) {
            if(lpbc.viewCurrentRadioButton.isSelected()) {
                Utilities.setEnabled(lpbc.viewCurrentPane, true);
                Utilities.setEnabled(lpbc.autoRefresh, true); // for logs this is not in the panel
                Utilities.setEnabled(lpbc.viewHistoricPane, false);
                refreshLogs();
            }
            else {
                Utilities.setEnabled(lpbc.viewCurrentPane, false);
                Utilities.setEnabled(lpbc.autoRefresh, false);
                Utilities.setEnabled(lpbc.viewHistoricPane, true);
                updateLogAutoRefresh();
            }

            setHint(isAutoRefresh() ? "Auto-Refresh" : null);
        }
    }

    private void clearLogCache() {
        getFilteredLogTableSorter().clearLogCache();
    }

    /**
     * Set the status hint, e.g. Disconnected, Auto-Refresh, etc
     */
    private void setHint(String hintText) {
        JLabel hintLabel = getLastUpdateTimeLabel();
        String currentLabel = hintLabel.getText();
        int hintIndex = currentLabel.lastIndexOf('[');
        if(hintText==null || hintText.length()==0) { // then clear
            if(hintIndex>0) {
                hintLabel.setText(currentLabel.substring(0, hintIndex-1));
            }
        }
        else { // set hint to given text
            String newLabel = hintIndex > 0 ? currentLabel.substring(0, hintIndex-1) : currentLabel;
            newLabel = newLabel.trim() + " [" + hintText + "]   ";
            hintLabel.setText(newLabel);
        }
    }

    public boolean getControlsExpanded() {
        boolean expanded = false;

        if(selectionSplitPane!=null) {
            expanded = selectionSplitPane.getDividerLocation()>=5;
        }

        return expanded;
    }

    public void setControlsExpanded(boolean expanded) {
        if(selectionSplitPane!=null) {
            if(expanded) {
                getControlPane().setMinimumSize(new Dimension(0,20));
                //bottomSplitPanel.setMaximumSize(new Dimension(10000, selectionSplitPane.getHeight()-30));
                selectionSplitPane.setResizeWeight(1.0);
                selectionSplitPane.setDividerLocation((int)getControlPane().getPreferredSize().getHeight());
            }
            else {
                getControlPane().setMinimumSize(new Dimension(0,0));
                selectionSplitPane.setResizeWeight(0);
                // We set to preferred size first to ensure this is the "last position"
                // if this is not done the "expand" button doesn't work
                selectionSplitPane.setDividerLocation((int)getControlPane().getPreferredSize().getHeight());
                selectionSplitPane.setDividerLocation(0);
            }
        }
    }

    public boolean getDetailsExpanded() {
        boolean expanded = false;

        if(logSplitPane!=null) {
            expanded = logSplitPane.getDividerLocation()<=(logSplitPane.getSize().getHeight()-25);
        }

        return expanded;
    }

    public void setDetailsExpanded(boolean expanded) {
        if(logSplitPane!=null) {
            if(expanded) {
                logSplitPane.setDividerLocation(0.69);
            }
            else {
                logSplitPane.setDividerLocation(1.0);
            }
        }
    }

    public boolean isAutoRefresh() {
        Window pWin = SwingUtilities.getWindowAncestor(this);
        return pWin!=null && pWin.isVisible()
                && lpbc.autoRefresh.isEnabled()
                && lpbc.autoRefresh.isSelected();
    }

    private void updateLogAutoRefresh() {
        if (isAutoRefresh()) {
            setHint("Auto-Refresh");
            getLogsRefreshTimer().start();
        } else {
            setHint(null);
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
        calendar.set(Calendar.HOUR_OF_DAY, lpbc.startDateComboBox.getSelectedIndex() * 3);

        long range = ((Integer)lpbc.rangeSpinner.getValue()).intValue();

        Date startDate;
        Date endDate;

        if(range>=0) {
            startDate = calendar.getTime();
            endDate = new Date(startDate.getTime() + (range * UNIT_FACTOR[lpbc.limitUnitComboBox.getSelectedIndex()]));
        }
        else {
            endDate = calendar.getTime();
            startDate = new Date(endDate.getTime() + (range * UNIT_FACTOR[lpbc.limitUnitComboBox.getSelectedIndex()]));
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
                    msg += "Event Type : Manager Action" + "\n";
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
                    msg += "User ID    : " + fixUserId(sum.getUserId()) + "\n";
                    msg += "User Name  : " + sum.getUserName() + "\n";
                    if (sum.getAuthenticationType() != null) {
                        msg += "Auth Method: " + sum.getAuthenticationType().getName() + "\n";
                    }

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
                    com.l7tech.common.Component component = fromId(sys.getComponentId());
                    boolean isClient = component != null && component.isClientComponent();
                    msg += "Event Type : System Message" + "\n";
                    if(isClient) {
                        msg += "Client IP  : " + arec.getIpAddress() + "\n";
                    }
                    else {
                        msg += "Node IP    : " + arec.getIpAddress() + "\n";
                    }
                    msg += "Action     : " + sys.getAction() + "\n";
                    msg += "Component  : " + fixComponent(sys.getComponentId()) + "\n";
                    if(isClient) {
                        msg += "User ID    : " + fixUserId(arec.getUserId()) + "\n";
                        msg += "User Name  : " + arec.getUserName() + "\n";
                    }
                    msg += "Entity name: " + arec.getName() + "\n";
                } else {
                    msg += "Event Type : Unknown" + "\n";
                    msg += "Entity name: " + arec.getName() + "\n";
                    msg += "IP Address : " + arec.getIpAddress() + "\n";
                }

                unformattedRequestXml.setLength(0);
                unformattedRequestXml.append(reqXmlDisplayed);
                if (reqXmlVisible && reqXmlDisplayed != null && reqXmlDisplayed.length() > 0 &&
                            getRequestReformatCheckbox().isSelected()) {
                    reqXmlDisplayed = reformat(reqXmlDisplayed);
                }

                unformattedResponseXml.setLength(0);
                unformattedResponseXml.append(respXmlDisplayed);
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

                List<AssociatedLog> associatedLogs = new ArrayList<AssociatedLog>();
                while(associatedLogsItr.hasNext()) {
                    AuditDetail ad = (AuditDetail) associatedLogsItr.next();


                    int id = ad.getMessageId();
                    // TODO get the CellRenderer to display the user messages differently when id < 0 (add field to AssociatedLog class?)
                    String associatedLogMessage = getMessageById(id);
                    String associatedLogLevel = Messages.getSeverityLevelNameById(id);

                    StringBuffer result = new StringBuffer();
                    if (associatedLogMessage != null) {
                        MessageFormat mf = new MessageFormat(associatedLogMessage);
                        mf.format(ad.getParams(), result, new FieldPosition(0));
                    }
                    AssociatedLog al = new AssociatedLog(ad.getTime(), associatedLogLevel, result.toString(), ad.getException(), ad.getOrdinal());
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

    private String getMessageById(int id) {
        // lookup each value only once per session. this should be fast enough
        String output = cachedAuditMessages.get(id);
        if (output == null) {
            Registry reg = Registry.getDefault();
            if (reg.isAdminContextPresent()) {
                try {
                    ClusterProperty prop = reg.getClusterStatusAdmin().findPropertyByName(Messages.OVERRIDE_PREFIX + id);
                    if (prop != null) output = prop.getValue();
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "cannot get property", e);
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get property", e);
                }
            }
            if (output == null) {
                output = Messages.getMessageById(id);
            }
            cachedAuditMessages.put(id, output);
        }
        return output;
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
        com.l7tech.common.Component c = fromId(componentId);
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

    private String fixUserId(String id) {
        return (id!=null ? id : "<No ID>");
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

    public String getMsgFilterNodeName() {
        return msgFilterNode;
    }

    public String getMsgFilterService() {
        return msgFilterService;
    }

    public String getMsgFilterThreadId() {
        return msgFilterThreadId;
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

        Utilities.setEnabled(lpbc.viewCurrentRadioButton, true);
        Utilities.setEnabled(lpbc.viewHistoricRadioButton, true);
        updateControlState();
    }

    /**
     * Performs the necessary cleanup when the connection with the cluster went down.
     */
    public void onDisconnect(){
        connected = false;

        clearLogCache();
        getMsgDetails().setText("");
        getAssociatedLogsTable().getTableSorter().setData(new ArrayList<AssociatedLog>());
        getRequestXmlTextArea().setText("");
        getResponseXmlTextArea().setText("");
        unformattedRequestXml.setLength(0);
        unformattedResponseXml.setLength(0);

        getLogsRefreshTimer().stop();
        getFilteredLogTableSorter().onDisconnect();

        setHint("Disconnected");
        Utilities.setEnabled(lpbc.controlPane, false);
        Utilities.setEnabled(lpbc.autoRefresh, false);
    }

    /**
     * Return SelectPane property value
     * @return JPanel
     */
    private JPanel getSelectPane(){
        if(selectPane == null) {
            JPanel selectPaneLower = new JPanel();
            selectPaneLower.setLayout(new BoxLayout(selectPaneLower, BoxLayout.X_AXIS));
            selectPaneLower.add(getFilterPane());
            selectPaneLower.add(Box.createHorizontalGlue());
            selectPaneLower.add(getStatusPane());

            selectPane = new JPanel();
            selectPane.setLayout(new BorderLayout());
            selectPane.add(getFilterLabel(), BorderLayout.NORTH);
            selectPane.add(selectPaneLower, BorderLayout.CENTER);
        }

        return selectPane;
    }

    /**
     * Return FilterPane property value
     * @return JPanel
     */
    private JPanel getFilterPane(){
        if(filterPane == null) {
            filterPane = new JPanel();
            filterPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
            filterPane.setLayout(new BoxLayout(filterPane, BoxLayout.X_AXIS));
            filterPane.add(getFilterSlider());

            JPanel expandingFilterPane = new JPanel();
            expandingFilterPane.setLayout(new BoxLayout(expandingFilterPane, BoxLayout.X_AXIS));
            if(isAuditType) {
                expandingFilterPane.add(getFilterNodePane());
                expandingFilterPane.add(getFilterServicePane());
            }
            else {
                expandingFilterPane.add(getFilterThreadPane());
            }
            expandingFilterPane.add(getFilterMessagePane());
            expandingFilterPane.setMaximumSize(new Dimension(800,100));
            filterPane.add(expandingFilterPane);

            if(!isAuditType) filterPane.add(getMicroControlPane());
            filterPane.add(Box.createHorizontalGlue());
        }

        return filterPane;
    }

    private JLabel getFilterLabel() {
        if(filterLabel == null) {
            filterLabel = new JLabel("Caution! Constraint may exclude some events.");
            filterLabel.setHorizontalAlignment(SwingConstants.CENTER);
            filterLabel.setFont(new java.awt.Font("Dialog", 0, 11));
            filterLabel.setVisible(false);
            filterLabel.setBackground(new Color(0xFF, 0xFF, 0xe1));
            filterLabel.setOpaque(true);
        }

        return filterLabel;
    }

    /**
     * Return filterSlider property value
     * @return JSlider
     */
    private JSlider getFilterSlider(){
        if(slider == null) {
            slider = new JSlider(0, 120);
            slider.setMajorTickSpacing(40);

            Dictionary<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
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

            slider.setMinimumSize(slider.getPreferredSize());
            slider.setMaximumSize(slider.getPreferredSize());
        }

        return slider;
    }

    private JPanel getFilterNodePane() {
        return buildFilterPane("Node:",
                new TextListener(){
                    public void textValueChanged(TextEvent e) {
                        updateMsgFilterNode(((JTextField)e.getSource()).getText());
                    }
                }, filterNodeTextField);
    }

    private JPanel getFilterServicePane() {
        return buildFilterPane("Service:",
                new TextListener(){
                    public void textValueChanged(TextEvent e) {
                        updateMsgFilterService(((JTextField)e.getSource()).getText());
                    }
                }, filterServiceTextField);
    }

    private JPanel getFilterThreadPane() {
        return buildFilterPane("Thread Id:",
                new TextListener(){
                    public void textValueChanged(TextEvent e) {
                        updateMsgFilterThreadId(((JTextField)e.getSource()).getText());
                    }
                }, filterThreadIdTextField);
    }

    private JPanel getFilterMessagePane() {
        return buildFilterPane("Message:",
                new TextListener(){
                    public void textValueChanged(TextEvent e) {
                        updateMsgFilterMessage(((JTextField)e.getSource()).getText());
                    }
                }, filterMessageTextField);
    }

    private JPanel buildFilterPane(String labelText, final TextListener listener, final JTextField textField) {
        JPanel filterPane = new JPanel();

        textField.setFont(new java.awt.Font("Dialog", 0, 11));
        textField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable(){
            public void run() {
                listener.textValueChanged(new TextEvent(textField, TextEvent.TEXT_VALUE_CHANGED));
            }
        }));

        JLabel label = new JLabel(labelText);
        label.setFont(new java.awt.Font("Dialog", 0, 11));

        filterPane.setLayout(new BorderLayout());
        filterPane.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        filterPane.add(label, BorderLayout.NORTH);
        filterPane.add(textField, BorderLayout.CENTER);

        return filterPane;
    }

    /**
     * Get the small control panel used for log viewing
     */
    private JPanel getMicroControlPane() {
        JPanel microControlPane = new JPanel();
        microControlPane.setLayout(new BoxLayout(microControlPane, BoxLayout.X_AXIS));

        lpbc.autoRefresh.setSelected(true);
        microControlPane.add(lpbc.autoRefresh);

        return microControlPane;
    }

    /**
     * Return ControlPane property value
     * @return  JPanel
     */
    private JPanel getControlPane(){
        return lpbc.controlPane;
    }

    /**
     * Return the total number of the messages being displayed.
     */
    private JLabel getMsgTotal(){
        if(msgTotal == null) {
            msgTotal = new JLabel(MSG_TOTAL_PREFIX + "0");
            msgTotal.setFont(new java.awt.Font("Dialog", 0, 12));
            msgTotal.setAlignmentY(0);
        }
        return msgTotal;
    }

    /**
     * Return MsgTable property value
     * @return JTable
     */
    private JTable getMsgTable(){
        if(msgTable == null) {
            msgTable = new JTable(getFilteredLogTableSorter(), getLogColumnModel());
            msgTable.setShowHorizontalLines(false);
            msgTable.setShowVerticalLines(false);
            msgTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            msgTable.setRowSelectionAllowed(true);
            msgTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

            addMouseListenerToHeaderInTable(msgTable);
        }

        return msgTable;
    }

    /**
     * Return MsgTablePane property value
     * @return JScrollPane
     */
    private JComponent getMsgTablePane(){
        if(msgTablePane == null) {
            msgTablePane = new JScrollPane();
            msgTablePane.setViewportView(getMsgTable());
            msgTablePane.getViewport().setBackground(getMsgTable().getBackground());
            msgTablePane.setMinimumSize(new Dimension(600, 40));
            msgTablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }

        return msgTablePane;
    }

    /**
     * Return MsgDetailsPane property value
     * @return JScrollPane
     */
    private JTabbedPane getMsgDetailsPane(){
        if(msgDetailsPane == null) {
            msgDetailsPane = new JTabbedPane();
            msgDetailsPane.addTab("Details", getDetailsScrollPane());
            msgDetailsPane.addTab("Associated Logs", getAssociatedLogsScrollPane());
            msgDetailsPane.addTab("Request XML", getRequestXmlPanel());
            msgDetailsPane.addTab("Response XML", getResponseXmlPanel());
        }

        return msgDetailsPane;
    }

    private JScrollPane getAssociatedLogsScrollPane() {
        if (associatedLogsScrollPane == null) {
            associatedLogsScrollPane = new JScrollPane();
            associatedLogsScrollPane.setViewportView(getAssociatedLogsTable());
            associatedLogsScrollPane.getViewport().setBackground(getAssociatedLogsTable().getBackground());
        }

        return associatedLogsScrollPane;
    }

    private AssociatedLogsTable getAssociatedLogsTable() {
        if(associatedLogsTable == null) {
            associatedLogsTable = new AssociatedLogsTable();
            associatedLogsTable.setShowHorizontalLines(false);
            associatedLogsTable.setShowVerticalLines(false);
            associatedLogsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            associatedLogsTable.setRowSelectionAllowed(true);
        }

        return associatedLogsTable;
    }

    private JScrollPane getDetailsScrollPane() {
        if (detailsScrollPane == null) {
            detailsScrollPane = new JScrollPane(getMsgDetails());
        }
        return detailsScrollPane;
    }

    private JScrollPane getRequestXmlScrollPane() {
        if (requestXmlScrollPane == null) {
            requestXmlScrollPane = new JScrollPane(getRequestXmlTextArea());
        }

        return requestXmlScrollPane;
    }

    private JScrollPane getResponseXmlScrollPane() {
        if (responseXmlScrollPane == null) {
            responseXmlScrollPane = new JScrollPane(getResponseXmlTextArea());
        }

        return responseXmlScrollPane;
    }

    /**
     * Return MsgDetails property value
     * @return  JTextArea
     */
    private JTextArea getMsgDetails() {
        if(msgDetails == null) {
            msgDetails = new ContextMenuTextArea();
            msgDetails.setEditable(false);
        }

        return msgDetails;
    }

    private JPanel getRequestXmlPanel() {
        if (requestXmlPanel == null) {
            requestXmlPanel = new JPanel();
            requestXmlPanel.setLayout(new BorderLayout());
            requestXmlPanel.add(getRequestXmlScrollPane(), BorderLayout.CENTER);
            requestXmlPanel.add(getRequestReformatCheckbox(), BorderLayout.SOUTH);
        }
        return requestXmlPanel;
    }

    private JCheckBox getRequestReformatCheckbox() {
        if (requestReformatCheckbox == null) {
            requestReformatCheckbox = new JCheckBox("Reformat Request XML");
            requestReformatCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doReformat(getRequestXmlTextArea(), requestReformatCheckbox.isSelected(), unformattedRequestXml);
                }
            });
        }
        return requestReformatCheckbox;
    }

    private JTextArea getRequestXmlTextArea() {
        if (requestXmlTextArea == null) {
            requestXmlTextArea = new ContextMenuTextArea();
            requestXmlTextArea.setEditable(false);
        }
        return requestXmlTextArea;
    }

    private JPanel getResponseXmlPanel() {
        if (responseXmlPanel == null) {
            responseXmlPanel = new JPanel();
            responseXmlPanel.setLayout(new BorderLayout());
            responseXmlPanel.add(getResponseXmlScrollPane(), BorderLayout.CENTER);
            responseXmlPanel.add(getResponseReformatCheckbox(), BorderLayout.SOUTH);
        }
        return responseXmlPanel;
    }

    private JCheckBox getResponseReformatCheckbox() {
        if (responseReformatCheckbox == null) {
            responseReformatCheckbox = new JCheckBox("Reformat Response XML");
            responseReformatCheckbox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doReformat(getResponseXmlTextArea(), responseReformatCheckbox.isSelected(), unformattedResponseXml);
                }
            });
        }
        return responseReformatCheckbox;
    }

    private void doReformat(JTextArea textArea, boolean format, StringBuffer textBuffer) {
        String text = textArea.getText();
        if (textArea.isEnabled() && textArea.isShowing() &&
                    text != null && text.length() > 0) {
            if (format) {
                textBuffer.setLength(0);
                textBuffer.append(text);
                textArea.setText(reformat(textBuffer.toString()));
            } else {
                textArea.setText(textBuffer.toString());
            }
            textArea.getCaret().setDot(0);
        }
    }

    private JTextArea getResponseXmlTextArea() {
        if (responseXmlTextArea == null) {
            responseXmlTextArea = new ContextMenuTextArea();
            responseXmlTextArea.setEditable(false);
        }
        return responseXmlTextArea;
    }

    /**
     * Return statusPane property value
     * @return  JPanel
     */
    private JPanel getStatusPane() {
        if(statusPane == null) {
            statusPane = new JPanel();
            statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.Y_AXIS));
            statusPane.add(Box.createVerticalGlue());
            statusPane.add(getMsgTotal());
            statusPane.add(getLastUpdateTimeLabel());
            statusPane.add(Box.createVerticalGlue());
        }

        return statusPane;
    }

    /**
     * Return lastUpdateTimeLabel property value
     * @return  JLabel
     */
    private JLabel getLastUpdateTimeLabel() {
        if(lastUpdateTimeLabel == null) {
            lastUpdateTimeLabel = new JLabel();
            lastUpdateTimeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            lastUpdateTimeLabel.setText("");
            lastUpdateTimeLabel.setAlignmentY(0);
        }

        return lastUpdateTimeLabel;
    }

    /**
     * Return LogColumnModel property value
     * @return  DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        tableColumnWidths[LOG_MSG_NUMBER_COLUMN_INDEX] = 20;
        tableColumnWidths[LOG_NODE_NAME_COLUMN_INDEX] = 50;
        tableColumnWidths[LOG_TIMESTAMP_COLUMN_INDEX] = 140;
        tableColumnWidths[LOG_THREAD_COLUMN_INDEX] = 20;
        tableColumnWidths[LOG_SEVERITY_COLUMN_INDEX] = 40;
        tableColumnWidths[LOG_SERVICE_COLUMN_INDEX] = 120;
        tableColumnWidths[LOG_MSG_DETAILS_COLUMN_INDEX] = 400;

        // Add columns according to configuration
        String showMsgFlag = resapplication.getString("Show_Message_Number_Column");
        if ((showMsgFlag != null) && showMsgFlag.equals("true")){
            columnModel.addColumn(new TableColumn(LOG_MSG_NUMBER_COLUMN_INDEX, tableColumnWidths[LOG_MSG_NUMBER_COLUMN_INDEX]));
        }
        columnModel.addColumn(new TableColumn(LOG_NODE_NAME_COLUMN_INDEX, tableColumnWidths[LOG_NODE_NAME_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_TIMESTAMP_COLUMN_INDEX, tableColumnWidths[LOG_TIMESTAMP_COLUMN_INDEX]));
        if(!isAuditType) {
            columnModel.addColumn(new TableColumn(LOG_THREAD_COLUMN_INDEX, tableColumnWidths[LOG_THREAD_COLUMN_INDEX]));
        }
        columnModel.addColumn(new TableColumn(LOG_SEVERITY_COLUMN_INDEX, tableColumnWidths[LOG_SEVERITY_COLUMN_INDEX]));
        if(isAuditType) { // show the service name if we are displaying audit messages
            columnModel.addColumn(new TableColumn(LOG_SERVICE_COLUMN_INDEX, tableColumnWidths[LOG_SERVICE_COLUMN_INDEX]));
        }
        columnModel.addColumn(new TableColumn(LOG_MSG_DETAILS_COLUMN_INDEX, tableColumnWidths[LOG_MSG_DETAILS_COLUMN_INDEX]));

        // Set headers
        for(int i = 0; i < columnModel.getColumnCount(); i++){
            TableColumn tc = columnModel.getColumn(i);
            tc.setMinWidth(50);
            tc.setHeaderRenderer(iconHeaderRenderer);
            tc.setHeaderValue(getLogTableModel().getColumnName(tc.getModelIndex()));
        }

        // Tooltip for details
        columnModel.getColumn(LOG_MSG_DETAILS_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if(comp instanceof JLabel) {
                    String detailText = ((JLabel)comp).getText();
                    if (detailText == null || detailText.trim().length() == 0) {
                        ((JComponent)comp).setToolTipText(null);
                    } else {
                        ((JComponent)comp).setToolTipText(detailText);
                    }
                }
                return comp;
            }
        });

        return columnModel;
    }

    /**
     * Return LogTableModelFilter property value
     * @return FilteredLogTableModel
     */
    private FilteredLogTableSorter getFilteredLogTableSorter(){
        if(logTableSorter == null) {
            logTableSorter = new FilteredLogTableSorter(getLogTableModel(),
                    isAuditType ? GenericLogAdmin.TYPE_AUDIT : GenericLogAdmin.TYPE_LOG);

            if(!isAuditType) {
                logTableSorter.setTimeOffset(System.currentTimeMillis()); // remove any time limits for logs
            }
        }

        return logTableSorter;
    }

    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     *
     */
    private DefaultTableModel getLogTableModel() {
        if (logTableModel == null) {
            String[] cols = {"Message #", "Node", "Time", "Severity", "Message", "Class",
                    "Method", "Request Id", "Node Id", "Service", "Thread Id"};
            String[][] rows = new String[][]{};

            logTableModel = new DefaultTableModel(rows, cols) {
                public boolean isCellEditable(int row, int col) {
                    // the table cells are not editable
                    return false;
                }
            };
        }

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

    public void refreshView() {
        if(connected) {
            if(lpbc.viewCurrentRadioButton.isSelected()) {
                refreshLogs();
            }
            else {
                updateViewSelection();
            }
        }
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
     * Performs the log retrieval.
     */
    public void refreshLogs(Date first, Date last) {
        getLogsRefreshTimer().stop();

        // retrieve the new logs
        Window window = SwingUtilities.getWindowAncestor(this);
        if(window!=null && window.isVisible()) {
            FilteredLogTableSorter flts = (FilteredLogTableSorter) getMsgTable().getModel();
            flts.clearLogCache();
            flts.refreshLogs(this, first, last, nodeId);
        }
    }

    /**
     * Displays the given log messages. Old display is cleared first.
     *
     * @param logs      log messages to load; as a map of gateway node ID and
     *                  corresponding collection of {@link LogMessage}s
     */
    public void setLogs(Map<String, Collection<LogMessage>> logs) {
        onDisconnect();
        getFilteredLogTableSorter().setLogs(this, logs);
        getLastUpdateTimeLabel().setVisible(false);    // It's not applicable in static view.
    }

    /**
     * Changes the message filter value.
     * @param message  message text
     */
    public void setMsgFilterMessage(String message) {
        filterMessageTextField.setText(message);
        updateMsgFilterMessage(message);
    }

    /**
     * Changes the gateway node filter value.
     * @param nodeName  gateway node name
     */
    public void setMsgFilterNode(String nodeName) {
        filterNodeTextField.setText(nodeName);
        updateMsgFilterNode(nodeName);
    }

    /**
     * Changes the published service filter value.
     * @param serviceName   name of published service
     */
    public void setMsgFilterService(String serviceName) {
        filterServiceTextField.setText(serviceName);
        updateMsgFilterService(serviceName);
    }

    /**
     * Changes the thread ID filter value.
     * @param threadId  thread ID
     */
    public void setMsgFilterThreadId(String threadId) {
        filterThreadIdTextField.setText(threadId);
        updateMsgFilterThreadId(threadId);
    }

    /**
     * Set the row of the log table which is currenlty selected by the user for viewing the details of the log message.
     *
     * @param msgNumber  The message number of the log being selected.
     */
    public void setSelectedRow(String msgNumber) {
        if (msgNumber != null && !msgNumber.equals("-1")) {
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
        // update filter warning
        JLabel filterWarn = getFilterLabel();
        if(msgFilterNode.trim().length() > 0 ||
                msgFilterService.trim().length() > 0 ||
                msgFilterThreadId.trim().length() > 0 ||
                msgFilterMessage.trim().length() > 0) {
            filterWarn.setVisible(true);
        }
        else {
            filterWarn.setVisible(false);
        }

        // get the selected row index
        int selectedRowIndexOld = getMsgTable().getSelectedRow();
        String msgNumSelected = null;

        // save the number of selected message
        if (selectedRowIndexOld >= 0) {
            msgNumSelected = getMsgTable().getModel().getValueAt(selectedRowIndexOld, LOG_MSG_NUMBER_COLUMN_INDEX).toString();
        }

        ((FilteredLogTableSorter) getMsgTable().getModel()).applyNewMsgFilter(msgFilterLevel, msgFilterNode, msgFilterService, msgFilterThreadId, msgFilterMessage);

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

    private void updateMsgFilterNode(String nodeMatch) {
        if (nodeMatch!=null && !nodeMatch.equals(msgFilterNode)) {
            msgFilterNode = nodeMatch;
            updateMsgFilter();
        }
    }

    private void updateMsgFilterService(String serviceMatch) {
        if (serviceMatch!=null && !serviceMatch.equals(msgFilterService)) {
            msgFilterService = serviceMatch;
            updateMsgFilter();
        }
    }

    private void updateMsgFilterThreadId(String threadIdMatch) {
        if (threadIdMatch!=null && !threadIdMatch.equals(msgFilterThreadId)) {
            msgFilterThreadId = threadIdMatch;
            updateMsgFilter();
        }
    }

    private void updateMsgFilterMessage(String messageMatch) {
        if (messageMatch!=null && !messageMatch.equals(msgFilterMessage)) {
            msgFilterMessage = messageMatch;
            updateMsgFilter();
        }
    }

    private void updateLogsRefreshTimerDelay() {
        int refreshSeconds = getFilteredLogTableSorter().getDelay();

        if(refreshSeconds >= 0 && refreshSeconds < 300) logsRefreshInterval = 1000 * refreshSeconds;
        else logsRefreshInterval = LOG_REFRESH_TIMER;

        if(logsRefreshInterval==0) logsRefreshInterval = Integer.MAX_VALUE; // disable refresh

        getLogsRefreshTimer().setInitialDelay(logsRefreshInterval);
        getLogsRefreshTimer().setDelay(logsRefreshInterval);
    }

    private TransferHandler getCopyPasteDisablingTransferHandler() {
        return new TransferHandler(){
            public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
                return false;
            }

            protected Transferable createTransferable(JComponent c) {
                return null;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.NONE;
            }

            public Icon getVisualRepresentation(Transferable t) {
                return super.getVisualRepresentation(t);
            }

            public boolean importData(JComponent comp, Transferable t) {
                return false;
            }

            // do nothing
            public void exportAsDrag(JComponent comp, InputEvent e, int action) {}
            protected void exportDone(JComponent source, Transferable data, int action) {}
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {}
        };
    }

    /**
     *  Clear the message table
     */
    public void clearMsgTable(){
        getMsgDetails().setText("");
        getMsgTotal().setText(MSG_TOTAL_PREFIX + "0");
        displayedLogMessage = null;
        clearLogCache();
    }

    /**
     * Return lgsRefreshTimer propery value
     *
     * @return javax.swing.Timer
     */
    public javax.swing.Timer getLogsRefreshTimer() {
        if (logsRefreshTimer == null) {
            //Create a refresh logs timer.
            logsRefreshTimer = new javax.swing.Timer(logsRefreshInterval, new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    refreshLogs();
                }
            });
        }

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
        getLastUpdateTimeLabel().setText("Last Updated: " + sdf.format(cal.getTime()) +
                (isAutoRefresh() ? " [Auto-Refresh]" : "   "));
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

            assert table != null;
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
        th.addMouseListener(new JTableColumnResizeMouseListener(tableView, tableColumnWidths));
    }

    /**
     * Export the currently displayed log/audit data.
     */
    public void exportView(File file) throws IOException {
        // process
        JTable table = getMsgTable();
        int rows = table.getRowCount();
        List<WriteableLogMessage> data = new ArrayList<WriteableLogMessage>(rows);
        FilteredLogTableSorter logTableSorter = getFilteredLogTableSorter();
        for(int i=0; i<rows; i++) {
            LogMessage rowMessage = logTableSorter.getLogMessageAtRow(i);
            data.add(new WriteableLogMessage(rowMessage));
        }
        Collections.sort(data);

        // write
        ObjectOutputStream oos = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            out.write(FILE_TYPE);
            oos = new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)));
            oos.writeObject(BuildInfo.getProductVersion());
            oos.writeObject(BuildInfo.getBuildNumber());
            oos.writeObject(data);
        }
        finally {
            // necessary to get the closing object tag
            ResourceUtils.closeQuietly(oos);
            ResourceUtils.closeQuietly(out);
        }
    }

    private void importError(final String dialogMessage) {
        logger.info(dialogMessage);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                ExceptionDialog d = ExceptionDialog.createExceptionDialog(
                        null, "SecureSpan Manager - Error", dialogMessage, null, Level.WARNING);
                d.pack();
                Utilities.centerOnScreen(d);
                DialogDisplayer.display(d);
            }
        });
    }

    public boolean importView(File file) throws IOException {
        lpbc.viewHistoricRadioButton.setSelected(true);

        InputStream in = null;
        ObjectInputStream ois = null;
        try {
            in = new FileInputStream(file);
            byte[] header = HexUtils.slurpStream(in, FILE_TYPE.length);
            if(header.length < FILE_TYPE.length ||
               !ArrayUtils.compareArrays(FILE_TYPE, 0, header, 0, FILE_TYPE.length)) {
                importError("Cannot import file, incorrect type.");
                return false;
            }
            ois = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(in)));
            Object fileProductVersionObj = ois.readObject();
            Object fileBuildNumberObj = ois.readObject();
            Object read = ois.readObject();
            if(fileProductVersionObj instanceof String &&
               fileBuildNumberObj instanceof String &&
               read instanceof List) {
                String fileProductVersion = (String) fileProductVersionObj;
                String fileBuildNumber = (String) fileBuildNumberObj;

                boolean buildMatch = fileBuildNumber.equals(BuildInfo.getBuildNumber());
                boolean versionMatch = fileProductVersion.equals(BuildInfo.getProductVersion());

                if(!buildMatch) {
                    String message;
                    if(!versionMatch) {
                        message = "Cannot import file for product version '"+fileProductVersion+"'.";
                    }
                    else {
                        message = "Cannot import file for product build '"+fileBuildNumber+"'.";
                    }
                    importError(message);
                    return false;
                }

                //noinspection unchecked
                List<WriteableLogMessage> data = (List) read;
                if(data.isEmpty()) {
                    logger.info("No data in file! '"+file.getAbsolutePath()+"'.");
                }
                Map<String, Collection<LogMessage>> loadedLogs = new HashMap<String, Collection<LogMessage>>();
                for (WriteableLogMessage message : data) {
                    Collection<LogMessage> logsForNode = loadedLogs.get(message.ssgLogRecord.getNodeId());
                    if (logsForNode == null) {
                        logsForNode = new LinkedHashSet<LogMessage>();
                        loadedLogs.put(message.ssgLogRecord.getNodeId(), logsForNode);
                    }
                    LogMessage lm = new LogMessage(message.ssgLogRecord);
                    lm.setNodeName(message.nodeName);
                    logsForNode.add(lm);
                }
                getFilteredLogTableSorter().setLogs(this, loadedLogs);
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
        return true;
    }

    public static class WriteableLogMessage implements Comparable, Serializable {
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

    private static class LogPanelBoundComponents {
        private JPanel controlPane;
        private JPanel viewHistoricPane;
        private JPanel viewCurrentPane;
        private JCheckBox autoRefresh;
        private JButton updateSelectionButton;
        private JComboBox startDateComboBox;
        private JRadioButton viewCurrentRadioButton;
        private JRadioButton viewHistoricRadioButton;
        private JPanel startDatePane;
        private JComboBox limitUnitComboBox;
        private JSpinner rangeSpinner;
    }
}
