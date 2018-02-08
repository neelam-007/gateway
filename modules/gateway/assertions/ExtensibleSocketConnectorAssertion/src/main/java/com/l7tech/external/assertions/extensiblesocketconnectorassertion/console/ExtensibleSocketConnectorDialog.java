package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.*;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 30/11/11
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorDialog extends JDialog {
    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JTextField nameField;
    private JComboBox directionComboBox;
    private JComboBox dataProtocolComboBox;
    private JPanel inboundTab;
    private JSpinner portInField;
    private JCheckBox useSslInCheckBox;
    private JLabel sslKeyInLabel;
    private JComboBox sslKeyInComboBox;
    private JLabel sslClientAuthLabel;
    private JComboBox sslClientAuthComboBox;
    private JSpinner threadPoolMinField;
    private JSpinner threadPoolMaxField;
    private JComboBox networkInterfaceComboBox;
    private JComboBox serviceComboBox;
    private JComboBox contentTypeInComboBox;
    private JSpinner maxMessageSizeField;
    private JCheckBox enabledCheckBox;
    private JPanel outboundTab;
    private JTextField hostnameField;
    private JSpinner portOutField;
    private JCheckBox useSslOutCheckBox;
    private JComboBox sslKeyOutComboBox;
    private JComboBox contentTypeOutComboBox;
    private JPanel protocolTab;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox exchangePatternComboBox;
    private JTextField listenTimeout;
    private JCheckBox keepAlive;
    private JTextField dnsDomainNameField;
    private JTextField dnsServiceField;
    private JRadioButton usePortValueRadioButton;
    private JRadioButton useDNSLookupRadioButton;

    private ExtensibleSocketConnectorEntity config;

    private CodecSettingsPanel codecSettingsPanel;
    private CodecConfiguration codecConfig;

    private boolean confirmed = false;

    private static final String INBOUND_OPTION = "Inbound";
    private static final String OUTBOUND_OPTION = "Outbound";

    public ExtensibleSocketConnectorDialog(Dialog parent, ExtensibleSocketConnectorEntity config) {
        super(parent, "Socket Connector Properties", true);

        this.config = config;
        initComponents();
        updateView();
    }

    public ExtensibleSocketConnectorDialog(Frame parent, ExtensibleSocketConnectorEntity config) {
        super(parent, "Socket Connector Properties", true);

        this.config = config;
        initComponents();
        updateView();
    }

    private void initComponents() {
        setContentPane(mainPanel);

        directionComboBox.setModel(new DefaultComboBoxModel(new String[]{
                INBOUND_OPTION,
                OUTBOUND_OPTION
        }));
        directionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setEnabledAt(1, INBOUND_OPTION.equals(directionComboBox.getSelectedItem()));
                tabbedPane.setEnabledAt(2, OUTBOUND_OPTION.equals(directionComboBox.getSelectedItem()));

                //update codec panel and settings depending on direction (inbound/outbound)
                codecConfig.setInbound(INBOUND_OPTION.equals(directionComboBox.getSelectedItem()));
                codecSettingsPanel.updateView(codecConfig);

                //update port settings on outbound tab when outbound selected
                if (OUTBOUND_OPTION.equals(directionComboBox.getSelectedItem())) {
                    updateOutboundPortData();
                }
            }
        });

        dataProtocolComboBox.setModel(new DefaultComboBoxModel(getEntityManager().getCodecModules()));
        dataProtocolComboBox.setSelectedIndex(0);
        dataProtocolComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                protocolTab.removeAll();

                CodecModule codecModule = (CodecModule) dataProtocolComboBox.getSelectedItem();
                try {
                    //create out codec settings panel
                    Class codecSettingsPanelClass = ExtensibleSocketConnectorDialog.class.getClassLoader().loadClass(
                            (codecModule).getDialogClassName()
                    );

                    CodecSettingsPanel csp = (CodecSettingsPanel) codecSettingsPanelClass.newInstance();

                    codecSettingsPanel = csp;
                    protocolTab.add(codecSettingsPanel.getPanel());

                    //get the codec config
                    try {
                        Class codecConfigClass = ExtensibleSocketConnectorDialog.class.getClassLoader().loadClass(
                                ((CodecModule) dataProtocolComboBox.getSelectedItem()).getConfigurationClassName()
                        );
                        codecConfig = (CodecConfiguration) codecConfigClass.newInstance();
                        codecConfig.setInbound(config.isIn());

                        codecSettingsPanel.updateView(codecConfig);
                    } catch (Exception exception) {
                        //
                    }

                    contentTypeInComboBox.setSelectedItem(codecModule.getDefaultContentType());
                    contentTypeOutComboBox.setSelectedItem(codecModule.getDefaultContentType());
                } catch (ClassNotFoundException ex) {
                    codecSettingsPanel = null;
                    JPanel panel = new JPanel();
                    panel.add(new JLabel("Cannot configure " + codecModule.getDisplayName()));
                    protocolTab.add(panel);
                } catch (InstantiationException ex) {
                    codecSettingsPanel = null;
                    JPanel panel = new JPanel();
                    panel.add(new JLabel("Cannot configure " + codecModule.getDisplayName()));
                    protocolTab.add(panel);
                } catch (IllegalAccessException ex) {
                    codecSettingsPanel = null;
                    JPanel panel = new JPanel();
                    panel.add(new JLabel("Cannot configure " + codecModule.getDisplayName()));
                    protocolTab.add(panel);
                }
            }
        });

        portInField.setModel(new SpinnerNumberModel(config.getDefaultPort(), 1, 65535, 1));

        useSslInCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sslKeyInLabel.setEnabled(useSslInCheckBox.isSelected());
                sslKeyInComboBox.setEnabled(useSslInCheckBox.isSelected());
                sslClientAuthLabel.setEnabled(useSslInCheckBox.isSelected());
                sslClientAuthComboBox.setEnabled(useSslInCheckBox.isSelected());
            }
        });

        try {
            Vector<String> keys = new Vector<String>();
            java.util.List<KeystoreFileEntityHeader> keystores = Registry.getDefault().getTrustedCertManager().findAllKeystores(true);
            keys = new Vector<String>();
            for (KeystoreFileEntityHeader keystore : keystores) {
                try {
                    java.util.List<SsgKeyEntry> keyEntries = Registry.getDefault().getTrustedCertManager().findAllKeys(keystore.getGoid(), true);
                    for (SsgKeyEntry keyEntry : keyEntries) {
                        if (keyEntry.getId() != null) {
                            keys.add(keyEntry.getId());
                        }
                    }
                } catch (CertificateException e) {
                }
            }

            sslKeyInComboBox.setModel(new DefaultComboBoxModel(keys));

            DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
            comboBoxModel.addElement("None");
            for (String keyId : keys) {
                comboBoxModel.addElement(keyId);
            }
            sslKeyOutComboBox.setModel(comboBoxModel);
        } catch (IOException e) {
        } catch (FindException e) {
        } catch (KeyStoreException e) {
        }

        sslClientAuthComboBox.setModel(new DefaultComboBoxModel(SSLClientAuthEnum.values()));

        threadPoolMinField.setModel(new SpinnerNumberModel(10, 0, Integer.MAX_VALUE, 1));
        threadPoolMaxField.setModel(new SpinnerNumberModel(20, 1, Integer.MAX_VALUE, 1));

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        java.util.List<String> entries = new ArrayList<String>();
        entries.add("0.0.0.0");
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }
        networkInterfaceComboBox.setModel(new DefaultComboBoxModel(entries.toArray()));

        try {
            serviceComboBox.setModel(new DefaultComboBoxModel(Registry.getDefault().getServiceManager().findAllPublishedServices()));
        } catch (FindException e) {
        }

        contentTypeInComboBox.setModel(new DefaultComboBoxModel(new String[]{((CodecModule) dataProtocolComboBox.getSelectedItem()).getDefaultContentType()}));

        maxMessageSizeField.setModel(new SpinnerNumberModel(1024 * 1024, 1, Integer.MAX_VALUE, 1024));

        usePortValueRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!portOutField.isEnabled()) {
                    portOutField.setEnabled(true);

                    //use the default port if the entered port is invalid
                    if (config.getPort() < 1 || config.getPort() > 65535) {
                        portOutField.setValue(config.getDefaultPort());
                    } else {
                        portOutField.setValue(config.getPort());
                    }
                }

                dnsDomainNameField.setEnabled(false);
                dnsDomainNameField.setText("");
                dnsServiceField.setEnabled(false);
                dnsServiceField.setText("");
            }
        });

        portOutField.setModel(new SpinnerNumberModel(config.getDefaultPort(), 1, 65535, 1));

        useDNSLookupRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (!dnsDomainNameField.isEnabled()) {
                    dnsDomainNameField.setEnabled(true);
                    dnsDomainNameField.setText(config.getDnsDomainName());
                }

                if (!dnsServiceField.isEnabled()) {
                    dnsServiceField.setEnabled(true);
                    dnsServiceField.setText(config.getDnsService());
                }

                portOutField.setEnabled(false);
                portOutField.setValue(0);
            }
        });

        useSslOutCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sslKeyOutComboBox.setEnabled(useSslOutCheckBox.isSelected());
            }
        });

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });

        sslKeyInLabel.setEnabled(useSslInCheckBox.isSelected());
        sslKeyInComboBox.setEnabled(useSslInCheckBox.isSelected());
        sslClientAuthLabel.setEnabled(useSslInCheckBox.isSelected());
        sslClientAuthComboBox.setEnabled(useSslInCheckBox.isSelected());

        sslKeyOutComboBox.setEnabled(useSslOutCheckBox.isSelected());

        contentTypeOutComboBox.setModel(new DefaultComboBoxModel(new String[]{((CodecModule) dataProtocolComboBox.getSelectedItem()).getDefaultContentType()}));
        exchangePatternComboBox.setModel(new DefaultComboBoxModel(ExchangePatternEnum.values()));
        exchangePatternComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (((ExchangePatternEnum) exchangePatternComboBox.getSelectedItem()) == ExchangePatternEnum.OutOnly) {
                    listenTimeout.setText("");
                    listenTimeout.setEnabled(false);
                } else {
                    listenTimeout.setEnabled(true);
                }
            }
        });

        Utilities.equalizeButtonSizes(okButton, cancelButton);

        setMinimumSize(new Dimension(400, -1));

        pack();
        Utilities.centerOnParentWindow(this);
    }

    private void ok() {
        long listenTimeoutValue = 0L;

        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No name was specified.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (INBOUND_OPTION.equals(directionComboBox.getSelectedItem())) {
            if (useSslInCheckBox.isSelected() && sslKeyInComboBox.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "No SSL Key was selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (((Number) threadPoolMaxField.getValue()).intValue() < ((Number) threadPoolMinField.getValue()).intValue()) {
                JOptionPane.showMessageDialog(this, "The thread pool max must be equal to or greater than the thread pool min.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (((String) contentTypeInComboBox.getSelectedItem()).trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "The content type cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (serviceComboBox.getSelectedItem() == null || ((ServiceHeader) serviceComboBox.getSelectedItem()).getGoid() == null) {
                JOptionPane.showMessageDialog(this, "Invalid service selected for inbound connector.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } else if (OUTBOUND_OPTION.equals(directionComboBox.getSelectedItem())) {
            if (hostnameField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hostname was specified.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ((ExchangePatternEnum) exchangePatternComboBox.getSelectedItem() == ExchangePatternEnum.OutIn) {
                try {
                    listenTimeoutValue = Long.parseLong(listenTimeout.getText());

                    if (listenTimeoutValue <= 0) {
                        JOptionPane.showMessageDialog(this, "Invalid value for Listen Timeout.  Value must be an integer greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid value for Listen Timeout.  Value must be an integer greater than 0.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        if (!codecSettingsPanel.validateView()) {
            return;
        }

        config.setName(nameField.getText().trim());
        if (INBOUND_OPTION.equals(directionComboBox.getSelectedItem())) {
            config.setIn(true);
        } else if (OUTBOUND_OPTION.equals(directionComboBox.getSelectedItem())) {
            config.setIn(false);
        }

        if (config.isIn()) {
            config.setPort(((Number) portInField.getValue()).intValue());
            config.setUseSsl(useSslInCheckBox.isSelected());
            if (config.isUseSsl()) {
                config.setSslKeyId((String) sslKeyInComboBox.getSelectedItem());
                config.setClientAuthEnum((SSLClientAuthEnum) sslClientAuthComboBox.getSelectedItem());
            } else {
                config.setSslKeyId(null);
                config.setClientAuthEnum(SSLClientAuthEnum.DISABLED);
            }
            config.setThreadPoolMin(((Number) threadPoolMinField.getValue()).intValue());
            config.setThreadPoolMax(((Number) threadPoolMaxField.getValue()).intValue());
            config.setBindAddress((String) networkInterfaceComboBox.getSelectedItem());
            config.setServiceGoid(((ServiceHeader) serviceComboBox.getSelectedItem()).getGoid());
            config.setContentType(((String) contentTypeInComboBox.getSelectedItem()).trim());
            config.setMaxMessageSize(((Number) maxMessageSizeField.getValue()).intValue());
            config.setEnabled(enabledCheckBox.isSelected());
        } else {
            config.setHostname(hostnameField.getText().trim());

            config.setUsePortValue(usePortValueRadioButton.isSelected());
            config.setPort(((Number) portOutField.getValue()).intValue());
            config.setUseDnsLookup(useDNSLookupRadioButton.isSelected());
            config.setDnsDomainName(dnsDomainNameField.getText());
            config.setDnsService(dnsServiceField.getText());

            config.setUseSsl(useSslOutCheckBox.isSelected());
            if (config.isUseSsl()) {
                if ("None".equals(sslKeyOutComboBox.getSelectedItem())) {
                    config.setSslKeyId(null);
                } else {
                    config.setSslKeyId((String) sslKeyOutComboBox.getSelectedItem());
                }
            }

            config.setContentType((String) contentTypeOutComboBox.getSelectedItem());
            config.setExchangePattern((ExchangePatternEnum) exchangePatternComboBox.getSelectedItem());
            config.setListenTimeout(listenTimeoutValue);
            config.setKeepAlive(keepAlive.isSelected());
        }

        codecSettingsPanel.updateModel(codecConfig);
        config.setCodecConfiguration(codecConfig);

        confirmed = true;
        dispose();
    }

    private void cancel() {
        dispose();
    }

    private void updateView() {
        nameField.setText(config.getName() == null ? "" : config.getName());

        if (config.getCodecConfiguration() != null) {
            for (int i = 0; i < dataProtocolComboBox.getItemCount(); i++) {
                CodecModule codecModule = (CodecModule) dataProtocolComboBox.getItemAt(i);
                if (codecModule.getConfigurationClassName().equals(config.getCodecConfiguration().getClass().getName())) {
                    dataProtocolComboBox.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            dataProtocolComboBox.setSelectedIndex(0);
        }

        directionComboBox.setSelectedItem(config.isIn() ? INBOUND_OPTION : OUTBOUND_OPTION);

        if (config.isIn()) {
            portInField.setValue(config.getPort());
            useSslInCheckBox.setSelected(config.isUseSsl());
            if (config.isUseSsl()) {
                sslKeyInComboBox.setSelectedItem(config.getSslKeyId());
                sslClientAuthComboBox.setSelectedItem(config.getClientAuthEnum());
            }
            threadPoolMinField.setValue(config.getThreadPoolMin());
            threadPoolMaxField.setValue(config.getThreadPoolMax());

            if (config.getBindAddress() != null) {
                networkInterfaceComboBox.setSelectedItem(config.getBindAddress());
            }

            if (config.getServiceGoid() != null) {
                for (int i = 0; i < serviceComboBox.getItemCount(); i++) {
                    ServiceHeader service = (ServiceHeader) serviceComboBox.getItemAt(i);
                    if (service.getGoid().equals(config.getServiceGoid())) {
                        serviceComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }

            if (config.getContentType() == null) {
                contentTypeInComboBox.setSelectedItem(((CodecModule) dataProtocolComboBox.getSelectedItem()).getDefaultContentType());
            } else {
                contentTypeInComboBox.setSelectedItem(config.getContentType());
            }

            maxMessageSizeField.setValue(config.getMaxMessageSize());

            enabledCheckBox.setSelected(config.isEnabled());

            outboundTab.setEnabled(false);
        } else {
            hostnameField.setText(config.getHostname() == null ? "" : config.getHostname());

            updateOutboundPortData();

            useSslOutCheckBox.setSelected(config.isUseSsl());
            if (config.isUseSsl() && config.getSslKeyId() != null) {
                sslKeyOutComboBox.setSelectedItem(config.getSslKeyId());
            } else {
                sslKeyOutComboBox.setSelectedItem("None");
            }
        }

        //update the codec settings... if this was a persisted configuration
        if (config.getCodecConfiguration() != null) {
            codecSettingsPanel.updateView(config.getCodecConfiguration());
            codecSettingsPanel.updateModel(codecConfig); //this creates a working copy we can use...
        }

        sslKeyInLabel.setEnabled(useSslInCheckBox.isSelected());
        sslKeyInComboBox.setEnabled(useSslInCheckBox.isSelected());
        sslClientAuthLabel.setEnabled(useSslInCheckBox.isSelected());
        sslClientAuthComboBox.setEnabled(useSslInCheckBox.isSelected());

        sslKeyOutComboBox.setEnabled(useSslOutCheckBox.isSelected());

        if (config.getContentType() == null) {
            contentTypeOutComboBox.setSelectedItem(((CodecModule) dataProtocolComboBox.getSelectedItem()).getDefaultContentType());
        } else {
            contentTypeOutComboBox.setSelectedItem(config.getContentType());
        }

        exchangePatternComboBox.setSelectedItem(config.getExchangePattern());
        listenTimeout.setText(Long.toString(config.getListenTimeout()));
        keepAlive.setSelected(config.isKeepAlive());
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void updateOutboundPortData() {
        if (config.isUsePortValue()) {
            usePortValueRadioButton.setSelected(true);
            usePortValueRadioButton.doClick(); //simulate a click on the radio button, this will activate the ActionListener that will setup the dns fields
            portOutField.setValue(config.getPort());
        }

        if (config.isUseDnsLookup()) {
            useDNSLookupRadioButton.setSelected(true);
            useDNSLookupRadioButton.doClick(); //simulate a click on the radio button, this will activate the ActionListener that will setup the port fields
            dnsDomainNameField.setText(config.getDnsDomainName());
            dnsServiceField.setText(config.getDnsService());
        }
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
    }
}
