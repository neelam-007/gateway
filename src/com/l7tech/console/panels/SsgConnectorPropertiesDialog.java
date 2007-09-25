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
import java.util.logging.Level;
import java.util.logging.Logger;

public class SsgConnectorPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgConnectorPropertiesDialog.class.getName());
    private static final String DIALOG_TITLE = "Listen Port Properties";
    private static final int TAB_SSL = 2;
    private static final int TAB_FTP = 3;

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
    private String[] allCiphers;
    private Set<String> defaultCiphers;
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

        initializeCipherSuiteControls();

        inputValidator.constrainTextFieldToBeNonEmpty("Name", nameField, null);
        inputValidator.validateWhenDocumentChanges(nameField);
        inputValidator.constrainTextFieldToNumberRange("Port", portField, 0, 65535);
        inputValidator.validateWhenDocumentChanges(portField);

        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
    }

    private static class CipherSuiteListEntry extends JCheckBox {
        public CipherSuiteListEntry(String text, boolean selected) {
            super(text, selected);
        }

        public ButtonModel getModel() {
            return super.getModel();
        }
    }

    private class CipherSuiteListModel extends AbstractListModel {
        private final List<CipherSuiteListEntry> entries = new ArrayList<CipherSuiteListEntry>();
        private int armedEntry = -1;

        public CipherSuiteListModel(List<CipherSuiteListEntry> entries) {
            this.entries.clear();
            this.entries.addAll(entries);
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
                populateCipherSuiteComboBox();
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
        populateCipherSuiteComboBox();
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
                populateCipherSuiteComboBox();
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

    private void populateCipherSuiteComboBox() {
        try {
            if (allCiphers == null)
                allCiphers = Registry.getDefault().getTransportAdmin().getAllCipherSuiteNames();
            if (defaultCiphers == null)
                defaultCiphers = new LinkedHashSet<String>(Arrays.asList(
                        Registry.getDefault().getTransportAdmin().getDefaultCipherSuiteNames()));
            List<CipherSuiteListEntry> entries = new ArrayList<CipherSuiteListEntry>();
            for (String cipher : allCiphers)
                entries.add(new CipherSuiteListEntry(cipher, defaultCiphers.contains(cipher)));
            cipherSuiteListModel = new CipherSuiteListModel(entries);
            cipherSuiteList.setModel(cipherSuiteListModel);
        } catch (RemoteException e) {
            showErrorMessage("Error", "Unable to load cipher suites: " + ExceptionUtils.getMessage(e), e);
        }
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
        tabbedPane.setEnabledAt(TAB_SSL, isSslProto(getSelectedProtocol()));
        tabbedPane.setEnabledAt(TAB_FTP, isFtpProto(getSelectedProtocol()));
    }

    private void enableOrDisableCipherSuiteButtons() {
        int index = cipherSuiteList.getSelectedIndex();
        moveUpButton.setEnabled(index > 0);
        moveDownButton.setEnabled(index >= 0 && index < cipherSuiteListModel.getSize() - 1);
    }

    private void modelToView() {
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        cipherSuiteListModel.setCipherListString(connector.getProperty(SsgConnector.PROP_CIPHERLIST));

        // TODO
        enableOrDisableComponents();
    }

    private void viewToModel() {
        connector.setName(nameField.getText());
        connector.setPort(Integer.parseInt(portField.getText()));
        connector.putProperty(SsgConnector.PROP_CIPHERLIST, cipherSuiteListModel.asCipherListString());

        // TODO
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
