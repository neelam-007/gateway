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
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 0, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(10, 0, 5, 5), -1, -1));
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 0, 0, null, null, null));
        final JButton _5;
        _5 = new JButton();
        searchButton = _5;
        _5.setText("Search");
        _4.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 0, 0, null, null, null));
        final JButton _6;
        _6 = new JButton();
        stopButton = _6;
        _6.setText("Stop");
        _4.add(_6, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 1, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _7;
        _7 = new com.intellij.uiDesigner.core.Spacer();
        _4.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JTabbedPane _8;
        _8 = new JTabbedPane();
        _3.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 100), null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 3, new Insets(10, 8, 8, 8), -1, -1));
        _8.addTab("Criteria", _9);
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Subject DN");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setText("Issuer Name");
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _12;
        _12 = new JTextField();
        subjectNameTextField = _12;
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _13;
        _13 = new JTextField();
        issuerNameTextField = _13;
        _9.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JComboBox _14;
        _14 = new JComboBox();
        subjectSearchComboBox = _14;
        _9.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 2, 0, null, null, null));
        final JComboBox _15;
        _15 = new JComboBox();
        issuerSearchComboBox = _15;
        _9.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 2, 0, null, null, null));
        final JPanel _16;
        _16 = new JPanel();
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _17;
        _17 = new JLabel();
        _17.setText("Search the trusted certificates in the store:");
        _16.add(_17, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _18;
        _18 = new JPanel();
        _18.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(8, 0, 0, 0), -1, -1));
        _1.add(_18, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JScrollPane _19;
        _19 = new JScrollPane();
        certScrollPane = _19;
        _18.add(_19, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JPanel _20;
        _20 = new JPanel();
        _20.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _18.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _21;
        _21 = new JLabel();
        _21.setText("Search Results:");
        _20.add(_21, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _22;
        _22 = new JPanel();
        _22.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        _18.add(_22, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _23;
        _23 = new JButton();
        selectButton = _23;
        _23.setText("Select");
        _22.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _24;
        _24 = new JButton();
        viewButton = _24;
        _24.setText("View");
        _22.add(_24, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _25;
        _25 = new com.intellij.uiDesigner.core.Spacer();
        _22.add(_25, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _26;
        _26 = new JButton();
        cancelButton = _26;
        _26.setText("Cancel");
        _22.add(_26, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _27;
        _27 = new com.intellij.uiDesigner.core.Spacer();
        _22.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final JLabel _28;
        _28 = new JLabel();
        resultCounter = _28;
        _28.setText("");
        _22.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }

}
