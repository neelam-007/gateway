package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelListener;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Logger;
import java.rmi.RemoteException;

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

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private TrustedCertsTable trustedCertTable = null;

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

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

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
              int counter = 0;

              /**
               * This fine grain notification tells listeners the exact range
               * of cells, rows, or columns that changed.
               */
              public void tableChanged(TableModelEvent e) {
                  if (e.getType() == TableModelEvent.INSERT) {
                      counter += e.getLastRow() - e.getFirstRow();
                      resultCounter.setText("[ " + counter + " objects found]");
                      searchButton.setEnabled(true);
                      stopButton.setEnabled(false);
                  }
              }
          });

        searchButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                loadTrustedCerts();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

            }
        });

        selectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

            }
        });

        viewButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
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
        int row = trustedCertTable.getSelectedRow();
        if (row >= 0) {
            viewEnabled = true;
            selectEnabled = true;
        }
        viewButton.setEnabled(viewEnabled);
        selectButton.setEnabled(selectEnabled);
    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() {

        java.util.List certList = null;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            Vector certs = new Vector();
            for (int i = 0; i < certList.size(); i++) {
                Object o = (Object) certList.get(i);
                certs.add(o);
            }
            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();

        } catch (RemoteException re) {
            JOptionPane.showMessageDialog(this, resources.getString("cert.remote.exception"),
                    resources.getString("load.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        } catch (FindException e) {
            JOptionPane.showMessageDialog(this, resources.getString("cert.find.error"),
                    resources.getString("load.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
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
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(10, 0, 5, 5), -1, -1));
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _5;
        _5 = new JButton();
        searchButton = _5;
        _5.setText("Search");
        _4.add(_5, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _6;
        _6 = new JButton();
        stopButton = _6;
        _6.setText("Stop");
        _4.add(_6, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _7;
        _7 = new com.intellij.uiDesigner.core.Spacer();
        _4.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JTabbedPane _8;
        _8 = new JTabbedPane();
        _3.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 100), null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 8, 8, 8), -1, -1));
        _8.addTab("Criteria", _9);
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Subject DN starts with:");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _11;
        _11 = new JLabel();
        _11.setText("Issuer name starts with:");
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _12;
        _12 = new JTextField();
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _13;
        _13 = new JTextField();
        _9.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPanel _14;
        _14 = new JPanel();
        _14.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _2.add(_14, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _15;
        _15 = new JLabel();
        _15.setText("Search the trusted certificates in the store:");
        _14.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _16;
        _16 = new JPanel();
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(8, 0, 0, 0), -1, -1));
        _1.add(_16, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JScrollPane _17;
        _17 = new JScrollPane();
        certScrollPane = _17;
        _16.add(_17, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JPanel _18;
        _18 = new JPanel();
        _18.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _16.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _19;
        _19 = new JLabel();
        _19.setText("Search Results:");
        _18.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _20;
        _20 = new JPanel();
        _20.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        _16.add(_20, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _21;
        _21 = new JButton();
        selectButton = _21;
        _21.setText("Select");
        _20.add(_21, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _22;
        _22 = new JButton();
        viewButton = _22;
        _22.setText("View");
        _20.add(_22, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _23;
        _23 = new com.intellij.uiDesigner.core.Spacer();
        _20.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _24;
        _24 = new JButton();
        cancelButton = _24;
        _24.setText("Cancel");
        _20.add(_24, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _25;
        _25 = new com.intellij.uiDesigner.core.Spacer();
        _20.add(_25, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final JLabel _26;
        _26 = new JLabel();
        resultCounter = _26;
        _26.setText("");
        _20.add(_26, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }

}
