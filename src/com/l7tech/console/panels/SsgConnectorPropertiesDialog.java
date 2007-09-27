package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.InputValidator;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.common.transport.SsgConnector;
import static com.l7tech.common.transport.SsgConnector.*;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
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

    // TODO look up real values from SSG
    private static final String INTERFACE_ANY = "*";
    private static final String INTERFACE_INTERNAL = "1.2.3.4";
    private static final String INTERFACE_EXTERNAL = "10.0.0.1";

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
    private JCheckBox enableSecureSpanManagerAccessCheckBox;
    private JCheckBox enableWebBasedAdministrationCheckBox;
    private JCheckBox enablePublishedServiceMessageCheckBox;
    private JCheckBox enableBuiltInServicesCheckBox;
    private JCheckBox enabledCheckBox;
    private JTabbedPane tabbedPane;

    private final InputValidator inputValidator = new InputValidator(this, DIALOG_TITLE);
    private SsgConnector connector;
    private boolean confirmed = false;
    private CipherSuiteListModel cipherSuiteListModel;

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
                try {
                    final PrivateKeyManagerWindow pkmw = new PrivateKeyManagerWindow(TopComponents.getInstance().getTopParent());
                    pkmw.pack();
                    Utilities.centerOnScreen(pkmw);
                    DialogDisplayer.display(pkmw, new Runnable() {
                        public void run() {
                            privateKeyComboBox.repopulate();
                        }
                    });
                } catch (RemoteException e1) {
                    showErrorMessage("Error", "Unable to manage private keys: " + ExceptionUtils.getMessage(e1), e1);
                }
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

        interfaceComboBox.setModel(new DefaultComboBoxModel(new Object[] {
                INTERFACE_ANY,
                INTERFACE_INTERNAL,
                INTERFACE_EXTERNAL
        }));

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
                if (!cipherSuiteListModel.isAnyCipherSuiteEnabled())
                    return "At least one cipher suite must be enabled for an SSL listener.";
                return null;
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
        if (nameField.getText().length() < 1)
            nameField.requestFocusInWindow();
    }

    private static class CipherSuiteListEntry extends JCheckBox {
        public CipherSuiteListEntry(String text, boolean selected) {
            super(text, selected);
        }

        public ButtonModel getModel() {
            return super.getModel();
        }
    }

    public static class CipherSuiteListModel extends AbstractListModel {
        private final String[] allCiphers;
        private final Set<String> defaultCiphers;
        private final List<CipherSuiteListEntry> entries = new ArrayList<CipherSuiteListEntry>();
        private int armedEntry = -1;

        public CipherSuiteListModel(String[] allCiphers, Set<String> defaultCiphers) {
            this.allCiphers = allCiphers;
            this.defaultCiphers = defaultCiphers;
        }

        public int getSize() {
            return entries.size();
        }

        public Object getElementAt(int index) {
            return getEntryAt(index);
        }

        public CipherSuiteListEntry getEntryAt(int index) {
            return entries.get(index);
        }

        public void swapEntries(int index1, int index2) {
            CipherSuiteListEntry value1 = entries.get(index1);
            CipherSuiteListEntry value2 = entries.get(index2);
            entries.set(index2, value1);
            entries.set(index1, value2);
            fireContentsChanged(this, index1, index2);
        }

        private void arm(int index) {
            disarm();
            if (index < 0) return;
            ButtonModel entryModel = getEntryAt(index).getModel();
            entryModel.setArmed(true);
            entryModel.setRollover(true);
            armedEntry = index;
            fireContentsChanged(this, armedEntry, armedEntry);
        }

        private void disarm() {
            if (armedEntry >= 0) {
                getEntryAt(armedEntry).getModel().setArmed(false);
                fireContentsChanged(this, armedEntry, armedEntry);
                armedEntry = -1;
            }
        }

        public void toggle(int index) {
            if (armedEntry >= 0 && armedEntry != index) disarm();
            CipherSuiteListEntry entry = getEntryAt(index);
            ButtonModel entryModel = entry.getModel();
            entryModel.setArmed(false);
            entryModel.setRollover(false);
            entry.setSelected(!entry.isSelected());
            fireContentsChanged(this, index, index);
        }

        /**
         * @return true if at least one cipher suite is checked.
         */
        public boolean isAnyCipherSuiteEnabled() {
            for (CipherSuiteListEntry entry : entries) {
                if (entry.isSelected()) return true;
            }
            return false;
        }

        /**
         * @return cipher list string corresponding to all checked cipher names in order, comma delimited, ie.
         *         "TLS_RSA_WITH_AES_128_CBC_SHA, SSL_RSA_WITH_3DES_EDE_CBC_SHA", or null if the default
         *         cipher list is in use.
         */
        public String asCipherListString() {
            String defaultList = buildDefaultCipherListString();
            String ourList = buildCipherListString();
            return defaultList.equals(ourList) ? null : ourList;
        }

        private String buildCipherListString() {
            StringBuilder ret = new StringBuilder();
            boolean isFirst = true;
            for (CipherSuiteListEntry entry : entries) {
                if (entry.isSelected()) {
                    if (!isFirst) ret.append(',');
                    ret.append(entry.getText());
                    isFirst = false;
                }
            }
            return ret.toString();
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

            Set<String> enabled = new LinkedHashSet<String>(Arrays.asList(cipherList.split("\\s*,\\s*")));
            Set<String> all = new LinkedHashSet<String>(Arrays.asList(allCiphers));
            entries.clear();
            for (String cipher : enabled) {
                entries.add(new CipherSuiteListEntry(cipher, true));
            }
            for (String cipher : all) {
                if (!enabled.contains(cipher))
                    entries.add(new CipherSuiteListEntry(cipher, false));
            }
        }

        /**
         * Reset the cipher list to the defaults.
         */
        public void setDefaultCipherList() {
            int oldsize = entries.size();
            entries.clear();
            for (String cipher : allCiphers)
                entries.add(new CipherSuiteListEntry(cipher, defaultCiphers.contains(cipher)));
            fireContentsChanged(this, 0, Math.max(oldsize, entries.size()));
        }
    }

    private class CipherSuiteListSelectionModel extends DefaultListSelectionModel {
        public void setSelectionInterval(int index0, int index1) {
            super.setSelectionInterval(index0, index1);
            cipherSuiteListModel.arm(index0);
        }
    }

    private static class CipherSuiteListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object valueObj, int index, boolean isSelected, boolean cellHasFocus) {
            CipherSuiteListEntry value = (CipherSuiteListEntry)valueObj;
            Component supe = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            value.setForeground(supe.getForeground());
            value.setBackground(supe.getBackground());
            value.setFont(supe.getFont());
            return value;
        }
    }

    private void initializeCipherSuiteControls() {
        try {
            String[] allCiphers = Registry.getDefault().getTransportAdmin().getAllCipherSuiteNames();
            LinkedHashSet<String> defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                    Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));
            cipherSuiteListModel = new CipherSuiteListModel(allCiphers, defaultCiphers);
            cipherSuiteList.setModel(cipherSuiteListModel);
        } catch (RemoteException e) {
            showErrorMessage("Error", "Unable to load cipher suites: " + ExceptionUtils.getMessage(e), e);
        }
        cipherSuiteList.setSelectionModel(new CipherSuiteListSelectionModel());
        cipherSuiteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cipherSuiteList.setCellRenderer(new CipherSuiteListCellRenderer());
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
     * Configure the GUI control states with information gathered from the connector instance.
     */
    private void modelToView() {
        protocolComboBox.setSelectedItem(connector.getScheme().trim().toUpperCase());
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        enabledCheckBox.setSelected(connector.isEnabled());
        interfaceComboBox.setSelectedItem(connector.getProperty(SsgConnector.PROP_BIND_ADDRESS)); // TODO set properly

        // FTP-specific properties
        String prs = connector.getProperty(SsgConnector.PROP_PORT_RANGE_START);
        portRangeStartField.setText(prs == null ? "" : prs);
        String prc = connector.getProperty(SsgConnector.PROP_PORT_RANGE_COUNT);
        portRangeCountField.setText(prc == null ? "" : prc);

        // SSL-specific properties
        cipherSuiteListModel.setCipherListString(connector.getProperty(SsgConnector.PROP_CIPHERLIST));
        clientAuthComboBox.setSelectedItem(ClientAuthType.bycode.get(connector.getClientAuth()));
        selectPrivateKey(connector.getKeystoreOid(), connector.getKeyAlias());

        // TODO enabled endpoints
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
        String bindAddress = null; // TODO get properly from interface drop-down
        connector.putProperty(SsgConnector.PROP_BIND_ADDRESS, bindAddress);

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

        // TODO enabled endpoints
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
