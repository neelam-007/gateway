package com.l7tech.proxy.gui;

import com.l7tech.common.gui.IntegerField;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.CertificatePanel;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.proxy.ClientProxy;
import com.l7tech.proxy.util.ClientLogger;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.PolicyAttachmentKey;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgEvent;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgListener;
import com.l7tech.proxy.datamodel.Managers;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.gui.util.IconManager;
import com.l7tech.proxy.gui.policy.PolicyTreeCellRenderer;
import com.l7tech.proxy.gui.policy.PolicyTreeModel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.TreeModel;
import javax.security.auth.x500.X500Principal;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Panel for editing properties of an SSG object.
 * User: mike
 * Date: May 26, 2003
 * Time: 11:14:36 AM
 */
public class SsgPropertyDialog extends PropertyDialog implements SsgListener {
    private static final ClientLogger log = ClientLogger.getInstance(SsgPropertyDialog.class);
    private static final Ssg referenceSsg = new Ssg(); // SSG bean with default values for all

    // Model
    private Ssg ssg; // The real Ssg instance, to which changes may be committed.
    private ClientProxy clientProxy;  // The Client Proxy we're attached to, so we can display the bind port.

    // View
    private int gridY = 0; // Used for layout

    //   View for General pane
    private JComponent generalPane;
    private JTextField fieldServerAddress;

    //   View for Identity pane
    private JComponent identityPane;
    private JTextField fieldUsername;
    private JPasswordField fieldPassword;
    private JButton clientCertButton;
    private JButton serverCertButton;
    private JCheckBox cbSavePassword;

    //   View for Network pane
    private JComponent networkPane;
    private WrappingLabel fieldLocalEndpoint;
    private WrappingLabel fieldWsdlEndpoint;
    private JRadioButton radioStandardPorts;
    private JRadioButton radioNonstandardPorts;
    private JTextField fieldSsgPort;
    private JTextField fieldSslPort;

    //   View for Agent Policy pane
    private JComponent agentPolicyPane;

    //   View for Service Policies pane
    private JComponent policiesPane;
    private JTree policyTree;
    private JTable policyTable;
    private ArrayList displayPolicies;
    private DisplayPolicyTableModel displayPolicyTableModel;
    private JButton buttonFlushPolicies;
    private boolean policyFlushRequested = false;

