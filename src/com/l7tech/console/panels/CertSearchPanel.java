package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.CertUtils;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertEvent;
import com.l7tech.objectmodel.FindException;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Vector;
import java.util.EventListener;
import java.util.logging.Logger;
import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CertSearchPanel extends JDialog {

    private JButton searchButton;
    private JButton stopButton;
    private JButton selectButton;
    private JButton viewButton;
    private JButton cancelButton;
    private JLabel resultCounter;
    private JComboBox subjectSearchComboBox;
    private JComboBox issuerSearchComboBox;
    private JTextField subjectNameTextField;
    private JTextField issuerNameTextField;
    private boolean cancelled;

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private TrustedCertsTable trustedCertTable = null;
    private EventListenerList listenerList = new EventListenerList();

    private final static String STARTS_WITH = "Starts with";
    private final static String EQUALS = "Equals";
    private final static int SEARCH_SELECTION_STARTS_WITH = 0;
    private final static int SEARCH_SELECTION_EQUALS = 1;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertSearchPanel.class.getName());

    /**
     * Constructor
     *
     * @param owner The parent component.
     */
    public CertSearchPanel(JDialog owner) {
        super(owner, resources.getString("cert.search.dialog.title"), true);
        initialize();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        stopButton.setEnabled(false);
        viewButton.setEnabled(false);
        selectButton.setEnabled(false);

        subjectSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS }));
        issuerSearchComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { STARTS_WITH, EQUALS }));

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);
        trustedCertTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        trustedCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            /**
             * Called whenever the value of the selection changes.
             *
             * @param e the event that characterizes the change.
             */
            public void valueChanged(ListSelectionEvent e) {

                enableOrDisableButtons();
            }
        });

        trustedCertTable.getTableSorter().addTableModelListener(new TableModelListener() {

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              public void tableChanged(TableModelEvent e) {
                  if (e.getType() == TableModelEvent.INSERT) {
                      resultCounter.setText("[ " + trustedCertTable.getTableSorter().getRealModel().getRowCount() + " objects found]");
                  }
              }
          });

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                cancelled = false;
                searchButton.setEnabled(false);
                stopButton.setEnabled(true);
                loadTrustedCerts();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                 cancelled = true;
            }
        });

        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row[] = trustedCertTable.getSelectedRows();
                TrustedCert tc;
                if (row.length > 0) {
                    TrustedCert[] certs = new TrustedCert[row.length];
                    for (int i = 0; i < row.length; i++) {
                        certs[i]  = (TrustedCert) trustedCertTable.getTableSorter().getData(row[i]);
                    }
                    fireEventCertSelected(certs);
                }

                dispose();
            }

        });

        viewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                CertPropertiesWindow cpw = null;
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    cpw = new CertPropertiesWindow(CertSearchPanel.this, (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                }

                cpw.show();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                dispose();
            }
        });

    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean viewEnabled = false;
        boolean selectEnabled = false;

        if (trustedCertTable.getSelectedRowCount() == 1) {
            viewEnabled = true;
        }

        if (trustedCertTable.getSelectedRowCount() > 0) {
            selectEnabled = true;
        }

        viewButton.setEnabled(viewEnabled);
        selectButton.setEnabled(selectEnabled);
    }

    /**
     * add the CertListener
     *
     * @param listener the CertListener
     */
    public void addCertListener(CertListener listener) {
        listenerList.add(CertListener.class, listener);
    }

    /**
     * remove the the CertListener
     *
     * @param listener the CertListener
     */
    public void removeCertListener(CertListener listener) {
        listenerList.remove(CertListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param certs the trusted certs
     */
    private void fireEventCertSelected(final TrustedCert[] certs) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                for (int i = 0; i < certs.length; i++) {
                    TrustedCert tc = certs[i];

                    CertEvent event = new CertEvent(this, tc);
                    EventListener[] listeners = listenerList.getListeners(CertListener.class);
                    for (int j = 0; j < listeners.length; j++) {
                        ((CertListener) listeners[j]).certSelected(event);
                    }
                }
            }
        });
    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() {

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {

                java.util.List certList = null;
                try {
                    certList = getTrustedCertAdmin().findAllCerts();

                    // clear the table
                    trustedCertTable.getTableSorter().setData(new Vector());
                    resultCounter.setText("[ " + trustedCertTable.getTableSorter().getRealModel().getRowCount() + " objects found]");
                    
                    //Vector certs = new Vector();
                    for (int i = 0; i < certList.size() && !cancelled ; i++) {
                        TrustedCert tc = (TrustedCert) certList.get(i);
                        if (isShown(tc)) {
                            trustedCertTable.getTableSorter().addRow(tc);
                        }
                    }

                } catch (RemoteException re) {
                    JOptionPane.showMessageDialog(CertSearchPanel.this, resources.getString("cert.remote.exception"),
                            resources.getString("load.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                } catch (FindException e) {
                    JOptionPane.showMessageDialog(CertSearchPanel.this, resources.getString("cert.find.error"),
                            resources.getString("load.error.title"),
                            JOptionPane.ERROR_MESSAGE);
                }
                
                stopButton.setEnabled(false);
                searchButton.setEnabled(true);
            }
        });
    }

    /**
     * Check if the cert should be shown or not
     * @param tc  The trusted cert
     * @return boolean TRUE if the cert should be shown, FALSE otherwise.
     */
    private boolean isShown(TrustedCert tc) {

        X509Certificate cert = null;
        try {
            cert = tc.getCertificate();
        } catch (CertificateException e) {
            logger.warning(resources.getString("cert.decode.error"));
            return false;

        } catch (IOException e) {
            logger.warning(resources.getString("cert.decode.error"));
            return false;
        }

        String subjectName = CertUtils.extractUsernameFromClientCertificate(cert);
        String issuerName = CertUtils.extractIssuerNameFromClientCertificate(cert);

        boolean show1 = true;
        boolean show2 = true;
        if (subjectNameTextField.getText().trim().length() > 0) {
            if (subjectSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_STARTS_WITH) {
                if(!subjectName.startsWith(subjectNameTextField.getText().trim())) {
                     show1 = false;
                }
            } else if (subjectSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_EQUALS) {
                if(!subjectName.equals(subjectNameTextField.getText().trim())) {
                     show1 = false;
                }
            }
        }

        if (issuerNameTextField.getText().trim().length() > 0) {
            if (issuerSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_STARTS_WITH) {
                if(!issuerName.startsWith(issuerNameTextField.getText().trim())) {
                     show2 = false;
                }
            } else if (issuerSearchComboBox.getSelectedIndex() == SEARCH_SELECTION_EQUALS) {
                 if(!issuerName.equals(issuerNameTextField.getText().trim())) {
                     show2 = false;
                }
            }
        }

        return (show1 && show2);
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException  if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca =
                (TrustedCertAdmin) Locator.
                getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not find registered " + TrustedCertAdmin.class);
        }

        return tca;
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
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(10, 0, 5, 5), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        searchButton = new JButton();
        searchButton.setText("Search");
        panel3.add(searchButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        stopButton = new JButton();
        stopButton.setText("Stop");
        panel3.add(stopButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        panel2.add(tabbedPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 100), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 3, new Insets(10, 8, 8, 8), -1, -1));
        tabbedPane1.addTab("Criteria", panel4);
        final JLabel label1 = new JLabel();
        label1.setText("Subject DN:");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Issuer Name:");
        panel4.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        subjectNameTextField = new JTextField();
        panel4.add(subjectNameTextField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        issuerNameTextField = new JTextField();
        panel4.add(issuerNameTextField, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        subjectSearchComboBox = new JComboBox();
        panel4.add(subjectSearchComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        issuerSearchComboBox = new JComboBox();
        panel4.add(issuerSearchComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Search the trusted certificates in the store:");
        panel5.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(3, 1, new Insets(8, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        certScrollPane = new JScrollPane();
        panel6.add(certScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Search Results:");
        panel7.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel8, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        selectButton = new JButton();
        selectButton.setText("Select");
        panel8.add(selectButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        viewButton = new JButton();
        viewButton.setText("View");
        panel8.add(viewButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer2 = new Spacer();
        panel8.add(spacer2, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel8.add(cancelButton, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer3 = new Spacer();
        panel8.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        resultCounter = new JLabel();
        resultCounter.setText("");
        panel8.add(resultCounter, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
