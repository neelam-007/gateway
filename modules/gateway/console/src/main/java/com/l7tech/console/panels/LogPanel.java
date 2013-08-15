package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.table.AssociatedLogsTable;
import com.l7tech.console.table.AuditLogTableSorterModel;
import com.l7tech.console.tree.PolicyEntityNode;
import com.l7tech.console.util.*;
import com.l7tech.console.util.jcalendar.TimeRangePicker;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.cluster.LogRequest;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.security.rbac.AttemptedOther;
import com.l7tech.gateway.common.security.rbac.OtherOperationName;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.ContextMenuTextArea;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.*;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.ref.SoftReference;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.l7tech.gateway.common.Component.fromId;

/**
 * A panel for displaying audit events.
 */
public class LogPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(LogPanel.class.getName());

    public static final int LOG_SIGNATURE_COLUMN_INDEX = 0;
    public static final int LOG_MSG_NUMBER_COLUMN_INDEX = 1;
    public static final int LOG_NODE_NAME_COLUMN_INDEX = 2;
    public static final int LOG_TIMESTAMP_COLUMN_INDEX = 3;
    public static final int LOG_SEVERITY_COLUMN_INDEX = 4;
    public static final int LOG_MSG_DETAILS_COLUMN_INDEX = 5;
    public static final int LOG_REQUEST_ID_COLUMN_INDEX = 6;
    public static final int LOG_NODE_ID_COLUMN_INDEX = 7;
    public static final int LOG_SERVICE_COLUMN_INDEX = 8;

    private static final String DATE_FORMAT_PATTERN = "yyyyMMdd HH:mm:ss.SSS";
    private static final String DATE_FORMAT_ZONE_PATTERN = "yyyyMMdd HH:mm:ss.SSS z";

    private static final String[] COLUMN_NAMES = {
            "Sig",          // digital signature
            "AuditRecord",  // Audit Record ID
            "Node",
            "Time",
            "Severity",
            "Message",
            "Request Id",
            "Node Id",
            "Service",
            "Thread Id"
    };
    public static final String AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP = "audit.lookup.policy.guid";

    public static final String MSG_TOTAL_PREFIX = "Total: ";

    private static final String SPLIT_PROPERTY_NAME = "last." + LogPanel.class.getSimpleName() + ".split.divider.location";
    private static final int LOG_REFRESH_TIMER = 3000;
    private int logsRefreshInterval;
    private javax.swing.Timer logsRefreshTimer;

    private static final byte[] FILE_TYPE = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xD0, (byte) 0x0D};

    private static final long MILLIS_IN_MINUTE = 1000L * 60L;
    private static final long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60L;

    private SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private TreeMap<String, String> entitiesMap; // Map an entity type name to an entity class name

    private LogPanelControlPanel controlPanel = new LogPanelControlPanel();
    private int[] tableColumnWidths = new int[20];
    private boolean signAudits;
    private JPanel bottomPane;
    private JLabel filterLabel;
    private JScrollPane msgTablePane;
    private JPanel statusPane;
    private JTable msgTable;
    private JTabbedPane msgDetailsPane;
    private JScrollPane associatedLogsScrollPane;
    private JTextArea msgDetails;
    private DefaultTableModel logTableModel;
    private AuditLogTableSorterModel auditLogTableSorterModel;
    private JLabel msgTotal;
    private JProgressBar msgProgressBar;
    private JLabel lastUpdateTimeLabel;
    private Icon upArrowIcon = new ArrowIcon(0);
    private Icon downArrowIcon = new ArrowIcon(1);
    private JScrollPane detailsScrollPane;
    private AbstractAuditMessage displayedLogMessage;
    private AssociatedLogsTable associatedLogsTable;
    private JScrollPane requestXmlScrollPane;
    private JTextArea requestXmlTextArea;
    private JPanel requestXmlPanel;
    private JCheckBox requestReformatCheckbox;
    private JPanel responseXmlPanel;
    private JScrollPane responseXmlScrollPane;
    private JCheckBox responseReformatCheckbox;
    private JTextArea responseXmlTextArea;
    /**
     * Contents of either the audit records request xml or the output of the audit viewer policy for the request.
     * Updated for each audit record viewed.
     */
    private final StringBuffer unformattedRequestXml;
    /**
     * Contents of either the audit records response xml or the output of the audit viewer policy for the response.
     * Updated for each audit record viewed.
     */
    private final StringBuffer unformattedResponseXml;
    private JSplitPane logSplitPane;
    private JSplitPane selectionSplitPane;
    private boolean connected = false;

    private final Map<Integer, String> cachedAuditMessages = new HashMap<Integer, String>();
    private final Map<Long, SoftReference<AuditMessage>> cachedLogMessages = Collections.synchronizedMap( new HashMap<Long, SoftReference<AuditMessage>>() );
    private JButton invokeRequestAVPolicyButton;
    private JButton invokeResponseAVPolicyButton;
    private JLabel sigStatusLabel;
    private boolean validationIsRunning = false;

    //
    // Data model for the audit events control panel.
    //
    /**
     * Modes of selection when downloading audit events.
     */
    private enum RetrievalMode {
        /**
         * Retrieves a fixed duration past current time.
         */
        DURATION,
        /**
         * Retrieves between a fixed start and end time.
         */
        TIME_RANGE,
        /**
         * No retrieval (offline data)
         */
        NONE
    }

    private RetrievalMode retrievalMode;
    /**
     * Duration in milliseconds. Applies when {@link #retrievalMode} == {@link RetrievalMode#DURATION}.
     */
    private long durationMillis;
    /**
     * Whether to auto-refresh. Applies when {@link #retrievalMode} == {@link RetrievalMode#DURATION}.
     */
    private boolean durationAutoRefresh;
    /**
     * Start time. Applies when {@link #retrievalMode} == {@link RetrievalMode#TIME_RANGE}.
     */
    private Date timeRangeStart;
    /**
     * End time. Applies when {@link #retrievalMode} == {@link RetrievalMode#TIME_RANGE}.
     */
    private Date timeRangeEnd;
    /**
     * Time zone. Applies when {@link #retrievalMode} == {@link RetrievalMode#TIME_RANGE}.
     */
    private TimeZone timeRangeTimeZone;

    /**
     * logging severity
     */

    private LogLevelOption logLevelOption = LogLevelOption.WARNING;
    /**
     * service name; applies when auditType is ALL or MESSAGE
     * can contain any number of wildcard '*' characters
     */

    private String serviceName;
    /**
     * audit message
     * can contain any number of wildcard '*' characters
     */
    private String message = "";

    /**
     * AuditType: Admin Auidt, System Audit, and Message Audit
     */
    private AuditType auditType;

    /**
     * node name (not node id)
     * can contain any number of wildcard '*' characters (although it may not be that useful)
     */
    private String node;

    /**
     * request ID; applies when auditType is ALL or MESSAGE
     * can contain any number of wildcard '*' characters (although it may not be that useful)
     */
    private String requestId;

    // User login
    private String userName;

    // User ID or User DN
    private String userIdOrDn;

    // Message ID (same as Audit Code)
    private Integer messageId;

    // The value of an audit detail parameter
    private String paramValue;

    // Entity Type Name (the short name of the entity class)
    private String entityTypeName;

    // Entity ID
    private Long entityId;

    // Operation
    private String operation;

    // Use audit lookup policy
    private boolean getFromPolicy = false;

    public enum LogLevelOption {
        ALL("All", Level.ALL), INFO("Info", Level.INFO), WARNING("Warning", Level.WARNING), SEVERE("Severe", Level.SEVERE);

        private String name;
        private Level level;

        LogLevelOption(String name, Level level) {
            this.name = name;
            this.level = level;
        }

        public Level getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Constructor
     */
    public LogPanel() {
        setLayout(new BorderLayout());

        this.logsRefreshInterval = LOG_REFRESH_TIMER;

        this.unformattedRequestXml = new StringBuffer();
        this.unformattedResponseXml = new StringBuffer();

        init();

        selectionSplitPane = new JSplitPane();
        selectionSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        selectionSplitPane.setTopComponent(getControlPane());
        selectionSplitPane.setOneTouchExpandable(true);
        selectionSplitPane.setDividerLocation((int) getControlPane().getPreferredSize().getHeight()); // init last pos
        selectionSplitPane.setDividerLocation(0);
        selectionSplitPane.setResizeWeight(0.0);

        // this listener ensures that the control pane cannot be maximized
        // and hides itself when the divider is moved up
        getControlPane().setMinimumSize(new Dimension(0, 0));
        getControlPane().setMaximumSize(getControlPane().getPreferredSize());
        getControlPane().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (getControlPane().getSize().getHeight() < (getControlPane().getPreferredSize().getHeight() - 50.0)) {
                    setControlsExpanded(false);
                } else {
                    setControlsExpanded(true);
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                componentResized(e);
            }
        });

        logSplitPane = new JSplitPane();
        logSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        logSplitPane.setTopComponent(getMsgTablePane());
        logSplitPane.setBottomComponent(getMsgDetailsPane());
        logSplitPane.setOneTouchExpandable(true);
        logSplitPane.setDividerLocation(300);
        logSplitPane.setResizeWeight(1.0);

        JPanel bottomSplitPanel = new JPanel();
        bottomSplitPanel.setLayout(new BorderLayout());
        bottomSplitPanel.add(logSplitPane, BorderLayout.CENTER);

        selectionSplitPane.setBottomComponent(bottomSplitPanel);


        add(selectionSplitPane, BorderLayout.CENTER);
        add(getBottomPane(), BorderLayout.SOUTH);

        setControlsExpanded(true);
        getMsgTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                    updateMsgDetails();
            }
        });
    }

    /**
     * Load the last split location and use it to set the current split location, if applicable.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        if (logSplitPane == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    double lastSplitLocation = Double.parseDouble(preferences.getString(SPLIT_PROPERTY_NAME));
                    logSplitPane.setDividerLocation(lastSplitLocation);
                } catch (Exception ex) {
                    logSplitPane.setDividerLocation(200);
                }
            }
        });
    }

    private void init() {
        final ActionListener l = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };

        controlPanel.viaAuditLookupPolicyRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange("source_vialookup",!controlPanel.viaAuditLookupPolicyRadioButton.isSelected(),controlPanel.viaAuditLookupPolicyRadioButton.isSelected());
            }
        });
        controlPanel.configureAuditLookupPolicyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String lookupPolicyGuid = getLookupPolicyGuid();
                if(lookupPolicyGuid==null){
                    DialogDisplayer.showMessageDialog(null, "Lookup policy not configured.","Configure Lookup Policy",JOptionPane.INFORMATION_MESSAGE, null);
                    return;
                }
                try {
                    Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(lookupPolicyGuid);
                    PolicyEntityNode node = new PolicyEntityNode(new PolicyHeader(policy));
                    Action editAction = new EditPolicyAction(node, true);
                    editAction.actionPerformed(null);
                    // bring manager window to front
                    TopComponents windowManager = TopComponents.getInstance();
                    windowManager.getTopParent().toFront();
                } catch (FindException e1) {
                    DialogDisplayer.showMessageDialog(null, "Lookup policy cannot be found.","Configure Lookup Policy",JOptionPane.INFORMATION_MESSAGE, null);
                    logger.warning("Failed to edit lookup policy");
                }

            }
        });

        //initialize time range panel widgets
        controlPanel.durationButton.addActionListener(l);
        controlPanel.timeRangeButton.addActionListener(l);

        controlPanel.hoursTextField.setDocument(new NumberField(4));
        controlPanel.minutesTextField.setDocument(new NumberField(2));

        controlPanel.autoRefreshCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                durationAutoRefresh = controlPanel.autoRefreshCheckBox.isSelected();
                updateLogAutoRefresh();
            }
        });

        //initizlize additional search field widgets
        controlPanel.levelComboBox.setModel(new DefaultComboBoxModel(LogLevelOption.values()));
        controlPanel.levelComboBox.setSelectedItem(LogLevelOption.WARNING.toString());
        controlPanel.auditTypeComboBox.setModel(new DefaultComboBoxModel(AuditType.values()));
        controlPanel.auditTypeComboBox.setSelectedItem(AuditType.ALL.toString());

        controlPanel.entityTypeComboBox.setModel(new DefaultComboBoxModel(getAllEntities().keySet().toArray()));
        controlPanel.entityTypeComboBox.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (isSelected) {
                    setBackground(list.getSelectionBackground());
                    setForeground(list.getSelectionForeground());

                    if (index >= 0) {
                        String tooltips;
                        if (index <=1) {
                            tooltips = index == 0? "Any entity types" : "Entity type is not specified.";
                        } else {
                            String entityType = (String) getAllEntities().keySet().toArray()[index];
                            tooltips = getAllEntities().get(entityType); // tooltips will be set to the corresponding entity class name.
                        }
                        list.setToolTipText(tooltips);
                    }
                } else {
                    setBackground(list.getBackground());
                    setForeground(list.getForeground());
                }
                setFont(list.getFont());
                setText(value.toString());
                return this;
            }
        });

        controlPanel.auditTypeComboBox.addItemListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        }));


        InputValidator inputValidator = new InputValidator(this, "LogPanel Control Panel");
        inputValidator.constrainTextField(controlPanel.nodeTextField, new InputValidator.ComponentValidationRule(controlPanel.nodeTextField) {
            @Override
            public String getValidationError() {
                String nodeName = controlPanel.nodeTextField.getText();
                if(nodeName == null || nodeName.length() <= 0) return null;

                Registry reg = Registry.getDefault();
                if (!reg.isAdminContextPresent()) return null;

                ClusterStatusAdmin clusterStatusAdmin = reg.getClusterStatusAdmin();
                ClusterNodeInfo[] clusterNodes;
                try {
                    clusterNodes = clusterStatusAdmin.getClusterStatus();
                    if (clusterNodes == null) return null;

                    for (ClusterNodeInfo nodeInfo : clusterNodes) {
                        if (nodeInfo.getName().equals(nodeName)) {
                            return null;
                        }
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Unable to find cluster status from server.", e);
                    return null;
                }
                return "Node name does not exist in cluster and will be ignored.";
            }
        });
        inputValidator.validateWhenDocumentChanges(controlPanel.nodeTextField);

        controlPanel.validateSignaturesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSignatureValidationState(controlPanel.validateSignaturesCheckBox.isSelected());
            }
        });

        getFilteredLogTableSorter().setValidationNoLongerRunningCallback(new Runnable() {
            @Override
            public void run() {
                //we are on the UI thread
                validationIsRunning = false;

                // update currently displayed rows.
                final Rectangle viewRect = getMsgTablePane().getViewport().getViewRect();
                final int firstRow = getMsgTable().rowAtPoint(new Point(0, viewRect.y));
                final int lastRow = getMsgTable().rowAtPoint(new Point(0, viewRect.y + viewRect.height - 1));
                if (firstRow != -1 && lastRow != -1) {
                    getFilteredLogTableSorter().fireTableRowsUpdated(firstRow, lastRow);
                } else {
                    //there is not a lot of data if there is no final row
                    getFilteredLogTableSorter().fireTableDataChanged();
                }

                setSignatureStatusText();
            }
        });

        getFilteredLogTableSorter().setValidationHasStartedCallback(new Runnable() {
            @Override
            public void run() {
                validationIsRunning = true;
                setSignatureStatusText();
            }
        });

        getSearchButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controlPanel.validateSignaturesCheckBox.setSelected(false);
                setSignatureValidationState(false);
                setDataFromControlPanel();
                savePreferences();
                updateControlState();
                updateMsgTotal();

                //clear the details for the currently selected audit record
                getMsgDetails().setText("");
                getAssociatedLogsTable().getTableSorter().clear();
                getRequestXmlTextArea().setText("");
                getResponseXmlTextArea().setText("");
                displayedLogMessage = null;

                setCautionIndicatorPanelState();
            }
        });


        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent actionEvent) {
                auditLogTableSorterModel.cancelRefresh();
            }
        });

        getClearButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearSearchParameters();
            }
        });

        applyPreferences();

        setCautionIndicatorPanelState();
    }

    private void clearSearchParameters() {
        controlPanel.serviceTextField.setText("");
        controlPanel.messageTextField.setText("");
        controlPanel.requestIdTextField.setText("");
        controlPanel.userIdOrDnTextField.setText("");
        controlPanel.userNameTextField.setText("");
        controlPanel.nodeTextField.setText("");
        controlPanel.levelComboBox.setSelectedItem(LogLevelOption.ALL);
        controlPanel.auditTypeComboBox.setSelectedItem(AuditType.ALL);

        controlPanel.entityIdTextField.setText("");
        controlPanel.auditCodeTextField.setText("");
        controlPanel.operationTextField.setText("");
        controlPanel.entityTypeComboBox.setSelectedItem(EntityType.ANY.getName());
    }

    /**
     * Turn on or off signature validation in the model based on the value of the validate signatures checkbox.
     * Updates the status text.
     */
    private void setSignatureValidationState(boolean validate) {
        final AuditLogTableSorterModel auditModel = getFilteredLogTableSorter();
        if (validate) {
            auditModel.setVerifySignatures(true);
        } else {
            auditModel.setVerifySignatures(false);
        }
        setSignatureStatusText();
    }

    private void setCautionIndicatorPanelState() {
        // Display the visual indication message, "Caution! Constraint may exclude some events", if any search criteria applied.
        // controlPanel.cautionIndicatorPanel.setVisible(isAuditType && hasSearchCriteriaApplied());  // Note: this indicator is not for Gateway Logs Events.

        final Color bgColor;
        final boolean showWarning = hasSearchCriteriaApplied();
        if (showWarning) {
            bgColor = new Color(255, 255, 225);
            controlPanel.cautionTextField.setVisible(true);
        } else {
            bgColor = controlPanel.mainPanel.getBackground();
            controlPanel.cautionTextField.setVisible(false);
        }
        controlPanel.cautionIndicatorPanel.setBackground(bgColor);
    }

    /**
     *  Check if there is any search criterion applied.  Make sure this method is called after all search criteria data have been collected.
     * @return true if there is at least one search criterion applied.
     */
    private boolean hasSearchCriteriaApplied() {
        return
            (! logLevelOption.getLevel().equals(Level.ALL)) ||
            (! auditType.equals(AuditType.ALL)) ||
            (! isNullOrEmpty(serviceName)) ||
            (! isNullOrEmpty(message)) ||
            (! isNullOrEmpty(node)) ||
            (! isNullOrEmpty(userName)) ||
            (! isNullOrEmpty(userIdOrDn)) ||
            (! isNullOrEmpty(requestId)) ||
            (! "<any>".equals(entityTypeName)) ||
            (entityId != null) ||
            (messageId != null) ||
            (! isNullOrEmpty(operation))  ;
    }

    /**
     * Check if a string variable is null or empty or just contains white spaces.
     * @param str: the string to be checked.
     * @return true if the string is null or empty, or just consists of white spaces.
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Get all entities with the following information, entity type names and corresponding entity class names.
     * @return a sorted map containing the information of all entities.
     */
    private TreeMap<String, String> getAllEntities() {
        if (entitiesMap == null) {
            entitiesMap = new TreeMap<String, String>();

            if (Registry.getDefault().isAdminContextPresent()) {
                AuditAdmin auditAdmin = Registry.getDefault().getAuditAdmin();
                String shortName;
                for (String entityClassName: auditAdmin.getAllEntityClassNames()) {
                    shortName = getShortName(entityClassName);
                    if (shortName != null && !shortName.trim().isEmpty()) {
                        entitiesMap.put(shortName, entityClassName);
                    }
                }
            }

            // Add two special cases
            entitiesMap.put("<any>", null);      // For searching any entity types
            entitiesMap.put("<none>", "<none>"); // For searching entity types specified as "<none>"
        }
        return entitiesMap;
    }

    /**
     * Get the short name in the long entity class name.
     * For example, "PublishedService" is the one returned by this method from the class name, "com.l7tech.gateway.common.service.PublishedService".
     * 
     * @param entityClassName: the name of the entity class
     * @return: the short name of the entity class name.
     */
    private String getShortName(String entityClassName) {
        if (entityClassName == null) return null;

        final int dotIdx = entityClassName.lastIndexOf('.');
        if (dotIdx < 0) return entityClassName;

        return entityClassName.substring(dotIdx + 1);
    }

    private void enableOrDisableComponents() {
        Utilities.setEnabled( getControlPane(), connected);
        if (connected) {
            //don't want cancel button enabled when radio buttons are selected
            getCancelButton().setEnabled(false);
            controlPanel.hoursTextField.setEnabled(controlPanel.durationButton.isSelected());
            controlPanel.minutesTextField.setEnabled(controlPanel.durationButton.isSelected());
            controlPanel.autoRefreshCheckBox.setEnabled(controlPanel.durationButton.isSelected());
            controlPanel.timeRangePicker.setEnabled(controlPanel.timeRangeButton.isSelected());

            /**
             * Enable or disable search fields such as Request ID, Entity Type, and Entity ID, depending on Audit Type is chosen.
             * If Audit Type is System Audit, then Request ID search field will be enabled and entity search fields will be disabled.
             * If Audit Type is Admin Audit, then Request ID search field will be disable and entity search fields will be enabled.
             */
            final AuditType selected = (AuditType) controlPanel.auditTypeComboBox.getSelectedItem();
            final boolean adminAuditSearchEnabled = AuditType.ALL.equals(selected) || AuditType.ADMIN.equals(selected);
            final boolean messageAuditSearchEnabled = AuditType.ALL.equals(selected) || AuditType.MESSAGE.equals(selected);

            controlPanel.entityTypeLabel.setEnabled(adminAuditSearchEnabled);
            controlPanel.entityTypeComboBox.setEnabled(adminAuditSearchEnabled);
            controlPanel.entityIdLabel.setEnabled(adminAuditSearchEnabled);
            controlPanel.entityIdTextField.setEnabled(adminAuditSearchEnabled);

            controlPanel.requestIdLabel.setEnabled(messageAuditSearchEnabled);
            controlPanel.requestIdTextField.setEnabled(messageAuditSearchEnabled);
            controlPanel.operationLabel.setEnabled(messageAuditSearchEnabled);
            controlPanel.operationTextField.setEnabled(messageAuditSearchEnabled);

            // prevent auto refresh from happening if user switches to time range instead of duration
            if(controlPanel.durationButton.isSelected()){
                getLogsRefreshTimer().restart();
            }else if (getLogsRefreshTimer().isRunning()){
                getLogsRefreshTimer().stop();
            }
        }
    }

    /**
     * Sets the data model using values in the audit events control panel.
     */
    private void setDataFromControlPanel() {
        if (controlPanel.durationButton.isSelected()) {
            retrievalMode = RetrievalMode.DURATION;
        } else if (controlPanel.timeRangeButton.isSelected()) {
            retrievalMode = RetrievalMode.TIME_RANGE;
        }

        try {
            long hours = 0L;
            if (controlPanel.hoursTextField.getText().length() != 0) {
                hours = Long.parseLong(controlPanel.hoursTextField.getText());
            }
            long minutes = 0L;
            if (controlPanel.minutesTextField.getText().length() != 0) {
                minutes = Long.parseLong(controlPanel.minutesTextField.getText());
            }
            durationMillis = hours * MILLIS_IN_HOUR + minutes * MILLIS_IN_MINUTE;
        } catch (NumberFormatException e) {
            durationMillis = 0L;
        }

        durationAutoRefresh = controlPanel.autoRefreshCheckBox.isSelected();
        timeRangeStart = controlPanel.timeRangePicker.getStartTime();
        timeRangeEnd = controlPanel.timeRangePicker.getEndTime();
        timeRangeTimeZone = controlPanel.timeRangePicker.getTimeZone();

        logLevelOption = (LogLevelOption) controlPanel.levelComboBox.getSelectedItem();
        serviceName = controlPanel.serviceTextField.getText();
        message = controlPanel.messageTextField.getText();
        auditType = (AuditType) controlPanel.auditTypeComboBox.getSelectedItem();
        node = controlPanel.nodeTextField.getText();

        // Special case: if the Request ID search field is disabled, this means the search criterion is not applied.
        requestId = controlPanel.requestIdTextField.isEnabled()? controlPanel.requestIdTextField.getText() : null;

        userName = controlPanel.userNameTextField.getText();
        userIdOrDn = controlPanel.userIdOrDnTextField.getText();

        getFromPolicy = controlPanel.viaAuditLookupPolicyRadioButton.isSelected();

        try {
            String msgIdTxt = controlPanel.auditCodeTextField.getText();
            if (msgIdTxt == null || msgIdTxt.trim().isEmpty()) {
                messageId = null;
            } else {
                messageId = Integer.parseInt(msgIdTxt);
            }
        } catch (NumberFormatException e) {
            messageId = Integer.MIN_VALUE; // This case presents  Invalid Message Id
        }
//        paramValue = controlPanel.paramValueTextField.getText();

        //Special case: if the entity search fields (Entity Type and Entity ID) are disabled, this means the entity search criteria are not applied.
        entityTypeName = controlPanel.entityTypeComboBox.isEnabled()? (String) controlPanel.entityTypeComboBox.getSelectedItem() : "";

        if (controlPanel.entityIdTextField.isEnabled()) {
            try {
                String entityIdTxt = controlPanel.entityIdTextField.getText();
                if (entityIdTxt == null || entityIdTxt.trim().isEmpty()) {
                    entityId = null;
                } else {
                    entityId = Long.parseLong(entityIdTxt);
                }
            } catch (NumberFormatException e) {
                entityId = Long.MIN_VALUE; // This case presents Invalid Entity Id.
            }
        } else {
            entityId = null;
        }


        // Special case: if the operation search field is disabled, this means the search criterion is not applied.
        operation = controlPanel.operationTextField.isEnabled()? controlPanel.operationTextField.getText() : null;
    }

    /**
     * Populates the control panel using values in the audit events data model.
     */
    private void setControlPanelFromData() {
        if (retrievalMode == RetrievalMode.DURATION) {
            controlPanel.durationButton.setSelected(true);
        } else if (retrievalMode == RetrievalMode.TIME_RANGE) {
            controlPanel.timeRangeButton.setSelected(true);
        }
        final long hours = durationMillis / MILLIS_IN_HOUR;
        final long minutes = (durationMillis - hours * MILLIS_IN_HOUR) / MILLIS_IN_MINUTE;
        controlPanel.hoursTextField.setText(Long.toString(hours));
        controlPanel.minutesTextField.setText(Long.toString(minutes));
        controlPanel.autoRefreshCheckBox.setSelected(durationAutoRefresh);
        controlPanel.timeRangePicker.setStartTime(timeRangeStart);
        controlPanel.timeRangePicker.setEndTime(timeRangeEnd);
        if (timeRangeTimeZone != null) controlPanel.timeRangePicker.setTimeZone(timeRangeTimeZone, true);

        if (logLevelOption != null) controlPanel.levelComboBox.setSelectedItem(logLevelOption);
        controlPanel.serviceTextField.setText(serviceName);
        controlPanel.messageTextField.setText(message);
        if (auditType != null) controlPanel.auditTypeComboBox.setSelectedItem(auditType);
        controlPanel.nodeTextField.setText(node);
        controlPanel.requestIdTextField.setText(requestId);
        controlPanel.userNameTextField.setText(userName);
        controlPanel.userIdOrDnTextField.setText(userIdOrDn);

        if (messageId != null) {
            String msgIdTxt = messageId.equals(Integer.MIN_VALUE)? preferences.getString(SsmPreferences.AUDIT_WINDOW_MESSAGE_ID) : messageId.toString();
            controlPanel.auditCodeTextField.setText(msgIdTxt);
        }

//        controlPanel.paramValueTextField.setText(paramValue);
        controlPanel.entityTypeComboBox.setSelectedItem(entityTypeName);

        if (entityId != null) {
            String entityIdTxt = entityId.equals(Long.MIN_VALUE)? preferences.getString(SsmPreferences.AUDIT_WINDOW_ENTITY_ID) : entityId.toString();
            controlPanel.entityIdTextField.setText(entityIdTxt);
        }

        controlPanel.operationTextField.setText(operation);

        controlPanel.viaAuditLookupPolicyRadioButton.setSelected(getFromPolicy && controlPanel.viaAuditLookupPolicyRadioButton.isEnabled());
        enableOrDisableComponents();
    }

    /**
     * Applies application preferences to the current state.
     * <p/>
     * <p>Default values when preference not available or invalid:
     * <ul>
     * <li>retrieval mode - by duration
     * <li>duration - 3 hours
     * <li>time range - from now to now
     * <li>time zone - default time zone on this host machine
     * </ul>
     */
    private void applyPreferences() {
        retrievalMode = RetrievalMode.DURATION;
        try {
            retrievalMode = RetrievalMode.valueOf(preferences.getString(SsmPreferences.AUDIT_WINDOW_RETRIEVAL_MODE));
        } catch (NullPointerException e) {
        } catch (IllegalArgumentException e) {
        }

        durationMillis = 3L * MILLIS_IN_HOUR;
        try {
            durationMillis = Long.parseLong(preferences.getString(SsmPreferences.AUDIT_WINDOW_DURATION_MILLIS));
        } catch (NumberFormatException e) {
        }

        durationAutoRefresh = true;
        String s = preferences.getString(SsmPreferences.AUDIT_WINDOW_DURATION_AUTO_REFRESH);
        if (s != null) {
            durationAutoRefresh = Boolean.parseBoolean(s);
        }

        final Date now = new Date();
        timeRangeStart = now;
        try {
            timeRangeStart = new Date(Long.parseLong(preferences.getString(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_START)));
        } catch (NumberFormatException e) {
        }

        timeRangeEnd = now;
        try {
            timeRangeEnd = new Date(Long.parseLong(preferences.getString(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_END)));
        } catch (NumberFormatException e) {
        }

        timeRangeTimeZone = TimeZone.getDefault();
        final String timeZoneId = preferences.getString(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_TIMEZONE);
        if (timeZoneId != null) {
            if (Arrays.asList(TimeZone.getAvailableIDs()).contains(timeZoneId)) {
                timeRangeTimeZone = TimeZone.getTimeZone(timeZoneId);
            }
        }

        final String logLevelProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_LOG_LEVEL);
        if (logLevelProperty != null) {
            logLevelOption = LogLevelOption.valueOf(logLevelProperty);
        } else {
            logLevelOption = LogLevelOption.ALL;
        }

        final String serviceNameProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_SERVICE_NAME);
        if (serviceNameProperty != null) {
            serviceName = serviceNameProperty;
        }

        final String messageProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_MESSAGE);
        if (messageProperty != null) {
            message = messageProperty;
        }

        final String auditTypeProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_AUDIT_TYPE);
        if (auditTypeProperty != null) {
            auditType = AuditType.valueOf(auditTypeProperty);
        } else {
            auditType = AuditType.ALL;
        }

        final String nodeProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_NODE);
        if (nodeProperty != null) {
            node = nodeProperty;
        }

        final String requestIdProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_REQUEST_ID);
        if (requestIdProperty != null) {
            requestId = requestIdProperty;
        }

        final String userNameProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_USER_NAME);
        if (userNameProperty != null) {
            userName = userNameProperty;
        }

        final String userIdOrDnProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_USER_ID_OR_DN);
        if (userIdOrDnProperty != null) {
            userIdOrDn = userIdOrDnProperty;
        }

        final String messageIdProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_MESSAGE_ID);
        if (messageIdProperty != null) {
            try {
                messageId = Integer.parseInt(messageIdProperty);
            } catch (NumberFormatException e) {
                messageId = Integer.MIN_VALUE; // This case represents Invalid Message Id
            }
        } else {
            messageId = null; // null = Any
        }

