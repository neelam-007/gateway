package com.l7tech.console.panels;

import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.event.*;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.util.*;
import java.util.logging.Logger;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.io.IOException;

/**
 * This class provides the step panel for the Federated Identity Provider dialog.
 *
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class FederatedIPTrustedCertsPanel extends IdentityProviderStepPanel {
    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private boolean holderOfKeySelected = false;
    private boolean x509CertSelected = false;
    private boolean limitationsAccepted = true;

    private TrustedCertsTable trustedCertTable = null;
    private X509Certificate ssgcert = null;

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(FederatedIPTrustedCertsPanel.class.getName());

    /**
     * Construstor
     *
     * @param next  The next step panel
     */
    public FederatedIPTrustedCertsPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    /**
     * Get the description of the step
     * @return String The description of the step
     */
    public String getDescription() {
        return "Select the certificates that will be trusted by this Identity Provider " +
               "from the SecureSpan Gateway's trusted certificate store.";
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return "Select the Trusted Certificates";
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        holderOfKeySelected = iProviderConfig.isSamlSupported() && iProviderConfig.getSamlConfig().isSubjConfHolderOfKey();
        x509CertSelected = iProviderConfig.isX509Supported();

        long[] oids = iProviderConfig.getTrustedCertOids();
        Vector certs = new Vector();
        TrustedCert cert = null;

        for (int i = 0; i < oids.length; i++) {

            try {
                cert = getTrustedCertAdmin().findCertByPrimaryKey(oids[i]);

                if (cert != null) {
                    certs.add(cert);
                }

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

        if (certs.size() > 0) {
            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();
        }
    }


    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
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

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        Vector data = trustedCertTable.getTableSorter().getAllData();
        long[] oids = new long[data.size()];

        for (int i = 0; i < data.size(); i++) {
            TrustedCert tc = (TrustedCert) data.elementAt(i);
            oids[i] = tc.getOid();
        }

        iProviderConfig.setTrustedCertOids(oids);

    }

    /**
     * Perform any panel-specific last-second checking at the time the user presses the "Next"
     * (or "Finish") button
     * while this panel is showing.  The panel may veto the action by returning false here.
     * Since this method is called in response to user input it may take possibly-lengthy actions
     * such as downloading a remote file.
     *
     * This differs from canAdvance() in that it is called when the user actually hits the Next button,
     * whereas canAdvance() is used to determine if the Next button is even enabled.
     *
     * @return true if it is safe to advance to the next step; false if not (and the user may have
     *            been pestered with an error dialog).
     */
     public boolean onNextButton() {
        final JDialog owner = getOwner();
        if(trustedCertTable.getModel().getRowCount() == 0) {

            FederatedIPWarningDialog d = new FederatedIPWarningDialog(owner, createMsgPanel());

            d.setSize(650, 350);
            d.addWizardListener(wizardListener);
            Utilities.centerOnScreen(d);
            d.setVisible(true);
        } else {
            return true;
        }

        return limitationsAccepted;
    }

    /**
     *  Create the Message Panel
     *
     * @return  JPanel  The Message Panel
     */
    private JPanel createMsgPanel() {
        Icon icon = new ImageIcon(getClass().getClassLoader().getResource(RESOURCE_PATH + "/check16.gif"));

        int position = 0;
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));

        final JLabel virtualGroupMsg = new JLabel();
        virtualGroupMsg.setText(" Virtual Group not supported");
        virtualGroupMsg.setIcon(icon);
        panel1.add(virtualGroupMsg, new GridConstraints(position++, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

        if(x509CertSelected) {
            final JLabel x509Msg = new JLabel();
            x509Msg.setText(" Every identity that you wish to authorize using X.509 credentials will need to have their certificate imported manually");
            x509Msg.setIcon(icon);
            panel1.add(x509Msg, new GridConstraints(position++, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        }

        if(holderOfKeySelected) {
            final JLabel holderOfKeyMsg = new JLabel();
            holderOfKeyMsg.setText(" Holder-of-Key Subject Confirmation Method not supported");
            holderOfKeyMsg.setIcon(icon);
            panel1.add(holderOfKeyMsg, new GridConstraints(position++, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        }

        return panel1;
    }

    /**
     * Initialize the components of the Panel
     */
    private void initComponents() {

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

        // initialize the button states
        enableOrDisableButtons();

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


        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                CertSearchPanel sp = new CertSearchPanel(getOwner());
                sp.addCertListener(certListener);
                sp.show();
                sp.setSize(400, 600);

            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    trustedCertTable.getTableSorter().deleteRow(row);
                }
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    CertPropertiesWindow cpw = new CertPropertiesWindow(getOwner(), (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    cpw.show();
                }
            }
        });


    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        int row = trustedCertTable.getSelectedRow();
        if (row >= 0) {
            removeEnabled = true;
            propsEnabled = true;
        }
        removeButton.setEnabled(removeEnabled);
        propertiesButton.setEnabled(propsEnabled);
    }

    /**
     * Listener for handling the event of adding a cert to the FIP
     */
    private final CertListener certListener = new CertListenerAdapter() {
        public void certSelected(CertEvent e) {
            if(!trustedCertTable.getTableSorter().contains(e.getCert())) {
                TrustedCert tc = e.getCert();
                try {
                    if (isCertRelatedToSSG(tc)) {
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                      "This cert cannot be used as a trusted cert in this " +
                                                      "federated identity\nprovider because it is related to the " +
                                                      "SecureSpan Gateway's root cert.",
                                                      "Cannot add this cert",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else if (isCertTrustedByAnotherProvider(tc)) {
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                      "This cert cannot be used as a trusted cert in this " +
                                                      "federated identity\nprovider because it is already " +
                                                      "trusted by another identity provider.",
                                                      "Cannot add this cert",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else {
                        trustedCertTable.getTableSorter().addRow(tc);
                    }

                } catch (IOException e1) {
                    throw new RuntimeException(e1); //  not expected to happen
                } catch (CertificateException e1) {
                    throw new RuntimeException(e1); //  not expected to happen
                }
            } else {
                // cert alreay exsits
                JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this, resources.getString("add.cert.duplicated"),
                        resources.getString("add.cert.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }

    };

    private boolean isCertTrustedByAnotherProvider(TrustedCert trustedCert) {
        return false;
    }

    /**
     * Check whether or not the passed cert is related somehow to the ssg's root cert.
     * @return true if it is
     */
    private boolean isCertRelatedToSSG(TrustedCert trustedCert) throws IOException, CertificateException {
        if (ssgcert == null) {
            ssgcert = getTrustedCertAdmin().getSSGRootCert();
        }
        byte[] certbytes = HexUtils.decodeBase64(trustedCert.getCertBase64());
        X509Certificate[] chainToVerify = CertUtils.decodeCertChain(certbytes);
        try {
            CertUtils.verifyCertificateChain(chainToVerify, ssgcert, chainToVerify.length);
        } catch (CertUtils.CertificateUntrustedException e) {
            // this is what we were hoping for
            logger.finest("the cert is not related.");
            return false;
        }
        logger.finest("The cert appears to be related!");
        return true;
    }

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the dialog has finished.
         *
         * @param we the event describing the dialog finish
         */
        public void wizardFinished(WizardEvent we) {
            limitationsAccepted = true;
        }

        /**
         * Invoked when the wizard has cancelled.
         *
         * @param we the event describing the dialog cancelled
         */
        public void wizardCanceled(WizardEvent we) {
            limitationsAccepted =false;
        }

    };

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
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(5, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        addButton = new JButton();
        addButton.setText("Add");
        panel3.add(addButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        removeButton = new JButton();
        removeButton.setText("Remove");
        panel3.add(removeButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        propertiesButton = new JButton();
        propertiesButton.setText("Properties");
        panel3.add(propertiesButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        certScrollPane = new JScrollPane();
        panel2.add(certScrollPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(450, 300), null));
        final JLabel label1 = new JLabel();
        label1.setText("Trusted Certificates:");
        mainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
