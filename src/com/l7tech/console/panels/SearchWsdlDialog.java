package com.l7tech.console.panels;

import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.table.WsdlTable;
import com.l7tech.console.table.WsdlTableSorter;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.service.ServiceAdmin;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.EventListener;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Logger;


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
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.SearchWsdlDialog", Locale.getDefault());
    private final static String EQUALS = "Equals";
    private final static String CONTAINS = "Contains";
    private EventListenerList listenerList = new EventListenerList();
    private JCheckBox caseSensitiveCheckBox;
    private JLabel retrievedRows;
    private JComboBox uddiURLcomboBox;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public SearchWsdlDialog(JDialog parent) throws RemoteException, FindException {
        super(parent, resources.getString("window.title"), true);
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() throws RemoteException, FindException {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        serviceNameFilterOptionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { CONTAINS, EQUALS }));

        ServiceAdmin seriveAdmin = Registry.getDefault().getServiceManager();
        if (seriveAdmin == null) throw new RuntimeException("Service Admin reference not found");
        String[] uddiRegistryURLs = null;

        try {
            uddiRegistryURLs = seriveAdmin.findUDDIRegistryURLs();
        } catch(RemoteException re) {
            logger.warning("Remote Exception caught. Unable to get the URLs of UDDI Registries");
            throw re;
        } catch(FindException fe) {
            logger.warning("Exception caught. Unable to get the URLs of UDDI Registries");
            throw fe;
        }

        uddiURLcomboBox.setModel(new javax.swing.DefaultComboBoxModel(uddiRegistryURLs));
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
                    if(((String)serviceNameFilterOptionComboBox.getSelectedItem()).equals(CONTAINS)) {
                        searchString = "%" + serviceNameSearchPattern.getText() + "%";
                    } else {
                        searchString =  serviceNameSearchPattern.getText();
                    }
                } else {
                    searchString = "%";
                }

                // clear the list
                ((WsdlTableSorter) wsdlTable.getModel()).clearData();
                retrievedRows.setText("Result: 0");

                Dialog rootDialog = (Dialog) SwingUtilities.getWindowAncestor(SearchWsdlDialog.this);
                final CancelableOperationDialog dlg = new CancelableOperationDialog(rootDialog,
                                                                                "Searching WSDL",
                                                                                "Please wait, Searching WSDL...");
                SwingWorker worker = new SwingWorker() {

                    public Object construct() {
                        try {

                            ServiceAdmin seriveAdmin = Registry.getDefault().getServiceManager();
                            if (seriveAdmin == null) throw new RuntimeException("Service Admin reference not found");

                            WsdlInfo[] urls = seriveAdmin.findWsdlUrlsFromUDDIRegistry((String) uddiURLcomboBox.getSelectedItem(), searchString, caseSensitiveCheckBox.isSelected());
                            return urls;
                        } catch (RemoteException e) {
                            JOptionPane.showMessageDialog(SearchWsdlDialog.this, "Remote Exception, " + e.getMessage(), "Search UDDI Registry", JOptionPane.ERROR_MESSAGE);
                            return null;
                        } catch (FindException e) {
                            JOptionPane.showMessageDialog(SearchWsdlDialog.this, "Find Exception, " + e.getMessage(), "Search UDDI Registry", JOptionPane.ERROR_MESSAGE);
                            return null;
                        }
                    }

                    public void finished() {
                        dlg.hide();
                    }
                };

                worker.start();
                dlg.show();
                worker.interrupt();
                Object result = worker.get();
                if (result == null)
                    return;    // canceled
                if (result instanceof WsdlInfo[]) {
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

            };
        });
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
}
