package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.external.assertions.xmppassertion.XMPPStartTLSAssertion;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.util.GoidUpgradeMapper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 15/03/12
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPStartTLSAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPStartTLSAssertion> {
    protected static final Logger logger = Logger.getLogger(XMPPSendToRemoteHostAssertionPropertiesDialog.class.getName());

    private static final String TO_SERVER = "To Server";
    private static final String TO_CLIENT = "To Client";
    
    private JPanel mainPanel;
    private JTextField sessionIdField;
    private JComboBox directionComboBox;
    private JLabel clientAuthLabel;
    private JCheckBox clientAuthEnabledCheckBox;
    private JLabel privateKeyLabel;
    private PrivateKeysComboBox privateKeyComboBox;
    private JLabel clientAuthTypeLabel;
    private JComboBox clientAuthTypeComboBox;

    public XMPPStartTLSAssertionPropertiesDialog(Window owner, XMPPStartTLSAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {TO_SERVER, TO_CLIENT}));
        directionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        clientAuthEnabledCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableComponents();
            }
        });

        clientAuthTypeComboBox.setModel(new DefaultComboBoxModel(XMPPStartTLSAssertion.ClientAuthType.values()));

        super.initComponents();
        pack();
    }
    
    private void enableDisableComponents() {
        if(TO_SERVER.equals(directionComboBox.getSelectedItem())) {
            clientAuthLabel.setVisible(true);
            clientAuthEnabledCheckBox.setVisible(true);
            privateKeyLabel.setEnabled(clientAuthEnabledCheckBox.isSelected());
            privateKeyComboBox.setEnabled(clientAuthEnabledCheckBox.isSelected());
            clientAuthTypeLabel.setVisible(false);
            clientAuthTypeComboBox.setVisible(false);
            pack();
        } else {
            clientAuthLabel.setVisible(false);
            clientAuthEnabledCheckBox.setVisible(false);
            privateKeyLabel.setEnabled(true);
            privateKeyComboBox.setEnabled(true);
            clientAuthTypeLabel.setVisible(true);
            clientAuthTypeComboBox.setVisible(true);
            pack();
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public XMPPStartTLSAssertion getData(XMPPStartTLSAssertion assertion) throws ValidationException {
        if(sessionIdField.getText().trim().isEmpty()) {
            throw new ValidationException("The session ID is required.");
        }
        
        if(TO_SERVER.equals(directionComboBox.getSelectedItem())) {
            if(clientAuthEnabledCheckBox.isSelected() && privateKeyComboBox.getSelectedItem() == null) {
                throw new ValidationException("The private key is required when client authentication is enabled.");
            }
        } else {
            if(privateKeyComboBox.getSelectedItem() == null) {
                throw new ValidationException("The private key is required.");
            }
        }
        
        assertion.setSessionId(sessionIdField.getText().trim());
        
        if(TO_SERVER.equals(directionComboBox.getSelectedItem())) {
            assertion.setToServer(true);
            if(clientAuthEnabledCheckBox.isSelected()) {
                assertion.setProvideClientCert(true);
                assertion.setPrivateKeyId(Goid.toString(privateKeyComboBox.getSelectedKeystoreId()) + ":" + privateKeyComboBox.getSelectedKeyAlias());
            } else {
                assertion.setProvideClientCert(false);
                assertion.setPrivateKeyId(null);
            }
            assertion.setClientAuthType(XMPPStartTLSAssertion.ClientAuthType.NONE);
        } else {
            assertion.setToServer(false);
            assertion.setPrivateKeyId(Goid.toString(privateKeyComboBox.getSelectedKeystoreId()) + ":" + privateKeyComboBox.getSelectedKeyAlias());
            assertion.setClientAuthType((XMPPStartTLSAssertion.ClientAuthType)clientAuthTypeComboBox.getSelectedItem());
        }

        return assertion;
    }

    @Override
    public void setData(XMPPStartTLSAssertion assertion) throws ValidationException {
        sessionIdField.setText(assertion.getSessionId() == null ? "" : assertion.getSessionId());
        
        if(assertion.isToServer()) {
            directionComboBox.setSelectedItem(TO_SERVER);
            clientAuthEnabledCheckBox.setSelected(assertion.isProvideClientCert());
            if(assertion.isProvideClientCert() && assertion.getPrivateKeyId() != null) {
                String[] parts = assertion.getPrivateKeyId().split(":");

                Goid keystoreGOID = null;
                long keystoreOID = Long.MIN_VALUE;
                try {
                    keystoreGOID = Goid.parseGoid(parts[0]);
                } catch (IllegalArgumentException iae) {
                    try {
                        keystoreOID = Long.parseLong(parts[0]);
                    } catch (NumberFormatException nfe) {
                        keystoreGOID = PersistentEntity.DEFAULT_GOID;
                    }
                }
                if (keystoreGOID == null) {
                    keystoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keystoreOID);
                }

                if(Goid.isDefault(keystoreGOID)) {
                    privateKeyComboBox.selectDefaultSsl();
                } else {
                    privateKeyComboBox.select(keystoreGOID, parts[1]);
                }
            } else {
                privateKeyComboBox.selectDefaultSsl();
            }
        } else {
            directionComboBox.setSelectedItem(TO_CLIENT);
            if(assertion.getClientAuthType() == null) {
                clientAuthTypeComboBox.setSelectedItem(XMPPStartTLSAssertion.ClientAuthType.NONE);
            } else {
                clientAuthTypeComboBox.setSelectedItem(assertion.getClientAuthType());
            }

            if(assertion.getPrivateKeyId() != null) {
                String[] parts = assertion.getPrivateKeyId().split(":");

                Goid keystoreGOID = null;
                long keystoreOID = Long.MIN_VALUE;
                try {
                    keystoreGOID = Goid.parseGoid(parts[0]);
                } catch (IllegalArgumentException iae) {
                    try {
                        keystoreOID = Long.parseLong(parts[0]);
                    } catch (NumberFormatException nfe) {
                        keystoreGOID = PersistentEntity.DEFAULT_GOID;
                    }
                }
                if (keystoreGOID == null) {
                    keystoreGOID = GoidUpgradeMapper.mapOid(EntityType.SSG_KEY_ENTRY, keystoreOID);
                }

                if(Goid.isDefault(keystoreGOID)) {
                    privateKeyComboBox.selectDefaultSsl();
                } else {
                    privateKeyComboBox.select(keystoreGOID, parts[1]);
                }
            } else {
                privateKeyComboBox.selectDefaultSsl();
            }
        }

        enableDisableComponents();
    }
}
