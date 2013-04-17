package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgConnector.*;

public class SsgConnectorPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgConnectorPropertiesDialog.class.getName());
    private static final boolean ENABLE_FTPS_TLS12 = ConfigFactory.getBooleanProperty( "com.l7tech.console.connector.allowFtpsTls12", false );
    private static final String DIALOG_TITLE = "Listen Port Properties";
    private static final int TAB_SSL = 1;
    private static final int TAB_HTTP = 2;
    private static final int TAB_FTP = 3;
    private static final int TAB_CUSTOM = 4;
    private static final int DEFAULT_POOL_SIZE = 20;

    private static class ClientAuthType {
        private static final Map<Integer, ClientAuthType> bycode = new ConcurrentHashMap<Integer, ClientAuthType>();
        final int code;
        final String label;
        public ClientAuthType(int code, String label) {
            this.code = code;
            this.label = label;
            bycode.put(code, this);
        }
        @Override
        public String toString() {
            return label;
        }
    }
    private static final Object CA_NONE = new ClientAuthType(SsgConnector.CLIENT_AUTH_NEVER, "None");
    private static final Object CA_OPTIONAL = new ClientAuthType(SsgConnector.CLIENT_AUTH_OPTIONAL, "Optional");
    private static final Object CA_REQUIRED = new ClientAuthType(SsgConnector.CLIENT_AUTH_ALWAYS, "Required");

    private static final String INTERFACE_ANY = "(All)";

    private static final String CPROP_WASENABLED = SsgConnectorPropertiesDialog.class.getName() + ".wasEnabled";

    private static final Icon collapseIcon = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/CollapseAll.gif"));
    private static final Icon expandIcon = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/ExpandAll.gif"));

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private SquigglyTextField nameField;
    private SquigglyTextField portField;
    private JComboBox protocolComboBox;
    private JComboBox interfaceComboBox;
    private PrivateKeysComboBox privateKeyComboBox;
    private JButton managePrivateKeysButton;
    private JComboBox clientAuthComboBox;
    private JList cipherSuiteList;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton uncheckAllButton;
    private JButton defaultCipherListButton;
    private JTextField portRangeStartField;
    private JTextField portRangeCountField;
    private JCheckBox enabledCheckBox;
    private JTabbedPane tabbedPane;
    private JCheckBox cbEnableMessageInput;
    private JCheckBox cbEnableBuiltinServices;
    private JCheckBox cbEnableEsmRemote;
    private JCheckBox cbEnableSsmRemote;
    private JCheckBox cbEnableSsmApplet;
    private JCheckBox cbEnableNode;
    private JCheckBox cbEnablePCAPI;
    private JButton addPropertyButton;
    private JButton editPropertyButton;
    private JButton removePropertyButton;
    private JList propertyList;
    private JSpinner threadPoolSizeSpinner;
    private JCheckBox usePrivateThreadPoolCheckBox;
    private JLabel threadPoolSizeLabel;
    private JButton interfacesButton;
    private JCheckBox tls10CheckBox;
    private JCheckBox tls11CheckBox;
    private JCheckBox tls12CheckBox;
    private JPanel otherSettingsPanel;
    private JComboBox contentTypeComboBox;
    private JCheckBox overrideContentTypeCheckBox;
    private JCheckBox hardwiredServiceCheckBox;
    private ServiceComboBox serviceNameComboBox;
    private JButton collapseOrExpandButton;
    private JCheckBox policyDiscoveryCheckBox;
    private JCheckBox pingServiceCheckBox;
    private JCheckBox stsCheckBox;
    private JCheckBox csrHandlerCheckBox;
    private JCheckBox passwordChangeCheckBox;
    private JCheckBox wsdlProxyCheckBox;
    private JCheckBox snmpQueryCheckBox;
    private JPanel builtinServicesPanel;
    private ByteLimitPanel requestByteLimitPanel;
    private SecurityZoneWidget zoneControl;

    private SsgConnector connector;
    private boolean confirmed = false;
    private CipherSuiteListModel cipherSuiteListModel;
    private DefaultComboBoxModel interfaceComboBoxModel;
    private DefaultListModel propertyListModel = new DefaultListModel();
    private List<String> toBeRemovedProperties = new ArrayList<String>();
    private boolean isCluster = true;
    private JCheckBox[] savedStateCheckBoxes = {
            cbEnableMessageInput,
            cbEnableSsmApplet,
            cbEnableSsmRemote,
            cbEnableEsmRemote,
            cbEnableNode,
            cbEnablePCAPI,
            tls10CheckBox,
            tls11CheckBox,
            tls12CheckBox,
            overrideContentTypeCheckBox,
            hardwiredServiceCheckBox,
    };
    private Map<Endpoint, JCheckBox> builtinServicesMap;

    private Map<String, TransportDescriptor> transportsByScheme = new TreeMap<String, TransportDescriptor>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, CustomTransportPropertiesPanel> customGuisByScheme = new TreeMap<String, CustomTransportPropertiesPanel>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Set<String>> reservedPropertyNamesByScheme = new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);

    private final boolean snmpQueryEnabled = Registry.getDefault().isAdminContextPresent() && Registry.getDefault().getTransportAdmin().isSnmpQueryEnabled();;

    public SsgConnectorPropertiesDialog(Window owner, SsgConnector connector, boolean isCluster) {
        super(owner, DIALOG_TITLE, ModalityType.DOCUMENT_MODAL);
        this.isCluster = isCluster;
        initialize(connector);
    }

    private void initialize(SsgConnector connector) {
        this.connector = connector;
        setContentPane(contentPane);
        getRootPane().setDefaultButton(okButton);

        InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        managePrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(SsgConnectorPropertiesDialog.this);
                pkmw.pack();
                Utilities.centerOnParentWindow(pkmw);
                DialogDisplayer.display(pkmw, new Runnable() {
                    @Override
                    public void run() {
                        privateKeyComboBox.repopulate();
                    }
                });
            }
        });

        final ActionListener enableOrDisableEndpointsListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableEndpoints();
            }
        };
        privateKeyComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        privateKeyComboBox.addActionListener(enableOrDisableEndpointsListener);

        otherSettingsPanel.setLayout(new CardLayout(8, 8));
        transportsByScheme.clear();
        Set<TransportDescriptor> protocols = new TreeSet<TransportDescriptor>(new ModularConnectorInfoComparator());
        TransportDescriptor[] ssgProtocols = Registry.getDefault().getTransportAdmin().getModularConnectorInfo();
        protocols.addAll(Arrays.asList(ssgProtocols));
        for (TransportDescriptor descriptor : protocols) {
            transportsByScheme.put(descriptor.getScheme(), descriptor);
            String customPropertiesPanelClassname = descriptor.getCustomPropertiesPanelClassname();
            CustomTransportPropertiesPanel panel = getCustomPropertiesPanel(descriptor, customPropertiesPanelClassname);
            if (panel != null)
                otherSettingsPanel.add(panel, descriptor.getScheme());
        }
        protocolComboBox.setModel(new ModularConnectorInfoComboBoxModel(protocols));

        protocolComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableComponents();
                enableOrDisableBuiltinServiceEndpoints(); // Do not merge this method into enableOrDisableComponents(), since it will be toggled only when the protocol is changed.
            }
        });

        clientAuthComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                CA_NONE,
                CA_OPTIONAL,
                CA_REQUIRED
        }));

        clientAuthComboBox.addActionListener(enableOrDisableEndpointsListener);

        // Make sure user-initiated changes to checkbox state get recorded so we can restore them
        final ActionListener stateSaver = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox source = (JCheckBox)e.getSource();
                saveCheckBoxState(source);
                enableOrDisableComponents();
            }
        };
        for (JCheckBox cb : savedStateCheckBoxes) {
            cb.addActionListener(stateSaver);
        }

        usePrivateThreadPoolCheckBox.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed(final ActionEvent e) {
                TransportDescriptor proto = getSelectedProtocol();
                threadPoolSizeSpinner.setEnabled( usePrivateThreadPoolCheckBox.isSelected() && (isHttpProto(proto) || isThreadPoolProto(proto)) );
                threadPoolSizeLabel.setEnabled( threadPoolSizeSpinner.isEnabled() );
            }
        } );

        initializeInterfaceComboBox();

        this.cipherSuiteListModel = CipherSuiteGuiUtil.createCipherSuiteListModel(this.cipherSuiteList, defaultCipherListButton, uncheckAllButton, null, moveUpButton, moveDownButton);

        propertyList.setModel(propertyListModel);
        propertyList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                //noinspection unchecked
                Pair<String, String> prop = (Pair<String, String>)value;
                String msg = prop.left + '=' + prop.right;
                setText(msg);
                return this;
            }
        });
        propertyList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisablePropertyButtons();
            }
        });
        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editProperty(null);
            }
        });
        editPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //noinspection unchecked
                Pair<String, String> property = (Pair<String, String>)propertyList.getSelectedValue();
                if (property == null) return;
                editProperty(property);
            }
        });
        removePropertyButton.addActionListener(new ActionListener() {
            @Override
            @SuppressWarnings({"unchecked"})
            public void actionPerformed(ActionEvent e) {
                // Check if the removing list contains the property name.  If so, don't add the property name.
                Pair<String, String> property = (Pair<String, String>)propertyList.getSelectedValue();
                boolean found = false;
                for (String propertyName: toBeRemovedProperties) {
                    if (propertyName.equals(property.left)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    toBeRemovedProperties.add(property.left);
                }

                int idx = propertyList.getSelectedIndex();
                if (idx >= 0) propertyListModel.remove(idx);
            }
        });

        interfacesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InterfaceTagsDialog.show(SsgConnectorPropertiesDialog.this, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String newInterfaceTags) {
                        Object prev = interfaceComboBox.getSelectedItem();
                        populateInterfaceComboBox(newInterfaceTags);
                        interfaceComboBox.getModel().setSelectedItem(prev);
                    }
                });
            }
        });

        collapseOrExpandButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Icon icon = collapseOrExpandButton.getIcon();
                if (collapseIcon.equals(icon)) {
                    collapseOrExpandButton.setIcon(expandIcon);
                    builtinServicesPanel.setVisible(false);
                } else {
                    collapseOrExpandButton.setIcon(collapseIcon);
                    builtinServicesPanel.setVisible(true);
                }
                DialogDisplayer.pack(SsgConnectorPropertiesDialog.this);
            }
        });

        cbEnableBuiltinServices.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean allBuiltinServicesChecked = cbEnableBuiltinServices.isSelected();

                if (allBuiltinServicesChecked) {
                    builtinServicesPanel.setVisible(true);
                    collapseOrExpandButton.setIcon(collapseIcon);
                    DialogDisplayer.pack(SsgConnectorPropertiesDialog.this);
                }

                policyDiscoveryCheckBox.setSelected(allBuiltinServicesChecked);
                pingServiceCheckBox.setSelected(allBuiltinServicesChecked);
                stsCheckBox.setSelected(allBuiltinServicesChecked);
                if (httpsEnabled()) { // If the protocol is HTTPS
                    csrHandlerCheckBox.setSelected(allBuiltinServicesChecked);
                    passwordChangeCheckBox.setSelected(allBuiltinServicesChecked);
                }
                wsdlProxyCheckBox.setSelected(allBuiltinServicesChecked);
                if (snmpQueryServicePropertyEnabled()) snmpQueryCheckBox.setSelected(allBuiltinServicesChecked);

                saveAllBuiltinServiceCheckboxStates();
                DialogDisplayer.pack(SsgConnectorPropertiesDialog.this);
            }
        });

        final ActionListener cleanCheckboxListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean isHttps = httpsEnabled();

                // If one of individual built-in services is not selected, then de-select the "Built-in services" checkbox.
                if (!policyDiscoveryCheckBox.isSelected() ||
                    !pingServiceCheckBox.isSelected() ||
                    !stsCheckBox.isSelected() ||
                    (isHttps && !csrHandlerCheckBox.isSelected()) ||
                    (isHttps && !passwordChangeCheckBox.isSelected()) ||
                    !wsdlProxyCheckBox.isSelected() ||
                    (snmpQueryServicePropertyEnabled() && !snmpQueryCheckBox.isSelected())) {

                    // Deselect the "Built-in services" checkbox
                    cbEnableBuiltinServices.setSelected(false);
                }

                saveAllBuiltinServiceCheckboxStates();
                DialogDisplayer.pack(SsgConnectorPropertiesDialog.this);
            }
        };
        policyDiscoveryCheckBox.addActionListener(cleanCheckboxListener);
        pingServiceCheckBox.addActionListener(cleanCheckboxListener);
        stsCheckBox.addActionListener(cleanCheckboxListener);
        csrHandlerCheckBox.addActionListener(cleanCheckboxListener);
        passwordChangeCheckBox.addActionListener(cleanCheckboxListener);
        wsdlProxyCheckBox.addActionListener(cleanCheckboxListener);
        snmpQueryCheckBox.addActionListener(cleanCheckboxListener);

        ActionListener enableOrDisableServiceDropdownsActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableServiceResolutionDropdowns();
            }
        };
        hardwiredServiceCheckBox.addActionListener(enableOrDisableServiceDropdownsActionListener);
        overrideContentTypeCheckBox.addActionListener(enableOrDisableServiceDropdownsActionListener);

        DefaultComboBoxModel contentTypeComboBoxModel = new DefaultComboBoxModel();
        ContentTypeHeader[] offeredTypes = new ContentTypeHeader[] {
                ContentTypeHeader.XML_DEFAULT,
                ContentTypeHeader.TEXT_DEFAULT,
                ContentTypeHeader.SOAP_1_2_DEFAULT,
                ContentTypeHeader.APPLICATION_JSON,
                ContentTypeHeader.OCTET_STREAM_DEFAULT,
        };
        for (ContentTypeHeader offeredType : offeredTypes) {
            contentTypeComboBoxModel.addElement(offeredType.getFullValue());
        }
        contentTypeComboBox.setModel(contentTypeComboBoxModel);
        serviceNameComboBox.setRenderer( TextListCellRenderer.<ServiceComboItem>basicComboBoxRenderer() );

        threadPoolSizeSpinner.setModel( new SpinnerNumberModel( DEFAULT_POOL_SIZE, 1, 10000, 1 ) );
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(threadPoolSizeSpinner, "Thread Pool Size"));

        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, null);
        inputValidator.validateWhenDocumentChanges(nameField);
        inputValidator.constrainTextFieldToNumberRange("Port", portField, 1025L, 65535L );
        inputValidator.validateWhenDocumentChanges(portField);
        inputValidator.constrainTextFieldToNumberRange("Port Range Start", portRangeStartField, 0L, 65535L );
        inputValidator.constrainTextFieldToNumberRange("Port Range Count", portRangeCountField, 1L, 65535L );
        inputValidator.constrainTextField(portRangeCountField, new InputValidator.ComponentValidationRule(portRangeCountField) {
            @Override
            public String getValidationError() {
                int start;
                try {
                    start = Integer.parseInt(portRangeStartField.getText());
                } catch (NumberFormatException nfe) {
                    // If start is invalid, count doesn't matter
                    return null;
                }
                try {
                    int count = Integer.parseInt(portRangeCountField.getText());
                    if (start + count > 65535)
                        return "Port Range Start plus Port Range Count cannot exceed 65535";
                    return null;
                } catch (NumberFormatException nfe) {
                    return "Port Range Count must be a number from 1 to 65535";
                }
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(privateKeyComboBox) {
            @Override
            public String getValidationError() {
                if (!privateKeyComboBox.isEnabled())
                    return null;
                if (privateKeyComboBox.getSelectedItem() == null)
                    return "A private key must be selected for an SSL listener.";
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(cipherSuiteList) {
            @Override
            public String getValidationError() {
                if (!cipherSuiteList.isEnabled())
                    return null;
                if (!cipherSuiteListModel.isAnyEntryChecked())
                    return "At least one cipher suite must be enabled for an SSL listener.";
                if (isEccCertButHasRsaCiphersEnabled())
                    return "The server private key uses elliptic curve crypto, but at least one RSA cipher suite is enabled.";
                if (!tls10CheckBox.isSelected() &&
                    !tls11CheckBox.isSelected() &&
                    !tls12CheckBox.isSelected())
                {
                    return "At least one version of TLS must be enabled to use HTTPS or FTPS.";
                }
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(cbEnableMessageInput) {
            @Override
            public String getValidationError() {
                boolean disabled = !enabledCheckBox.isSelected();
                if (disabled ||
                    cbEnableBuiltinServices.isSelected() ||
                    policyDiscoveryCheckBox.isSelected() ||
                    pingServiceCheckBox.isSelected() ||
                    stsCheckBox.isSelected() ||
                    (httpsEnabled() && csrHandlerCheckBox.isSelected()) ||
                    (httpsEnabled() && passwordChangeCheckBox.isSelected()) ||
                    wsdlProxyCheckBox.isSelected() ||
                    (snmpQueryServicePropertyEnabled() && snmpQueryCheckBox.isSelected()) ||
                    cbEnableMessageInput.isSelected() ||
                    cbEnableSsmApplet.isSelected() ||
                    cbEnableSsmRemote.isSelected() ||
                    cbEnableEsmRemote.isSelected() ||
                    cbEnableNode.isSelected() ||
                    cbEnablePCAPI.isSelected())
                {
                    return null;
                }

                return "An enabled listener must have at least one endpoint enabled.";
            }
        });
        for (final Component customPropertiesPanelComponent : otherSettingsPanel.getComponents()) {
            inputValidator.addRule(new InputValidator.ComponentValidationRule(customPropertiesPanelComponent) {
                @Override
                public String getValidationError() {
                    TransportDescriptor selectedProtocol = getSelectedProtocol();
                    if (selectedProtocol != null) {
                        String selectedCustomPanelName = selectedProtocol.getCustomPropertiesPanelClassname();
                        if (selectedCustomPanelName != null) {
                            String customPanelName = customPropertiesPanelComponent.getClass().getName();
                            if (selectedCustomPanelName.equals(customPanelName)) {
                                return ((CustomTransportPropertiesPanel) customPropertiesPanelComponent).getValidationError();
                            }
                        }
                    }
                    return null;
                }
            });
        }
        inputValidator.addRule(new InputValidator.ComponentValidationRule(serviceNameComboBox) {
            @Override
            public String getValidationError() {
                TransportDescriptor td = getSelectedProtocol();
                if (td == null)
                    return null;

                if (!td.isRequiresHardwiredServiceResolutionAlways() && !td.isRequiresHardwiredServiceResolutionForNonXml())
                    return null;

                String msg = "A " + td + " port must be directly associated with a valid published service.";

                if (!td.isRequiresHardwiredServiceResolutionAlways() && overrideContentTypeCheckBox.isSelected()) {
                    ContentTypeHeader ctype = getSelectedOverrideContentType();
                    if (ctype != null && ctype.isXml())
                        return null;

                    msg = "A " + td + " port must be directly associated with a valid published service unless the content type is XML.";
                }

                if (hardwiredServiceCheckBox.isSelected() && serviceNameComboBox.getSelectedItem() != null)
                    return null;

                return msg;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(contentTypeComboBox) {
            @Override
            public String getValidationError() {
                TransportDescriptor td = getSelectedProtocol();
                if (td == null || !td.isRequiresSpecifiedContentType() || getSelectedOverrideContentType() != null)
                    return null;

                Object ctypeObj = contentTypeComboBox.getSelectedItem();
                if (ctypeObj != null && ctypeObj.toString().trim().length() > 0)
                    return "Specified content type is not formatted correctly.";

                return "A " + td + " port must specify a content type to assume for incoming requests.";
            }
        });

        inputValidator.addRule(new InputValidator.ValidationRule(){
            @Override
            public String getValidationError() {
                return requestByteLimitPanel.validateFields();
            }
        });
        zoneControl.setEntityType(EntityType.SSG_CONNECTOR);

        Utilities.enableGrayOnDisabled(contentTypeComboBox);
        Utilities.enableGrayOnDisabled(serviceNameComboBox);
        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
    }

    private CustomTransportPropertiesPanel getCustomPropertiesPanel(TransportDescriptor descriptor, String customPropertiesPanelClassname) {
        if (customPropertiesPanelClassname == null)
            return null;

        final String scheme = descriptor.getScheme();
        CustomTransportPropertiesPanel panel = customGuisByScheme.get(scheme);
        if (panel != null)
            return panel;

        DialogDisplayer.pack(this);

        String owningAssertionClassname = descriptor.getModularAssertionClassname();
        ClassLoader panelLoader = owningAssertionClassname == null ? Thread.currentThread().getContextClassLoader()
                : TopComponents.getInstance().getAssertionRegistry().getClassLoaderForAssertion(owningAssertionClassname);

        try {
            //noinspection unchecked
            panel = (CustomTransportPropertiesPanel) panelLoader.loadClass(customPropertiesPanelClassname).newInstance();
            panel.setSsgConnectorPropertiesDialog(SsgConnectorPropertiesDialog.this);
            customGuisByScheme.put(scheme, panel);
            String[] reservedPropertyNames = panel.getAdvancedPropertyNamesUsedByGui();
            if (reservedPropertyNames == null) {
                reservedPropertyNamesByScheme.put(scheme, Collections.<String>emptySet());
            } else {
                reservedPropertyNamesByScheme.put(scheme, new HashSet<String>(Arrays.asList(reservedPropertyNames)));
            }

            return panel;
        } catch (InstantiationException e) {
            logger.log(Level.WARNING, "Unable to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Unable to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Unable to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    private boolean isEccCertButHasRsaCiphersEnabled() {
        String alg = privateKeyComboBox.getSelectedKeyAlgorithm();
        if (!("EC".equals(alg) || "ECDSA".equals(alg)))
            return false;
        final String cipherList = cipherSuiteListModel.asCipherListStringOrNullIfDefault();
        return cipherList == null || cipherList.indexOf("RSA_") > 0;
    }

    private void editProperty(@Nullable final Pair<String, String> origPair) {
        final SimplePropertyDialog dlg = origPair == null ? new SimplePropertyDialog(this) : new SimplePropertyDialog(this, origPair);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            /** @noinspection unchecked*/
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    List<Pair<String,String>> elms = (List<Pair<String,String>>)Collections.list(propertyListModel.elements());
                    for (Pair<String, String> elm : elms)
                        if (elm.left.equals(property.left)) propertyListModel.removeElement(elm);
                    if (origPair != null) propertyListModel.removeElement(origPair);
                    propertyListModel.addElement(property);
                }
            }
        });
    }

    private static void saveCheckBoxState(JCheckBox... boxes) {
        for (JCheckBox box : boxes)
            box.putClientProperty(CPROP_WASENABLED, box.isSelected());
    }

    private void initializeInterfaceComboBox() {
        populateInterfaceComboBox(null);

        final ActionListener commonActionListener = new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                enableOrDisableEndpoints();
            }
        };

        if(isCluster) {
            interfaceComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    final Object item = interfaceComboBox.getSelectedItem();
                    if(item != null && item.toString().matches("^\\d.*")) {
                        JOptionPane.showMessageDialog(SsgConnectorPropertiesDialog.this,
                                "With a cluster, using an interface identified by a raw IP address can have unexpected effects.",
                                "Possible Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                    commonActionListener.actionPerformed( evt );
                }
            });
        } else {
            interfaceComboBox.addActionListener( commonActionListener );
        }
    }

    private void populateInterfaceComboBox(@Nullable String interfaceTags) {
        List<String> entries = new ArrayList<String>();

        entries.add(INTERFACE_ANY);

        try {
            if (interfaceTags == null) {
                ClusterProperty tagProp = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(InterfaceTag.PROPERTY_NAME);
                if (tagProp != null)
                    interfaceTags = tagProp.getValue();
            }
            if (interfaceTags != null)
                for (InterfaceTag tag : InterfaceTag.parseMultiple(interfaceTags))
                    entries.add(tag.getName());
        } catch (FindException e) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "FindException looking up cluster property " + InterfaceTag.PROPERTY_NAME + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            /* FALLTHROUGH and do without */
        } catch (ParseException e) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Bad value for cluster property " + InterfaceTag.PROPERTY_NAME + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            /* FALLTHROUGH and do without */
        }

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }

        interfaceComboBoxModel = new DefaultComboBoxModel(entries.toArray());
        interfaceComboBox.setModel(interfaceComboBoxModel);
    }

    private TransportDescriptor getSelectedProtocol() {
        return (TransportDescriptor)protocolComboBox.getSelectedItem();
    }

    private static boolean isSslProto(TransportDescriptor protocol) {
        return protocol != null && protocol.isUsesTls();
    }

    private static boolean isFtpProto(TransportDescriptor protocol) {
        return protocol != null && protocol.isFtpBased();
    }

    private static boolean isHttpProto(TransportDescriptor protocol) {
        return protocol != null && protocol.isHttpBased();
    }

    private boolean isThreadPoolProto(TransportDescriptor proto) {
        return proto != null && proto.isSupportsPrivateThreadPool();
    }

    private boolean isLocalhostListener( final String address ) {
        return INTERFACE_ANY.equals( address ) ||
                InetAddressUtil.isLoopbackAddress( address ) ||
                InetAddressUtil.isAnyHostAddress( address );
    }

    private void enableOrDisableComponents() {
        enableOrDisableTabs();
        enableOrDisableEndpoints();
        enableOrDisableTlsVersions();
        CipherSuiteGuiUtil.enableOrDisableCipherSuiteButtons(cipherSuiteList, cipherSuiteListModel, moveUpButton, moveDownButton);
        enableOrDisablePropertyButtons();
        enableOrDisableServiceResolutionCheckboxes();
        enableOrDisableServiceResolutionDropdowns();
    }

    private void enableOrDisableServiceResolutionDropdowns() {
        serviceNameComboBox.setEnabled(hardwiredServiceCheckBox.isSelected());
        contentTypeComboBox.setEnabled(overrideContentTypeCheckBox.isSelected());
    }

    private void enableOrDisablePropertyButtons() {
        boolean haveSel = propertyList.getSelectedIndex() >= 0;
        editPropertyButton.setEnabled(haveSel);
        removePropertyButton.setEnabled(haveSel);
    }

    private void enableOrDisableTabs() {
        TransportDescriptor proto = getSelectedProtocol();
        boolean isSsl = isSslProto(proto);
        boolean isFtp = isFtpProto(proto);
        boolean isHttp = isHttpProto(proto);

        boolean isPool = isThreadPoolProto(proto);

        tabbedPane.setEnabledAt(TAB_SSL, isSsl);
        tabbedPane.setEnabledAt(TAB_HTTP, isHttp || isPool);

        usePrivateThreadPoolCheckBox.setEnabled(isHttp || isPool);
        threadPoolSizeSpinner.setEnabled(usePrivateThreadPoolCheckBox.isEnabled() && usePrivateThreadPoolCheckBox.isSelected());
        threadPoolSizeLabel.setEnabled( threadPoolSizeSpinner.isEnabled() );

        tabbedPane.setEnabledAt(TAB_FTP, isFtp);
        portRangeStartField.setEnabled(isFtp);  // disable controls InputValidator will ignore them when not relevant
        portRangeCountField.setEnabled(isFtp);

        if (!isSsl) cipherSuiteList.clearSelection();
        cipherSuiteList.setEnabled(isSsl);
        moveUpButton.setEnabled(isSsl);
        moveDownButton.setEnabled(isSsl);
        uncheckAllButton.setEnabled(isSsl);
        defaultCipherListButton.setEnabled(isSsl);
        clientAuthComboBox.setEnabled(isSsl);
        privateKeyComboBox.setEnabled(isSsl);
        managePrivateKeysButton.setEnabled(isSsl);

        // Show custom controls, if any
        if (proto != null && proto.getCustomPropertiesPanelClassname() != null) {
            ((CardLayout)otherSettingsPanel.getLayout()).show(otherSettingsPanel, proto.getScheme());
            tabbedPane.setEnabledAt(TAB_CUSTOM, true);
        } else {
            tabbedPane.setEnabledAt(TAB_CUSTOM, false);
        }
    }

    private void enableOrDisableServiceResolutionCheckboxes() {
        TransportDescriptor proto = getSelectedProtocol();

        if (!cbEnableMessageInput.isSelected()) {
            setEnableAndSelect(false, false, "Disabled because published service message input is not enabled", overrideContentTypeCheckBox, hardwiredServiceCheckBox);
            return;
        }

        boolean oct = proto != null && proto.isSupportsSpecifiedContentType();
        if (oct) {
            if (proto.isRequiresSpecifiedContentType()) {
                setEnableAndSelect(false, true, "Checked because the current protocol requires specifying a content type", overrideContentTypeCheckBox);
            } else {
                enableAndRestore(overrideContentTypeCheckBox);
            }
        } else {
            setEnableAndSelect(false, false, "Disabled because the current protocol does not support content type overrides", overrideContentTypeCheckBox);
        }
        contentTypeComboBox.setEnabled(oct);

        boolean hws = proto != null && proto.isSupportsHardwiredServiceResolution();
        if (hws) {
            if (proto.isRequiresHardwiredServiceResolutionAlways()) {
                setEnableAndSelect(false, true, "Checked because the current protocol always requires hardwired service resolution", hardwiredServiceCheckBox);
            } else {
                enableAndRestore(hardwiredServiceCheckBox);
            }
        } else {
            setEnableAndSelect(false, false, "Disabled because the current protocol does not support hardwired service resolution", hardwiredServiceCheckBox);
        }
        serviceNameComboBox.setEnabled(hws);
    }

    private void disableOrRestoreEndpointCheckBox(Set<Endpoint> endpoints, SsgConnector.Endpoint endpoint, JCheckBox checkBox) {
        if (endpoints.contains(endpoint)) {
            enableAndRestore(checkBox);
        } else {
            setEnableAndSelect(false, false, "Disabled because it is not supported with the selected transport protocol", checkBox);
        }
    }

    private void enableOrDisableEndpoints() {
        TransportDescriptor proto = getSelectedProtocol();
        boolean ssl = isSslProto(proto);
        boolean sslWithCert = ssl && clientAuthComboBox.getSelectedItem() != CA_NONE;
        boolean local = isLocalhostListener( (String) interfaceComboBox.getSelectedItem() );

        Set<Endpoint> endpoints = proto == null ? Collections.<Endpoint>emptySet() : proto.getSupportedEndpoints();
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.MESSAGE_INPUT, cbEnableMessageInput);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.PC_NODE_API, cbEnablePCAPI);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.ADMIN_REMOTE_ESM, cbEnableEsmRemote);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.ADMIN_REMOTE_SSM, cbEnableSsmRemote);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.ADMIN_APPLET, cbEnableSsmApplet);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.NODE_COMMUNICATION, cbEnableNode);

        // If this transport supports nothing but message input, hardwire that checkbox
        if (endpoints.size() == 1 && endpoints.contains(SsgConnector.Endpoint.MESSAGE_INPUT)) {
            setEnableAndSelect(false, true, "Enabled because it is required for the selected transport protocol", cbEnableMessageInput);
        }

        if (!ssl) {
            setEnableAndSelect(false, false, "Disabled because it requires a TLS-based transport", cbEnableSsmApplet, cbEnableSsmRemote, cbEnableNode, cbEnablePCAPI);
        }

        if (!sslWithCert) {
            setEnableAndSelect(false, false, "Disabled because it requires a TLS-based transport supporting client certificate authentication", cbEnableEsmRemote);
        }

        if (!local) {
            setEnableAndSelect(false, false, "Disabled because it requires listening on the localhost address", cbEnablePCAPI);
        }

        if (!cbEnableSsmRemote.isSelected()) {
            setEnableAndSelect(false, false, "Disabled because it requires enabling Policy Manager access", cbEnableSsmApplet);
        }
    }

    private void enableOrDisableTlsVersions() {
        // TODO remove this hack once a way has been found to resolve the compatibility problem between RSA SSL-J and Apache FtpServer MINA's use of the SSL socket (Bug #8493)
        if (isFtpProto(getSelectedProtocol()) && !ENABLE_FTPS_TLS12) {
            setEnableAndSelect(false, false, "Disabled because it is not currently supported with FTPS", tls11CheckBox, tls12CheckBox);
            setEnableAndSelect(false, true, "Enabled because it is currently the only supported TLS version for FTPS", tls10CheckBox);
        } else {
            enableAndRestore(tls10CheckBox, tls11CheckBox, tls12CheckBox);
        }
    }

    private static void enableAndRestore(JCheckBox... boxes) {
        for (JCheckBox box : boxes) {
            box.setEnabled(true);
            box.setToolTipText(null);
            final Boolean we = (Boolean)box.getClientProperty(CPROP_WASENABLED);
            if (we != null) box.setSelected(we);
        }
    }

    private static void setEnableAndSelect(boolean enabled, boolean selected, String toolTipText, JCheckBox... boxes) {
        for (JCheckBox box : boxes) {
            box.setSelected(selected);
            box.setEnabled(enabled);
            box.setToolTipText(toolTipText);
        }
    }

    private ContentTypeHeader getSelectedOverrideContentType() {
        if (!overrideContentTypeCheckBox.isSelected())
            return null;

        Object ctypeObj = contentTypeComboBox.getSelectedItem();
        if (ctypeObj == null)
            return null;

        String ctypeStr = String.valueOf(ctypeObj);
        if (ctypeStr.trim().length() < 1)
            return null;

        try {
            return ContentTypeHeader.parseValue(ctypeStr);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Invalid content type value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    private void selectPrivateKey(Long oid, String alias) {
        if (oid != null && alias != null) {
            privateKeyComboBox.select(oid, alias);
        } else {
            if (privateKeyComboBox.getModel().getSize() > 0)
                privateKeyComboBox.setSelectedIndex(0);
        }
    }

    /**
     * Examine the state of the endpoint checkboxes and generate an endpoint name string.
     *
     * @return a String in the form "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET".  Never null.
     */
    private String getEndpointList() {
        List<String> endpoints = new ArrayList<String>();

        if (cbEnableMessageInput.isSelected()) endpoints.add(Endpoint.MESSAGE_INPUT.name());
        if (cbEnableEsmRemote.isSelected()) endpoints.add(Endpoint.ADMIN_REMOTE_ESM.name());
        if (cbEnableSsmRemote.isSelected()) endpoints.add(Endpoint.ADMIN_REMOTE_SSM.name());
        if (cbEnableSsmApplet.isSelected()) endpoints.add(Endpoint.ADMIN_APPLET.name());
        if (cbEnableNode.isSelected()) endpoints.add(Endpoint.NODE_COMMUNICATION.name());
        if (cbEnablePCAPI.isSelected()) endpoints.add(Endpoint.PC_NODE_API.name());

        // The only case to save all built-in services as OTHER_SERVLETS is when th protocol is HTTPS and all six individual built-in services are selected.
        // Otherwise, go thru each checkbox and set the endpoint if the checkbox is selected.
        if (httpsEnabled() && snmpQueryServicePropertyEnabled() &&
            policyDiscoveryCheckBox.isSelected() &&
            pingServiceCheckBox.isSelected() &&
            stsCheckBox.isSelected() &&
            csrHandlerCheckBox.isSelected() &&
            passwordChangeCheckBox.isSelected() &&
            wsdlProxyCheckBox.isSelected() &&
            snmpQueryCheckBox.isSelected()) {

            endpoints.add(SsgConnector.Endpoint.OTHER_SERVLETS.name());
        } else {
            if (policyDiscoveryCheckBox.isSelected()) endpoints.add(Endpoint.POLICYDISCO.name());
            if (pingServiceCheckBox.isSelected()) endpoints.add(Endpoint.PING.name());
            if (stsCheckBox.isSelected()) endpoints.add(Endpoint.STS.name());
            if (httpsEnabled() && csrHandlerCheckBox.isSelected()) endpoints.add(Endpoint.CSRHANDLER.name());
            if (httpsEnabled() && passwordChangeCheckBox.isSelected()) endpoints.add(Endpoint.PASSWD.name());
            if (wsdlProxyCheckBox.isSelected()) endpoints.add(Endpoint.WSDLPROXY.name());
            if (snmpQueryServicePropertyEnabled() && snmpQueryCheckBox.isSelected()) endpoints.add(Endpoint.SNMPQUERY.name());
        }

        return TextUtils.join(",", endpoints.toArray(new String[endpoints.size()])).toString();
    }

    /**
     * Set the endpoint checkbox state to correspond to the specified endpoint name string, if possible.
     * If not possible, we will come as close as we can get.
     *
     * @param endpoints a String in the form "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET".  If null, no endpoints
     *                  will be enabled.
     */
    private void setEndpointList(String endpoints) {
        String[] names = endpoints == null ? ArrayUtils.EMPTY_STRING_ARRAY : CipherSuiteListModel.WS_COMMA_WS.split(endpoints);

        boolean messages = false;
        boolean esmRemote = false;
        boolean ssmRemote = false;
        boolean ssmApplet = false;
        boolean node = false;
        boolean pcapi = false;
        boolean builtin = false;
        boolean policyDisco = false;
        boolean ping = false;
        boolean sts = false;
        boolean csrHandler = false;
        boolean passwordChange = false;
        boolean wsdlProxy = false;
        boolean snmpQuery = false;

        // Currently the GUI has fewer checkboxes than there are endpoint types, with multiple endpoints bundled inside
        // a single "built-in services" checkbox, so we'll try to behave sensibly if the endpoint
        // list has been customized in more detail than our GUI allows.  On load, we'll check the checkbox
        // if any builtin service is enabled.
        for (String name : names) {
            try {
                Endpoint endpoint = Endpoint.valueOf(name);
                //noinspection EnumSwitchStatementWhichMissesCases
                switch (endpoint) {
                    case MESSAGE_INPUT:     messages  = true;   break;
                    case ADMIN_REMOTE:      ssmRemote = true;
                                            esmRemote = true;   break;
                    case ADMIN_REMOTE_ESM:  esmRemote = true;   break;
                    case ADMIN_REMOTE_SSM:  ssmRemote = true;   break;
                    case ADMIN_APPLET:      ssmApplet = true;   break;
                    case NODE_COMMUNICATION: node     = true;   break;
                    case PC_NODE_API:       pcapi     = true;   break;
                    case OTHER_SERVLETS: builtin   = true;   break;
                    case POLICYDISCO: policyDisco = true; break;
                    case PING: ping = true; break;
                    case STS: sts = true; break;
                    case CSRHANDLER: csrHandler = true; break;
                    case PASSWD: passwordChange = true; break;
                    case WSDLPROXY: wsdlProxy = true; break;
                    case SNMPQUERY: snmpQuery = true; break;
                }
            } catch (IllegalArgumentException iae) {
                logger.fine("Ignoring unrecognized endpoint name: " + name);
            }
        }

        cbEnableMessageInput.setSelected(messages);
        cbEnableEsmRemote.setSelected(esmRemote);
        cbEnableSsmRemote.setSelected(ssmRemote);
        cbEnableSsmApplet.setSelected(ssmApplet);
        cbEnableBuiltinServices.setSelected(builtin);
        cbEnableNode.setSelected(node);
        cbEnablePCAPI.setSelected(pcapi);

        // For all individual built-in services
        policyDiscoveryCheckBox.setSelected(policyDisco);
        pingServiceCheckBox.setSelected(ping);
        stsCheckBox.setSelected(sts);
        csrHandlerCheckBox.setSelected(csrHandler);
        passwordChangeCheckBox.setSelected(passwordChange);
        wsdlProxyCheckBox.setSelected(wsdlProxy);
        snmpQueryCheckBox.setSelected(snmpQuery);

        setupBuiltinServiceCheckboxes();

        // For listen ports last saved pre-Pandora, have GUI reflect what the actual system behavior will be (Bug #8802)
        if (cbEnableSsmApplet.isSelected())
            cbEnableSsmRemote.setSelected(true);
    }

    /**
     * Set up built-in service checkboxes such as visible, enabled, disabled, or selected.
     */
    private void setupBuiltinServiceCheckboxes() {
        final boolean httpEnabled = httpEnabled();   // HTTP or HTTPS
        final boolean httpsEnabled = httpsEnabled(); // HTTPS
        final boolean snmpPropEnabled = snmpQueryServicePropertyEnabled();

        // If the protocol is not HTTPS, then disable both CSR Handler service and Password changing service.
        csrHandlerCheckBox.setEnabled(httpsEnabled);
        passwordChangeCheckBox.setEnabled(httpsEnabled);
        if (! httpsEnabled) {
            csrHandlerCheckBox.setSelected(false);
            passwordChangeCheckBox.setSelected(false);
        }

        // If the SNMP Query Service cluster property is set as disabled, then make the checkbox of SNMP query service be invisible.
        // If the protocol is HTTP or HTTPS and the cluster property is set as true., set the SNMP query service checkbox as visible and enabled.
        snmpQueryCheckBox.setVisible(httpEnabled &&snmpPropEnabled);
        snmpQueryCheckBox.setEnabled(httpEnabled && snmpPropEnabled);
        if (!httpEnabled || !snmpPropEnabled) snmpQueryCheckBox.setSelected(false);

        // If one of individual built-in services is enabled, then the "Built-in services" checkbox should be enabled..
        if (policyDiscoveryCheckBox.isEnabled() ||
            pingServiceCheckBox.isEnabled() ||
            stsCheckBox.isEnabled() ||
            (httpsEnabled && csrHandlerCheckBox.isEnabled()) ||
            (httpsEnabled && passwordChangeCheckBox.isEnabled()) ||
            wsdlProxyCheckBox.isEnabled() ||
            (snmpPropEnabled && snmpQueryCheckBox.isEnabled())) {

            cbEnableBuiltinServices.setEnabled(true);
            collapseOrExpandButton.setEnabled(true);
        } else {
            // If all built-in service checkboxes (including the parent one) are disabled, then disable the collapse/expand button.
            collapseOrExpandButton.setEnabled(false);
        }

        // If all individual built-in services are not selected, then collapse the built-in service list
        if (!policyDiscoveryCheckBox.isSelected() &&
            !pingServiceCheckBox.isSelected() &&
            !stsCheckBox.isSelected() &&
            (!httpsEnabled || !csrHandlerCheckBox.isSelected()) &&
            (!httpsEnabled || !passwordChangeCheckBox.isSelected()) &&
            !wsdlProxyCheckBox.isSelected() &&
            (!snmpPropEnabled || !snmpQueryCheckBox.isSelected())) {

            builtinServicesPanel.setVisible(false);
            collapseOrExpandButton.setIcon(expandIcon);
        } else {
            builtinServicesPanel.setVisible(true);
            collapseOrExpandButton.setIcon(collapseIcon);
        }

        // If the "Built-in services" checkbox is checked, then all individual built-in service checkboxes should be checked.
        if (cbEnableBuiltinServices.isEnabled() && cbEnableBuiltinServices.isSelected()) {
            policyDiscoveryCheckBox.setSelected(true);
            pingServiceCheckBox.setSelected(true);
            stsCheckBox.setSelected(true);
            if (httpsEnabled) {
                csrHandlerCheckBox.setEnabled(true);
                csrHandlerCheckBox.setSelected(true);

                passwordChangeCheckBox.setEnabled(true);
                passwordChangeCheckBox.setSelected(true);
            }
            wsdlProxyCheckBox.setSelected(true);
            if (snmpPropEnabled) {
                snmpQueryCheckBox.setVisible(true);
                snmpQueryCheckBox.setEnabled(true);
                snmpQueryCheckBox.setSelected(true);
            }

            collapseOrExpandButton.setEnabled(true);
        }

        saveAllBuiltinServiceCheckboxStates();
        DialogDisplayer.pack(this);
    }

    private void saveAllBuiltinServiceCheckboxStates() {
        for (JCheckBox checkBox: getBuiltinServicesMap().values()) {
            saveCheckBoxState(checkBox);
        }
    }

    /**
     * Enable or disable the checkboxs of built-iin services.
     * Note:  do not merge this method into enableOrDisableComponents(), since this method will be toggled only when the protocol is changed.
     */
    private void enableOrDisableBuiltinServiceEndpoints() {
        final TransportDescriptor protocol = getSelectedProtocol();
        Set<Endpoint> endpoints = protocol == null ? Collections.<Endpoint>emptySet() : protocol.getSupportedEndpoints();

        for (Endpoint endpoint: getBuiltinServicesMap().keySet()) {
            final JCheckBox checkBox = getBuiltinServicesMap().get(endpoint);
            if (endpoints.contains(endpoint)) {
                checkBox.setEnabled(true);

                final Boolean wasSelected = (Boolean)checkBox.getClientProperty(CPROP_WASENABLED);
                if (wasSelected != null) checkBox.setSelected(wasSelected);
            } else {
                checkBox.setEnabled(false);
                checkBox.setSelected(false);
            }
        }

        setupBuiltinServiceCheckboxes();
    }

    private Map<Endpoint, JCheckBox> getBuiltinServicesMap() {
        if (builtinServicesMap == null) {
            builtinServicesMap = new HashMap<Endpoint, JCheckBox>();
            builtinServicesMap.put(Endpoint.OTHER_SERVLETS, cbEnableBuiltinServices);
            builtinServicesMap.put(Endpoint.POLICYDISCO, policyDiscoveryCheckBox);
            builtinServicesMap.put(Endpoint.PING, pingServiceCheckBox);
            builtinServicesMap.put(Endpoint.STS, stsCheckBox);
            builtinServicesMap.put(Endpoint.CSRHANDLER, csrHandlerCheckBox);
            builtinServicesMap.put(Endpoint.PASSWD, passwordChangeCheckBox);
            builtinServicesMap.put(Endpoint.WSDLPROXY, wsdlProxyCheckBox);
            builtinServicesMap.put(Endpoint.SNMPQUERY, snmpQueryCheckBox);
        }
        return builtinServicesMap;
    }

    private boolean httpEnabled() {
        final TransportDescriptor protocol = getSelectedProtocol();
        return isHttpProto(protocol);
    }

    private boolean httpsEnabled() {
        final TransportDescriptor protocol = getSelectedProtocol();
        return isHttpProto(protocol) && isSslProto(protocol);
    }

    private boolean snmpQueryServicePropertyEnabled() {
        return snmpQueryEnabled;
    }

    /**
     * Configure the GUI control states with information gathered from the connector instance.
     */
    private void modelToView() {
        protocolComboBox.setSelectedItem(transportsByScheme.get(connector.getScheme().trim()));
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        enabledCheckBox.setSelected(connector.isEnabled());

        selectCurrentBindAddress();

        setEndpointList(connector.getEndpoints());

        String hardwiredServiceIdStr = connector.getProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID);
        long hardwiredServiceId = hardwiredServiceIdStr != null && hardwiredServiceIdStr.trim().length() > 0 ? Long.parseLong(hardwiredServiceIdStr) : -1L;
        boolean usingHardwired = serviceNameComboBox.populateAndSelect(hardwiredServiceId != -1L, hardwiredServiceId);
        hardwiredServiceCheckBox.setSelected(usingHardwired);

        String ctype = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        if (ctype == null) {
            overrideContentTypeCheckBox.setSelected(false);
            contentTypeComboBox.setSelectedIndex(-1);
        } else {
            overrideContentTypeCheckBox.setSelected(true);
            contentTypeComboBox.setSelectedItem(ctype);
            if (!ctype.equalsIgnoreCase((String)contentTypeComboBox.getSelectedItem())) {
                ((DefaultComboBoxModel)contentTypeComboBox.getModel()).addElement(ctype);
                contentTypeComboBox.setSelectedItem(ctype);
            }
        }

        usePrivateThreadPoolCheckBox.setSelected( false );
        threadPoolSizeSpinner.setValue( DEFAULT_POOL_SIZE );
        String size = connector.getProperty(SsgConnector.PROP_THREAD_POOL_SIZE);
        if ( size != null ) {
            try {
                Integer poolSize = Integer.parseInt( size );
                usePrivateThreadPoolCheckBox.setSelected( true );
                threadPoolSizeSpinner.setValue( poolSize );
            } catch ( NumberFormatException nfe ) {
                logger.warning("Ignoring invalid pool size value '"+size+"'.");
            }
        }

        // FTP-specific properties
        String prs = connector.getProperty(SsgConnector.PROP_PORT_RANGE_START);
        portRangeStartField.setText(prs == null ? "" : prs);
        String prc = connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT);
        portRangeCountField.setText(prc == null ? "" : prc);

        // SSL-specific properties
        cipherSuiteListModel.setCipherListString(connector.getProperty(SsgConnector.PROP_TLS_CIPHERLIST));
        clientAuthComboBox.setSelectedItem(ClientAuthType.bycode.get(connector.getClientAuth()));
        selectPrivateKey(connector.getKeystoreOid(), connector.getKeyAlias());

        List<String> propNames = new ArrayList<String>(connector.getPropertyNames());

        // Don't show properties that are already exposed via specialized controls
        propNames.remove(SsgConnector.PROP_BIND_ADDRESS);
        propNames.remove(SsgConnector.PROP_TLS_CIPHERLIST);
        propNames.remove(SsgConnector.PROP_TLS_PROTOCOLS);
        propNames.remove(SsgConnector.PROP_PORT_RANGE_COUNT);
        propNames.remove(SsgConnector.PROP_PORT_RANGE_START);
        propNames.remove(SsgConnector.PROP_THREAD_POOL_SIZE);
        propNames.remove(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        propNames.remove(SsgConnector.PROP_HARDWIRED_SERVICE_ID);
        propNames.remove(SsgConnector.PROP_REQUEST_SIZE_LIMIT);

        // Also hide properties reserved for use by custom GUI panels
        // TODO should only really hide them when the corresponding transport protocol is selected
        for (Set<String> reserved : reservedPropertyNamesByScheme.values()) {
            propNames.removeAll(reserved);
        }

        propertyListModel.removeAllElements();
        for (String propName : propNames) {
            final String value = connector.getProperty(propName);
            if (value != null) propertyListModel.addElement(new Pair<String,String>(propName, value));
        }

        String protocols = connector.getProperty(SsgConnector.PROP_TLS_PROTOCOLS);
        if (protocols == null) {
            tls10CheckBox.setSelected(true);
            tls11CheckBox.setSelected(false);
            tls12CheckBox.setSelected(false);
        } else {
            Set<String> protos = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            protos.addAll(Arrays.asList(protocols.split("\\s*,\\s*")));
            tls10CheckBox.setSelected(protos.contains("TLSv1"));
            tls11CheckBox.setSelected(protos.contains("TLSv1.1"));
            tls12CheckBox.setSelected(protos.contains("TLSv1.2"));
        }

        // Update any custom GUI panels
        for (Map.Entry<String, CustomTransportPropertiesPanel> entry : customGuisByScheme.entrySet()) {
            CustomTransportPropertiesPanel customPanel = entry.getValue();
            Set<String> varsUsedByPanel = reservedPropertyNamesByScheme.get(entry.getKey());
            Map<String, String> varHolder = new HashMap<String, String>();
            for (String var : varsUsedByPanel) {
                varHolder.put(var, connector.getProperty(var));
            }
            customPanel.setData(varHolder);
        }

        String requestLimit = connector.getProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT);
        requestByteLimitPanel.setValue(requestLimit, Registry.getDefault().getTransportAdmin().getXmlMaxBytes());
        zoneControl.setSelectedZone(connector.getSecurityZone());

        saveCheckBoxState(savedStateCheckBoxes);
        enableOrDisableComponents();
    }

    private void selectCurrentBindAddress() {
        String bindAddr = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        interfaceComboBox.setSelectedItem(bindAddr != null ? bindAddr : INTERFACE_ANY);
        if (bindAddr != null && interfaceComboBox.getSelectedItem() == null) {
            // It wasn't there -- add it so we don't lose user data
            interfaceComboBoxModel.addElement(bindAddr);
            interfaceComboBox.setSelectedItem(bindAddr);
        }
    }

    /**
     * Configure the connector instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {
        TransportDescriptor proto = (TransportDescriptor)protocolComboBox.getSelectedItem();
        if (proto != null) connector.setScheme(proto.getScheme());
        connector.setName(nameField.getText());
        connector.setPort(Integer.parseInt(portField.getText()));
        connector.setEnabled(enabledCheckBox.isSelected());
        String bindAddress = (String)interfaceComboBox.getSelectedItem();
        connector.putProperty(SsgConnector.PROP_BIND_ADDRESS, INTERFACE_ANY.equals(bindAddress) ? null : bindAddress);
        connector.setEndpoints(getEndpointList());

        // HTTP-specific properties
        if ( usePrivateThreadPoolCheckBox.isEnabled() && usePrivateThreadPoolCheckBox.isSelected()  ) {
            connector.putProperty(SsgConnector.PROP_THREAD_POOL_SIZE, threadPoolSizeSpinner.getValue().toString() );
        } else {
            connector.removeProperty(SsgConnector.PROP_THREAD_POOL_SIZE);
        }

        // FTP-specific properties
        boolean isFtp = isFtpProto(proto);
        String rangeStart = portRangeStartField.getText().trim();
        String rangeCount = portRangeCountField.getText().trim();
        connector.putProperty(SsgConnector.PROP_PORT_RANGE_START, isFtp ? rangeStart : null);
        connector.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, isFtp ? rangeCount : null);

        // SSL-specific properties
        boolean isSsl = isSslProto(proto);
        connector.setSecure(isSsl);
        connector.setKeystoreOid(null);
        connector.setKeyAlias(null);
        if (isSsl) {
            final String alias = privateKeyComboBox.getSelectedKeyAlias();
            if (!privateKeyComboBox.isSelectedDefaultSsl()) {
                connector.setKeystoreOid(privateKeyComboBox.getSelectedKeystoreId());
                connector.setKeyAlias(alias);
            }
        }
        connector.setClientAuth(((ClientAuthType)clientAuthComboBox.getSelectedItem()).code);
        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, cipherSuiteListModel.asCipherListStringOrNullIfDefault());

        Set<String> protos = new LinkedHashSet<String>();
        if (tls10CheckBox.isSelected()) protos.add("TLSv1");
        if (tls11CheckBox.isSelected()) protos.add("TLSv1.1");
        if (tls12CheckBox.isSelected()) protos.add("TLSv1.2");
        String protoString = TextUtils.join(",", protos).toString();
        if ("TLSv1".equals(protoString)) {
            // We will avoid specifying a protocol string if it would match the default one anyway
            protoString = null;
        }
        connector.putProperty(SsgConnector.PROP_TLS_PROTOCOLS, protoString);

        connector.removeProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID);
        if (hardwiredServiceCheckBox.isSelected()) {
            PublishedService ps = serviceNameComboBox.getSelectedPublishedService();
            if (ps != null)
                connector.putProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, String.valueOf(ps.getOid()));
        }

        connector.removeProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        if (overrideContentTypeCheckBox.isSelected()) {
            Object value = contentTypeComboBox.getSelectedItem();
            if (value != null)
                connector.putProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE, value.toString());
        }

        // Delete those removed properties
        // Note: make sure the step (removeProperty) is prior to the next step (putProperty), since there
        // is a case - some property is removed first and then added back again.
        for (String propertyName: toBeRemovedProperties)
            connector.removeProperty(propertyName);

        // Save custom GUI panel properties, but only if the active protocol uses them
        CustomTransportPropertiesPanel customPanel = customGuisByScheme.get(connector.getScheme());
        if (customPanel != null) {
            Map<String, String> updated = customPanel.getData();
            for (Map.Entry<String, String> entry : updated.entrySet()) {
                String prop = entry.getKey();
                String value = entry.getValue();
                if (value == null) {
                    connector.removeProperty(prop);
                } else {
                    connector.putProperty(prop, value);
                }
            }
        }

        // Save user-overridden properties last
        //noinspection unchecked
        List<Pair<String,String>> props = (List<Pair<String,String>>)Collections.list(propertyListModel.elements());
        for (Pair<String, String> prop : props)
            connector.putProperty(prop.left, prop.right);

        if(requestByteLimitPanel.isSelected()){
            connector.putProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT,requestByteLimitPanel.getValue());
        }else {
            connector.removeProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT);
        }
        connector.setSecurityZone(zoneControl.getSelectedZone());
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    public void selectName(){
        nameField.requestFocus();
        nameField.selectAll();
    }

    private void onOk() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    /** @return true if the dialog has been dismissed with the ok button */
    public boolean isConfirmed() {
        return confirmed;
    }

    private static class ModularConnectorInfoComboBoxModel extends SortedListModel<TransportDescriptor> implements ComboBoxModel {
        TransportDescriptor selectedObject = null;

        public ModularConnectorInfoComboBoxModel(Collection<TransportDescriptor> items) {
            super(new ModularConnectorInfoComparator());
            addAll(items);
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if (anItem != null && !(anItem instanceof TransportDescriptor))
                throw new ClassCastException("expected " + TransportDescriptor.class + ", got" + anItem.getClass());
            TransportDescriptor ci = (TransportDescriptor) anItem;
            int size = getSize();
            if (ci != null) for (int i = 0; i < size; ++i) {
                TransportDescriptor desc = getElementAt(i);
                if (desc.getScheme().equalsIgnoreCase(ci.getScheme())) {
                    selectedObject = desc;
                    return;
                }
            }
            selectedObject = null;
        }

        @Override
        public Object getSelectedItem() {
            return selectedObject;
        }
    }

    /**
     * A comparator for TransportDescriptor that sorts by URL scheme, but with HTTP, HTTPS, FTP, and FTPS
     * sorted to the front of the list in that order.
     */
    private static class ModularConnectorInfoComparator implements Comparator<TransportDescriptor>, Serializable {
        @Override
        public int compare(TransportDescriptor a, TransportDescriptor b) {
            return toSortKey(a).compareTo(toSortKey(b));
        }

        private String toSortKey(TransportDescriptor connectorInfo) {
            String scheme = connectorInfo.getScheme();
            if (scheme == null) {
                return "99";
            } else if (scheme.equalsIgnoreCase(SCHEME_HTTP)) {
                return "01" + scheme.toUpperCase();
            } else if (scheme.equalsIgnoreCase(SCHEME_HTTPS)) {
                return "02" + scheme.toUpperCase();
            } else if (scheme.equalsIgnoreCase(SCHEME_FTP)) {
                return "03" + scheme.toUpperCase();
            } else if (scheme.equalsIgnoreCase(SCHEME_FTPS)) {
                return "04" + scheme.toUpperCase();
            } else {
                return "50" + scheme.toUpperCase();
            }
        }
    }
}