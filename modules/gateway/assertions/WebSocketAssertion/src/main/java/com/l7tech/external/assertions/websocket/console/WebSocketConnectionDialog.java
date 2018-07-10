package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.panels.CipherSuiteDialog;
import com.l7tech.console.panels.PrivateKeysComboBox;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.websocket.InvalidRangeException;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WebSocketConnectionDialog extends JDialog {
    protected static final Logger logger = Logger.getLogger(WebSocketConnectionDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField description;
    private JCheckBox outboundOnlyCheckBox;
    private JCheckBox enabledCheckBox;
    private JTabbedPane connectionTabbedPanel;
    private JTextField inboundListenPort;
    private JTextField maxInboundConnections;
    private JTextField outboundUrl;
    private JTextField connectionName;
    private JComboBox inboundPolicyComboBox;
    private JComboBox outboundPolicyComboBox;
    private JTextField outBoundMaxIdleTime;
    private JTextField inBoundMaxIdleTime;
    private JCheckBox useInboundSSLCheckBox;
    private WebSocketConnectionEntity connection;
    private boolean confirmed = false;
    private PrivateKeysComboBox inboundPrivateKeysComboBox;
    private JCheckBox useOutboundSSLCheckBox;
    private PrivateKeysComboBox outboundPrivateKeysComboBox;
    private JCheckBox useClientAuthenticationCheckBox;
    private JComboBox inboundClientAuthenticationComboBox;
    private JComboBox connectionPolicyComboBox;
    private JComboBox outboundConnectionPolicyComboBox;
    private JButton outboundCipherSuiteButton;
    private JButton outboundTlsProtocolButton;
    private JButton inboundTlsButton;
    private JButton inboundCipherSuitesButton;
    private JLabel outboundPrivateKeyLabel;
    private boolean dirtyPortFlag;
    private int oldInboundListenPort;
    private boolean oldEnableFlag;
    private boolean dirtyEnableFlag;
    private String[] inboundTlsProtocols;
    private String[] inboundTlsCipherSuites;
    private String[] outboundTlsProtocols;
    private String[] outboundTlsCipherSuites;


    public WebSocketConnectionDialog(Window owner, WebSocketConnectionEntity connection) {
        super(owner, WebSocketConstants.MANAGE_CONNECTION_TITLE);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        this.connection = connection;

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        useInboundSSLCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onInboundSsLChecked();
            }
        });

        useOutboundSSLCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOutboundSslChecked();
            }
        });

        useClientAuthenticationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUseClientAuthorizationChecked();
            }
        });

        outboundOnlyCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOutboundOnlyCheckBoxChecked();
            }
        });

        inboundTlsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TlsSelectionDialog.show(WebSocketConnectionDialog.this, "Enabled TLS Protocols", null, inboundTlsProtocols, new Functions.UnaryVoid<String[]>() {
                    @Override
                    public void call(String[] s) {
                        inboundTlsProtocols = s;
                    }
                });
            }
        });

        inboundCipherSuitesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String inboundTlsCipherSuitesString = null;
                if (inboundTlsCipherSuites != null){
                    inboundTlsCipherSuitesString = String.join(",", inboundTlsCipherSuites);
                }
                CipherSuiteDialog.show(WebSocketConnectionDialog.this, "Enabled Cipher Suites", null, false, inboundTlsCipherSuitesString, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String s) {
                        if (s==null){
                            inboundTlsCipherSuites = null;
                        } else {
                            inboundTlsCipherSuites = s.split(",");
                        }
                    }
                });
            }
        });
        Utilities.equalizeButtonSizes(new JButton[] {inboundTlsButton, inboundCipherSuitesButton });

        outboundTlsProtocolButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TlsSelectionDialog.show(WebSocketConnectionDialog.this, "Enabled TLS Protocols", null, outboundTlsProtocols, new Functions.UnaryVoid<String[]>() {
                    @Override
                    public void call(String[] s) {
                        outboundTlsProtocols = s;
                    }
                });
            }
        });

        outboundCipherSuiteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String outboundTlsCipherSuitesString = null;
                if (outboundTlsCipherSuites != null){
                    outboundTlsCipherSuitesString = String.join(",", outboundTlsCipherSuites);
                }

                CipherSuiteDialog.show(WebSocketConnectionDialog.this, "Enabled Cipher Suites", null, false, outboundTlsCipherSuitesString, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String s) {
                        if (s == null){
                            // use default list.
                            outboundTlsCipherSuites = null;
                        } else {
                            outboundTlsCipherSuites = s.split(",");
                        }
                    }
                });
            }
        });
        Utilities.equalizeButtonSizes(new JButton[] {outboundTlsProtocolButton, outboundCipherSuiteButton });


// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        //fieldInitializer
        setPolicyComboBoxes(connection);
        setKeyComboBoxes(connection);
        setClientAuthComboBoxes(connection);
        description.setText(connection.getDescription());
        enabledCheckBox.setSelected(connection.isEnabled());
        inboundListenPort.setText(WebSocketUtils.getDisplayValueOfInt(connection.getInboundListenPort()));
        oldInboundListenPort = connection.getInboundListenPort();
        maxInboundConnections.setText(WebSocketUtils.getDisplayValueOfInt(connection.getInboundMaxConnections()));
        outboundUrl.setText(connection.getOutboundUrl());
        connectionName.setText(connection.getName());
        outBoundMaxIdleTime.setText(WebSocketUtils.getDisplayValueOfInt(connection.getOutboundMaxIdleTime()));
        inBoundMaxIdleTime.setText(WebSocketUtils.getDisplayValueOfInt(connection.getInboundMaxIdleTime()));
        enabledCheckBox.setSelected(connection.isEnabled());
        oldEnableFlag = connection.isEnabled();
        useInboundSSLCheckBox.setSelected(connection.isInboundSsl());
        useOutboundSSLCheckBox.setSelected(connection.isOutboundSsl());
        inboundTlsButton.setEnabled(connection.isInboundSsl());
        inboundCipherSuitesButton.setEnabled(connection.isInboundSsl());
        outboundTlsProtocolButton.setEnabled(connection.isOutboundSsl());
        outboundCipherSuiteButton.setEnabled(connection.isOutboundSsl());
        useClientAuthenticationCheckBox.setSelected(connection.isOutboundClientAuthentication());
        inboundPrivateKeysComboBox.setEnabled(connection.isInboundSsl());
        outboundPrivateKeyLabel.setEnabled(connection.isOutboundSsl() && connection.isOutboundClientAuthentication());
        outboundPrivateKeysComboBox.setEnabled(connection.isOutboundSsl() && connection.isOutboundClientAuthentication());
        inboundClientAuthenticationComboBox.setEnabled(connection.isInboundSsl());
        inboundTlsProtocols = connection.getInboundTlsProtocols();
        inboundTlsCipherSuites = connection.getInboundCipherSuites();
        outboundTlsProtocols = connection.getOutboundTlsProtocols();
        outboundTlsCipherSuites = connection.getOutboundCipherSuites();
        outboundOnlyCheckBox.setSelected(connection.isOutboundOnly());

    }

    public void setReadOnly(boolean readOnly) {
        connectionName.setEnabled(!readOnly);
        description.setEnabled(!readOnly);
        enabledCheckBox.setEnabled(!readOnly);
        outboundUrl.setEnabled(!readOnly);
        maxInboundConnections.setEnabled(!readOnly);
        maxInboundConnections.setEnabled(!readOnly);
        inboundListenPort.setEnabled(!readOnly);
        outBoundMaxIdleTime.setEnabled(!readOnly);
        inBoundMaxIdleTime.setEnabled(!readOnly);
        inboundPolicyComboBox.setEnabled(!readOnly);
        connectionPolicyComboBox.setEnabled(!readOnly);
        outboundPolicyComboBox.setEnabled(!readOnly);
        outboundConnectionPolicyComboBox.setEnabled(!readOnly);
        buttonOK.setEnabled(!readOnly);
        useInboundSSLCheckBox.setEnabled(!readOnly);
        useOutboundSSLCheckBox.setEnabled(!readOnly);

        inboundPrivateKeysComboBox.setEnabled(false);
        inboundClientAuthenticationComboBox.setEnabled(false);
        if (!readOnly && useInboundSSLCheckBox.isSelected() ) {
            inboundPrivateKeysComboBox.setEnabled(true);
            inboundClientAuthenticationComboBox.setEnabled(true);
        }

        if (!readOnly && useOutboundSSLCheckBox.isSelected() ) {
            useClientAuthenticationCheckBox.setEnabled(true);
            if (useClientAuthenticationCheckBox.isSelected()) {
                outboundPrivateKeysComboBox.setEnabled(true);
            } else {
                outboundPrivateKeysComboBox.setEnabled(false);
            }

        } else {
            outboundPrivateKeysComboBox.setEnabled(false);
            useClientAuthenticationCheckBox.setEnabled(false);
        }

        outboundOnlyCheckBox.setEnabled(!readOnly);
        onOutboundOnlyCheckBoxChecked();
    }

    private boolean validateInboundIdleTime(String idletime) throws WebSocketNumberFormatException {
        return outboundOnlyCheckBox.isSelected() || WebSocketUtils.isInt(idletime, "Inbound Maximum Idle Time") > -1;
    }

    private boolean validateOutboundIdleTime(String url, String idletime) throws WebSocketNumberFormatException {
        return url == null || "".equals(url) || WebSocketUtils.isInt(idletime, "Outbound Maximum Idle Time") > -1;
    }

    private void onOK() {
        try {
            if ("".equals(connectionName.getText())) {
                DialogDisplayer.showMessageDialog(this, "Name field can not be empty", null);
            } else if (!validateInboundIdleTime(inBoundMaxIdleTime.getText())) {
                DialogDisplayer.showMessageDialog(this, "Inbound idle time must be positive and non-zero", null);
            } else if (!validateOutboundIdleTime(outboundUrl.getText(), outBoundMaxIdleTime.getText())) {
                DialogDisplayer.showMessageDialog(this, "Outbound idle time must be positive and non-zero", null);
            } else {
                validateAndSetPolicyOIDS(connection);
                validateAndSetPrivateKeys(connection);
                connection.setName(connectionName.getText());
                connection.setDescription(description.getText());
                connection.setEnabled(enabledCheckBox.isEnabled());
                connection.setEnabled(enabledCheckBox.isSelected());
                if (oldEnableFlag != enabledCheckBox.isSelected()){
                    dirtyEnableFlag = true;
                } else {
                    dirtyEnableFlag = false;
                }
                if(!outboundOnlyCheckBox.isSelected()) {
                    int listenPort = validateListenPort(WebSocketUtils.isInt(inboundListenPort.getText(), "Port"));
                    connection.setInboundListenPort(listenPort);
                    if (oldInboundListenPort != listenPort) {
                        dirtyPortFlag = true;
                    } else {
                        dirtyPortFlag = false;
                    }
                    connection.setRemovePortFlag(dirtyPortFlag || dirtyEnableFlag);
                    connection.setInboundMaxConnections(WebSocketUtils.isInt(maxInboundConnections.getText(), "Maximum Connections"));
                    connection.setInboundMaxIdleTime(WebSocketUtils.isInt(inBoundMaxIdleTime.getText(), "Inbound Maximum Idle Time"));

                    connection.setInboundSsl(useInboundSSLCheckBox.isSelected());
                    if (useInboundSSLCheckBox.isSelected()){
                        connection.setInboundTlsProtocols(inboundTlsProtocols);
                        connection.setInboundCipherSuites(inboundTlsCipherSuites);
                    } else {
                        connection.setInboundTlsProtocols(null);
                        connection.setInboundCipherSuites(null);
                    }
                    connection.setInboundClientAuth((WebSocketConnectionEntity.ClientAuthType)inboundClientAuthenticationComboBox.getSelectedItem());
                } else {
                    // Clear all inbound properties.
                    connection.setInboundListenPort(0);
                    connection.setRemovePortFlag(false);
                    connection.setInboundMaxConnections(0);
                    connection.setInboundMaxIdleTime(0);
                    connection.setInboundSsl(false);
                    connection.setInboundTlsProtocols(null);
                    connection.setInboundCipherSuites(null);
                    connection.setInboundClientAuth(null);
                    connection.setInboundPrivateKeyId(null);
                    connection.setInboundPrivateKeyAlias(null);
                }
                connection.setOutboundUrl(outboundUrl.getText());
                connection.setOutboundMaxIdleTime(WebSocketUtils.isInt(outBoundMaxIdleTime.getText(), "Outbound Maximum Idle Time"));

                connection.setOutboundSsl(useOutboundSSLCheckBox.isSelected());
                if (useOutboundSSLCheckBox.isSelected()) {
                    connection.setOutboundTlsProtocols(outboundTlsProtocols);
                    connection.setOutboundCipherSuites(outboundTlsCipherSuites);
                } else {
                    connection.setOutboundTlsProtocols(null);
                    connection.setOutboundCipherSuites(null);
                }

                connection.setOutboundClientAuthentication(useClientAuthenticationCheckBox.isSelected());
                connection.setOutboundOnly(outboundOnlyCheckBox.isSelected());

                confirmed = true;
                dispose();
            }
        } catch (InvalidRangeException e) {
            logger.log(Level.WARNING, "WebSocket listen port can not be set out of bounds");
            error(e.getMessage());
        } catch (WebSocketNumberFormatException e) {
            logger.log(Level.WARNING, "Non numeric value entered into a numeric field");
            error(e.getMessage() +  ": Non numeric value entered");
        } catch (InvalidPortException e) {
            logger.log(Level.WARNING, "Port entered is either invalid or in use");
            error(e.getMessage());
        }

    }

    /*
     * Checks to see if the port entered is already defined for a SsgConnector
     */
    private int validateListenPort(int port) throws InvalidPortException {

        //Check ports in use
        if ( port < WebSocketConstants.MIN_LISTEN_PORT || port > WebSocketConstants.MAX_LISTEN_PORT ) {
            throw new InvalidPortException("Trying to use System port");
        }

        try {
            Collection<SsgConnector> connectors = Registry.getDefault().getTransportAdmin().findAllSsgConnectors();
            for ( SsgConnector ssgConnector : connectors) {
                if ( ssgConnector.getPort() == port) {
                    throw new InvalidPortException("Port is already in use");
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "SsgConnectors are unavailable ", e);
        }

        return port;
    }

    private void  validateAndSetPrivateKeys(WebSocketConnectionEntity connection) {
        if (!outboundOnlyCheckBox.isSelected() && useInboundSSLCheckBox.isSelected()) {
            connection.setInboundPrivateKeyId(inboundPrivateKeysComboBox.getSelectedKeystoreId());
            connection.setInboundPrivateKeyAlias(inboundPrivateKeysComboBox.getSelectedKeyAlias());
        }

        if (useOutboundSSLCheckBox.isSelected() && useClientAuthenticationCheckBox.isSelected()) {
            connection.setOutboundPrivateKeyId(outboundPrivateKeysComboBox.getSelectedKeystoreId());
            connection.setOutboundPrivateKeyAlias(outboundPrivateKeysComboBox.getSelectedKeyAlias());
        }
    }

    /*
     * Validates service OIDs before setting them to the connection. Handles the "NONE" case.
     */
    private void validateAndSetPolicyOIDS(WebSocketConnectionEntity connection) {
        if(!outboundOnlyCheckBox.isSelected()) {
            Object inboundObject = inboundPolicyComboBox.getSelectedItem();
            Object connectionObject = connectionPolicyComboBox.getSelectedItem();
            if (inboundObject instanceof ServiceHeader) {
                connection.setInboundPolicyOID(((ServiceHeader) inboundObject).getGoid());
            } else {
                connection.setInboundPolicyOID(Goid.DEFAULT_GOID);
            }
            if (connectionObject instanceof ServiceHeader) {
                connection.setConnectionPolicyGOID(((ServiceHeader) connectionObject).getGoid());
            } else {
                connection.setConnectionPolicyGOID(Goid.DEFAULT_GOID);
            }
        } else {
            connection.setInboundPolicyOID(Goid.DEFAULT_GOID);
            connection.setConnectionPolicyGOID(Goid.DEFAULT_GOID);
        }

        Object outboundObject = outboundPolicyComboBox.getSelectedItem();
        if (outboundObject instanceof  ServiceHeader) {
            connection.setOutboundPolicyOID(((ServiceHeader) outboundObject).getGoid());
        } else {
            connection.setOutboundPolicyOID(Goid.DEFAULT_GOID);
        }

        Object outboundConnectionObject = outboundConnectionPolicyComboBox.getSelectedItem();
        if (outboundConnectionObject instanceof  ServiceHeader) {
            connection.setOutboundConnectionPolicyId(((ServiceHeader) outboundConnectionObject).getGoid());
        } else {
            connection.setOutboundConnectionPolicyId(Goid.DEFAULT_GOID);
        }
    }

    private void setKeyComboBoxes(WebSocketConnectionEntity connection) {

        if ( Goid.isDefault(connection.getInboundPrivateKeyId()) ) {
            inboundPrivateKeysComboBox.setSelectedIndex(0);
        } else {
            inboundPrivateKeysComboBox.select(connection.getInboundPrivateKeyId(), connection.getInboundPrivateKeyAlias());
        }

        if ( Goid.isDefault(connection.getOutboundPrivateKeyId()) ) {
            outboundPrivateKeysComboBox.setSelectedIndex(0);
        } else {
            outboundPrivateKeysComboBox.select(connection.getOutboundPrivateKeyId(), connection.getOutboundPrivateKeyAlias());
        }
    }

    private void setClientAuthComboBoxes(WebSocketConnectionEntity connection) {
        inboundClientAuthenticationComboBox.setModel(new DefaultComboBoxModel(WebSocketConnectionEntity.ClientAuthType.values()));
        inboundClientAuthenticationComboBox.setSelectedItem(connection.getInboundClientAuth());
    }

    private void setPolicyComboBoxes(WebSocketConnectionEntity connection) {
        try {
            ServiceHeader[] serviceHeaders = Registry.getDefault().getServiceManager().findAllPublishedServices();
            Arrays.sort(serviceHeaders, new Comparator<ServiceHeader>() {
                public int compare(ServiceHeader sh1, ServiceHeader sh2) {
                    String name1 = sh1.getName();
                    String name2 = sh2.getName();
                    if (name1 == null) name1 = "";
                    if (name2 == null) name2 = "";
                    return name1.toLowerCase().compareTo(name2.toLowerCase());
                }
            });
            inboundPolicyComboBox.setModel(new DefaultComboBoxModel(serviceHeaders));
            connectionPolicyComboBox.setModel(new DefaultComboBoxModel(serviceHeaders));
            outboundPolicyComboBox.setModel(new DefaultComboBoxModel(serviceHeaders));
            outboundConnectionPolicyComboBox.setModel(new DefaultComboBoxModel(serviceHeaders));
            inboundPolicyComboBox.insertItemAt("NONE", 0);
            connectionPolicyComboBox.insertItemAt("NONE", 0);
            outboundPolicyComboBox.insertItemAt("NONE", 0);
            outboundConnectionPolicyComboBox.insertItemAt("NONE", 0);


            inboundPolicyComboBox.setSelectedIndex(0);
            for(int i = 1;i < inboundPolicyComboBox.getItemCount();i++) {
                ServiceHeader serviceHeader = (ServiceHeader)inboundPolicyComboBox.getItemAt(i);
                if(Goid.equals(serviceHeader.getGoid(), connection.getInboundPolicyOID())) {
                    inboundPolicyComboBox.setSelectedIndex(i);
                    break;
                }
            }

            connectionPolicyComboBox.setSelectedIndex(0);
            for(int i = 1;i < connectionPolicyComboBox.getItemCount();i++) {
                ServiceHeader serviceHeader = (ServiceHeader)connectionPolicyComboBox.getItemAt(i);
                if(Goid.equals(serviceHeader.getGoid(), connection.getConnectionPolicyGOID())) {
                    connectionPolicyComboBox.setSelectedIndex(i);
                    break;
                }
            }

            outboundPolicyComboBox.setSelectedIndex(0);
            for(int i = 1;i < outboundPolicyComboBox.getItemCount();i++) {
                ServiceHeader serviceHeader = (ServiceHeader)outboundPolicyComboBox.getItemAt(i);
                if(Goid.equals(serviceHeader.getGoid(), connection.getOutboundPolicyOID())) {
                    outboundPolicyComboBox.setSelectedIndex(i);
                    break;
                }
            }

            outboundConnectionPolicyComboBox.setSelectedIndex(0);
            for(int i = 1; i < outboundConnectionPolicyComboBox.getItemCount(); i++) {
                ServiceHeader serviceHeader = (ServiceHeader)outboundConnectionPolicyComboBox.getItemAt(i);
                if(Goid.equals(serviceHeader.getGoid(), connection.getOutboundConnectionPolicyId())) {
                    outboundConnectionPolicyComboBox.setSelectedIndex(i);
                    break;
                }
            }

        } catch (FindException e) {
            logger.log(Level.WARNING, "Could not find published services");
            //Meant to do nothing.
        }
    }

    private void onCancel() {
        dispose();
    }

    private void onInboundSsLChecked() {
        inboundPrivateKeysComboBox.setEnabled(useInboundSSLCheckBox.isSelected());
        inboundClientAuthenticationComboBox.setEnabled(useInboundSSLCheckBox.isSelected());
        if ( inboundClientAuthenticationComboBox.getSelectedIndex() == -1)  {
            inboundClientAuthenticationComboBox.setSelectedIndex(0);
        }
        inboundTlsButton.setEnabled(useInboundSSLCheckBox.isSelected());
        inboundCipherSuitesButton.setEnabled(useInboundSSLCheckBox.isSelected());

    }

    private void onOutboundSslChecked() {
        useClientAuthenticationCheckBox.setEnabled(useOutboundSSLCheckBox.isSelected());
        onUseClientAuthorizationChecked();
        outboundTlsProtocolButton.setEnabled(useOutboundSSLCheckBox.isSelected());
        outboundCipherSuiteButton.setEnabled(useOutboundSSLCheckBox.isSelected());
    }

    private void onUseClientAuthorizationChecked() {
        outboundPrivateKeysComboBox.setEnabled(useClientAuthenticationCheckBox.isSelected() && useClientAuthenticationCheckBox.isEnabled());
        outboundPrivateKeyLabel.setEnabled(useClientAuthenticationCheckBox.isSelected() && useClientAuthenticationCheckBox.isEnabled());
        if (!useClientAuthenticationCheckBox.isSelected()) {
            outboundPrivateKeysComboBox.setSelectedIndex(0);
        }
    }

    private void onOutboundOnlyCheckBoxChecked() {
        boolean outOnly = outboundOnlyCheckBox.isSelected();
        inboundListenPort.setEnabled(!outOnly);
        maxInboundConnections.setEnabled(!outOnly);
        inBoundMaxIdleTime.setEnabled(!outOnly);
        connectionPolicyComboBox.setEnabled(!outOnly);
        inboundPolicyComboBox.setEnabled(!outOnly);
        useInboundSSLCheckBox.setEnabled(!outOnly);
        if (!outOnly) {
            onInboundSsLChecked();
        } else {
            inboundTlsButton.setEnabled(!outOnly);
            inboundCipherSuitesButton.setEnabled(!outOnly);
            inboundClientAuthenticationComboBox.setEnabled(!outOnly);
            inboundPrivateKeysComboBox.setEnabled(!outOnly);
        }
    }

    private void error(String s) {
        DialogDisplayer.showMessageDialog(this, s, null);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public WebSocketConnectionEntity getConnection() {
        return connection;
    }

}
