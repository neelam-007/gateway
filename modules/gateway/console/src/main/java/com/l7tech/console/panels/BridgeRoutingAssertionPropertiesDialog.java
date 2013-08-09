/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.event.*;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.PropertyPanel;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.wsdl.Wsdl;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allows properties of a {@link com.l7tech.policy.assertion.BridgeRoutingAssertion} to be edited.
 */
public class BridgeRoutingAssertionPropertiesDialog extends LegacyAssertionPropertyDialog {
    private static final Logger logger = Logger.getLogger(BridgeRoutingAssertionPropertiesDialog.class.getName());

    private JPanel rootPanel;
    private JRadioButton rbPolicyAutoDisco;
    private JRadioButton rbPolicyManual;
    private JTextArea policyXmlText;
    private JButton buttonCancel;
    private JButton buttonOk;
    private JButton buttonHttpProperties;
    private JRadioButton rbServerCertAuto;
    private JRadioButton rbServerCertManual;
    private JLabel xmlMessages;
    private TrustedCertsTable trustedCertTable;
    private JButton newServerCert;
    private JButton selectCert;
    private JCheckBox useSslByDefaultCheckBox;
    private JCheckBox overridePortsCheckBox;
    private SquigglyTextField httpPortTextField;
    private SquigglyTextField httpsPortTextField;
    private JScrollPane certScrollPane;
    private JPanel propertyPanelHolder;
    private PropertyPanel propertyPanel;

    private final BridgeRoutingAssertion assertion; // live copy of assertion -- do not write to it until Ok pressed
    private BridgeRoutingAssertion lastRoutingProperties = null; // copy last confirmed by HTTP dialog; all except policy XML is up-to-date

    private final EventListenerList listenerList = new EventListenerList();
    private InputValidator inputValidator;
    private List<TrustedCert> certList;
    private TrustedCert serverCert;

