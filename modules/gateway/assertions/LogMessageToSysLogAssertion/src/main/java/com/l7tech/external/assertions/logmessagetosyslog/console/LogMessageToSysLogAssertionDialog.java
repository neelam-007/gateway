package com.l7tech.external.assertions.logmessagetosyslog.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.logmessagetosyslog.LogMessageToSysLogAssertion;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.annotations.common.reflection.java.JavaXMember;
import org.springframework.context.ApplicationEvent;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogMessageToSysLogAssertionDialog extends AssertionPropertiesOkCancelSupport<LogMessageToSysLogAssertion> {

    protected static final Logger logger = Logger.getLogger(LogMessageToSysLogAssertionDialog.class.getName());

    private JTextField messageTextField;
    private JComboBox sysLogComboBox;
    private JPanel contentPane;
    private JLabel Message;
    private JLabel SysLog;
    private JComboBox severityComboBox;
    private JLabel Severity;
    private JCheckBox jcbxCEF;
    private JTabbedPane tabpCEF;
    private JTextField jtxtfCEFVersion;
    private JTextField jtxtfDeviceVendor;
    private JTextField jtxtfDeviceProduct;
    private JTextField jtxtfDeviceVersion;
    private JTextField jtxtfSignatureId;
    private JTextField jtxtfSignatureName;
    private JSpinner spinSeverity;
    private JTable jtableCEFExtension;
    private Map<String, Goid> mapSyslogNamestoGoid;
    private JScrollPane jscrollCEFExtension;
    private JTable jtableCEFKeysOverview;
    private JScrollPane jscpCEFKeysOverview;
    private String cefHeaderFixed[];

    private List<CellEditorListener> cellEditorListeners;

    // constructors
    public LogMessageToSysLogAssertionDialog(Window owner, LogMessageToSysLogAssertion assertion) {
        super(LogMessageToSysLogAssertion.class, owner, "Log Message to Syslog Assertion", true);

        cefHeaderFixed = LogMessageToSysLogAssertion.CEFHeaderFixed.split("[|]");

        initComponents();
    }

    @Override
    public LogMessageToSysLogAssertion getData(LogMessageToSysLogAssertion assertion) throws ValidationException {

        String chosenSysLog = sysLogComboBox.getSelectedItem().toString();
        Goid syslogGoid = mapSyslogNamestoGoid.get(chosenSysLog);
        assertion.setSyslogGoid(syslogGoid);

        assertion.setMessageText(messageTextField.getText());
        String chosenSeverity = this.severityComboBox.getSelectedItem().toString();
        assertion.setSysLogSeverity(chosenSeverity);

        assertion.setCEFEnabled(jcbxCEF.isSelected());
        assertion.setCefSignatureId(jtxtfSignatureId.getText());
        assertion.setCefSignatureName(jtxtfSignatureName.getText());
        assertion.setCefSeverity((Integer) spinSeverity.getValue());

        String key, value;
        Map<String, String> extensionValues = new HashMap<String, String>();
        for (int i = 0; i < jtableCEFExtension.getRowCount(); i++) {
            key = (String) jtableCEFExtension.getValueAt(i, 0);
            value = (String) jtableCEFExtension.getValueAt(i, 1);
            if ((key != null) && (value != null) && (key.trim().length() > 0) && (value.trim().length() > 0))
                extensionValues.put(key, value);
        }
        assertion.setCefExtensionKeyValuePairs(extensionValues);
        return assertion;
    }

    @Override
    public void setData(LogMessageToSysLogAssertion assertion) {
        messageTextField.setText(assertion.getMessageText());
        this.loadSinkConfigurationIntoComboBox(assertion.getSyslogGoid());
        this.loadSysLogSeverityIntoComboBox(assertion.getSysLogSeverity());

        setCEFHeaderFixed();
        jtxtfSignatureId.setText(assertion.getCefSignatureId());
        jtxtfSignatureName.setText(assertion.getCefSignatureName());
        spinSeverity.setValue(assertion.getCefSeverity());

        int row = 0;
        Map<String, String> cefExtension = assertion.getCefExtensionKeyValuePairs();
        Set<String> sortedKeys = new TreeSet<String>(cefExtension.keySet());
        for (String key : sortedKeys) {
            jtableCEFExtension.setValueAt(key, row, 0);
            jtableCEFExtension.setValueAt(cefExtension.get(key), row++, 1);
        }
        packColumn(jtableCEFExtension, 2);
        jcbxCEF.setSelected(assertion.isCEFEnabled());
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        /* ---------------- Ok button enablement ---------*/
        // Action Listener called everytime any of the input fields change
        final EventListener genericChangeListener = new RunOnChangeListener(new Runnable() {
            public void run() {
//                enableOrDisableOkButton();
                updateOkButtonEnableState();
            }
        });

        // Add Listeners
        addListenerToTextField(messageTextField, (DocumentListener) genericChangeListener);
        severityComboBox.addItemListener((ItemListener) genericChangeListener);
        sysLogComboBox.addItemListener((ItemListener) genericChangeListener);

        // load various maps
        this.loadSinkConfigurations();

//        enableOrDisableOkButton();
        updateOkButtonEnableState();
        // configure CEF components
        //=====================================================
        cellEditorListeners = new ArrayList<CellEditorListener>();

        setCEFHeaderFixed();

        // Add listeners
        addListenerToTextField(jtxtfSignatureId, (DocumentListener) genericChangeListener);
        addListenerToTextField(jtxtfSignatureName, (DocumentListener) genericChangeListener);
        jcbxCEF.addChangeListener((ChangeListener) genericChangeListener);
        jcbxCEF.addChangeListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                setOrdenaryState(!jcbxCEF.isSelected());
                setCEFState(jcbxCEF.isSelected());
            }
        }));

        SpinnerNumberModel spModel = new SpinnerNumberModel(0, 0, 10, 1);
        spinSeverity.setModel(spModel);
        ((JSpinner.DefaultEditor) spinSeverity.getEditor()).getTextField().setEditable(false);

        jcbxCEF.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (jcbxCEF.isSelected()) {
                    setOrdenaryState(false);
                    setCEFState(true);
                } else {
                    setOrdenaryState(true);
                    setCEFState(false);
                }
            }
        });

        TableModel tm = new DefaultTableModel(new String[]{"Key", "Value"}, 20);
        setupTable(tm, jtableCEFExtension, jscrollCEFExtension.getPreferredSize());

        String colHeaderKeys[] = new String[]{"CEF Name", "Full Name", "Data Type", "Length", "Meaning"};
        String[][] dataKeysOverview = getKeyValueOverviewValues();

        TableModel tmKeys = new DefaultTableModel(dataKeysOverview, colHeaderKeys);
        setupTable(tmKeys, jtableCEFKeysOverview, jtableCEFKeysOverview.getPreferredSize());

        // disable CEF for a start
        setOrdenaryState(true);
        setCEFState(false);

        getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = jtableCEFExtension.getSelectedRow();
                if (selectedRow >= 0) {
                    TableCellEditor editor = jtableCEFExtension.getCellEditor(selectedRow, jtableCEFExtension.getSelectedColumn());
                    if (editor != null)
                        editor.stopCellEditing();
                }
            }
        });
    }

    private String[][] getKeyValueOverviewValues() {
        return new String[][]{
                    {"act", "deviceAction", "String", "63", "Action taken by the device"},
                    {"app", "applicationProtocol", "String", "31", "ApplicationEvent Level protocol, example: HTTP, POP"},
                    {"c6a1", "deviceCustomIPv6Address1", "IPv6 address", "", "One of four IPV6 address fields available to map fields that do not apply to any other in this dictionary"},
                    {"c6a2", "deviceCustomIPv6Address2", "IPv6 address", "", "One of four IPV6 address fields available to map fields that do not apply to any other in this dictionary"},
                    {"c6a3", "deviceCustomIPv6Address3", "IPv6 address", "", "One of four IPV6 address fields available to map fields that do not apply to any other in this dictionary"},
                    {"c6a4", "deviceCustomIPv6Address4", "IPv6 address", "", "One of four IPV6 address fields available to map fields that do not apply to any other in this dictionary"},
                    {"c6a1Label", "deviceCustomIPv6Address1Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"c6a2Label", "deviceCustomIPv6Address2Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"c6a3Label", "deviceCustomIPv6Address3Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"c6a4Label", "deviceCustomIPv6Address4Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"cat", "deviceEventCategory", "String", "1023", "Represents the category assigned by the originating device, example: /Monitor/Disk/Read"},
                    {"cfp1", "deviceCustomFloatingPoint1", "Floating Point", "", "One of four floating point fields available to map fields that do not apply to any other in this dictionary"},
                    {"cfp2", "deviceCustomFloatingPoint2", "Floating Point", "", "One of four floating point fields available to map fields that do not apply to any other in this dictionary"},
                    {"cfp3", "deviceCustomFloatingPoint3", "Floating Point", "", "One of four floating point fields available to map fields that do not apply to any other in this dictionary"},
                    {"cfp4", "deviceCustomFloatingPoint4", "Floating Point", "", "One of four floating point fields available to map fields that do not apply to any other in this dictionary"},
                    {"cfp1Label", "deviceCustomFloatingPoint1Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"cfp2Label", "deviceCustomFloatingPoint2Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"cfp3Label", "deviceCustomFloatingPoint3Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"cfp4Label", "deviceCustomFloatingPoint4Label", "String", "", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"cnt", "baseEventCount", "Integer", "", "A count associated with this event"},
                    {"cn1", "deviceCusomtNumber1", "Long", "", "One of three number fields available to map fields that do not apply to any other (use sparingly!)"},
                    {"cn2", "deviceCusomtNumber2", "Long", "", "One of three number fields available to map fields that do not apply to any other (use sparingly!)"},
                    {"cn3", "deviceCusomtNumber3", "Long", "", "One of three number fields available to map fields that do not apply to any other (use sparingly!)"},
                    {"cn1Label", "deviceCustomNumber1Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cn2Label", "deviceCustomNumber2Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cn3Label", "deviceCustomNumber3Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs1", "deviceCustomString1", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs2", "deviceCustomString2", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs3", "deviceCustomString3", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs4", "deviceCustomString4", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs5", "deviceCustomString5", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs6", "deviceCustomString6", "String", "1023", "One of six strings available to map fields that do not apply to any other (use sparingly!)"},
                    {"cs1Label", "deviceCustomString1Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs2Label", "deviceCustomString2Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs3Label", "deviceCustomString3Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs4Label", "deviceCustomString4Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs5Label", "deviceCustomString5Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"cs6Label", "deviceCustomString6Label", "String", "1023", "All custom fields have a corresponding label field where the field itself can be described"},
                    {"dhost", "destinationHostName", "String", "1023", "Identifies the destination that an event refers to in an IP network. The format should be a fully qualified domain name"},
                    {"dpid", "destinationProcessId", "Integer", "", "Provides the ID of the destination process associated with the event. For example, if an event contains process ID 105, '105' is the process ID"},
                    {"dmac", "destinationMacAddress", "MAC Address", "", "Six colon-separated hexadecimal numbers. Example: '00:0D:60:AF:1B:61'"},
                    {"dntdom", "destinationNtDomain", "String", "255", "The Windows domain name of the destination address"},
                    {"dpriv", "destinationUserPrivileges", "String", "1023", "The typical values are: 'Administrator', 'User', and 'Guest'"},
                    {"dproc", "destinationProcessName", "String", "1023", "The name of the event's destination process. Example: 'telnetd', or 'sshd'"},
                    {"dpt", "destinationPort", "Integer", "", "The valid port numbers are between 0 and 65535"},
                    {"dst", "destinationAddress", "IPv4 Address", "", "Identifies the destination address that the event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"duid", "destinationUserId", "String", "1023", "Identifies the destination user by ID. For example, in UNIX, the root user is generally associated with user ID 0"},
                    {"duser", "destinationUserName", "String", "1023", "Identifies the destination user by name. This is the user associated with the event's destination. Email addresses are often mapped into the UserName fields. The recipient is a candidate to put into destinationUserName"},
                    {"dvc", "deviceAddress", "IPV4 Address", "16", "Identifies the device address that an event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"dvchost", "deviceHostName", "String", "100", "The format should be a fully qualified domain name associated with the device node, when a node is available (FQDN). Examples: 'host.domain.com' or 'host'"},
                    {"dvcpid", "deviceProcessId", "Integer", "", "Provides the ID of the process on the device generating the event"},
                    {"end", "endTime", "Time Stamp", "", "The time at which the activity related to the event ended. The format is 'MMM dd yyyy HH:mm:ss' or milliseconds since epoch (Jan 1st 1970). An example would be reporting the end of a session"},
                    {"fname", "fileName", "String", "1023", "Name of the file"},
                    {"fsize", "fileSize", "Integer", "", "Size of the file"},
                    {"in", "bytesIn", "Integer", "", "Number of bytes transferred inbound. Inbound relative to the source to destination relationship, meaning that data was flowing from source to destination"},
                    {"msg", "message", "String", "1023", "An arbitrary message giving more details about the event. Multi-line entries can be produced by using \\n (backslash-n) as the new-line separator"},
                    {"outcome", "eventOutcome", "String", "63", "Displays the outcome, usually as 'success' or 'failure'"},
                    {"out", "bytesOut", "Integer", "", "Number of bytes transferred outbound relative to the source to destination relationship. I.e., the byte number of data flowing from the destination to the source"},
                    {"proto", "transportProtocol", "String", "31", "Identifies the Layer-4 protocol used. The possible values are protocols such as TCP or UDP"},
                    {"request", "requestURL", "String", "1023", "In the case of an HTTP request, this field contains the URL accessed. The URL should contain the protocol as well. Example: 'http://www.security.com'"},
                    {"rt", "receiptTime", "Time Stamp", "", "The time at which the event related to the activity was received. The format is 'MMM dd yyyy HH:mm:ss' or milliseconds since epoch (Jan 1st 1970)"},
                    {"shost", "sourceHostName", "String", "1023", "Identifies the source that an event refers to in an IP network. The format should be a fully qualified domain name associated with the source node, when a node is available (FQDN). Examples: 'host.domain.com' or 'host'"},
                    {"smac", "sourceMacAddress", "MAC Address", "", "Six colon-separated hexadecimal numbers. Example: '00:0D:60:AF:1B:61'"},
                    {"sntdom", "sourceNtDomain", "String", "255", "The Windows domain name for the source address"},
                    {"spid", "sourceProcessId", "Integer", "", "The ID of the source process associated with the event"},
                    {"sproc", "sourceProcessName", "String", "1023", "The name of the event's source process"},
                    {"spt", "sourcePort", "Integer", "", "The valid port numbers are 0 to 65535"},
                    {"spriv", "sourceUserPrivileges", "String", "1023", "The typical values are: 'Administrator', 'User', and 'Guest'. It identifies the source user's privileges"},
                    {"src", "sourceAddress", "IPv4 Address", "", "Identifies the source that an event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"start", "startTime", "Time Stamp", "", "The time when the activity the event referred to started. The format is 'MMM dd yyyy HH:mm:ss' or milliseconds since epoch (Jan 1st 1970)"},
                    {"suid", "sourceUserId", "String", "1023", "Identifies the source user by ID. This is the user associated with the source of the event"},
                    {"suser", "sourceUserName", "String", "1023", "Identifies the source user by name. Email addresses are also mapped into the UserName fields. The sender is a candidate to put into sourceUserName"},
                    {"<use full name>", "destinationDnsDomain", "String", "255", "The DNS domain part of the complete fully qualified domain name (FQDN)"},
                    {"<use full name>", "destinationServiceName", "String", "1023", "The service targeted by this event. Example: 'sshd'"},
                    {"<use full name>", "destinationTranslatedAddress", "IPv4 address", "", "Identifies the translated destination that the event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"<use full name>", "destinationTranslatedPort", "Integer", "", "Port after it was translated; for example, a firewall. Valid port numbers are 0 to 65535"},
                    {"<use full name>", "deviceCustomDate1", "TimeStamp", "", "One of two timestamp fields available to map fields that do not apply to any other in this dictionary. Use sparingly and seek a more specific, dictionary supplied field when possible"},
                    {"<use full name>", "deviceCustomDate2", "TimeStamp", "", "One of two timestamp fields available to map fields that do not apply to any other in this dictionary. Use sparingly and seek a more specific, dictionary supplied field when possible"},
                    {"<use full name>", "deviceCustomDate1Label", "String", "1023", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"<use full name>", "deviceCustomDate2Label", "String", "1023", "All custom fields have a corresponding label field. Each of these fields is a string and describes the purpose of the custom field"},
                    {"<use full name>", "deviceDirection", "Integer", "", "Any information about what direction the observed communication has taken. Examples: 'inbound' or 'outbound'"},
                    {"<use full name>", "deviceDnsDomain", "String", "255", "The DNS domain part of the complete fully qualified domain name (FQDN)"},
                    {"<use full name>", "deviceExternalId", "String", "255", "A name that uniquely identifies the device generating this event"},
                    {"<use full name>", "deviceFacility", "String", "1023", "The facility generating this event. For example, Syslog has an explicit facility associated with every event"},
                    {"<use full name>", "deviceInboundInterface", "String", "15", "Interface on which the packet or data entered the device"},
                    {"<use full name>", "deviceMacAddress", "MAC Address", "", "Six colon-separated hexadecimal numbers. Example: '00:0D:60:AF:1B:61'"},
                    {"<use full name>", "deviceNtDomain", "String", "255", "The Windows domain name of the device address"},
                    {"<use full name>", "deviceOutboundInterface", "String", "15", "Interface on which the packet or data left the device"},
                    {"<use full name>", "deviceProcessName", "String", "1023", "Process name associated with the event. An example might be the process generating the syslog entry in UNIX"},
                    {"<use full name>", "deviceTranslatedAddress", "IPv4 Address", "", "Identifies the translated device address that the event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"<use full name>", "externalId", "Integer", "", "The ID used by an originating device. They are usually increasing numbers, associated with events"},
                    {"<use full name>", "fileCreateTime", "TimeStamp", "", "Time when the file was created"},
                    {"<use full name>", "fileHash", "String", "255", "Hash of a file"},
                    {"<use full name>", "fileId", "String", "1023", "An ID associated with a file could be the inode"},
                    {"<use full name>", "fileModificationTime", "TimeStamp", "", "Time when the file was last modified"},
                    {"<use full name>", "filePath", "String", "1023", "Full path to the file, including file name itself. Example: C:\\Program Files\\WindowsNT\\Accessories\\wordpad.exe or /usr/bin/zip"},
                    {"<use full name>", "filePermission", "String", "1023", "Permissions of the file"},
                    {"<use full name>", "fileType", "String", "1023", "Type of file (pipe, socket, etc.)"},
                    {"<use full name>", "oldFileCreateTime", "TimeStamp", "", "Time when old file was created"},
                    {"<use full name>", "oldFileHash", "String", "255", "Hash of the old file"},
                    {"<use full name>", "oldFileId", "String", "1023", "An ID associated with the old file could be inode"},
                    {"<use full name>", "oldFileModificationTime", "TimeStamp", "", "Time when old file was las modified"},
                    {"<use full name>", "oldFileName", "String", "1023", "Name of the old file"},
                    {"<use full name>", "oldFilePath", "String", "1023", "Full path to the old file, including the file name itself. Examples: C:\\Program Files\\WindowsNT\\Accessories\\wordpad.exe and /usr/bin/zip"},
                    {"<use full name>", "oldFilePermission", "String", "1023", "Permissions of the old file"},
                    {"<use full name>", "oldFileSize", "Integer", "", "Size of the old file"},
                    {"<use full name>", "oldFileType", "String", "1023", "Type of the old file (pipe, socket, etc.)"},
                    {"<use full name>", "reason", "String", "1023", "The reason an audit event was generated. For example 'Bad password' or 'Unknown User' This could also be an error or return code. Example: '0x1234'"},
                    {"<use full name>", "requestClientApplication", "String", "1023", "The User-Agent associated with the request"},
                    {"<use full name>", "requestCookies", "String", "1023", "Cookies associated with the request"},
                    {"<use full name>", "requestMethod", "String", "1023", "The method used to access a URL. Possible values: 'POST', 'GET', etc"},
                    {"<use full name>", "sourceDnsDomain", "String", "255", "The DNS domain part of the complete fully qualified domain name (FQDN)"},
                    {"<use full name>", "sourceServiceName", "String", "1023", "The service which is responsible for generating this event"},
                    {"<use full name>", "sourceTranslatedAddress", "IPv4 Address", "", "Identifies the translated source that the event refers to in an IP network. The format is an IPv4 address. Example: '192.168.10.1'"},
                    {"<use full name>", "sourceTranslatedPort", "Integer", "", "A port number after being translated by, for example, a firewall. Valid port numbers are 0 to 65535"}
            };
    }

    /**
     * Creates the proper columns width, sets the tool tip
     * @param tm
     * @param table
     * @param minDimension
     */
    private void setupTable(TableModel tm, final JTable table, Dimension minDimension) {
        table.setModel(tm);
        table.setRowHeight(20);
        table.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                int row = table.rowAtPoint(p);
                if (row >= 0) {
                    int column = table.columnAtPoint(p);
                    final String tipText = String.valueOf(table.getValueAt(row, column)).trim();
                    if (tipText.length() > 0 && !"null".equals(tipText))
                        table.setToolTipText(tipText);
                    else
                        table.setToolTipText("key - value pairs");
                }
            }//end MouseMoved
        }); // end MouseMotionAdapter
        packColumn(table, 2);
        if (table.getPreferredSize().getWidth() < minDimension.width)
            table.setPreferredSize(minDimension);
    }

    public void packColumn(JTable table, int margin) {
        int colCount = table.getColumnCount();
        for (int c = 0; c < colCount; c++) {
            packColumn(table, c, margin);
        }
    }

    /**
     * Sets the preferred width of the visible column specified by vColIndex. The column
     * will be just wide enough to show the column head and the widest cell in the column.
     * margin pixels are added to the left and right
     * (resulting in an additional width of 2*margin pixels).
     * Fond this method at: http://www.exampledepot.com/egs/javax.swing.table/PackCol.html
     */
    private void packColumn(JTable table, int vColIndex, int margin) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width = 0;

        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(
                table, col.getHeaderValue(), false, false, 0, 0);
        width = comp.getPreferredSize().width;

        // Get maximum width of column data
        for (int r = 0; r < table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(
                    table, table.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // Add margin
        width += 2 * margin;

        // Set the width
        col.setPreferredWidth(width);
    }

    private void addListenerToTextField(JTextField textField, DocumentListener genericChangeListener) {
        if (textField != null)
            textField.getDocument().addDocumentListener(genericChangeListener);
    }

    private void setCEFHeaderFixed() {
        jtxtfCEFVersion.setText(cefHeaderFixed[0].substring(4));
        jtxtfDeviceVendor.setText(cefHeaderFixed[1]);
        jtxtfDeviceProduct.setText(cefHeaderFixed[2]);
        jtxtfDeviceVersion.setText(cefHeaderFixed[3]);
    }

    private void setCEFState(boolean enable) {
        tabpCEF.setEnabled(enable);
        jtxtfSignatureId.setEnabled(enable);
        jtxtfSignatureName.setEnabled(enable);
        spinSeverity.setEnabled(enable);
    }

    private void setOrdenaryState(boolean enable) {
        messageTextField.setEnabled(enable);
    }

    @Override
    // Have to override this field to ensure the "OK" button is enabled/disabled propertly when the dialog is first opened
    protected void updateOkButtonEnableState() {

        // call local enable or disable button method.  Also check for Key Text Field
//        enableOrDisableOkButton();

        boolean enabled = false;
        if (jcbxCEF.isSelected()) {
            enabled = textExsists(jtxtfSignatureId.getText()) && textExsists(jtxtfSignatureName.getText());
        } else {
            enabled = textExsists(messageTextField.getText()) &&
                    (sysLogComboBox.getSelectedItem() != null && textExsists(sysLogComboBox.getSelectedItem().toString())) &&
                    (severityComboBox.getSelectedItem() != null && textExsists(severityComboBox.getSelectedItem().toString()));
        }
        getOkButton().setEnabled(enabled && !isReadOnly());
    }


    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }


    // private methods
//    private void enableOrDisableOkButton() {
//        boolean enabled = false;
//        if (jcbxCEF.isSelected()) {
//            enabled = textExsists(jtxtfSignatureId.getText()) && textExsists(jtxtfSignatureName.getText());
//        } else {
//            enabled = textExsists(messageTextField.getText()) &&
//                    (sysLogComboBox.getSelectedItem() != null && textExsists(sysLogComboBox.getSelectedItem().toString())) &&
//                    (severityComboBox.getSelectedItem() != null && textExsists(severityComboBox.getSelectedItem().toString()));
//        }
//        getOkButton().setEnabled(enabled && !isReadOnly());
//    }

    private boolean textExsists(String text) {
        return text != null && (text.trim().length() > 0);
    }

    private LogSinkAdmin getLogSinkAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getLogSinkAdmin();
    }

    private void loadSinkConfigurations() {
        try {
            LogSinkAdmin logSinkAdmin = getLogSinkAdmin();
            //if (!flags.canReadSome() || logSinkAdmin == null) {
            if (logSinkAdmin == null) {
                return;
            }
            Collection<SinkConfiguration> sinkConfigurations = logSinkAdmin.findAllSinkConfigurations();
            mapSyslogNamestoGoid = new HashMap<String, Goid>();
            for (SinkConfiguration sinkConfiguration : sinkConfigurations)
                // we only want SysLog Sinks that are disabled, have the proper prefix and are syslogs
                if (sinkConfiguration.getName().startsWith(LogMessageToSysLogAssertion.SYSLOG_LOG_SINK_PREFIX) && !sinkConfiguration.isEnabled() && (sinkConfiguration.getType() == SinkConfiguration.SinkType.SYSLOG)) {
                    mapSyslogNamestoGoid.put(sinkConfiguration.getName(), sinkConfiguration.getGoid());
                }

        } catch (FindException e) {
            logger.log(Level.WARNING, "Log Message To SysLog Assertion failed to load Log Sinks because:  " + ExceptionUtils.getMessage(e),
                    e);
        }
    }

    private void loadSinkConfigurationIntoComboBox(Goid selectedSyslogGoid) {
        Set<String> keys = mapSyslogNamestoGoid.keySet();
        Set<String> sortedKeys = new TreeSet<String>(keys);

        for (String key : sortedKeys) {
            this.sysLogComboBox.addItem(key);

            Goid currentGoid = mapSyslogNamestoGoid.get(key);

            if (currentGoid.equals(selectedSyslogGoid)) {
                this.sysLogComboBox.setSelectedItem(key);
            }
        }

    }

    private void loadSysLogSeverityIntoComboBox(String previouslyChosenSyslogSeverity) {
        String[] keys = LogMessageToSysLogAssertion.sysLogSeverityStrings;

        for (String key : keys) {
            this.severityComboBox.addItem(key);

            if (key.equals(previouslyChosenSyslogSeverity)) {
                this.severityComboBox.setSelectedItem(key);
            }
        }
    }
}


