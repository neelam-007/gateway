package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.SwingWorker;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.console.table.WsdlTable;
import com.l7tech.console.table.WsdlTableSorter;
import com.l7tech.console.event.WsdlListener;
import com.l7tech.console.event.WsdlEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.objectmodel.FindException;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Vector;
import java.util.EventListener;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.io.IOException;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
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

                    Vector urlList = new Vector();
                    for (int i = 0; i < ((WsdlInfo[])result).length; i++) {
                        urlList.add(((WsdlInfo[])result)[i]);
                    }

                    // populate the data to the table
                    ((WsdlTableSorter) wsdlTable.getModel()).setData(urlList);
                    retrievedRows.setText("Result: " + urlList.size());

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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 4, new Insets(0, 5, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel label1 = new JLabel();
        label1.setText("Criteria:");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        searchButton = new JButton();
        searchButton.setText("Search");
        panel2.add(searchButton, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        serviceNameSearchPattern = new JTextField();
        panel2.add(serviceNameSearchPattern, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JLabel label2 = new JLabel();
        label2.setText("Service Name:");
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        serviceNameFilterOptionComboBox = new JComboBox();
        panel2.add(serviceNameFilterOptionComboBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        caseSensitiveCheckBox = new JCheckBox();
        caseSensitiveCheckBox.setText("Case Sensitive");
        panel2.add(caseSensitiveCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("UDDI Registry URL:");
        panel2.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        uddiURLcomboBox = new JComboBox();
        panel2.add(uddiURLcomboBox, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Search WSDLs in UDDI Registry:");
        panel1.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        selectButton = new JButton();
        selectButton.setText("Select");
        panel3.add(selectButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel3.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 5, 0, 0), -1, -1));
        mainPanel.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        wsdlScrollPane = new JScrollPane();
        panel4.add(wsdlScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        retrievedRows = new JLabel();
        retrievedRows.setText("Result:");
        panel4.add(retrievedRows, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
