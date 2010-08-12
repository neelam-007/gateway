package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SortedListModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.TransportDescriptor;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
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
    private static final boolean INCLUDE_ALL_CIPHERS = SyspropUtil.getBoolean("com.l7tech.console.connector.includeAllCiphers");
    private static final boolean ENABLE_FTPS_TLS12 = SyspropUtil.getBoolean("com.l7tech.console.connector.allowFtpsTls12");
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

    private static final String WS_COMMA_WS = "\\s*,\\s*";

    private static final String CPROP_WASENABLED = SsgConnectorPropertiesDialog.class.getName() + ".wasEnabled";

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
    private JButton defaultCipherListButton;
    private JTextField portRangeStartField;
    private JTextField portRangeCountField;
    private javax.swing.JCheckBox enabledCheckBox;
    private JTabbedPane tabbedPane;
    private JCheckBox cbEnableMessageInput;
    private JCheckBox cbEnableBuiltinServices;
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

    private SsgConnector connector;
    private boolean confirmed = false;
    private CipherSuiteListModel cipherSuiteListModel;
    private DefaultComboBoxModel interfaceComboBoxModel;
    private DefaultListModel propertyListModel = new DefaultListModel();
    private List<String> toBeRemovedProperties = new ArrayList<String>();
    private boolean isCluster = false;
    private JCheckBox[] savedStateCheckBoxes = {
            cbEnableBuiltinServices,
            cbEnableMessageInput,
            cbEnableSsmApplet,
            cbEnableSsmRemote,
            cbEnableNode,
            cbEnablePCAPI,
            tls10CheckBox,
            tls11CheckBox,
            tls12CheckBox,
            overrideContentTypeCheckBox,
            hardwiredServiceCheckBox,
    };

    private Map<String, TransportDescriptor> transportsByScheme = new TreeMap<String, TransportDescriptor>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, CustomTransportPropertiesPanel> customGuisByScheme = new TreeMap<String, CustomTransportPropertiesPanel>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, Set<String>> reservedPropertyNamesByScheme = new TreeMap<String, Set<String>>(String.CASE_INSENSITIVE_ORDER);

    public SsgConnectorPropertiesDialog(Window owner, SsgConnector connector, boolean isCluster) {
        super(owner, DIALOG_TITLE, SsgConnectorPropertiesDialog.DEFAULT_MODALITY_TYPE);
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
                final PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(TopComponents.getInstance().getTopParent());
                pkmw.pack();
                Utilities.centerOnScreen(pkmw);
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

        initializeCipherSuiteControls();

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

        threadPoolSizeSpinner.setModel( new SpinnerNumberModel( DEFAULT_POOL_SIZE, 1, 10000, 1 ) );
        inputValidator.addRule(new InputValidator.NumberSpinnerValidationRule(threadPoolSizeSpinner, "Thread Pool Size"));

        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, null);
        inputValidator.validateWhenDocumentChanges(nameField);
        inputValidator.constrainTextFieldToNumberRange("Port", portField, 1025, 65535);
        inputValidator.validateWhenDocumentChanges(portField);
        inputValidator.constrainTextFieldToNumberRange("Port Range Start", portRangeStartField, 0, 65535);
        inputValidator.constrainTextFieldToNumberRange("Port Range Count", portRangeCountField, 1, 65535);
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
                    cbEnableMessageInput.isSelected() ||
                    cbEnableSsmApplet.isSelected() ||
                    cbEnableSsmRemote.isSelected() ||
                    cbEnableNode.isSelected() ||
                    cbEnablePCAPI.isSelected())
                {
                    return null;
                }

                return "An enabled listener must have at least one endpoint enabled.";
            }
        });
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

        String owningAssertionClassname = descriptor.getModularAssertionClassname();
        ClassLoader panelLoader = owningAssertionClassname == null ? Thread.currentThread().getContextClassLoader()
                : TopComponents.getInstance().getAssertionRegistry().getClassLoaderForAssertion(owningAssertionClassname);

        try {
            //noinspection unchecked
            panel = (CustomTransportPropertiesPanel) panelLoader.loadClass(customPropertiesPanelClassname).newInstance();
            customGuisByScheme.put(scheme, panel);
            String[] reservedPropertyNames = panel.getAdvancedPropertyNamesUsedByGui();
            if (reservedPropertyNames == null) {
                reservedPropertyNamesByScheme.put(scheme, Collections.<String>emptySet());
            } else {
                reservedPropertyNamesByScheme.put(scheme, new HashSet<String>(Arrays.asList(reservedPropertyNames)));
            }

            return panel;
        } catch (InstantiationException e) {
            logger.log(Level.WARNING, "Unablet to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (IllegalAccessException e) {
            logger.log(Level.WARNING, "Unablet to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        } catch (ClassNotFoundException e) {
            logger.log(Level.WARNING, "Unablet to load custom panel for " + descriptor + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    private boolean isEccCertButHasRsaCiphersEnabled() {
        String alg = privateKeyComboBox.getSelectedKeyAlgorithm();
        if (!("EC".equals(alg) || "ECDSA".equals(alg)))
            return false;
        final String cipherList = cipherSuiteListModel.asCipherListString();
        return cipherList == null || cipherList.indexOf("RSA_") > 0;
    }

    private void editProperty(final Pair<String, String> origPair) {
        final Frame p = TopComponents.getInstance().getTopParent();
        final SimplePropertyDialog dlg = origPair == null ? new SimplePropertyDialog(p) : new SimplePropertyDialog(p, origPair);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
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
                }
            });
        }
    }

    private void populateInterfaceComboBox(String interfaceTags) {
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

    public static class CipherSuiteListModel extends JCheckBoxListModel {
        private final String[] allCiphers;
        private final Set<String> defaultCiphers;

        public CipherSuiteListModel(String[] allCiphers, Set<String> defaultCiphers) {
            super(new ArrayList<JCheckBox>());
            this.allCiphers = ArrayUtils.copy(allCiphers);
            this.defaultCiphers = new LinkedHashSet<String>(defaultCiphers);
        }

        /**
         * @return cipher list string corresponding to all checked cipher names in order, comma delimited, ie.
         *         "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA", or null if the default
         *         cipher list is in use.
         */
        public String asCipherListString() {
            String defaultList = buildDefaultCipherListString();
            String ourList = buildEntryCodeString();
            return defaultList.equals(ourList) ? null : ourList;
        }

        private String buildDefaultCipherListString() {
            StringBuilder ret = new StringBuilder(128);
            boolean isFirst = true;
            for (String cipher : allCiphers) {
                if (defaultCiphers.contains(cipher)) {
                    if (!isFirst) ret.append(',');
                    ret.append(cipher);
                    isFirst = false;
                }
            }
            return ret.toString();
        }

        /**
         * Populate the list model from the specified cipher list string.
         * This will first build a master list of ciphers by appending any missing ciphers from allCiphers
         * to the end of the provided cipherList, then marking as "checked" only those ciphers that were present
         * in cipherList.
         *
         * @param cipherList a cipher list string, ie "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA",
         *                   or null to just use the default cipher list.
         */
        public void setCipherListString(String cipherList) {
            if (cipherList == null) {
                setDefaultCipherList();
                return;
            }

            Set<String> enabled = new LinkedHashSet<String>(Arrays.asList(cipherList.split(WS_COMMA_WS)));
            Set<String> all = new LinkedHashSet<String>(Arrays.asList(allCiphers));
            List<JCheckBox> entries = getEntries();
            entries.clear();
            for (String cipher : enabled) {
                entries.add(new JCheckBox(cipher, true));
            }
            for (String cipher : all) {
                if (!enabled.contains(cipher))
                    entries.add(new JCheckBox(cipher, false));
            }
        }

        /**
         * Reset the cipher list to the defaults.
         */
        public void setDefaultCipherList() {
            List<JCheckBox> entries = getEntries();
            int oldsize = entries.size();
            entries.clear();
            for (String cipher : allCiphers)
                entries.add(new JCheckBox(cipher, defaultCiphers.contains(cipher)));
            fireContentsChanged(this, 0, Math.max(oldsize, entries.size()));
        }
    }

    private static String[] getCipherSuiteNames() {
        String[] unfiltered = Registry.getDefault().getTransportAdmin().getAllCipherSuiteNames();
        if (INCLUDE_ALL_CIPHERS)
            return unfiltered;

        List<String> ret = new ArrayList<String>();
        for (String name : unfiltered) {
            if (cipherSuiteShouldBeVisible(name))
                ret.add(name);
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Check if the specified cipher suite should be shown or hidden in the UI.
     * <P/>
     * Currently a cipher suite is shown if is RSA based, as long as it is neither _WITH_NULL_ (which
     * does no encryption) or _anon_ (which does no authentication).
     *
     * @param cipherSuiteName the name of an SSL cipher suite to check, ie "TLS_RSA_WITH_AES_128_CBC_SHA".  Required.
     * @return true if this cipher suite should be shown in the UI (regardless of whether it should be checked by default
     *         in new connectors).  False if this cipher suite should be hidden in the UI.
     */
    public static boolean cipherSuiteShouldBeVisible(String cipherSuiteName) {
        return !contains(cipherSuiteName, "_WITH_NULL_") && !contains(cipherSuiteName, "_anon_") && (
                contains(cipherSuiteName, "_RSA_") ||
                contains(cipherSuiteName, "_ECDSA_") 
        );
    }

    /**
     * Check if the specified cipher suite should be checked by default in newly created listen ports.
     *
     * @param cipherSuiteName the name of an SSL cipher suite to check, ie "TLS_RSA_WITH_AES_128_CBC_SHA".  Required.
     * @return true if this cipher suite should be checked by default in the UI.
     */
    public static boolean cipherSuiteShouldBeCheckedByDefault(String cipherSuiteName) {
        return cipherSuiteShouldBeVisible(cipherSuiteName) && !contains(cipherSuiteName, "_EXPORT_");
    }

    private static boolean contains(String string, String substr) {
        return string.indexOf(substr) > 0;
    }

    private void initializeCipherSuiteControls() {
        String[] allCiphers = getCipherSuiteNames();
        Set<String> defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));
        this.cipherSuiteListModel = new CipherSuiteListModel(allCiphers, defaultCiphers);
        final JList cipherSuiteList = this.cipherSuiteList;
        final CipherSuiteListModel cipherSuiteListModel = this.cipherSuiteListModel;
        cipherSuiteListModel.attachToJList(cipherSuiteList);
        this.cipherSuiteList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableCipherSuiteButtons();
            }
        });

        defaultCipherListButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.setDefaultCipherList();
            }
        });

        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = SsgConnectorPropertiesDialog.this.cipherSuiteList.getSelectedIndex();
                if (index < 1) return;
                int prevIndex = index - 1;
                cipherSuiteListModel.swapEntries(prevIndex, index);
                SsgConnectorPropertiesDialog.this.cipherSuiteList.setSelectedIndex(prevIndex);
                SsgConnectorPropertiesDialog.this.cipherSuiteList.ensureIndexIsVisible(prevIndex);
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = SsgConnectorPropertiesDialog.this.cipherSuiteList.getSelectedIndex();
                if (index < 0 || index >= cipherSuiteListModel.getSize() - 1) return;
                int nextIndex = index + 1;
                cipherSuiteListModel.swapEntries(index, nextIndex);
                SsgConnectorPropertiesDialog.this.cipherSuiteList.setSelectedIndex(nextIndex);
                SsgConnectorPropertiesDialog.this.cipherSuiteList.ensureIndexIsVisible(nextIndex);
            }
        });
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

    private void enableOrDisableComponents() {
        enableOrDisableTabs();
        enableOrDisableEndpoints();
        enableOrDisableTlsVersions();
        enableOrDisableCipherSuiteButtons();
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
        tabbedPane.setTitleAt(TAB_HTTP, isPool && !isHttp ? "Pool Settings" : "HTTP Settings");
        
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

        Set<Endpoint> endpoints = proto == null ? Collections.<Endpoint>emptySet() : proto.getSupportedEndpoints();
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.MESSAGE_INPUT, cbEnableMessageInput);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.PC_NODE_API, cbEnablePCAPI);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.OTHER_SERVLETS, cbEnableBuiltinServices);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.ADMIN_REMOTE, cbEnableSsmRemote);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.ADMIN_APPLET, cbEnableSsmApplet);
        disableOrRestoreEndpointCheckBox(endpoints, SsgConnector.Endpoint.NODE_COMMUNICATION, cbEnableNode);

        // If this transport supports nothing but message input, hardwire that checkbox
        if (endpoints.size() == 1 && endpoints.contains(SsgConnector.Endpoint.MESSAGE_INPUT)) {
            setEnableAndSelect(false, true, "Enabled because it is required for the selected transport protocol", cbEnableMessageInput);
        }

        if (!ssl) {
            setEnableAndSelect(false, false, "Disabled because it requires a TLS-based transport", cbEnableSsmApplet, cbEnableSsmRemote, cbEnableNode);
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

    private void enableOrDisableCipherSuiteButtons() {
        int index = cipherSuiteList.getSelectedIndex();
        moveUpButton.setEnabled(index > 0);
        moveDownButton.setEnabled(index >= 0 && index < cipherSuiteListModel.getSize() - 1);
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
        if (cbEnableSsmRemote.isSelected()) endpoints.add(Endpoint.ADMIN_REMOTE.name());
        if (cbEnableSsmApplet.isSelected()) endpoints.add(Endpoint.ADMIN_APPLET.name());
        if (cbEnableBuiltinServices.isSelected()) endpoints.add(Endpoint.OTHER_SERVLETS.name());
        if (cbEnableNode.isSelected()) endpoints.add(Endpoint.NODE_COMMUNICATION.name());
        if (cbEnablePCAPI.isSelected()) endpoints.add(Endpoint.PC_NODE_API.name());

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
        String[] names = endpoints == null ? ArrayUtils.EMPTY_STRING_ARRAY : endpoints.split(WS_COMMA_WS);

        boolean messages = false;
        boolean ssmRemote = false;
        boolean ssmApplet = false;
        boolean builtin = false;
        boolean node = false;
        boolean pcapi = false;

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
                    case ADMIN_REMOTE:      ssmRemote = true;   break;
                    case ADMIN_APPLET:      ssmApplet = true;   break;
                    case NODE_COMMUNICATION: node     = true;   break;
                    case PC_NODE_API:       pcapi     = true;   break;            
                    default:                builtin   = true;   break;
                }
            } catch (IllegalArgumentException iae) {
                logger.fine("Ignoring unrecognized endpoint name: " + name);
            }
        }

        cbEnableMessageInput.setSelected(messages);
        cbEnableSsmRemote.setSelected(ssmRemote);
        cbEnableSsmApplet.setSelected(ssmApplet);
        cbEnableBuiltinServices.setSelected(builtin);
        cbEnableNode.setSelected(node);
        cbEnablePCAPI.setSelected(pcapi);

        // For listen ports last saved pre-Pandora, have GUI reflect what the actual system behavior will be (Bug #8802)
        if (cbEnableSsmApplet.isSelected())
            cbEnableSsmRemote.setSelected(true);
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
        long hardwiredServiceId = hardwiredServiceIdStr != null && hardwiredServiceIdStr.trim().length() > 0 ? Long.parseLong(hardwiredServiceIdStr) : -1;
        boolean usingHardwired = serviceNameComboBox.populateAndSelect(hardwiredServiceId != -1, hardwiredServiceId);
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
        connector.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, cipherSuiteListModel.asCipherListString());

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
    }

    @Override
    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
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
    private static class ModularConnectorInfoComparator implements Comparator<TransportDescriptor> {
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