    /** Create an SsgPropertyDialog ready to edit an Ssg instance. */
    private SsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        super("Gateway Properties");
        this.clientProxy = clientProxy;
        tabbedPane.add("General", getGeneralPane());
        tabbedPane.add("Identity", getIdentityPane());
        tabbedPane.add("Network", getNetworkPane());
        //tabbedPane.add("Agent Policy", getAgentPolicyPane());
        tabbedPane.add("Service Policies", getPoliciesPane());
        ssg.addSsgListener(this);
        setSsg(ssg);
        pack();
    }

    protected void finalize() throws Throwable {
        ssg.removeSsgListener(this);
        super.finalize();
    }

    /**
     * Attempt to build an "edit properties" dialog box for the given Ssg.
     * @param ssg The ssg whose properties we intend to edit
     * @return The property dialog that will edit said properties.  Call show() on it to run it.
     */
    public static SsgPropertyDialog makeSsgPropertyDialog(ClientProxy clientProxy, final Ssg ssg) {
        return new SsgPropertyDialog(clientProxy, ssg);
    }

    private class DisplayPolicyTableModel extends AbstractTableModel {
        public int getRowCount() {
            return displayPolicies.size();
        }

        public int getColumnCount() {
            return 2;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getUri();
                case 1:
                    return ((PolicyAttachmentKey)displayPolicies.get(rowIndex)).getSoapAction();
            }
            log.error("SsgPropertyDialog: policyTable: invalid columnIndex: " + columnIndex);
            return null;
        }
    }

    private JComponent getAgentPolicyPane() {
        if (agentPolicyPane == null) {
            int y = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            agentPolicyPane = new JScrollPane(pane);
            agentPolicyPane.setBorder(BorderFactory.createEmptyBorder());


        }
        return agentPolicyPane;
    }

    private JComponent getPoliciesPane() {
        if (policiesPane == null) {
            int y = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            policiesPane = new JScrollPane(pane);
            policiesPane.setBorder(BorderFactory.createEmptyBorder());

            pane.add(new JLabel("<HTML><h4>Service Policies being cached by this Agent</h4></HTML>"),
                     new GridBagConstraints(0, y++, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.NORTHWEST,
                                            GridBagConstraints.BOTH,
                                            new Insets(14, 6, 6, 6), 3, 3));

            pane.add(new JLabel("Web services with cached policies:"),
                     new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(6, 6, 0, 6), 3, 3));

            buttonFlushPolicies = new JButton("Clear Policy Cache");
            buttonFlushPolicies.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    policyFlushRequested = true;
                    updatePolicyPanel();
                }
            });
            pane.add(buttonFlushPolicies,
                     new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(14, 6, 0, 6), 0, 0));

            displayPolicies = new ArrayList();
            displayPolicyTableModel = new DisplayPolicyTableModel();
            policyTable = new JTable(displayPolicyTableModel);
            policyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            policyTable.setCellSelectionEnabled(false);
            policyTable.setRowSelectionAllowed(true);
            policyTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            policyTable.setAutoCreateColumnsFromModel(true);
            policyTable.getColumnModel().getColumn(0).setHeaderValue("Body Namespace");
            policyTable.getColumnModel().getColumn(1).setHeaderValue("SOAPAction");
            policyTable.getTableHeader().setReorderingAllowed(false);
            JScrollPane policyTableSp = new JScrollPane(policyTable);
            policyTableSp.setPreferredSize(new Dimension(120, 120));
            pane.add(policyTableSp,
                     new GridBagConstraints(0, y++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 6, 3, 6), 0, 0));

            pane.add(new JLabel("Associated policy:"),
                     new GridBagConstraints(0, y++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(4, 6, 0, 6), 0, 0));

            policyTree = new JTree((TreeModel)null);
            policyTree.setCellRenderer(new PolicyTreeCellRenderer());
            JScrollPane policyTreeSp = new JScrollPane(policyTree);
            policyTreeSp.setPreferredSize(new Dimension(120, 120));
            pane.add(policyTreeSp,
                     new GridBagConstraints(0, y++, 2, 1, 100.0, 100.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(2, 6, 6, 6), 3, 3));

            policyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    displaySelectedPolicy();
                }
            });
        }
        return policiesPane;
    }

    private void displaySelectedPolicy() {
        // do this?    if (e.getValueIsAdjusting()) return;
        Policy policy = null;
        int row = policyTable.getSelectedRow();
        if (row >= 0 && row < displayPolicies.size())
            policy = ssg.lookupPolicy((PolicyAttachmentKey)displayPolicies.get(row));
        policyTree.setModel(policy == null ? null : new PolicyTreeModel(policy.getClientAssertion()));
    }

    private JComponent getIdentityPane() {
        if (identityPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            identityPane = new JScrollPane(pane);
            identityPane.setBorder(BorderFactory.createEmptyBorder());

            // Authentication panel

            JPanel authp = new JPanel(new GridBagLayout());
            authp.setBorder(BorderFactory.createTitledBorder(" Your username and password "));
            pane.add(authp,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(14, 5, 0, 5), 0, 0));

            int oy = gridY;
            gridY = 0;

            fieldUsername = new JTextField();
            fieldUsername.setPreferredSize(new Dimension(200, 20));
            authp.add(new JLabel("Username:"),
                      new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 0, 0), 0, 0));
            authp.add(fieldUsername,
                      new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(5, 5, 0, 5), 0, 0));

            fieldPassword = new JPasswordField();
            fieldPassword.setPreferredSize(new Dimension(200, 20));
            authp.add(new JLabel("Password:"),
                      new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.EAST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 5, 0), 0, 0));
            authp.add(fieldPassword,
                      new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.HORIZONTAL,
                                             new Insets(5, 5, 5, 5), 0, 0));

            cbSavePassword = new JCheckBox("Save this password to your hard disk");
            authp.add(cbSavePassword,
                      new GridBagConstraints(1, gridY++, 1, 1, 0.0, 0.0,
                                             GridBagConstraints.WEST,
                                             GridBagConstraints.NONE,
                                             new Insets(5, 5, 5, 0), 0, 0));

            gridY = oy;

            // Certificate buttons

            JPanel cpan = new JPanel(new GridBagLayout());
            cpan.setBorder(BorderFactory.createTitledBorder(" Certificates "));
            pane.add(cpan,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(14, 5, 0, 5), 0, 0));
            oy = gridY;
            gridY = 0;
            JButton cb = getClientCertificateButton();
            cpan.add(cb,
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 5, 5, 0), 0, 0));
            JButton scb = getServerCertificateButton();
            cpan.add(scb,
                     new GridBagConstraints(1, gridY++, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 5, 5, 5), 0, 0));

            gridY = oy;

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));

        }
        return identityPane;
    }

    private JComponent getNetworkPane() {
        if (networkPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            networkPane = new JScrollPane(pane);
            networkPane.setBorder(BorderFactory.createEmptyBorder());

            // Endpoint panel

            JPanel epp = new JPanel(new GridBagLayout());
            epp.setBorder(BorderFactory.createTitledBorder(" Incoming requests to this Agent "));
            pane.add(epp, new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                                 GridBagConstraints.WEST,
                                                 GridBagConstraints.HORIZONTAL,
                                                 new Insets(14, 5, 0, 5), 0, 0));

            int oy = gridY;
            gridY = 0;

            WrappingLabel splain01 = new WrappingLabel("The Agent will listen for incoming messages at this local " +
                                                       "URL, and route any such messages to this Gateway:", 2);
            epp.add(splain01,
                    new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(0, 5, 0, 0), 0, 0));

            fieldLocalEndpoint = new WrappingLabel("");
            fieldLocalEndpoint.setContextMenuEnabled(true);
            epp.add(new JLabel("Proxy URL:"),
                    new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.EAST,
                                           GridBagConstraints.NONE,
                                           new Insets(5, 15, 5, 0), 0, 0));
            epp.add(fieldLocalEndpoint,
                    new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(5, 5, 5, 5), 0, 0));

            WrappingLabel splain02 = new WrappingLabel("The Agent will offer proxied WSDL lookups at this local URL:", 1);
            epp.add(splain02,
                    new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(10, 5, 0, 0), 0, 0));
            fieldWsdlEndpoint = new WrappingLabel("");
            fieldWsdlEndpoint.setContextMenuEnabled(true);
            epp.add(new JLabel("WSDL URL:"),
                    new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.EAST,
                                           GridBagConstraints.NONE,
                                           new Insets(5, 15, 5, 0), 0, 0));
            epp.add(fieldWsdlEndpoint,
                    new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(5, 5, 5, 5), 0, 0));

            gridY = oy;


            // Gateway ports panel

            JPanel gpp = new JPanel(new GridBagLayout());
            gpp.setBorder(BorderFactory.createTitledBorder(" Outgoing requests to the Gateway "));
            pane.add(gpp,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(14, 5, 0, 5), 0, 0));

            gpp.add(new WrappingLabel("If your Gateway is listening on nonstandard ports, " +
                                      "you can configure them here.", 2),
                    new GridBagConstraints(0, 0, 5, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(0, 5, 0, 0), 0, 0));

            ButtonGroup bg = new ButtonGroup();
            radioStandardPorts = new JRadioButton("Gateway is using standard ports", true);
            radioStandardPorts.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateCustomPortsEnableState();
                }
            });
            bg.add(radioStandardPorts);
            gpp.add(radioStandardPorts,
                    new GridBagConstraints(0, 1, 5, 1, 0.0, 0.0,
                                           GridBagConstraints.SOUTHWEST,
                                           GridBagConstraints.NONE,
                                           new Insets(5, 5, 0, 5), 0, 0));

            radioNonstandardPorts = new JRadioButton("Gateway requires custom ports:", false);
            radioNonstandardPorts.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    updateCustomPortsEnableState();
                }
            });
            bg.add(radioNonstandardPorts);
            gpp.add(radioNonstandardPorts,
                    new GridBagConstraints(0, 2, 5, 1, 0.0, 0.0,
                                           GridBagConstraints.NORTHWEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 5, 5, 0), 0, 0));

            gpp.add(new JLabel("Web Service port:"),
                    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 40, 5, 0), 0, 0));
            fieldSsgPort = new JTextField("");
            initDfg(fieldSsgPort);
            fieldSsgPort.setDocument(new IntegerField(0, 65535));
            fieldSsgPort.setPreferredSize(new Dimension(50, 20));
            gpp.add(fieldSsgPort,
                    new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 5, 5, 5), 0, 0));
            gpp.add(new JLabel("SSL port:"),
                    new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 15, 5, 0), 0, 0));
            fieldSslPort = new JTextField("");
            initDfg(fieldSslPort);
            fieldSslPort.setDocument(new IntegerField(0, 65535));
            fieldSslPort.setPreferredSize(new Dimension(50, 20));
            gpp.add(fieldSslPort,
                    new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                                           GridBagConstraints.WEST,
                                           GridBagConstraints.NONE,
                                           new Insets(0, 5, 5, 5), 0, 0));
            gpp.add(new JPanel(),
                    new GridBagConstraints(4, 3, 1, 1, 1000.0, 0.0,
                                           GridBagConstraints.CENTER,
                                           GridBagConstraints.HORIZONTAL,
                                           new Insets(0, 0, 0, 0), 0, 0));

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 1, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }
        return networkPane;
    }

    /** Create panel controls.  Should be called only from a constructor. */
    private JComponent getGeneralPane() {
        if (generalPane == null) {
            gridY = 0;
            JPanel pane = new JPanel(new GridBagLayout());
            generalPane = new JScrollPane(pane);
            generalPane.setBorder(BorderFactory.createEmptyBorder());

            JLabel image = new JLabel(IconManager.getSplashImageIcon());
            image.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
            pane.add(image,
                     new GridBagConstraints(0, gridY++, 2, 1, 0.0, 0.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.NONE,
                                            new Insets(25, 5, 0, 0), 0, 0));

            String splaintext = "Please enter the host name or Internet address " +
                                "of the SecureSpan Gateway that will be processing your requests.";
            WrappingLabel splain01 = new WrappingLabel(splaintext, 3);
            pane.add(splain01,
                     new GridBagConstraints(0, gridY++, 2, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(25, 25, 0, 25), 0, 0));

            pane.add(new JLabel("Gateway Hostname:"),
                     new GridBagConstraints(0, gridY, 1, 1, 0.0, 0.0,
                                            GridBagConstraints.EAST,
                                            GridBagConstraints.NONE,
                                            new Insets(5, 25, 0, 0), 0, 0));
            pane.add(getFieldServerAddress(),
                     new GridBagConstraints(1, gridY++, 1, 1, 1000.0, 0.0,
                                            GridBagConstraints.WEST,
                                            GridBagConstraints.HORIZONTAL,
                                            new Insets(5, 5, 0, 25), 0, 0));

            // Have a spacer eat any leftover space
            pane.add(new JPanel(),
                     new GridBagConstraints(0, gridY++, 2, 1, 1.0, 1.0,
                                            GridBagConstraints.CENTER,
                                            GridBagConstraints.BOTH,
                                            new Insets(0, 0, 0, 0), 0, 0));
        }

        return generalPane;
    }

    private JButton getServerCertificateButton() {
        if (serverCertButton == null) {
            serverCertButton = new JButton("View Gateway's certificate");
            serverCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        X509Certificate cert = SsgKeyStoreManager.getServerCert(ssg);
                        if (cert == null) {
                            JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                          "We haven't yet discovered the server certificate\n" +
                                                          "for the Gateway " + ssgName(),
                                                          "No server certificate",
                                                          JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        new CertDialog(cert, "Server Certificate", "Server Certificate for Gateway " + ssgName()).show();
                    } catch (Exception e1) {
                        log.error(e1);
                        e1.printStackTrace();
                        Gui.errorMessage("Unable to access server certificate",
                                         "Unable to access server certificate for Gateway " + ssgName(),
                                         e1);
                    }
                }
            });
        }
        return serverCertButton;
    }

    private void updateCustomPortsEnableState() {
        boolean en = radioNonstandardPorts.isSelected();
        setEnabled(fieldSsgPort, en);
        setEnabled(fieldSslPort, en);
    }

    private JButton getClientCertificateButton() {
        if (clientCertButton == null) {
            clientCertButton = new JButton("View your client certificate");
            clientCertButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        X509Certificate cert = SsgKeyStoreManager.getClientCert(ssg);
                        if (cert == null) {
                            JOptionPane.showMessageDialog(Gui.getInstance().getFrame(),
                                                          "We don't currently have a client certificate\n" +
                                                          "for the Gateway " + ssgName(),
                                                          "No client certificate",
                                                          JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        new CertDialog(cert, "Client Certificate", "Client Certificate for Gateway " + ssgName()).show();
                    } catch (Exception e1) {
                        log.error(e1);
                        e1.printStackTrace();
                        Gui.errorMessage("Unable to access client certificate",
                                         "Unable to access client certificate for Gateway " + ssgName(),
                                         e1);
                    }
                }
            });
        }
        return clientCertButton;
    }

    private class CertDialog extends JDialog {
        CertDialog(X509Certificate cert, String title, String mess) throws CertificateEncodingException, NoSuchAlgorithmException {
            super(SsgPropertyDialog.this, title, true);
            Container c = this.getContentPane();
            c.setLayout(new GridBagLayout());
            c.setSize(new Dimension(300, 200));
            c.add(new JLabel(mess),
                  new GridBagConstraints(0, 0, 3, 1, 0.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(5, 5, 5, 5), 0, 0));
            CertificatePanel cpan = new CertificatePanel(cert);
            c.add(cpan,
                  new GridBagConstraints(0, 1, 3, 1, 1000.0, 1000.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.BOTH,
                                         new Insets(5, 5, 5, 5), 0, 0));
            c.add(getCloseButton(),
                  new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                         GridBagConstraints.CENTER,
                                         GridBagConstraints.NONE,
                                         new Insets(5, 5, 5, 5), 0, 0));
            pack();
            Utilities.centerOnScreen(this);
        }

        JButton getCloseButton() {
            JButton cb = new JButton("Ok");
            cb.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    CertDialog.this.hide();
                }
            });
            return cb;
        }
    }

    /** Enable or disable the Ok button, depending on whether all input is acceptable. */
    private void checkOk() {
        if (fieldServerAddress.getText().length() > 1)
            SsgPropertyDialog.this.enableOk();
        else
            SsgPropertyDialog.this.disableOk();
    }

    /** Get the Server URL text field. */
    private JTextField getFieldServerAddress() {
        if (fieldServerAddress == null) {
            fieldServerAddress = new JTextField();
            fieldServerAddress.setPreferredSize(new Dimension(220, 20));
            fieldServerAddress.setToolTipText("<HTML>Gateway hostname or address, for example<br><address>gateway.example.com");
            fieldServerAddress.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    checkOk();
                }

                public void removeUpdate(DocumentEvent e) {
                    checkOk();
                }

                public void changedUpdate(DocumentEvent e) {
                    checkOk();
                }
            });
        }
        return fieldServerAddress;
    }

    /** Update the policy display panel with information from the Ssg bean. */
    private void updatePolicyPanel() {
        displayPolicies.clear();
        if (!policyFlushRequested)
            displayPolicies = new ArrayList(ssg.getPolicyAttachmentKeys());
        displayPolicyTableModel.fireTableDataChanged();
        displaySelectedPolicy();
    }

    private boolean isPortsCustom(Ssg ssg) {
        return referenceSsg.getSsgPort() != ssg.getSsgPort() || referenceSsg.getSslPort() != ssg.getSslPort();
    }

    /** Set the Ssg object being edited by this panel. */
    public void setSsg(final Ssg ssg) {
        this.ssg = ssg;
        synchronized (ssg) {

            fieldLocalEndpoint.setText("http://localhost:" + clientProxy.getBindPort() + "/" +
                                       ssg.getLocalEndpoint());
            fieldWsdlEndpoint.setText("http://localhost:" + clientProxy.getBindPort() + "/" +
                                      ssg.getLocalEndpoint() + ClientProxy.WSIL_SUFFIX);
            fieldServerAddress.setText(ssg.getSsgAddress());
            fieldUsername.setText(ssg.getUsername());
            char[] pass = ssg.cmPassword();
            boolean hasPassword = pass != null;
            fieldPassword.setText(new String(hasPassword ? pass : "".toCharArray()));
            policyFlushRequested = false;
            fieldSsgPort.setText(Integer.toString(ssg.getSsgPort()));
            fieldSslPort.setText(Integer.toString(ssg.getSslPort()));
            boolean customPorts = isPortsCustom(ssg);
            radioStandardPorts.setSelected(!customPorts);
            radioNonstandardPorts.setSelected(customPorts);
            cbSavePassword.setSelected(ssg.isSavePasswordToDisk());
            updateCustomPortsEnableState();

            try {
                if (SsgKeyStoreManager.isClientCertAvailabile(ssg)) {
                    X509Certificate cert = null;
                    cert = SsgKeyStoreManager.getClientCert(ssg);
                    X500Principal certName = new X500Principal(cert.getSubjectDN().toString());
                    String certNameString = certName.getName(X500Principal.CANONICAL);
                    fieldUsername.setText(certNameString.substring(3));
                    fieldUsername.setEditable(false);
                }
            } catch (KeyStoreCorruptException e) {
                try {
                    Managers.getCredentialManager().notifyKeyStoreCorrupt(ssg);
                    SsgKeyStoreManager.deleteKeyStore(ssg);
                    // FALLTHROUGH -- continue, with newly-blank keystore
                } catch (OperationCanceledException e1) {
                    // FALLTHROUGH -- continue, pretending we had no keystore
                }
            }

            updatePolicyPanel();
        }
        checkOk();
    }

    /**
     * Called when the Ok button is pressed.
     * Should copy any updated properties into the target object and return normally.
     * Caller is responsible for hiding and disposing of the property dialog.
     */
    protected void commitChanges() {
        synchronized (ssg) {
            ssg.setSsgAddress(fieldServerAddress.getText().trim().toLowerCase());
            ssg.setUsername(fieldUsername.getText().trim());
            ssg.setSavePasswordToDisk(cbSavePassword.isSelected());

            // We'll treat a blank password as though it's unconfigured.  If the user really needs to use
            // a blank password to access a service, he can leave the password field blank in the logon
            // dialog when it eventually appears.
            char[] pass = fieldPassword.getPassword();

            // If it's been changed, make sure prompting is enabled
            if ((pass == null) != (ssg.cmPassword() == null) ||
                    (ssg.cmPassword() != null && !new String(ssg.cmPassword()).equals(new String(pass))))
                ssg.promptForUsernameAndPassword(true);
            ssg.cmPassword(pass.length > 0 ? fieldPassword.getPassword() : null);

            if (radioNonstandardPorts.isSelected()) {
                ssg.setSsgPort(Integer.parseInt(fieldSsgPort.getText()));
                ssg.setSslPort(Integer.parseInt(fieldSslPort.getText()));
            } else {
                ssg.setSsgPort(referenceSsg.getSsgPort());
                ssg.setSslPort(referenceSsg.getSslPort());
            }

            if (policyFlushRequested)
                ssg.clearPolicies();
        }
        setSsg(ssg);
    }

    /**
     * Find the SSG name to display.  Uses the name from the SSG, if any; otherwise, the typed
     * in name; otherwise the string "&lt;New Gateway&gt;".
     * @return
     */
    private String ssgName() {
        String result =  ssg.getSsgAddress();
        if (result != null && result.length() > 0)
            return result;
        result = getFieldServerAddress().getText();
        if (result != null && result.length() > 0)
            return result;
        return "<New Gateway>";
    }

    /**
     * This event is fired when a policy is attached to an Ssg with a PolicyAttachmentKey, either new
     * or updated.
     *
     * @param evt
     */
    public void policyAttached(SsgEvent evt) {
        updatePolicyPanel();
    }

    public void show() {
        getFieldServerAddress().requestFocus();
        super.show();
    }
}
