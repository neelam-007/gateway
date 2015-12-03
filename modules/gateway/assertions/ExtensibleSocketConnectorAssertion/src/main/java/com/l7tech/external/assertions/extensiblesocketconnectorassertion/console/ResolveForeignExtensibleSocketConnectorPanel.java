package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.*;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28/03/12
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResolveForeignExtensibleSocketConnectorPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignExtensibleSocketConnectorPanel.class.getName());

    private JPanel mainPanel;
    private JTextField nameField;
    private JComboBox directionComboBox;
    private JLabel bindAddressLabel;
    private JComboBox bindAddressComboBox;
    private JLabel hostnameLabel;
    private JTextField hostnameField;
    private JSpinner portField;
    private JCheckBox useSSLCheckBox;
    private DefaultComboBoxModel sslPrivateKeyInComboBoxModel;
    private DefaultComboBoxModel sslPrivateKeyOutComboBoxModel;
    private JLabel sslPrivateKeyLabel;
    private JComboBox sslPrivateKeyComboBox;
    private JLabel clientAuthLabel;
    private JComboBox clientAuthComboBox;
    private JLabel threadPoolMinLabel;
    private JSpinner threadPoolMinField;
    private JLabel threadPoolMaxLabel;
    private JSpinner threadPoolMaxField;
    private JLabel serviceLabel;
    private JComboBox serviceComboBox;
    private JLabel contentTypeLabel;
    private JComboBox contentTypeComboBox;
    private JLabel maxMessageSizeLabel;
    private JSpinner maxMessageSizeField;
    private JLabel enabledLabel;
    private JComboBox enabledComboBox;
    private JButton viewCodecConfigurationButton;
    private JRadioButton changeRadioButton;
    private JComboBox updateAssertionsExtensibleSocketConnectorComboBox;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JButton createExtensibleSocketConnectorButton;
    private JComboBox exchangePatternComboBox;

    private ExtensibleSocketConnectorReference foreignRef;

    public ResolveForeignExtensibleSocketConnectorPanel(WizardStepPanel next, ExtensibleSocketConnectorReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved ExtensibleSocketConnector Connection " + foreignRef.getName();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (updateAssertionsExtensibleSocketConnectorComboBox.getSelectedIndex() < 0) return false;

            final ExtensibleSocketConnectorEntity connector = (ExtensibleSocketConnectorEntity) updateAssertionsExtensibleSocketConnectorComboBox.getSelectedItem();
            foreignRef.setLocalizeReplace(connector.getGoid());
        } else if (removeRadioButton.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        populateConnectorComboBox();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        initializeFields();
        setFieldValues();
        initializeListeners();
        showHideComponents();
        enableDisableConnectorComponents();

        populateConnectorComboBox();
        enableAndDisableComponents();
    }

    private void initializeFields() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[]{"Inbound", "Outbound"}));

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        java.util.List<String> entries = new ArrayList<String>();
        entries.add("0.0.0.0");
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }
        bindAddressComboBox.setModel(new DefaultComboBoxModel(entries.toArray(new String[entries.size()])));

        portField.setModel(new SpinnerNumberModel(8888, 1024, 65535, 1));

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

            sslPrivateKeyInComboBoxModel = new DefaultComboBoxModel(keys);

            sslPrivateKeyOutComboBoxModel = new DefaultComboBoxModel();
            sslPrivateKeyOutComboBoxModel.addElement("None");
            for (String keyId : keys) {
                sslPrivateKeyOutComboBoxModel.addElement(keyId);
            }
        } catch (IOException e) {
        } catch (FindException e) {
        } catch (KeyStoreException e) {
        }

        clientAuthComboBox.setModel(new DefaultComboBoxModel(SSLClientAuthEnum.values()));

        threadPoolMinField.setModel(new SpinnerNumberModel(10, 1, 500, 1));
        threadPoolMaxField.setModel(new SpinnerNumberModel(20, 1, 500, 1));

        try {
            serviceComboBox.setModel(new DefaultComboBoxModel(Registry.getDefault().getServiceManager().findAllPublishedServices()));
        } catch (FindException e) {
        }

        Vector<CodecModule> codecModules = getEntityManager().getCodecModules();
        boolean found = false;
        for (CodecModule codecModule : codecModules) {
            if (codecModule.getConfigurationClassName().equals(foreignRef.getCodecConfiguration().getClass().getName())) {
                contentTypeComboBox.setModel(new DefaultComboBoxModel(new String[]{codecModule.getDefaultContentType()}));
            }
        }

        if (!found) {
            contentTypeComboBox.setModel(new DefaultComboBoxModel(new String[0]));
        }

        maxMessageSizeField.setModel(new SpinnerNumberModel(1024 * 1024, 1, Integer.MAX_VALUE, 1024));

        enabledComboBox.setModel(new DefaultComboBoxModel(new String[]{"Yes", "No"}));
    }

    private void setFieldValues() {
        nameField.setText(foreignRef.getName());
        directionComboBox.setSelectedIndex(foreignRef.isIn() ? 0 : 1);

        if (foreignRef.isIn()) {
            for (int i = 0; i < bindAddressComboBox.getItemCount(); i++) {
                String address = (String) bindAddressComboBox.getItemAt(i);
                if (address.equals(foreignRef.getBindAddress())) {
                    bindAddressComboBox.setSelectedIndex(i);
                    break;
                }
            }

            portField.setValue(foreignRef.getPort());

            useSSLCheckBox.setSelected(foreignRef.isUseSsl());
            if (foreignRef.isUseSsl()) {
                sslPrivateKeyComboBox.setModel(sslPrivateKeyInComboBoxModel);
                for (int i = 0; i < sslPrivateKeyInComboBoxModel.getSize(); i++) {
                    String keyId = (String) sslPrivateKeyInComboBoxModel.getElementAt(i);
                    if (keyId.equals(foreignRef.getSslKeyId())) {
                        sslPrivateKeyComboBox.setSelectedIndex(i);
                        break;
                    }
                }

                clientAuthComboBox.setSelectedItem(foreignRef.getClientAuthEnum());
            }

            threadPoolMinField.setValue(foreignRef.getThreadPoolMin());
            threadPoolMaxField.setValue(foreignRef.getThreadPoolMax());

            for (int i = 0; i < serviceComboBox.getItemCount(); i++) {
                ServiceHeader serviceHeader = (ServiceHeader) serviceComboBox.getItemAt(i);
                if (serviceHeader.getGoid().equals(foreignRef.getGoid())) {
                    serviceComboBox.setSelectedIndex(i);
                    break;
                }
            }

            contentTypeComboBox.setSelectedItem(foreignRef.getContentType());

            maxMessageSizeField.setValue(foreignRef.getMaxMessageSize());

            enabledComboBox.setSelectedIndex(foreignRef.isEnabled() ? 0 : 1);
        } else {
            hostnameField.setText(foreignRef.getHostname());
            portField.setValue(foreignRef.getPort());

            useSSLCheckBox.setSelected(foreignRef.isUseSsl());
            if (foreignRef.isUseSsl()) {
                sslPrivateKeyComboBox.setModel(sslPrivateKeyOutComboBoxModel);
                if (foreignRef.getSslKeyId() == null) {
                    sslPrivateKeyComboBox.setSelectedIndex(0);
                } else {
                    sslPrivateKeyComboBox.setSelectedIndex(0);
                    for (int i = 1; i < sslPrivateKeyOutComboBoxModel.getSize(); i++) {
                        String keyId = (String) sslPrivateKeyOutComboBoxModel.getElementAt(i);
                        if (keyId.equals(foreignRef.getSslKeyId())) {
                            sslPrivateKeyComboBox.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }

            exchangePatternComboBox.setSelectedItem(foreignRef.getExchangePattern());
        }

        // default is delete
        removeRadioButton.setSelected(true);
    }

    private void showHideComponents() {
        boolean isIn = directionComboBox.getSelectedIndex() == 0;
        bindAddressLabel.setVisible(isIn);
        bindAddressComboBox.setVisible(isIn);
        hostnameLabel.setVisible(!isIn);
        hostnameField.setVisible(!isIn);
        clientAuthLabel.setVisible(isIn);
        clientAuthComboBox.setVisible(isIn);
        threadPoolMinLabel.setVisible(isIn);
        threadPoolMinField.setVisible(isIn);
        threadPoolMaxLabel.setVisible(isIn);
        threadPoolMaxField.setVisible(isIn);
        serviceLabel.setVisible(isIn);
        serviceComboBox.setVisible(isIn);
        contentTypeLabel.setVisible(isIn);
        contentTypeComboBox.setVisible(isIn);
        maxMessageSizeLabel.setVisible(isIn);
        maxMessageSizeField.setVisible(isIn);
        enabledLabel.setVisible(isIn);
        enabledComboBox.setVisible(isIn);
        exchangePatternComboBox.setVisible(!isIn);
    }

    private void enableDisableConnectorComponents() {
        sslPrivateKeyLabel.setEnabled(useSSLCheckBox.isSelected());
        sslPrivateKeyComboBox.setEnabled(useSSLCheckBox.isSelected());
        if (directionComboBox.getSelectedIndex() == 0) {
            clientAuthLabel.setEnabled(useSSLCheckBox.isSelected());
            clientAuthComboBox.setEnabled(useSSLCheckBox.isSelected());
        }
    }

    private void initializeListeners() {
        directionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHideComponents();
            }
        });

        useSSLCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableConnectorComponents();
            }
        });

        viewCodecConfigurationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Vector<CodecModule> codecModules = getEntityManager().getCodecModules();
                ViewCodecSettingsDialog dialog = null;
                for (CodecModule codecModule : codecModules) {
                    if (codecModule.getConfigurationClassName().equals(foreignRef.getClass().getName())) {
                        try {
                            Class codecSettingsPanelClass = ResolveForeignExtensibleSocketConnectorPanel.class.getClassLoader().loadClass(
                                    codecModule.getDialogClassName()
                            );
                            CodecSettingsPanel codecSettingsPanel = (CodecSettingsPanel) codecSettingsPanelClass.newInstance();

                            dialog = new ViewCodecSettingsDialog(TopComponents.getInstance().getTopParent(),
                                    foreignRef.getCodecConfiguration(),
                                    codecSettingsPanel);
                        } catch (Exception ex) {
                            //
                        }
                    }
                }

                if (dialog == null) {
                    dialog = new ViewCodecSettingsDialog(TopComponents.getInstance().getTopParent(), foreignRef.getCodecConfiguration(), null);
                }

                Utilities.centerOnScreen(dialog);
                dialog.setVisible(true);
            }
        });

        updateAssertionsExtensibleSocketConnectorComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsExtensibleSocketConnectorComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsExtensibleSocketConnectorComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsExtensibleSocketConnectorComboBox.setEnabled(false);
            }
        });

        updateAssertionsExtensibleSocketConnectorComboBox.setRenderer(new TextListCellRenderer<ExtensibleSocketConnectorEntity>(new Functions.Unary<String, ExtensibleSocketConnectorEntity>() {
            @Override
            public String call(final ExtensibleSocketConnectorEntity entity) {
                return getConnectorInfo(entity);
            }
        }));

        createExtensibleSocketConnectorButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createExtensibleSocketConnector();
            }
        });
    }

    private void createExtensibleSocketConnector() {
        final ExtensibleSocketConnectorEntity newConnection = new ExtensibleSocketConnectorEntity();
        newConnection.setName(foreignRef.getName());
        newConnection.setIn(foreignRef.isIn());
        if (newConnection.isIn()) {
            newConnection.setBindAddress(foreignRef.getBindAddress());
            newConnection.setThreadPoolMin(foreignRef.getThreadPoolMin());
            newConnection.setThreadPoolMax(foreignRef.getThreadPoolMax());
            newConnection.setServiceGoid(foreignRef.getServiceGoid());
            newConnection.setContentType(foreignRef.getContentType());
            newConnection.setMaxMessageSize(foreignRef.getMaxMessageSize());
            newConnection.setEnabled(foreignRef.isEnabled());
        } else {
            newConnection.setHostname(foreignRef.getHostname());
            newConnection.setExchangePattern(foreignRef.getExchangePattern());
            newConnection.setKeepAlive(foreignRef.isKeepAlive());
            newConnection.setListenTimeout(foreignRef.getListenTimeOut());
            newConnection.setUseDnsLookup(foreignRef.isUseDnsLookup());
            if (newConnection.isUseDnsLookup()) {
                newConnection.setDnsDomainName(foreignRef.getDnsDomainName());
                newConnection.setDnsService(foreignRef.getDnsService());
            }
        }
        newConnection.setUseSsl(foreignRef.isUseSsl());
        if (newConnection.isUseSsl()) {
            newConnection.setSslKeyId(foreignRef.getSslKeyId());
            newConnection.setClientAuthEnum(foreignRef.getClientAuthEnum());
        }
        newConnection.setPort(foreignRef.getPort());
        newConnection.setCodecConfiguration(foreignRef.getCodecConfiguration());

        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);
    }

    private void editAndSave(final ExtensibleSocketConnectorEntity connector) {
        final ExtensibleSocketConnectorDialog dialog = new ExtensibleSocketConnectorDialog(TopComponents.getInstance().getTopParent(), connector);
        Utilities.centerOnScreen(dialog);

        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            try {
                getEntityManager().save(connector);

                if (connector.isIn()) {
                    return; // They changed it to inbound, which cannot be referenced from an assertion
                }

                populateConnectorComboBox();

                for (int i = 0; i < updateAssertionsExtensibleSocketConnectorComboBox.getItemCount(); i++) {
                    ExtensibleSocketConnectorEntity entity = (ExtensibleSocketConnectorEntity) updateAssertionsExtensibleSocketConnectorComboBox.getItemAt(i);
                    if (connector.getGoid().equals(entity.getGoid())) {
                        updateAssertionsExtensibleSocketConnectorComboBox.setSelectedIndex(i);
                        break;
                    }
                }

                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                updateAssertionsExtensibleSocketConnectorComboBox.setEnabled(true);
            } catch (SaveException e) {
                logger.log(Level.INFO, "Failed to save the new ExtensibleSocketConnector connection.", e);
            } catch (UpdateException e) {
                logger.log(Level.INFO, "Failed to save the new ExtensibleSocketConnector connection.", e);
            }
        }
    }

    private static ExtensibleSocketConnectorEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(ExtensibleSocketConnectorEntityAdmin.class, null);
    }

    private void populateConnectorComboBox() {
        ExtensibleSocketConnectorEntityAdmin admin = getEntityManager();
        if (admin == null) return;

        final Object selectedItem = updateAssertionsExtensibleSocketConnectorComboBox.getSelectedItem();
        final Collection<ExtensibleSocketConnectorEntity> connections = findAllOutboundExtensibleSocketConnectors();

        // Sort connectors by combination name
        Collections.sort((java.util.List<ExtensibleSocketConnectorEntity>) connections, new Comparator<ExtensibleSocketConnectorEntity>() {
            @Override
            public int compare(ExtensibleSocketConnectorEntity o1, ExtensibleSocketConnectorEntity o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // Add all items into the combo box.
        updateAssertionsExtensibleSocketConnectorComboBox.setModel(Utilities.comboBoxModel(connections));

        if (selectedItem != null && updateAssertionsExtensibleSocketConnectorComboBox.getModel().getSize() > 0) {
            updateAssertionsExtensibleSocketConnectorComboBox.setSelectedItem(selectedItem);
            if (updateAssertionsExtensibleSocketConnectorComboBox.getSelectedIndex() == -1) {
                updateAssertionsExtensibleSocketConnectorComboBox.setSelectedIndex(0);
            }
        }
    }

    private java.util.List<ExtensibleSocketConnectorEntity> findAllOutboundExtensibleSocketConnectors() {
        try {
            final ExtensibleSocketConnectorEntityAdmin admin = getEntityManager();
            ArrayList<ExtensibleSocketConnectorEntity> entities = new ArrayList<ExtensibleSocketConnectorEntity>();
            for (ExtensibleSocketConnectorEntity entity : admin.findAll()) {
                if (!entity.isIn()) {
                    entities.add(entity);
                }
            }

            return entities;
        } catch (IllegalStateException e) {
            // no admin context available
            logger.info("Unable to access queues from server.");
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error loading queues");
        }
        return emptyList();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = updateAssertionsExtensibleSocketConnectorComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }

    private String getConnectorInfo(final ExtensibleSocketConnectorEntity connection) {
        final StringBuilder builder = new StringBuilder();
        builder.append(connection.getName());
        builder.append(" [");
        builder.append(connection.getHostname());
        builder.append(':');
        builder.append(connection.getPort());
        builder.append(']');
        return builder.toString();
    }
}