//            final String paramValueProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_PARAM_VALUE);
//            if (paramValueProperty != null) {
//                paramValue = paramValueProperty;
//            }

        final String entityTypeProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_ENTITY_TYPE);
        if (entityTypeProperty != null) {
            entityTypeName = entityTypeProperty;
        } else {
            entityTypeName = getAllEntities().keySet().toArray(new String[]{})[0];  // get the first item from the entity type list
        }

        final String entityIdProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_ENTITY_ID);
        if (entityIdProperty != null) {
            try {
                entityId = Long.parseLong(entityIdProperty);
            } catch (NumberFormatException e) {
                entityId = Long.MIN_VALUE; // This case represents Invalid Entity Id
            }
        } else {
            entityId = null; // null = Any
        }

        final String operationProperty = preferences.getString(SsmPreferences.AUDIT_WINDOW_OPERATION);
        if (requestIdProperty != null) {
            operation = operationProperty;
        }

        final String useLookupString = preferences.getString(SsmPreferences.AUDIT_WINDOW_USE_LOOKUP_POLICY);
        if(useLookupString!=null){
            getFromPolicy = Boolean.valueOf(useLookupString);
        }

        setControlPanelFromData();
    }

    /**
     * Saves the current state to application preferences.
     */
    private void savePreferences() {
        if (retrievalMode != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_RETRIEVAL_MODE, retrievalMode.name());
        }
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_DURATION_MILLIS, Long.toString(durationMillis));
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_DURATION_AUTO_REFRESH, Boolean.toString(durationAutoRefresh));
        if (timeRangeStart != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_START, Long.toString(timeRangeStart.getTime()));
        }
        if (timeRangeEnd != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_END, Long.toString(timeRangeEnd.getTime()));
        }
        if (timeRangeTimeZone != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_TIME_RANGE_TIMEZONE, timeRangeTimeZone.getID());
        }

        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_LOG_LEVEL, logLevelOption.toString().toUpperCase());
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_SERVICE_NAME, serviceName);
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_MESSAGE, message);
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_AUDIT_TYPE, auditType.toString().toUpperCase());
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_NODE, node);

        if (requestId != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_REQUEST_ID, requestId);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_REQUEST_ID);
        }

        if (userName != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_USER_NAME, userName);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_USER_NAME);
        }
        if (userIdOrDn != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_USER_ID_OR_DN, userIdOrDn);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_USER_ID_OR_DN);
        }
        if (messageId != null) {
            String msgIdPreference = messageId.equals(Integer.MIN_VALUE)? controlPanel.auditCodeTextField.getText() : messageId.toString();
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_MESSAGE_ID, msgIdPreference);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_MESSAGE_ID);
        }
        if (paramValue != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_PARAM_VALUE, paramValue);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_PARAM_VALUE);
        }
        if (entityTypeName != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_ENTITY_TYPE, entityTypeName);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_ENTITY_TYPE);
        }
        if (entityId != null) {
            String entityIdPreference = entityId.equals(Long.MIN_VALUE)? controlPanel.entityIdTextField.getText() : entityId.toString();
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_ENTITY_ID, entityIdPreference);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_ENTITY_ID);
        }
        if (operation != null) {
            preferences.putProperty(SsmPreferences.AUDIT_WINDOW_OPERATION, operation);
        } else {
            preferences.remove(SsmPreferences.AUDIT_WINDOW_OPERATION);
        }
        preferences.putProperty(SsmPreferences.AUDIT_WINDOW_USE_LOOKUP_POLICY,Boolean.toString(controlPanel.viaAuditLookupPolicyRadioButton.isSelected()));
    }

    private void updateControlState() {
        getMsgProgressBar().setVisible(true);   // Shows progress bar only upon full retrieval; not upon incremental auto-refresh.
        clearLogCache();
        if (retrievalMode == RetrievalMode.DURATION) {
            getFilteredLogTableSorter().setTimeZone(null);
            refreshLogs(true);
        } else if (retrievalMode == RetrievalMode.TIME_RANGE) {
            auditLogTableSorterModel.setTimeZone( timeRangeTimeZone );
            updateLogAutoRefresh();
            updateViewSelection();
        }

        setHint(isAutoRefreshEffective() ? "Auto-Refresh" : null);
    }

    private void clearLogCache() {
        getFilteredLogTableSorter().clearLogCache();
        cachedLogMessages.clear();
    }

    /**
     * Set the status hint, e.g. Disconnected, Auto-Refresh, etc
     */
    private void setHint(String hintText) {
        JLabel hintLabel = getLastUpdateTimeLabel();
        String currentLabel = hintLabel.getText();
        int hintIndex = currentLabel.lastIndexOf('[');
        if (hintText == null || hintText.length() == 0) { // then clear
            if (hintIndex > 0) {
                hintLabel.setText(currentLabel.substring(0, hintIndex - 1));
            }
        } else { // set hint to given text
            String newLabel = hintIndex > 0 ? currentLabel.substring(0, hintIndex - 1) : currentLabel;
            newLabel = newLabel.trim() + " [" + hintText + "]   ";
            hintLabel.setText(newLabel);
        }
    }

    public boolean getControlsExpanded() {
        boolean expanded = false;

        if (selectionSplitPane != null) {
            expanded = selectionSplitPane.getDividerLocation() >= 5;
        }

        return expanded;
    }

    public void setControlsExpanded(boolean expanded) {
        if (selectionSplitPane != null) {
            if (expanded) {
                getControlPane().setMinimumSize(new Dimension(0, 20));
                selectionSplitPane.setDividerLocation((int) getControlPane().getPreferredSize().getHeight());
            } else {
                getControlPane().setMinimumSize(new Dimension(0, 0));
                // We set to preferred size first to ensure this is the "last position"
                // if this is not done the "expand" button doesn't work
                selectionSplitPane.setDividerLocation((int) getControlPane().getPreferredSize().getHeight());
                selectionSplitPane.setDividerLocation(0);
            }
        }
    }

    public boolean getDetailsExpanded() {
        boolean expanded = false;

        if (logSplitPane != null) {
            expanded = (double) logSplitPane.getDividerLocation() <= (logSplitPane.getSize().getHeight() - 25.0);
        }

        return expanded;
    }

    public void setDetailsExpanded(boolean expanded) {
        if (logSplitPane != null) {
            if (expanded) {
                logSplitPane.setDividerLocation(0.69);
            } else {
                logSplitPane.setDividerLocation(1.0);
            }
        }
    }

    public boolean isAutoRefreshEffective() {
        Window pWin = SwingUtilities.getWindowAncestor(this);
        return pWin != null
                && pWin.isVisible()
                && retrievalMode == RetrievalMode.DURATION
                && durationAutoRefresh;
    }

    private void updateLogAutoRefresh() {
        if (isAutoRefreshEffective()) {
            setHint("Auto-Refresh");
            getLogsRefreshTimer().start();
        } else {
            setHint(null);
            getLogsRefreshTimer().stop();
        }
    }

    private void updateViewSelection() {
        refreshLogs(timeRangeStart, timeRangeEnd, true);
    }

    private AuditMessage getCachedLogMessage( final AbstractAuditMessage logMessage ) {
        AuditMessage auditLogMessage = logMessage instanceof AuditMessage ? (AuditMessage) logMessage : null;

        if ( auditLogMessage == null ) {
            SoftReference<AuditMessage> messageRef = this.cachedLogMessages.get( logMessage.getMsgNumber() );
            if ( messageRef != null ) {
                auditLogMessage = messageRef.get();
            }
        }

        return auditLogMessage;
    }

    private AuditMessage cacheLogMessage( final AbstractAuditMessage logMessage, final AuditMessage auditLogMessage ) {
        auditLogMessage.setNodeName(logMessage.getNodeName());
        this.cachedLogMessages.put( logMessage.getMsgNumber(), new SoftReference<AuditMessage>( auditLogMessage ) );
        return auditLogMessage;
    }

    private void doUpdateMsgDetails( final AbstractAuditMessage logMessage, final Functions.UnaryVoid<AuditRecord> auditLoadedCallback) {
        AuditMessage auditLogMessage = getCachedLogMessage( logMessage );
        if ( auditLogMessage != null ) {
            updateMsgDetails( auditLogMessage );
        } else {
            //then we need to retrieve it from the SSG if possible
            if ( logMessage instanceof AuditHeaderMessage ) {
                getMsgProgressBar().setVisible(true);
                final AuditRecordWorker auditRecordWorker = new AuditRecordWorker(Registry.getDefault().getAuditAdmin(), (AuditHeaderMessage)logMessage) {
                    @Override
                    public void finished() {
                        final AuditMessage fullLogMessage = (AuditMessage) this.get();
                        if ( fullLogMessage != null ) {
                            doUpdateMsgDetails( cacheLogMessage( logMessage, fullLogMessage ), null);
                            final AuditRecord auditRecord = fullLogMessage.getAuditRecord();
                            if (auditRecord != null) {
                                auditLoadedCallback.call(auditRecord);
                            }
                        }
                        getMsgProgressBar().setVisible(false);
                    }
                };
                auditRecordWorker.start();
            }
        }
    }

    private void updateMsgDetails( final AbstractAuditMessage lm ) {
        String msg = "";

        TimeZone timeZone = retrievalMode == RetrievalMode.TIME_RANGE ? timeRangeTimeZone : null;
        SimpleDateFormat sdf = timeZone==null || timeZone.equals( TimeZone.getDefault() ) ?
                new SimpleDateFormat( DATE_FORMAT_PATTERN ):
                new SimpleDateFormat( DATE_FORMAT_ZONE_PATTERN );
        if ( timeZone != null ) sdf.setTimeZone( timeZone );

        final int maxWidth = ((lm instanceof AuditMessage) && (((AuditMessage) lm).getAuditRecord() instanceof AdminAuditRecord))? 20 : 15; // "Identity Provider ID" width = 20 and "Audit Record ID" width = 15
        msg += nonull(TextUtils.pad("Node", maxWidth) + ": ", lm.getNodeName());
        msg += nonull(TextUtils.pad("Time", maxWidth) + ": ", sdf.format( lm.getTimestamp() ));
        msg += nonull(TextUtils.pad("Severity", maxWidth) + ": ", lm.getSeverity());
        msg += nonule(TextUtils.pad("Request Id", maxWidth) + ": ", lm.getReqId());
        msg += nonull(TextUtils.pad("Message", maxWidth) + ": ", lm.getMsgDetails());
        if (lm instanceof AuditMessage ) {
            msg += nonull(TextUtils.pad("Audit Record ID", maxWidth) + ": ", ((AuditMessage) lm).getAuditRecord().getId());
        }

        if ( lm instanceof AuditMessage ) {
            AuditRecord arec = ((AuditMessage) lm).getAuditRecord();
            msg += "\n";

            boolean reqXmlVisible = false;
            boolean respXmlVisible = false;
            String reqXmlDisplayed = "";
            String respXmlDisplayed = "";
            if (arec instanceof AdminAuditRecord) {
                AdminAuditRecord aarec = (AdminAuditRecord) arec;
                msg += TextUtils.pad("Event Type", maxWidth) + ": Manager Action" + "\n";
                msg += TextUtils.pad("Admin User Name", maxWidth) + ": " + aarec.getUserName() + "\n";
                msg += TextUtils.pad("Admin User ID", maxWidth) + ": " + aarec.getUserId() + "\n";
                msg += TextUtils.pad("Identity Provider ID", maxWidth) + ": " + aarec.getIdentityProviderGoid() + "\n"; // "Identity Provider ID" width = 20
                msg += TextUtils.pad("Admin IP", maxWidth) + ": " + arec.getIpAddress() + "\n";
                msg += "\n";
                msg += TextUtils.pad("Action", maxWidth) + ": " + fixAction(aarec.getAction()) + "\n";
                if ( (int) AdminAuditRecord.ACTION_LOGIN != (int) aarec.getAction() &&
                        (int) AdminAuditRecord.ACTION_OTHER != (int) aarec.getAction() ) {
                    msg += TextUtils.pad("Entity Name", maxWidth) + ": " + arec.getName() + "\n";
                    msg += TextUtils.pad("Entity ID", maxWidth) + ": " + aarec.getEntityOid() + "\n";
                    msg += TextUtils.pad("Entity Type", maxWidth) + ": " + fixType(aarec.getEntityClassname()) + "\n";
                }
            } else if (arec instanceof MessageSummaryAuditRecord) {
                MessageSummaryAuditRecord sum = (MessageSummaryAuditRecord) arec;
                msg += TextUtils.pad("Event Type", maxWidth) + ": Message Summary" + "\n";
                msg += TextUtils.pad("Client IP", maxWidth) + ": " + arec.getIpAddress() + "\n";
                msg += TextUtils.pad("Service", maxWidth) + ": " + sum.getName() + "\n";
                msg += TextUtils.pad("Operation", maxWidth) + ": " + sum.getOperationName() + "\n";
                msg += TextUtils.pad("Rqst Length", maxWidth) + ": " + fixNegative(sum.getRequestContentLength(), "<Not Saved>") + "\n";
                msg += TextUtils.pad("Resp Length", maxWidth) + ": " + fixNegative(sum.getResponseContentLength(), "<Not Saved>") + "\n";
                msg += TextUtils.pad("Resp Status", maxWidth) + ": " + sum.getResponseHttpStatus() + "\n";
                msg += TextUtils.pad("Resp Time", maxWidth) + ": " + sum.getRoutingLatency() + "ms\n";
                msg += TextUtils.pad("User ID", maxWidth) + ": " + fixUserId(sum.getUserId()) + "\n";
                msg += TextUtils.pad("User Name", maxWidth) + ": " + sum.getUserName() + "\n";
                if (sum.getAuthenticationType() != null) {
                    msg += TextUtils.pad("Auth Method", maxWidth) + ": " + sum.getAuthenticationType().getName() + "\n";
                }

                MessageContextMapping[] mappings = sum.obtainMessageContextMappings();
                if (mappings != null && mappings.length > 0) {
                    StringBuilder sb = new StringBuilder("\nMessage Context Mappings\n");
                    boolean foundCustomMapping = false;
                    for (MessageContextMapping mapping : mappings) {
                        sb.append(TextUtils.pad("Mapping Key", maxWidth)).append(": ").append(mapping.getKey()).append("\n");
                        sb.append(TextUtils.pad("Mapping Value", maxWidth)).append(": ").append(mapping.getValue()).append("\n");
                        foundCustomMapping = true;
                    }

                    if (foundCustomMapping) {
                        msg += sb.toString();
                    }
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
                SystemAuditRecord sys = (SystemAuditRecord) arec;
                com.l7tech.gateway.common.Component component = fromId(sys.getComponentId());
                boolean isClient = component != null && component.isClientComponent();
                msg += TextUtils.pad("Event Type", maxWidth) + ": System Message" + "\n";
                msg += TextUtils.pad(isClient? "Client IP" : "Node IP", maxWidth) + ": " + arec.getIpAddress() + "\n";
                msg += TextUtils.pad("Action", maxWidth) + ": " + sys.getAction() + "\n";
                msg += TextUtils.pad("Component", maxWidth) + ": " + fixComponent(sys.getComponentId()) + "\n";
                if (isClient) {
                    msg += TextUtils.pad("User ID", maxWidth) + ": " + fixUserId(arec.getUserId()) + "\n";
                    msg += TextUtils.pad("User Name", maxWidth) + ": " + arec.getUserName() + "\n";
                }
                msg += TextUtils.pad("Entity name", maxWidth) + ": " + arec.getName() + "\n";
            } else {
                msg += TextUtils.pad("Event Type", maxWidth) + ": Unknown" + "\n";
                msg += TextUtils.pad("Entity name", maxWidth) + ": " + arec.getName() + "\n";
                msg += TextUtils.pad("IP Address", maxWidth) + ": " + arec.getIpAddress() + "\n";
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

            getMsgDetailsPane().setEnabledAt(1, associatedLogsItr.hasNext());
            List<AssociatedLog> associatedLogs = new ArrayList<AssociatedLog>();
            while (associatedLogsItr.hasNext()) {
                AuditDetail ad = (AuditDetail) associatedLogsItr.next();

                int id = ad.getMessageId();
                // TODO get the CellRenderer to display the user messages differently when id < 0 (add field to AssociatedLog class?)
                String associatedLogMessage = getMessageById(id);
                AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(id);
                String associatedLogLevel = message == null ? null : message.getLevelName();

                StringBuffer result = new StringBuffer();
                if (associatedLogMessage != null) {
                    MessageFormat mf = new MessageFormat(associatedLogMessage);
                    mf.format(ad.getParams(), result, new FieldPosition(0));
                }
                AssociatedLog al = new AssociatedLog(arec.getOid(), ad.getTime(), associatedLogLevel, result.toString(), ad.getException(), ad.getMessageId(), ad.getOrdinal());
                associatedLogs.add(al);
            }
            getAssociatedLogsTable().getTableSorter().setData(associatedLogs);
        }

        updateMsgDetailText( msg );
    }

    private void updateMsgDetailText( final String msg ) {
        // update the msg details field only if the content has changed.
        if (!msg.equals(getMsgDetails().getText())) {
            getMsgDetails().setText(msg);
            if (msg.length() > 0)
                // Scroll to top
                getMsgDetails().getCaret().setDot(1);
        }
    }

    /**
     * Only call from swing thread.
     */
    private void updateMsgDetails() {
        final int row = getMsgTable().getSelectedRow();

        if (row == -1) return;

        final TableModel model = getMsgTable().getModel();
        if (model instanceof AuditLogTableSorterModel) {
            final AuditLogTableSorterModel auditModel = (AuditLogTableSorterModel) model;
            final AbstractAuditMessage logHeader = auditModel.getLogMessageAtRow(row);
            if (logHeader == displayedLogMessage) return;
            displayedLogMessage = logHeader;

            //update the Sig column when the audit record has been loaded.
            final Functions.UnaryVoid<AuditRecord> auditRecordRetrievedCallback = new Functions.UnaryVoid<AuditRecord>() {
                @Override
                public void call(AuditRecord auditRecord) {
                    if (logHeader instanceof AuditHeaderMessage ) {
                        AuditHeaderMessage actualHeader = (AuditHeaderMessage) logHeader;
                        actualHeader.setSignatureDigest(auditRecord.computeSignatureDigest());
                        auditModel.fireTableRowsUpdated(row, row);
                    }
                }
            };

            doUpdateMsgDetails( logHeader, (logHeader instanceof AuditHeaderMessage)? auditRecordRetrievedCallback: null );
        } else {
            updateMsgDetailText( "" );
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
                } catch (FindException e) {
                    logger.log(Level.WARNING, "cannot get property", e);
                }
            }
            if (output == null) {
                AuditDetailMessage message = MessagesUtil.getAuditDetailMessageById(id);
                output = message == null ? null : message.getMessage();
            }
            cachedAuditMessages.put(id, output);
        }
        return output;
    }

    private String reformat(String xml) {
        try {
            boolean omitXmlDeclaration = !xml.startsWith("<?xml");
            Document node = XmlUtil.stringToDocument(xml);
            return nodeToFormattedString(node, node.getXmlEncoding(), omitXmlDeclaration);
        } catch (Exception e) {
            return xml;
        }
    }

    /**
     * Reformat a node (here it is a document) to a string using specified encoding and the setting of
     * omitting xml declaration.  The method is similar to the method nodeToString in {@link com.l7tech.util.DomUtils}
     *
     * @param node
     * @param encoding
     * @param omitXmlDeclaration
     * @return a formatted string
     * @throws IOException
     */
    private String nodeToFormattedString(Node node, String encoding, boolean omitXmlDeclaration) throws IOException {
        final StringWriter writer = new StringWriter(1024);
        try {
            XMLSerializer ser = new XMLSerializer();
            OutputFormat format = new OutputFormat();
            format.setIndent(4);
            format.setEncoding(encoding);
            format.setOmitXMLDeclaration(omitXmlDeclaration);
            ser.setOutputFormat(format);
            ser.setOutputCharStream(writer);

            if (node instanceof Document)
                ser.serialize((Document) node);
            else if (node instanceof Element)
                ser.serialize((Element) node);
            else
                throw new IllegalArgumentException("Node must be either a Document or an Element");

            return writer.toString();
        } finally {
            writer.close();
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

    private String fixComponent(int componentId) {
        com.l7tech.gateway.common.Component c = fromId(componentId);
        if (c == null) return "Unknown Component #" + componentId;
        StringBuilder ret = new StringBuilder(c.getName());
        while (c.getParent() != null && c.getParent() != c) {
            ret.insert(0, ": ");
            ret.insert(0, c.getParent().getName());
            c = c.getParent();
        }
        return ret.toString();
    }

    /**
     * Strip the "com.l7tech." from the start of a class name.
     */
    private String fixType(String entityClassname) {
        final String coml7tech = "com.l7tech.";
        if (entityClassname == null) {
            return "<unknown>";
        } else if (entityClassname.startsWith(coml7tech))
            return entityClassname.substring(coml7tech.length());
        return entityClassname;
    }

    private String fixUserId(String id) {
        return (id != null ? id : "<No ID>");
    }

    /**
     * Convert a single-character action into a human-readable String.
     */
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
            case AdminAuditRecord.ACTION_LOGOUT:
                return "Admin Logout";
            case AdminAuditRecord.ACTION_OTHER:
                return "Other";
            default:
                return "Unknown Action '" + action + "'";
        }
    }

    /**
     * Stop the refresh timer.
     */
    public void stopRefreshTimer() {
        getLogsRefreshTimer().stop();
    }

    /**
     * Performs the necessary initialization when the connection with the cluster is established.
     */
    public void onConnect() {
        connected = true;
        getFilteredLogTableSorter().onConnect();
        updateLogsRefreshTimerDelay();
        clearMsgTable();
        enableOrDisableComponents();
        updateControlState();
    }

    /**
     * Performs the necessary cleanup when the connection with the cluster went down.
     */
    public void onDisconnect() {
        connected = false;

        clearLogCache();
        getMsgDetails().setText("");
        getAssociatedLogsTable().getTableSorter().setData(new ArrayList<AssociatedLog>());
        getRequestXmlTextArea().setText("");
        getResponseXmlTextArea().setText("");
        unformattedRequestXml.setLength(0);
        unformattedResponseXml.setLength(0);

        getLogsRefreshTimer().stop();
        getMsgProgressBar().setVisible(false);
        getFilteredLogTableSorter().onDisconnect();

        setHint("Disconnected");
        enableOrDisableComponents();
    }

    /**
     * Return SelectPane property value
     *
     * @return JPanel
     */
    private JPanel getBottomPane() {
        if (bottomPane == null) {
            bottomPane = new JPanel();
            bottomPane.setLayout(new BorderLayout());
            bottomPane.add(getStatusPane(), BorderLayout.WEST);

            JPanel buttonPanel = new JPanel();
            //buttonPanel.add(getSearchButton());
            bottomPane.add(buttonPanel, BorderLayout.EAST);
        }

        return bottomPane;
    }

    String getLookupPolicyGuid (){
    //initialize source panel widgets
        boolean enabledLookupPolicy = false;
        try{
            return ClusterPropertyCrud.getClusterProperty(AUDIT_LOOKUP_POLICY_GUID_CLUSTER_PROP);
        }catch(FindException e){
            // failed to get lookup policy guid, disable button
        }
        return null;
    }

    /**
     * Return ControlPane property value
     *
     * @return JPanel
     */
    private JPanel getControlPane() {
        return controlPanel.mainPanel;
    }

    public JRadioButton getViaAuditLookupPolicyRadioButton(){
        return controlPanel.viaAuditLookupPolicyRadioButton;
    }
    /**
     * @return the label that shows the total number of the messages being displayed.
     */
    private JLabel getMsgTotal() {
        if (msgTotal == null) {
            msgTotal = new JLabel(MSG_TOTAL_PREFIX + "0");
            msgTotal.setFont(new java.awt.Font("Dialog", 0, 12));
            msgTotal.setAlignmentY( 0.0F );
        }
        return msgTotal;
    }

    private JLabel getSignatureStatusLabel() {
        if (sigStatusLabel == null) {
            sigStatusLabel = new JLabel();
            sigStatusLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            sigStatusLabel.setAlignmentY( 0.0F );
        }

        return sigStatusLabel;
    }

    private void setSignatureStatusText() {
        final JLabel statusLabel = getSignatureStatusLabel();

        StringBuilder builder = new StringBuilder();
        if (controlPanel.validateSignaturesCheckBox.isSelected()) {
            builder.append("Signature validation is on");
            if (validationIsRunning) {
                builder.append(" [In progress]");
            }
        }
        statusLabel.setText(builder.toString());
    }

    /**
     * @return the progress bar that shows message query is in progress
     */
    public JProgressBar getMsgProgressBar() {
        if (msgProgressBar == null) {
            msgProgressBar = new JProgressBar(SwingConstants.HORIZONTAL);
            msgProgressBar.setIndeterminate(true);
            msgProgressBar.setPreferredSize(new Dimension(80, 12));
            msgProgressBar.setMinimumSize(new Dimension(80, 12));
            msgProgressBar.setMaximumSize(new Dimension(80, 12));
            msgProgressBar.setVisible(false);
        }
        return msgProgressBar;
    }

    /**
     * Return MsgTable property value
     *
     * @return JTable. The model used is guaranteed to be an instance of AuditLogTableSorterModel
     */
    public JTable getMsgTable() {
        if (msgTable == null) {
            //create the table with an AuditLogTableSorterModel model
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
     *
     * @return JScrollPane
     */
    private JScrollPane getMsgTablePane() {
        if (msgTablePane == null) {
            msgTablePane = new JScrollPane();
            msgTablePane.setViewportView(getMsgTable());
            msgTablePane.getViewport().setBackground(getMsgTable().getBackground());
            msgTablePane.setMinimumSize(new Dimension(600, 40));
            msgTablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            // Add a component listener to keep the change of the split location.
            msgTablePane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    if (logSplitPane == null) {
                        return;
                    }
                    double logSplitPaneSplitLocation = (double) logSplitPane.getDividerLocation() / (double) (logSplitPane.getHeight() - logSplitPane.getDividerSize());
                    preferences.putProperty(SPLIT_PROPERTY_NAME, String.valueOf(logSplitPaneSplitLocation));
                }
            });
        }

        return msgTablePane;
    }

    /**
     * Return MsgDetailsPane property value
     *
     * @return JScrollPane
     */
    private JTabbedPane getMsgDetailsPane() {
        if (msgDetailsPane == null) {
            msgDetailsPane = new JTabbedPane();
            msgDetailsPane.addTab("Details", getDetailsScrollPane());
            msgDetailsPane.addTab("Associated Logs", getAssociatedLogsScrollPane());
            msgDetailsPane.addTab("Request", getRequestXmlPanel());
            msgDetailsPane.addTab("Response", getResponseXmlPanel());
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
        if (associatedLogsTable == null) {
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
     *
     * @return JTextArea
     */
    private JTextArea getMsgDetails() {
        if (msgDetails == null) {
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
            final JPanel holderSouthPanel = new JPanel();
            holderSouthPanel.setLayout(new BoxLayout(holderSouthPanel, BoxLayout.X_AXIS));
            holderSouthPanel.add(getRequestReformatCheckbox());
            holderSouthPanel.add(getInvokeRequestAVPolicyButton());
            holderSouthPanel.add(Box.createHorizontalGlue());
            requestXmlPanel.add(holderSouthPanel, BorderLayout.SOUTH);
        }
        return requestXmlPanel;
    }

    private JButton getInvokeRequestAVPolicyButton(){
        if(invokeRequestAVPolicyButton == null){
            invokeRequestAVPolicyButton = new JButton("Invoke Audit Viewer Policy");
            if(Registry.getDefault().isAdminContextPresent()){
                invokeRequestAVPolicyButton.setEnabled(enableInvokeButton());
                invokeRequestAVPolicyButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String output = getAVPolicyOutput();
                        if(output != null){
                            getRequestXmlTextArea().setText(output);
                            unformattedRequestXml.setLength(0);
                            unformattedRequestXml.append(output);
                            doReformat(getRequestXmlTextArea(), getRequestReformatCheckbox().isSelected(), unformattedRequestXml);
                        } else {
                            final String msg = "Error processing " + PolicyType.TAG_AUDIT_VIEWER + " policy.";
                            getRequestXmlTextArea().setText(msg);
                            unformattedRequestXml.setLength(0);
                            unformattedRequestXml.append(msg);
                        }
                        getRequestXmlTextArea().setCaretPosition(0);
                    }
                });
            } else {
                invokeRequestAVPolicyButton.setEnabled(false);
            }
        }

        return invokeRequestAVPolicyButton;
    }

    private JCheckBox getRequestReformatCheckbox() {
        if (requestReformatCheckbox == null) {
            requestReformatCheckbox = new JCheckBox("Reformat Request XML");
            requestReformatCheckbox.setSelected(true); //turn this on by default
            requestReformatCheckbox.addActionListener(new ActionListener() {
                @Override
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

            final JPanel holderSouthPanel = new JPanel();
            holderSouthPanel.setLayout(new BoxLayout(holderSouthPanel, BoxLayout.X_AXIS));
            holderSouthPanel.add(getResponseReformatCheckbox());
            holderSouthPanel.add(getInvokeResponseAVPolicyButton());
            responseXmlPanel.add(holderSouthPanel, BorderLayout.SOUTH);
        }
        return responseXmlPanel;
    }

    private JButton getInvokeResponseAVPolicyButton(){
        if(invokeResponseAVPolicyButton == null){
            invokeResponseAVPolicyButton = new JButton("Invoke Audit Viewer Policy");
            if(Registry.getDefault().isAdminContextPresent()){
                invokeResponseAVPolicyButton.setEnabled(enableInvokeButton());
                invokeResponseAVPolicyButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String output = getAVPolicyOutput();
                        if(output != null){
                            getResponseXmlTextArea().setText(output);
                            unformattedResponseXml.setLength(0);
                            unformattedResponseXml.append(output);
                            doReformat(getResponseXmlTextArea(), getResponseReformatCheckbox().isSelected(), unformattedResponseXml);
                        } else {
                            final String msg = "Error processing " + PolicyType.TAG_AUDIT_VIEWER + " policy.";
                            getResponseXmlTextArea().setText(msg);
                            unformattedResponseXml.setLength(0);
                            unformattedResponseXml.append(msg);
                        }
                        getRequestXmlTextArea().setCaretPosition(0);
                    }
                });
            } else {
                invokeResponseAVPolicyButton.setEnabled(false);
            }
        }

        return invokeResponseAVPolicyButton;
    }

    private boolean enableInvokeButton(){
        final boolean avPermGranted = Registry.getDefault().getSecurityProvider().hasPermission(
                new AttemptedOther(EntityType.AUDIT_RECORD, OtherOperationName.AUDIT_VIEWER_POLICY.getOperationName()));
        
        final boolean avPolicyIsActive;
        if(avPermGranted){
            avPolicyIsActive = Registry.getDefault().getAuditAdmin().isAuditViewerPolicyAvailable();
        } else {
            avPolicyIsActive = false;
        }

        return avPermGranted && avPolicyIsActive;
    }

    private String getAVPolicyOutput(){
        final int row = getMsgTable().getSelectedRow();

        if (row == -1) return null;

        final AuditLogTableSorterModel model = (AuditLogTableSorterModel) getMsgTable().getModel();
        final AbstractAuditMessage logHeader = model.getLogMessageAtRow(row);

        final boolean isRequest = getRequestXmlPanel() == getMsgDetailsPane().getSelectedComponent();
        final boolean isResponse = getResponseXmlPanel() == getMsgDetailsPane().getSelectedComponent();

        //either the request or response tab is active when button is pressed.
        if(!isRequest && !isResponse) return null;

        final AuditAdmin auditAdmin = Registry.getDefault().getAuditAdmin();
        try {
            return auditAdmin.invokeAuditViewerPolicyForMessage(logHeader.getMsgNumber(), isRequest);
        } catch (FindException e) {
            return ExceptionUtils.getMessage(e);
        } catch (AuditViewerPolicyNotAvailableException e) {
            invokeRequestAVPolicyButton.setEnabled(enableInvokeButton());
            invokeResponseAVPolicyButton.setEnabled(enableInvokeButton());
            return ExceptionUtils.getMessage(e) + ". Reopen the audit viewer if the policy is recreated or enabled.";
        }
    }

    private JCheckBox getResponseReformatCheckbox() {
        if (responseReformatCheckbox == null) {
            responseReformatCheckbox = new JCheckBox("Reformat Response XML");
            responseReformatCheckbox.setSelected(true); //turn this on by default
            responseReformatCheckbox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doReformat(getResponseXmlTextArea(), responseReformatCheckbox.isSelected(), unformattedResponseXml);
                }
            });
        }
        return responseReformatCheckbox;
    }

    /**
     * Updates the contents of textArea with the contents of unformattedTextBuffer, when the unformattedTextBuffer
     * contains text and the textArea is enabled. The contents of unformattedTextBuffer are formatted prior to updating
     * the textArea if format is true.
     * @param textArea text area to update
     * @param format true if contents of text area should be formatted
     * @param unformattedTextBuffer original unformatted audit contents. May be direct from audit viewer or the output
     * of the audit viewer policy.
     */
    private void doReformat(JTextArea textArea, boolean format, StringBuffer unformattedTextBuffer) {
        final String text = unformattedTextBuffer.toString();
        if (textArea.isEnabled() && textArea.isShowing() &&
                text != null && text.length() > 0) {
            if (format) {
                textArea.setText(reformat(unformattedTextBuffer.toString()));
            } else {
                textArea.setText(unformattedTextBuffer.toString());
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
     *
     * @return JPanel
     */
    private JPanel getStatusPane() {
        if (statusPane == null) {
            getMsgTotal().setAlignmentY(Component.CENTER_ALIGNMENT);
            getMsgProgressBar().setAlignmentY(Component.CENTER_ALIGNMENT);
            final JPanel msgTotalPanel = new JPanel();
            msgTotalPanel.setLayout(new BoxLayout(msgTotalPanel, BoxLayout.X_AXIS));
            msgTotalPanel.add(getMsgTotal());
            msgTotalPanel.add(Box.createHorizontalStrut(10));
            msgTotalPanel.add(getMsgProgressBar());
            msgTotalPanel.add(Box.createHorizontalGlue());

            msgTotalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            getLastUpdateTimeLabel().setAlignmentX(Component.LEFT_ALIGNMENT);

            final JPanel lowerPanel = new JPanel();
            lowerPanel.setLayout(new BoxLayout(lowerPanel, BoxLayout.X_AXIS));
            lowerPanel.add(getLastUpdateTimeLabel());
            lowerPanel.add(Box.createHorizontalStrut(10));
            lowerPanel.add(getSignatureStatusLabel());
            lowerPanel.add(Box.createHorizontalGlue());

            lowerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            statusPane = new JPanel();
            statusPane.setLayout(new BoxLayout(statusPane, BoxLayout.Y_AXIS));
            statusPane.add(Box.createVerticalGlue());
            statusPane.add(msgTotalPanel);
            statusPane.add(lowerPanel);
            statusPane.add(Box.createVerticalGlue());
        }

        return statusPane;
    }

    public JButton getSearchButton() {
        return controlPanel.searchButton;
    }

    public JButton getCancelButton() {
        return controlPanel.cancelButton;
    }

    public JButton getClearButton(){
        return controlPanel.clearButton;
    }

    /**
     * Return lastUpdateTimeLabel property value
     *
     * @return JLabel
     */
    private JLabel getLastUpdateTimeLabel() {
        if (lastUpdateTimeLabel == null) {
            lastUpdateTimeLabel = new JLabel();
            lastUpdateTimeLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            lastUpdateTimeLabel.setText("");
            lastUpdateTimeLabel.setAlignmentY( 0.0F );
        }

        return lastUpdateTimeLabel;
    }

    /**
     * Return LogColumnModel property value
     *
     * @return DefaultTableColumnModel
     */
    private DefaultTableColumnModel getLogColumnModel() {
        DefaultTableColumnModel columnModel = new DefaultTableColumnModel();

        // Preferred and inital widths.
        tableColumnWidths[LOG_SIGNATURE_COLUMN_INDEX] = 25;
        tableColumnWidths[LOG_MSG_NUMBER_COLUMN_INDEX] = 75;
        tableColumnWidths[LOG_NODE_NAME_COLUMN_INDEX] = 50;
        tableColumnWidths[LOG_TIMESTAMP_COLUMN_INDEX] = 140;
        tableColumnWidths[LOG_SEVERITY_COLUMN_INDEX] = 60;
        tableColumnWidths[LOG_SERVICE_COLUMN_INDEX] = 100;
        tableColumnWidths[LOG_MSG_DETAILS_COLUMN_INDEX] = 500;

        // Add columns according to configuration
        columnModel.addColumn(new TableColumn(LOG_SIGNATURE_COLUMN_INDEX, tableColumnWidths[LOG_SIGNATURE_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_MSG_NUMBER_COLUMN_INDEX, tableColumnWidths[LOG_MSG_NUMBER_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_NODE_NAME_COLUMN_INDEX, tableColumnWidths[LOG_NODE_NAME_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_TIMESTAMP_COLUMN_INDEX, tableColumnWidths[LOG_TIMESTAMP_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_SEVERITY_COLUMN_INDEX, tableColumnWidths[LOG_SEVERITY_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_SERVICE_COLUMN_INDEX, tableColumnWidths[LOG_SERVICE_COLUMN_INDEX]));
        columnModel.addColumn(new TableColumn(LOG_MSG_DETAILS_COLUMN_INDEX, tableColumnWidths[LOG_MSG_DETAILS_COLUMN_INDEX]));

        // Set headers
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn tc = columnModel.getColumn(i);
            tc.setMinWidth(20);
            tc.setHeaderRenderer(iconHeaderRenderer);
            tc.setHeaderValue(getLogTableModel().getColumnName(tc.getModelIndex()));
        }

        // Displays icon and tooltip in cells of digital signature column.
        final TableColumn signatureColumn = findTableModelColumn(columnModel, LOG_SIGNATURE_COLUMN_INDEX);
        if (signatureColumn != null) {
            signatureColumn.setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (value instanceof AuditLogTableSorterModel.DigitalSignatureUIState && comp instanceof JLabel) {
                        final AuditLogTableSorterModel.DigitalSignatureUIState state = (AuditLogTableSorterModel.DigitalSignatureUIState) value;
                        Icon icon = state.getIcon16();
                        if (state == AuditLogTableSorterModel.DigitalSignatureUIState.NONE) {
                            if (!isSignAudits(false)) {
                                icon = null;
                            }
                        }
                        final JLabel label = (JLabel) comp;
                        label.setIcon(icon);
                        label.setText(null);
                        label.setHorizontalAlignment(JLabel.CENTER);
                        label.setToolTipText(state.getDescription());
                    }
                    return comp;
                }
            });
        }

        // Tooltip for details
        findTableModelColumn(columnModel, LOG_MSG_DETAILS_COLUMN_INDEX).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (comp instanceof JLabel) {
                    String detailText = ((JLabel) comp).getText();
                    if (detailText == null || detailText.trim().length() == 0) {
                        ((JComponent) comp).setToolTipText(null);
                    } else {
                        ((JComponent) comp).setToolTipText(detailText);
                    }
                }
                return comp;
            }
        });

        return columnModel;
    }

    /**
     * Find a table column given its model index.
     *
     * @param columnModel      the table column model
     * @param columnModelIndex model index of the column
     * @return the table column; null if there is no column with such index
     */
    private static TableColumn findTableModelColumn(TableColumnModel columnModel, int columnModelIndex) {
        Enumeration<TableColumn> e = columnModel.getColumns();
        for (; e.hasMoreElements();) {
            TableColumn col = e.nextElement();
            if (col.getModelIndex() == columnModelIndex) {
                return col;
            }
        }
        return null;
    }

    /**
     * Return LogTableModelFilter property value
     *
     * @return FilteredLogTableModel
     */
    private AuditLogTableSorterModel getFilteredLogTableSorter() {
        if (auditLogTableSorterModel == null) {
            auditLogTableSorterModel = new AuditLogTableSorterModel(getLogTableModel());
        }
        return auditLogTableSorterModel;
    }

    /**
     * create the table model with log fields
     *
     * @return DefaultTableModel
     */
    private DefaultTableModel getLogTableModel() {
        if (logTableModel == null) {
            String[][] rows = new String[][]{};

            logTableModel = new DefaultTableModel(rows, COLUMN_NAMES) {
                @Override
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
     * @return String  The message number of the selected row in the log table.
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
                msgNumSelected = nodeId.toString().trim() + mesNum.toString().trim();
            }
        }

        return msgNumSelected;
    }

    public void refreshView() {
        if (connected) {
            if (retrievalMode == RetrievalMode.DURATION) {
                refreshLogs(true);
            } else {
                updateViewSelection();
            }
        }
    }

    /**
     * Performs the log retrieval. This function is called when the refresh timer is expired.
     * @param disableSearch whether the search button should be disabled and cancel enabled
     */
    public void refreshLogs(final boolean disableSearch) {
        getLogsRefreshTimer().stop();
        if(disableSearch){
            disableSearch();
        }

        isSignAudits(true); // updates cached value

        // retrieve the new logs
        Window window = SwingUtilities.getWindowAncestor(this);

        if (window != null && window.isVisible()) {

            String nodeToUse = node;

            //construct a LogRequest for the current search criteria
            LogRequest logRequest = new LogRequest.Builder().
                startMsgDate(new Date(System.currentTimeMillis() - durationMillis)).
                nodeName(nodeToUse).
                auditType(auditType).
                logLevel(logLevelOption.getLevel()).
                message(message).
                serviceName(serviceName).
                requestId(requestId).
                userName(userName).
                userIdOrDn(userIdOrDn).
                messageId(messageId).
                paramValue(paramValue).
                entityClassName(entityTypeName == null? null : getAllEntities().get(entityTypeName)).
                entityId(entityId).
                operation(operation).
                getFromPolicy(getFromPolicy).
                build();

            //save the log request
            ((AuditLogTableSorterModel) getMsgTable().getModel()).refreshLogs(this, logRequest, isAutoRefreshEffective());
        }

    }

    /**
     * Disable search button and enable cancel button.
     */
    private void disableSearch() {
        getSearchButton().setEnabled(false);
        getCancelButton().setEnabled(true);
    }

    /**
     * Performs the log retrieval.
     * @param disableSearch whether the search button should be disabled and cancel enabled
     */
    public void refreshLogs( final Date first, final Date last, final boolean disableSearch ) {
        getLogsRefreshTimer().stop();
        if(disableSearch){
            disableSearch();
        }

        isSignAudits(true); // updates cached value

        // retrieve the new logs
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null && window.isVisible()) {
            //construct a LogRequest for the current search criteria
            LogRequest logRequest = new LogRequest.Builder().
                startMsgDate(first).
                endMsgDate(last).
                nodeName(node).
                auditType(auditType).
                logLevel(logLevelOption.getLevel()).
                message(message).
                serviceName(serviceName).
                requestId(requestId).
                userName(userName).
                userIdOrDn(userIdOrDn).
                messageId(messageId).
                paramValue(paramValue).
                entityClassName(entityTypeName == null? null : getAllEntities().get(entityTypeName)).
                entityId(entityId).
                getFromPolicy(getFromPolicy).
                operation(operation).
                build();

            //save the log request
            AuditLogTableSorterModel altsm = (AuditLogTableSorterModel) getMsgTable().getModel();
            altsm.clearLogCache();
            altsm.refreshLogs(this, logRequest, false);
        }
    }

    /**
     * Displays the given log messages. Old display is cleared first.
     *
     * @param logs log messages to load; as a map of gateway node ID and
     *             corresponding collection of {@link AuditMessage}s
     */
    public void setLogs(Map<Long, ? extends AbstractAuditMessage> logs) {
        onDisconnect();
        retrievalMode = RetrievalMode.NONE;
        getFilteredLogTableSorter().setLogs(this, logs);
        setDynamicData(false);
    }

    /**
     * Reset UI according to whether data is dynamic or static.
     *
     * @param dynamic <code>true</code> for dynamic, <code>false</code> for static
     */
    public void setDynamicData(final boolean dynamic) {
        if (dynamic) durationAutoRefresh = false;

        // update control visibility / enable for static views
        getLastUpdateTimeLabel().setVisible( dynamic );
        controlPanel.autoRefreshCheckBox.setVisible( dynamic );

        if (selectionSplitPane != null) {
            // No time range controls for static data.
            selectionSplitPane.setTopComponent(dynamic ? getControlPane() : null);
            selectionSplitPane.setOneTouchExpandable(dynamic);
        }
    }

    /**
     * Set the row of the log table which is currently selected by the user for viewing the details of the log message.
     *
     * @param msgNumber The message number of the log being selected.
     */
    public void setSelectedRow(String msgNumber) {
        if (msgNumber != null && !msgNumber.equals("-1")) {
            // keep the current row selection
            int rowCount = getMsgTable().getRowCount();
            boolean rowFound = false;
            for (int i = 0; i < rowCount; i++) {
                Object nodeId = getMsgTable().getModel().getValueAt(i, LOG_NODE_ID_COLUMN_INDEX);
                Object mesNum = getMsgTable().getModel().getValueAt(i, LOG_MSG_NUMBER_COLUMN_INDEX);

                if (nodeId != null && mesNum != null) {
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

    private void updateLogsRefreshTimerDelay() {
        int refreshSeconds = getFilteredLogTableSorter().getDelay();

        if (refreshSeconds >= 0 && refreshSeconds < 300) logsRefreshInterval = 1000 * refreshSeconds;
        else logsRefreshInterval = LOG_REFRESH_TIMER;

        if (logsRefreshInterval == 0) logsRefreshInterval = Integer.MAX_VALUE; // disable refresh

        getLogsRefreshTimer().setInitialDelay(logsRefreshInterval);
        getLogsRefreshTimer().setDelay(logsRefreshInterval);
    }

    /**
     * Clear the message table
     */
    public void clearMsgTable() {
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
                @Override
                public void actionPerformed(ActionEvent evt) {
                    refreshLogs(false);
                }
            });
        }

        return logsRefreshTimer;
    }

    /**
     * Update the message total.
     */
    public void updateMsgTotal() {
        getMsgTotal().setText(MSG_TOTAL_PREFIX + msgTable.getRowCount() + (getFilteredLogTableSorter().isTruncated() ? " (truncated)" : ""));
    }

    /**
     * Update the timestamp of the "last updated" label.
     */
    public void updateTimeStamp(java.util.Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        getLastUpdateTimeLabel().setText("Last Updated: " + sdf.format(cal.getTime()) +
                (isAutoRefreshEffective() ? " [Auto-Refresh]" : "   "));
    }

    /**
     * @param refresh whether to query the server afresh
     * @return true if audit signing is enabled
     */
    private boolean isSignAudits(final boolean refresh) {
        if (refresh) {
            boolean result = signAudits;
            try {
                result = Registry.getDefault().getAuditAdmin().isSigningEnabled();
            } catch (FindException e) {
                // keep old value
            }
            signAudits = result;
        }

        return signAudits;
    }

    // This customized renderer can render objects of the type TextandIcon
    TableCellRenderer iconHeaderRenderer = new DefaultTableCellRenderer() {
        @Override
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
            } else {
                setIcon(null);
            }

            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(JLabel.CENTER);

            if (table.convertColumnIndexToModel(column) == LOG_SIGNATURE_COLUMN_INDEX) {
                setToolTipText("Digital Signature");
            } else {
                setToolTipText(null);
            }

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
            @Override
            public void mouseClicked(MouseEvent e) {

                int viewColumn = tableView.columnAtPoint(e.getPoint());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {

                    String msgNumSelected = logPane.getSelectedMsgNumber();

                    ((AuditLogTableSorterModel) tableView.getModel()).sortData(column, true);
                    ((AuditLogTableSorterModel) tableView.getModel()).fireTableDataChanged();

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
    public void exportView(final File file) throws IOException {
        // process
        JTable table = getMsgTable();
        int rows = table.getRowCount();
        List<WriteableLogMessage> data = new ArrayList<WriteableLogMessage>(rows);
        AuditLogTableSorterModel logTableSorterModel = getFilteredLogTableSorter();
        for (int i = 0; i < rows; i++) {
            AbstractAuditMessage logMessage = logTableSorterModel.getLogMessageAtRow(i);
            if ( logMessage instanceof AuditMessage ) {
                data.add(new WriteableLogMessage((AuditMessage)logMessage));
            }  else {
                data.add(new LazyWriteableLogMessage(logMessage));
            }
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
            @Override
            public void run() {
                DialogDisplayer.showMessageDialog(null, dialogMessage, null);
            }
        });
    }

    /**
     * Set the selected historic audit data.
     *
     * @param date  The target date.
     * @param range The range in hours around the date (negative for before)
     */
    public void setSelectionDetails(Date date, int range) {
        controlPanel.timeRangeButton.setSelected(true);
        Date startDate;
        Date endDate;
        if (range >= 0) {
            startDate = date;
            endDate = new Date(date.getTime() + (long) range * MILLIS_IN_HOUR);
        } else {
            startDate = new Date(date.getTime() + (long) range * MILLIS_IN_HOUR);
            endDate = date;
        }
        controlPanel.timeRangePicker.setStartTime(startDate);
        controlPanel.timeRangePicker.setEndTime(endDate);
        setControlsExpanded(true);
    }

    public boolean importView(File file) throws IOException {
        retrievalMode = RetrievalMode.NONE;
        setDynamicData(false);

        InputStream in = null;
        ObjectInputStream ois = null;
        try {
            in = new FileInputStream(file);
            byte[] header = new byte[FILE_TYPE.length];
            int readCount = in.read(header);
            if ( readCount != FILE_TYPE.length ||
                 !ArrayUtils.compareArrays(FILE_TYPE, 0, header, 0, FILE_TYPE.length)) {
                importError("Cannot import file, incorrect type.");
                return false;
            }
            ois = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(in)));
            Object fileProductVersionObj = ois.readObject();
            Object fileBuildNumberObj = ois.readObject();
            Object read = ois.readObject();
            if (fileProductVersionObj instanceof String &&
                    fileBuildNumberObj instanceof String &&
                    read instanceof List) {
                String fileProductVersion = (String) fileProductVersionObj;
                String fileBuildNumber = (String) fileBuildNumberObj;

                boolean buildMatch = fileBuildNumber.equals(BuildInfo.getBuildNumber());
                boolean versionMatch = fileProductVersion.equals(BuildInfo.getProductVersion());

                if (!buildMatch) {
                    String message;
                    if (!versionMatch) {
                        message = "Cannot import file for product version '" + fileProductVersion + "'.";
                    } else {
                        message = "Cannot import file for product build '" + fileBuildNumber + "'.";
                    }
                    importError(message);
                    return false;
                }

                //noinspection unchecked
                List<WriteableLogMessage> data = (List) read;
                if (data.isEmpty()) {
                    logger.info("No data in file! '" + file.getAbsolutePath() + "'.");
                }
                Map<Long, AbstractAuditMessage> loadedLogs = new HashMap<Long, AbstractAuditMessage>();
                for (WriteableLogMessage message : data) {
                    AbstractAuditMessage lm =  new AuditMessage((AuditRecord)message.ssgLogRecord);
                    lm.setNodeName(message.nodeName);
                    loadedLogs.put(lm.getMsgNumber(), lm);
                }
                getFilteredLogTableSorter().setLogs(this, loadedLogs);
            } else {
                logger.warning("File '" + file.getAbsolutePath() + "' contains invalid data! '" +
                        (read == null ? "null" : read.getClass().getName()) + "'.");
            }
        }
        catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Error reading data file '" + file.getAbsolutePath() + "'.", cnfe);
        }
        finally {
            ResourceUtils.closeQuietly(ois);
            ResourceUtils.closeQuietly(in);
        }
        return true;
    }

    private static AuditMessage getLogMessage( final AbstractAuditMessage logMessage ) throws FindException {
        AuditRecord record = null;
        if(logMessage instanceof AuditHeaderMessage){
            String guid =((AuditHeaderMessage) logMessage).getGuid();
            if(guid!=null){
                record = Registry.getDefault().getAuditAdmin().findByPrimaryKey( guid, false);
            }
        }

        if(record == null){
            record = Registry.getDefault().getAuditAdmin().findByPrimaryKey( Long.toString(logMessage.getMsgNumber()),true );
        }

        if ( record == null )
            throw new FindException("Missing audit record for id '"+logMessage.getMsgNumber()+"'.");
        return new AuditMessage( record, logMessage.getNodeName() );
    }

    public static class WriteableLogMessage implements Comparable, Serializable {
        private String nodeName;
        private AuditRecord ssgLogRecord;

        public WriteableLogMessage( AuditMessage lm ) {
            nodeName = lm.getNodeName();
            ssgLogRecord = lm.getAuditRecord();
        }


        protected WriteableLogMessage() {
        }

        @Override
        public int compareTo(Object o) {
            return new Long(ssgLogRecord.getMillis()).compareTo(((WriteableLogMessage) o).ssgLogRecord.getMillis());
        }
    }

    public static class LazyWriteableLogMessage extends WriteableLogMessage {
        private AbstractAuditMessage logMessage;

        public LazyWriteableLogMessage(AbstractAuditMessage logMessage) {
            super();
            this.logMessage = logMessage;
        }

        @Override
        public int compareTo(Object o) {
            return new Long(logMessage.getTimestamp()).compareTo(((LazyWriteableLogMessage) o).logMessage.getTimestamp());
        }

        private Object writeReplace() throws ObjectStreamException {
            try {
                return new WriteableLogMessage( getLogMessage( logMessage ) );
            } catch ( FindException fe ) {
                throw (ObjectStreamException) new InvalidObjectException( ExceptionUtils.getMessage(fe) ).initCause( fe );
            }
        }
    }

    /**
     * Control panel for selecting times of audit events to download.
     */
    private static class LogPanelControlPanel {
        private JPanel mainPanel;
        private JRadioButton durationButton;
        private JRadioButton timeRangeButton;
        private JTextField hoursTextField;
        private JTextField minutesTextField;
        private JCheckBox autoRefreshCheckBox;
        private TimeRangePicker timeRangePicker;
        private JTextField serviceTextField;
        private JTextField messageTextField;
        private JLabel requestIdLabel;
        private JTextField requestIdTextField;
        private SquigglyTextField nodeTextField;
        private JComboBox auditTypeComboBox;
        private JComboBox levelComboBox;
        private JPanel requestIdPane;
        private JPanel servicePane;
        private JPanel auditTypePane;
        private JPanel nodePane;
        private JPanel timeRangePane;
        private JButton searchButton;
        private JTextField userNameTextField;
        private JTextField userIdOrDnTextField;
        private JTextField auditCodeTextField;
        private JLabel entityIdLabel;
        private JTextField entityIdTextField;
        private JLabel entityTypeLabel;
        private JComboBox entityTypeComboBox;
        private JPanel entitySearchingPane;
        private JPanel associatedLogsSearchingPane;
        private JPanel userNamePane;
        private JPanel userIdPane;
        private JPanel cautionIndicatorPanel;
        private JLabel cautionTextField;
        private JCheckBox validateSignaturesCheckBox;
        private JButton clearButton;
        private JButton cancelButton;
        private JRadioButton internalDatabaseRadioButton;
        private JRadioButton viaAuditLookupPolicyRadioButton;
        private JButton configureAuditLookupPolicyButton;
        private JPanel messagePropertySearchingPane;
        private JTextField operationTextField;
        private JLabel operationLabel;
    }
}
