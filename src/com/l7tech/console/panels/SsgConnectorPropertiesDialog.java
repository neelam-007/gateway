package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.common.transport.SsgConnector;
import static com.l7tech.common.transport.SsgConnector.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SsgConnectorPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgConnectorPropertiesDialog.class.getName());
    private static final String DIALOG_TITLE = "Listen Port Properties";
    private static final int TAB_SSL = 2;
    private static final int TAB_FTP = 3;

    private static class ClientAuthType {
        private static final Map<Integer, ClientAuthType> bycode = new ConcurrentHashMap<Integer, ClientAuthType>();
        final int code;
        final String label;
        public ClientAuthType(int code, String label) {
            this.code = code;
            this.label = label;
            bycode.put(code, this);
        }
        public String toString() {
            return label;
        }
    }
    private static final Object CA_NONE = new ClientAuthType(SsgConnector.CLIENT_AUTH_NEVER, "None");
    private static final Object CA_OPTIONAL = new ClientAuthType(SsgConnector.CLIENT_AUTH_OPTIONAL, "Optional");
    private static final Object CA_REQUIRED = new ClientAuthType(SsgConnector.CLIENT_AUTH_ALWAYS, "Required");

    private static final String INTERFACE_ANY = "(All)";

    private static final String WS_COMMA_WS = "\\s*,\\s*";

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
    private javax.swing.JCheckBox cbEnableMessageInput;
    private javax.swing.JCheckBox cbEnableBuiltinServices;
    private javax.swing.JCheckBox cbEnableSsmRemote;
    private javax.swing.JCheckBox cbEnableSsmApplet;

    private final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
    private SsgConnector connector;
    private boolean confirmed = false;
    private CipherSuiteListModel cipherSuiteListModel;
    private DefaultComboBoxModel interfaceComboBoxModel;

    public SsgConnectorPropertiesDialog(Frame owner, SsgConnector connector) {
        super(owner, DIALOG_TITLE);
        initialize(connector);
    }

    public SsgConnectorPropertiesDialog(Dialog owner, SsgConnector connector) {
        super(owner, DIALOG_TITLE);
        initialize(connector);
    }

    private void initialize(SsgConnector connector) {
        this.connector = connector;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);

        inputValidator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        managePrivateKeysButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(TopComponents.getInstance().getTopParent());
                pkmw.pack();
                Utilities.centerOnScreen(pkmw);
                DialogDisplayer.display(pkmw, new Runnable() {
                    public void run() {
                        privateKeyComboBox.repopulate();
                    }
                });
            }
        });

        protocolComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                SCHEME_HTTP,
                SCHEME_HTTPS,
                SCHEME_FTP,
                SCHEME_FTPS,
        }));

        protocolComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                enableOrDisableTabs();
            }
        });

        clientAuthComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                CA_NONE,
                CA_OPTIONAL,
                CA_REQUIRED
        }));

        initializeInterfaceComboBox();

        initializeCipherSuiteControls();

        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, null);
        inputValidator.validateWhenDocumentChanges(nameField);
        inputValidator.constrainTextFieldToNumberRange("Port", portField, 0, 65535);
        inputValidator.validateWhenDocumentChanges(portField);
        inputValidator.constrainTextFieldToNumberRange("Port Range Start", portRangeStartField, 0, 65535);
        inputValidator.constrainTextFieldToNumberRange("Port Range Count", portRangeCountField, 1, 65535);
        inputValidator.constrainTextField(portRangeCountField, new InputValidator.ComponentValidationRule(portRangeCountField) {
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
            public String getValidationError() {
                if (!privateKeyComboBox.isEnabled())
                    return null;
                if (privateKeyComboBox.getSelectedItem() == null)
                    return "A private key must be selected for an SSL listener.";
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(cipherSuiteList) {
            public String getValidationError() {
                if (!cipherSuiteList.isEnabled())
                    return null;
                if (!cipherSuiteListModel.isAnyEntryChecked())
                    return "At least one cipher suite must be enabled for an SSL listener.";
                return null;
            }
        });
        inputValidator.addRule(new InputValidator.ComponentValidationRule(cbEnableMessageInput) {
            public String getValidationError() {
                if ((!enabledCheckBox.isSelected()) ||
                    cbEnableBuiltinServices.isSelected() ||
                    cbEnableMessageInput.isSelected() ||
                    cbEnableSsmApplet.isSelected() ||
                    cbEnableSsmRemote.isSelected())
                {
                    return null;
                }

                return "An enabled listener must have at least one endpoint enabled.";
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
    }

    private void initializeInterfaceComboBox() {
        Vector<String> entries = new Vector<String>();

        entries.add(INTERFACE_ANY);
        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }

        interfaceComboBoxModel = new DefaultComboBoxModel(entries);
        interfaceComboBox.setModel(interfaceComboBoxModel);
    }

    public static class JCheckBoxListModel extends AbstractListModel {
        public static final String CLIENT_PROPERTY_ENTRY_CODE = "JCheckBoxListModel.entryCode";
        protected final List<JCheckBox> entries;
        private int armedEntry = -1;

        public JCheckBoxListModel(List<JCheckBox> entries) {
            this.entries = entries;
        }

        public int getSize() {
            return entries.size();
        }

        public Object getElementAt(int index) {
            return getEntryAt(index);
        }

        public JCheckBox getEntryAt(int index) {
            return entries.get(index);
        }

        public void swapEntries(int index1, int index2) {
            JCheckBox value1 = entries.get(index1);
            JCheckBox value2 = entries.get(index2);
            entries.set(index2, value1);
            entries.set(index1, value2);
            fireContentsChanged(this, index1, index2);
        }

        /**
         * Set the "armed" state for the checkbox at the specified index.
         * Any currently-armed checkbox will be disarmed.
         * <p/>
         * The "armed" state is normally shown on mousedown to show that a checkbox is toggling.
         *
         * @param index index of list entry to arm
         */
        public void arm(int index) {
            disarm();
            if (index < 0) return;
            ButtonModel entryModel = getEntryAt(index).getModel();
            entryModel.setArmed(true);
            entryModel.setRollover(true);
            armedEntry = index;
            fireContentsChanged(this, armedEntry, armedEntry);
        }

        /**
         * Clear the "armed" state from any checkbox that was armed by a call to {@link #arm}.
         */
        public void disarm() {
            if (armedEntry >= 0) {
                getEntryAt(armedEntry).getModel().setArmed(false);
                fireContentsChanged(this, armedEntry, armedEntry);
                armedEntry = -1;
            }
        }

        /**
         * Toggle the checkbox at the specified index.
         * @param index the index to toggle.  Must be between 0 and getSize() - 1 inclusive.
         */
        public void toggle(int index) {
            if (armedEntry >= 0 && armedEntry != index) disarm();
            JCheckBox entry = getEntryAt(index);
            ButtonModel entryModel = entry.getModel();
            entryModel.setArmed(false);
            entryModel.setRollover(false);
            entry.setSelected(!entry.isSelected());
            fireContentsChanged(this, index, index);
        }

        /**
         * @return true if at least one check box is checked.
         */
        public boolean isAnyEntryChecked() {
            for (JCheckBox entry : entries) {
                if (entry.isSelected()) return true;
            }
            return false;
        }

        /**
         * Get the code name for the specified entry.
         *
         * @param entry  one of the checkbox list entries.  Required.
         * @return the code name for this entry, ie "SSL_RSA_WITH_3DES_EDE_CBC_SHA".
         */
        protected String getEntryCode(JCheckBox entry) {
            Object code = entry.getClientProperty(CLIENT_PROPERTY_ENTRY_CODE);
            return code != null ? code.toString() : entry.getText();
        }

        protected String buildEntryCodeString() {
            StringBuilder ret = new StringBuilder();
            boolean isFirst = true;
            for (JCheckBox entry : entries) {
                if (entry.isSelected()) {
                    if (!isFirst) ret.append(',');
                    ret.append(getEntryCode(entry));
                    isFirst = false;
                }
            }
            return ret.toString();
        }
    }

    public static class CipherSuiteListModel extends JCheckBoxListModel {
        private final String[] allCiphers;
        private final Set<String> defaultCiphers;

        public CipherSuiteListModel(String[] allCiphers, Set<String> defaultCiphers) {
            super(new ArrayList<JCheckBox>());
            this.allCiphers = allCiphers;
            this.defaultCiphers = defaultCiphers;
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
            StringBuilder ret = new StringBuilder();
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
            int oldsize = entries.size();
            entries.clear();
            for (String cipher : allCiphers)
                entries.add(new JCheckBox(cipher, defaultCiphers.contains(cipher)));
            fireContentsChanged(this, 0, Math.max(oldsize, entries.size()));
        }
    }

    private class CipherSuiteListSelectionModel extends DefaultListSelectionModel {
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(index0, index1);
            cipherSuiteListModel.arm(index0);
        }
    }

    /**
     * A cell renderer for lists whose list items are actually components.
     */
    private static class ComponentListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object valueObj, int index, boolean isSelected, boolean cellHasFocus) {
            Component value = (JCheckBox)valueObj;
            Component supe = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            value.setForeground(supe.getForeground());
            value.setBackground(supe.getBackground());
            value.setFont(supe.getFont());
            return value;
        }
    }

    private void initializeCipherSuiteControls() {
        String[] allCiphers = Registry.getDefault().getTransportAdmin().getAllCipherSuiteNames();
        LinkedHashSet<String> defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));
        cipherSuiteListModel = new CipherSuiteListModel(allCiphers, defaultCiphers);
        cipherSuiteList.setModel(cipherSuiteListModel);
        cipherSuiteList.setSelectionModel(new CipherSuiteListSelectionModel());
        cipherSuiteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cipherSuiteList.setCellRenderer(new ComponentListCellRenderer());
        cipherSuiteList.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent e) {
                cipherSuiteListModel.disarm();
            }

            public void mousePressed(MouseEvent e) {
                int selectedIndex = cipherSuiteList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                cipherSuiteListModel.disarm();
                cipherSuiteListModel.arm(selectedIndex);
            }

            public void mouseReleased(MouseEvent e) {
                int selectedIndex = cipherSuiteList.locationToIndex(e.getPoint());
                if (selectedIndex < 0) return;
                cipherSuiteListModel.toggle(selectedIndex);
            }
        });
        // Change unmodified space from 'addToSelection' to 'toggleCheckBox' (ie, same as our above single-click handler)
        cipherSuiteList.getInputMap().put(KeyStroke.getKeyStroke(' '), "toggleCheckBox");
        cipherSuiteList.getActionMap().put("toggleCheckBox", new AbstractAction("toggleCheckBox") {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = cipherSuiteList.getSelectedIndex();
                cipherSuiteListModel.toggle(selectedIndex);
            }
        });
        cipherSuiteList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableCipherSuiteButtons();
            }
        });

        defaultCipherListButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cipherSuiteListModel.setDefaultCipherList();
            }
        });

        moveUpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = cipherSuiteList.getSelectedIndex();
                if (index < 1) return;
                int prevIndex = index - 1;
                cipherSuiteListModel.swapEntries(prevIndex, index);
                cipherSuiteList.setSelectedIndex(prevIndex);
                cipherSuiteList.ensureIndexIsVisible(prevIndex);
            }
        });

        moveDownButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = cipherSuiteList.getSelectedIndex();
                if (index < 0 || index >= cipherSuiteListModel.getSize() - 1) return;
                int nextIndex = index + 1;
                cipherSuiteListModel.swapEntries(index, nextIndex);
                cipherSuiteList.setSelectedIndex(nextIndex);
                cipherSuiteList.ensureIndexIsVisible(nextIndex);
            }
        });
    }

    private String getSelectedProtocol() {
        return protocolComboBox.getSelectedItem().toString();
    }

    private boolean isSslProto(String protocol) {
        return SCHEME_HTTPS.equals(protocol) || SCHEME_FTPS.equals(protocol);
    }

    private boolean isFtpProto(String protocol) {
        return SCHEME_FTP.equals(protocol) || SCHEME_FTPS.equals(protocol);
    }

    private void enableOrDisableComponents() {
        enableOrDisableTabs();
        enableOrDisableCipherSuiteButtons();
    }

    private void enableOrDisableTabs() {
        String proto = getSelectedProtocol();
        boolean isSsl = isSslProto(proto);
        boolean isFtp = isFtpProto(proto);

        tabbedPane.setEnabledAt(TAB_SSL, isSsl);
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
    }

    private void enableOrDisableCipherSuiteButtons() {
        int index = cipherSuiteList.getSelectedIndex();
        moveUpButton.setEnabled(index > 0);
        moveDownButton.setEnabled(index >= 0 && index < cipherSuiteListModel.getSize() - 1);
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

        return HexUtils.join(",", endpoints.toArray(new String[0])).toString();
    }

    /**
     * Set the endpoint checkbox state to correspond to the specified endpoint name string, if possible.
     * If not possible, we will come as close as we can get.
     *
     * @param endpoints a String in the form "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET".  If null, no endpoints
     *                  will be enabled.
     */
    private void setEndpointList(String endpoints) {
        String[] names = endpoints == null ? new String[0] : endpoints.split(WS_COMMA_WS);

        boolean messages = false;
        boolean ssmRemote = false;
        boolean ssmApplet = false;
        boolean builtin = false;
        long alacart = 0;

        // Currently the GUI only has four checkboxes, so we'll try to behave sensibly if the endpoint
        // list has been customized in more detail than our GUI allows.
        for (String name : names) {
            try {
                Endpoint endpoint = Endpoint.valueOf(name);
                switch (endpoint) {
                    case MESSAGE_INPUT:     messages  = true;   break;
                    case ADMIN_REMOTE:      ssmRemote = true;   break;
                    case ADMIN_APPLET:      ssmApplet = true;   break;
                    case OTHER_SERVLETS:    builtin   = true;   break;
                    case POLICYDISCO:       alacart |=   1;     break;
                    case STS:               alacart |=   2;     break;
                    case CSRHANDLER:        alacart |=   4;     break;
                    case PASSWD:            alacart |=   8;     break;
                    case WSDLPROXY:         alacart |=  16;     break;
                    case SNMPQUERY:         alacart |=  32;     break;
                    default:
                        logger.fine("Ignoring unrecognized endpoint: " + endpoint);
                }
            } catch (IllegalArgumentException iae) {
                logger.fine("Ignoring unrecognized endpoint name: " + name);
            }
        }

        if ((alacart & 63) == 63) {
            // All builtin services currently known to this SSM were individually enabled
            builtin = true;
        }

        cbEnableMessageInput.setSelected(messages);
        cbEnableSsmRemote.setSelected(ssmRemote);
        cbEnableSsmApplet.setSelected(ssmApplet);
        cbEnableBuiltinServices.setSelected(builtin);
    }

    /**
     * Configure the GUI control states with information gathered from the connector instance.
     */
    private void modelToView() {
        protocolComboBox.setSelectedItem(connector.getScheme().trim().toUpperCase());
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        enabledCheckBox.setSelected(connector.isEnabled());

        String bindAddr = connector.getProperty(SsgConnector.PROP_BIND_ADDRESS);
        interfaceComboBox.setSelectedItem(bindAddr != null ? bindAddr : INTERFACE_ANY);
        if (bindAddr != null && interfaceComboBox.getSelectedItem() == null) {
            // It wasn't there -- add it so we don't lose user data
            interfaceComboBoxModel.addElement(bindAddr);
            interfaceComboBox.setSelectedItem(bindAddr);
        }

        setEndpointList(connector.getEndpoints());

        // FTP-specific properties
        String prs = connector.getProperty(SsgConnector.PROP_PORT_RANGE_START);
        portRangeStartField.setText(prs == null ? "" : prs);
        String prc = connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT);
        portRangeCountField.setText(prc == null ? "" : prc);

        // SSL-specific properties
        cipherSuiteListModel.setCipherListString(connector.getProperty(SsgConnector.PROP_CIPHERLIST));
        clientAuthComboBox.setSelectedItem(ClientAuthType.bycode.get(connector.getClientAuth()));
        selectPrivateKey(connector.getKeystoreOid(), connector.getKeyAlias());

        enableOrDisableComponents();
    }

    /**
     * Configure the connector instance with information gathered from the GUI control states.
     * Assumes caller has already checked view state against the inputValidator.
     */
    private void viewToModel() {
        String proto = protocolComboBox.getSelectedItem().toString();
        connector.setScheme(proto);
        connector.setName(nameField.getText());
        connector.setPort(Integer.parseInt(portField.getText()));
        connector.setEnabled(enabledCheckBox.isSelected());
        String bindAddress = (String)interfaceComboBox.getSelectedItem();
        connector.putProperty(SsgConnector.PROP_BIND_ADDRESS, bindAddress == null ? INTERFACE_ANY : bindAddress);
        connector.setEndpoints(getEndpointList());

        // FTP-specific properties
        boolean isFtp = isFtpProto(proto);
        String rangeStart = portRangeStartField.getText().trim();
        String rangeCount = portRangeCountField.getText().trim();
        connector.putProperty(SsgConnector.PROP_PORT_RANGE_START, isFtp ? rangeStart : null);
        connector.putProperty(SsgConnector.PROP_PORT_RANGE_COUNT, isFtp ? rangeCount : null);

        // SSL-specific properties
        boolean isSsl = isSslProto(proto);
        connector.setSecure(isSsl);
        if (isSsl) {
            connector.setKeystoreOid(privateKeyComboBox.getSelectedKeystoreId());
            connector.setKeyAlias(privateKeyComboBox.getSelectedKeyAlias());
        } else {
            connector.setKeystoreOid(null);
            connector.setKeyAlias(null);
        }
        connector.setClientAuth(((ClientAuthType)clientAuthComboBox.getSelectedItem()).code);
        connector.putProperty(SsgConnector.PROP_CIPHERLIST, cipherSuiteListModel.asCipherListString());
    }

    public void setVisible(boolean b) {
        if (b && !isVisible()) confirmed = false;
        super.setVisible(b);
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    public SsgConnector getConnector() {
        return connector;
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
}