    public BridgeRoutingAssertionPropertiesDialog(final Frame owner,
                                                  final BridgeRoutingAssertion a,
                                                  final Policy policy,
                                                  final Wsdl wsdl,
                                                  final boolean readOnly) {
        super(owner, a, true);
        this.assertion = a;
        inputValidator = new InputValidator(this, a.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString());

        setContentPane(rootPanel);

        buttonHttpProperties.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Make a temporary copy for the routing assertion dialog, and remember the changes only if it was Ok'ed
                // (but do not actually copy them into the live assertion until the outer dialog is Ok'ed)  (Bug #3617)
                final BridgeRoutingAssertion editingCopy;
                try {
                    if (lastRoutingProperties == null) lastRoutingProperties = cloneAssertion(assertion);
                    editingCopy = cloneAssertion(lastRoutingProperties);
                } catch (IOException e1) {
                    throw new RuntimeException(e1); // can't happen, but just in case, pass up to error handler
                }

                final HttpRoutingAssertionDialog httpDialog = new HttpRoutingAssertionDialog(owner, editingCopy, policy, wsdl, readOnly);
                httpDialog.setAssertionToUseInSearchForPredecessorVariables(assertion);
                httpDialog.setModal(true);
                httpDialog.pack();

                Utilities.centerOnScreen(httpDialog);
                DialogDisplayer.display(httpDialog, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (httpDialog.isConfirmed()) {
                                lastRoutingProperties = cloneAssertion(editingCopy);
                            }
                        } catch (IOException e1) {
                            throw new RuntimeException(e1); // can't happen, but just in case, pass up to error handler
                        }

                    }
                });
            }
        });

        Utilities.equalizeButtonSizes(new AbstractButton[] { buttonOk, buttonCancel });

        buttonOk.setEnabled( !readOnly );
        inputValidator.attachToButton(buttonOk, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //validate policy xml to be sure it's valid
                if (!isValidPolicyXml()) return;

                copyViewToModel();
                fireEventAssertionChanged(assertion);
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BridgeRoutingAssertionPropertiesDialog.this.dispose();
            }
        });

        ActionListener updateEnableStates = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnableStates();
            }
        };

        tcpPort(httpPortTextField);
        tcpPort(httpsPortTextField);

        for (AbstractButton b : Arrays.asList(rbPolicyAutoDisco, rbPolicyManual, rbServerCertAuto, rbServerCertManual, overridePortsCheckBox))
            b.addActionListener(updateEnableStates);

        //populate the available server certs list
        initializeServerCerts();

        Utilities.enableGrayOnDisabled(policyXmlText);

        final Utilities.DefaultContextMenuFactory cmf =
                new Utilities.DefaultContextMenuFactory() {
                    @Override
                    public JPopupMenu createContextMenu(final JTextComponent tc) {
                        final JPopupMenu m = super.createContextMenu(tc);
                        if (tc.isEditable()) {
                            m.add(new JSeparator());
                            JMenuItem reformXml = new JMenuItem("Reformat All XML");
                            reformXml.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    String xml = tc.getText();
                                    try {
                                        tc.setText(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(xml)));
                                        xmlMessages.setText("");
                                    } catch (IOException e1) {
                                        logger.log(Level.SEVERE, "Unable to reformat XML", e1); // Can't happen
                                    } catch (SAXException e1) {
                                        logger.log(Level.SEVERE, "Unable to reformat XML", e1); // Oh well
                                        xmlMessages.setText("XML is not well-formed");
                                    }
                                }
                            });
                            m.add(reformXml);
                        }
                        return m;
                    }
                };
        policyXmlText.addMouseListener(Utilities.createContextMenuMouseListener(policyXmlText, cmf));

        propertyPanel = new PropertyPanel();
        propertyPanel.setTitle("Additional Properties");
        propertyPanel.setPropertyEditTitle("Client Policy Property");

        propertyPanelHolder.setLayout(new BorderLayout());
        propertyPanelHolder.add(propertyPanel, BorderLayout.CENTER);

        copyModelToView();
        updateEnableStates();
    }

    private void initializeServerCerts() {

        certList = new ArrayList<TrustedCert>();
        trustedCertTable = new TrustedCertsTable();
        trustedCertTable.getTableSorter().setData(certList);

        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert issuer column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);
        trustedCertTable.setEnabled(true);


        rbServerCertAuto.setSelected(true);
        rbServerCertManual.setEnabled(true);

        newServerCert.setEnabled(true);
        newServerCert.addActionListener( new NewTrustedCertificateAction(certListener, "Add"));

        selectCert.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CertSearchPanel sp = new CertSearchPanel(BridgeRoutingAssertionPropertiesDialog.this, false, true);
                sp.addCertListener(certListener);
                sp.pack();
                Utilities.centerOnScreen(sp);
                DialogDisplayer.display(sp);
            }
        });

        inputValidator.addRule(new InputValidator.ComponentValidationRule(trustedCertTable) {
            @Override
            public String getValidationError() {
                if (rbServerCertManual.isSelected()) {
                    if (serverCert == null)
                        return "A cert must be selected";
                    else
                        return null;
                    
                } else return null;
            }
        });

        Utilities.enableGrayOnDisabled(trustedCertTable);
    }

    private static BridgeRoutingAssertion cloneAssertion(BridgeRoutingAssertion assertion) throws IOException {
        return (BridgeRoutingAssertion)WspReader.getDefault().parseStrictly(WspWriter.getPolicyXml(assertion), WspReader.INCLUDE_DISABLED);
    }

    /** @param tc a JTextComponent to configure as holding a TCP port. */
    private void tcpPort(JTextComponent tc) {
        Utilities.enableGrayOnDisabled(tc);
        Utilities.attachDefaultContextMenu(tc);
        Utilities.attachClipboardKeyboardShortcuts(tc);
        Utilities.enableSelectAllOnFocus(tc);
        tc.setDocument(new NumberField(5));
        inputValidator.constrainTextFieldToNumberRange("HTTP(S) port", tc, 1, 65535);
    }

    /** Update the GUI state to reflect the policy assertion settings. */
    private void copyModelToView() {
        // Update GUI state
        String policyXml = assertion.getPolicyXml();
        rbPolicyAutoDisco.setSelected(policyXml == null);
        rbPolicyManual.setSelected(policyXml != null);
        policyXmlText.setText(policyXml != null ? policyXml : "");

        final int httpPort = assertion.getHttpPort();
        final int httpsPort = assertion.getHttpsPort();
        httpPortTextField.setText(String.valueOf(httpPort));
        httpsPortTextField.setText(String.valueOf(httpsPort));
        overridePortsCheckBox.setSelected(httpPort > 0 || httpsPort > 0);

        useSslByDefaultCheckBox.setSelected(assertion.isUseSslByDefault());

        propertyPanel.setProperties( new LinkedHashMap<String,String>(assertion.getClientPolicyProperties()) );

        Goid certOid = assertion.getServerCertificateGoid();
        if (certOid != null) {
            try {
                serverCert = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey(certOid);
                updateTrustedCertTable();
            } catch (FindException e) {
                logger.warning("Could not find the specified certificate in the trust store. " + ExceptionUtils.getMessage(e));
            }
        }
        rbServerCertAuto.setSelected(serverCert == null);
        rbServerCertManual.setSelected(serverCert != null);

    }

    /** Update the policy assertion settings to reflect the GUI state. */
    private void copyViewToModel() {
        // Copy in any edited routing assertion properties, if any
        // Do this first so any stale/bogus BRA-specific properties get overwritten by the rest of this method
        if (lastRoutingProperties != null) assertion.copyFrom(lastRoutingProperties);

        // Populate policy assertion settings
        if (rbPolicyManual.isSelected())
            assertion.setPolicyXml(policyXmlText.getText());
        else
            assertion.setPolicyXml(null);

        if (overridePortsCheckBox.isSelected()) {
            assertion.setHttpPort(Integer.parseInt(httpPortTextField.getText()));
            assertion.setHttpsPort(Integer.parseInt(httpsPortTextField.getText()));
        } else {
            assertion.setHttpPort(0);
            assertion.setHttpsPort(0);
        }

        assertion.setUseSslByDefault(useSslByDefaultCheckBox.isSelected());

        assertion.setClientPolicyProperties( propertyPanel.getProperties() );

        if (rbServerCertManual.isSelected()) {
            assertion.setServerCertificateGoid(serverCert.getGoid());
        }
        else {
            assertion.setServerCertificateGoid((Goid) null);
        }
    }

    private void updateEnableStates() {
        policyXmlText.setEnabled(rbPolicyManual.isSelected());
        boolean customPorts = overridePortsCheckBox.isSelected();
        httpPortTextField.setEnabled(customPorts);
        httpsPortTextField.setEnabled(customPorts);
        trustedCertTable.setEnabled(rbServerCertManual.isSelected());
        selectCert.setEnabled(rbServerCertManual.isSelected());
        newServerCert.setEnabled(rbServerCertManual.isSelected());
    }

    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    private void fireEventAssertionChanged(final Assertion a) {
        if (a == null) return;
        if (a.getParent() == null || a.getParent().getChildren() == null) return;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (EventListener listener : listeners) {
                    ((PolicyListener) listener).assertionsChanged(event);
                }
            }
        });
    }

    private void updateTrustedCertTable() {
        certList.clear();

        if (serverCert != null) certList.add(serverCert);
        trustedCertTable.getTableSorter().setData(certList);
        trustedCertTable.getTableSorter().fireTableDataChanged();
    }

    private CertListener certListener = new CertListenerAdapter() {
        @Override
        public void certSelected(CertEvent e) {
                serverCert = e.getCert();
                if (serverCert != null) updateTrustedCertTable();
        }
    };

    /**
     * Validates the policy xml to make sure that it is not empty and the xml is valid.
     *
     * @return  TRUE if policy is valid, else FALSE
     */
    private boolean isValidPolicyXml() {
        if (rbPolicyManual.isSelected() && policyXmlText.isEnabled()) {
            
            //policy XML content empty data or xml is not well-formed
            if (policyXmlText.getText().trim().length() == 0) {
                JOptionPane.showMessageDialog(
                        BridgeRoutingAssertionPropertiesDialog.this, "This manually specified policy XML cannot be empty.",
                        "Invalid Policy XML", JOptionPane.OK_OPTION);
                return false;
            }
            //is xml valid
            try {
                XmlUtil.stringToDocument(policyXmlText.getText());
            } catch (SAXException se) {
                JOptionPane.showMessageDialog(
                        BridgeRoutingAssertionPropertiesDialog.this, "This manually specified policy XML is not valid.",
                        "Invalid Policy XML", JOptionPane.OK_OPTION);
                return false;
            }

        }
        return true;
    } 
}
