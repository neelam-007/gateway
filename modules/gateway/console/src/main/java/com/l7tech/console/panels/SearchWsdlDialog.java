package com.l7tech.console.panels;

import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.util.ArrayUtils;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.table.WsdlTable;
import com.l7tech.console.table.WsdlTableSorter;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 */
public class SearchWsdlDialog extends JDialog {

    private JScrollPane wsdlScrollPane;
    private JPanel mainPanel;
    private JButton searchButton;
    private JComboBox serviceNameFilterOptionComboBox;
    private JTextField serviceNameSearchPattern;
    private JButton selectButton;
    private JButton cancelButton;
    private WsdlTable wsdlTable = null;
    private EventListenerList listenerList = new EventListenerList();
    private JCheckBox caseSensitiveCheckBox;
    private JLabel retrievedRows;
    private JComboBox uddiTypeComboBox;
    private JComboBox uddiURLcomboBox;
    private JTextField uddiAccountNameTextField;
    private JPasswordField uddiAccountPasswordField;

    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private UDDIRegistryInfo[] registryTypeInfo;

    private static final Logger logger = Logger.getLogger(SearchWsdlDialog.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.SearchWsdlDialog", Locale.getDefault());
    private static final String EQUALS = "Equals";
    private static final String CONTAINS = "Contains";
    private static final String UDDI_TYPE = "UDDI.TYPE";
    private static final String UDDI_URL = "UDDI.URL";
    private static final String UDDI_ACCOUNT_NAME = "UDDI.ACCOUNT.NAME";
    private static final int MAX_SERVICE_NAME_LENGTH = 255;

    public SearchWsdlDialog(JDialog parent) throws FindException {
        super(parent, resources.getString("window.title"), true);
        initialize();
    }

    public SearchWsdlDialog(JFrame parent) throws FindException {
        super(parent, resources.getString("window.title"), true);
        initialize();
    }

    public static boolean uddiEnabled() {
        boolean enabled = false;

        try {
            ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
            enabled = serviceAdmin.findUDDIRegistryURLs().length > 0 &&
                    !serviceAdmin.getUDDIRegistryInfo().isEmpty();
        }
        catch(Exception e) {
            logger.log(Level.WARNING, "Could not check if UDDI is enabled. '"+e.getMessage()+"'.");
        }

        return enabled;
    }

    private void initialize() throws FindException {
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        // load prefs
        String uddiType = preferences.getString(UDDI_TYPE, "");
        String uddiUrl = preferences.getString(UDDI_URL, "");
        String uddiAccount = preferences.getString(UDDI_ACCOUNT_NAME, "");

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        // The default-service-name-filter option is "Contains", so the valid max length of the search input
        // is MAX_SERVICE_NAME_LENGTH - 2, since there are two '%'s at the begin and the end respectively.
        ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(MAX_SERVICE_NAME_LENGTH - 2));
        serviceNameFilterOptionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { CONTAINS, EQUALS }));
        serviceNameFilterOptionComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                JComboBox cb = (JComboBox)actionEvent.getSource();
                String filterName = (String)cb.getSelectedItem();
                int validMaxLen = MAX_SERVICE_NAME_LENGTH;
                if (CONTAINS.equals(filterName)) {
                    // since there are two '%' in the search input: the first % locates at the begin and the second % locates the end.
                    validMaxLen = MAX_SERVICE_NAME_LENGTH - 2;
                    // Check if keeping the search string when swapping the two filter options
                    if (serviceNameSearchPattern.getText().length() > validMaxLen) {
                        serviceNameSearchPattern.setText("");
                    }
                }
                ((AbstractDocument)serviceNameSearchPattern.getDocument()).setDocumentFilter(new DocumentSizeFilter(validMaxLen));
            }
        });

        ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
        if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");

        registryTypeInfo = serviceAdmin.getUDDIRegistryInfo().toArray(new UDDIRegistryInfo[0]);
        String[] typeNames = toNames(registryTypeInfo);
        uddiTypeComboBox.setModel(new DefaultComboBoxModel(typeNames));
        if ( ArrayUtils.contains( typeNames, uddiType ) ) {
            uddiTypeComboBox.setSelectedItem(uddiType);
        } else if (typeNames.length>0) {
            uddiTypeComboBox.setSelectedItem(typeNames[0]);
        }

        String[] uddiRegistryURLs = null;
        try {
            uddiRegistryURLs = serviceAdmin.findUDDIRegistryURLs();
        } catch(FindException fe) {
            logger.warning("Exception caught. Unable to get the URLs of UDDI Registries");
            throw fe;
        }

        uddiURLcomboBox.setModel(new javax.swing.DefaultComboBoxModel(uddiRegistryURLs));
        for (String url : uddiRegistryURLs) {
            if ( url.startsWith(uddiUrl) ) {
                uddiURLcomboBox.setSelectedItem(url);
                uddiAccountNameTextField.setText(uddiAccount);
                break;
            }
        }

        if (wsdlTable == null) {
            wsdlTable = new WsdlTable();
        }
        wsdlScrollPane.setViewportView(wsdlTable);
        wsdlScrollPane.getViewport().setBackground(Color.white);

        selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                int row = wsdlTable.getSelectedRow();
                if (row != -1) {
                    WsdlInfo si = (WsdlInfo) wsdlTable.getTableSorter().getData(row);

                    fireEventWsdlSelected(si);
                }
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                SearchWsdlDialog.this.dispose();
            }
        });

        searchButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                final String searchString;

                if(serviceNameSearchPattern.getText().length() > 0) {
                    String escapedStr = escapeString(serviceNameSearchPattern.getText());
                    if(((String)serviceNameFilterOptionComboBox.getSelectedItem()).equals(CONTAINS)) {
                        searchString = "%" + escapedStr + "%";
                    } else {
                        searchString = escapedStr;
                    }
                } else {
                    searchString = "%";
                }

                // clear the list
                ((WsdlTableSorter) wsdlTable.getModel()).clearData();
                retrievedRows.setText("Result: 0");

                final CancelableOperationDialog dlg =
                        new CancelableOperationDialog(SearchWsdlDialog.this, "Searching WSDL", "Please wait, Searching WSDL...");

                final String type = (String) uddiTypeComboBox.getSelectedItem();
                final String url = (String) uddiURLcomboBox.getSelectedItem();
                final String username = uddiAccountNameTextField.getText();
                final char[] password = uddiAccountPasswordField.getPassword();

                final SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        try {

                            ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                            if (serviceAdmin == null) throw new RuntimeException("Service Admin reference not found");

                            UDDIRegistryInfo info = getRegistryTypeInfo(registryTypeInfo, type);
                            WsdlInfo[] urls = serviceAdmin.findWsdlUrlsFromUDDIRegistry(url, info, username, password, searchString, caseSensitiveCheckBox.isSelected());
                            return urls;
                        } catch (FindException e) {
                            logger.log(Level.WARNING, "error finding wsdl urls from uddi", e);
                            JOptionPane.showMessageDialog(SearchWsdlDialog.this, "Find Exception, " + e.getMessage(), "Search UDDI Registry", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }

                    public void finished() {
                        dlg.dispose();
                    }
                };

                worker.start();

                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        worker.interrupt();
                        Object result = worker.get();
                        if (result == null)
                            return;    // canceled
                        if (result instanceof WsdlInfo[]) {
                            // store prefs on successful search
                            preferences.putProperty(UDDI_TYPE, type);
                            preferences.putProperty(UDDI_URL, url);
                            preferences.putProperty(UDDI_ACCOUNT_NAME, username);

                            boolean searchTruncated = false;
                            Vector urlList = new Vector();
                            for (int i = 0; i < ((WsdlInfo[])result).length; i++) {
                                final WsdlInfo wi = ((WsdlInfo[])result)[i];
                                if (WsdlInfo.MAXED_OUT_WSDL_URL.equals(wi.getWsdlUrl())) {
                                    // Flag value indicating that search results were truncated
                                    searchTruncated = true;
                                } else {
                                    // Normal result
                                    urlList.add(wi);
                                }
                            }

                            // populate the data to the table
                            ((WsdlTableSorter) wsdlTable.getModel()).setData(urlList);
                            String warning = "";
                            if (searchTruncated) {
                                warning = "  <b>(QUERY TOO BROAD - Only the first " + urlList.size() + " results are presented)</b>";
                                retrievedRows.setForeground(new Color(255, 64, 64));
                            } else {
                                retrievedRows.setForeground(new JLabel().getForeground());
                            }
                            retrievedRows.setText("<HTML>Result: " + urlList.size() + warning);

                            return;
                        }

                        retrievedRows.setText("Result: 0");

                    }
                });
            }
        });

        pack();
        Utilities.setDoubleClickAction(wsdlTable, selectButton);
        Utilities.centerOnScreen(this);
    }

    /**
     * notfy the listeners
     *
     * @param wsdlInfo the trusted certs
     */
    private void fireEventWsdlSelected(final WsdlInfo wsdlInfo) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                WsdlEvent event = new WsdlEvent(this, wsdlInfo);
                EventListener[] listeners = listenerList.getListeners(WsdlListener.class);
                for (int j = 0; j < listeners.length; j++) {
                    ((WsdlListener) listeners[j]).wsdlSelected(event);
                }
            }
        });
    }

    private UDDIRegistryInfo getRegistryTypeInfo(UDDIRegistryInfo[] registryInfos, String name) {
        UDDIRegistryInfo info = null;

        for ( UDDIRegistryInfo currentInfo : registryInfos ) {
            if ( name.equals(currentInfo.getName()) ) {
                info = currentInfo;
                break;
            }
        }

        return info;
    }

    private String[] toNames(UDDIRegistryInfo[] registryInfos) {
        String[] names = new String[registryInfos.length];

        for (int i=0; i<registryInfos.length; i++) {
            UDDIRegistryInfo info = registryInfos[i];
            names[i] = info.getName();
        }

        return names;
    }

    /**
     * add the WsdlListener
     *
     * @param listener the WsdlListener
     */
    public void addWsdlListener(WsdlListener listener) {
        listenerList.add(WsdlListener.class, listener);
    }

    /**
     * remove the the WsdlListener
     *
     * @param listener the WsdlListener
     */
    public void removeWsdlListener(WsdlListener listener) {
        listenerList.remove(WsdlListener.class, listener);
    }

    /**
     * @param string    The string that will be escaped.
     * @return  The new escaped string which will only escape the following characters:
     *
     *<ul>
     * <li>% --> \%</li>
     * <li>_ --> \_</li>
     * <li>\ --> \\</li> 
     * </ul>
     */
    public String escapeString(String string) {
        StringBuffer result = new StringBuffer("");
        for (int i=0; i < string.length(); i++) {
            char character = string.charAt(i);
            if (character == '%') {
                result.append("\\%");
            } else if (character == '_') {
                result.append("\\_");
            } else if (character == '\\') {
                result.append("\\\\");
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }
}
