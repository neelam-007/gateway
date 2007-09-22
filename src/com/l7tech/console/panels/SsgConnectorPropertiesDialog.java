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

        defaultCipherListButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                populateCipherSuiteComboBox();
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
                enableOrDisableComponents();
            }
        });

        populateCipherSuiteComboBox();

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

    private static class CipherSuiteListModel extends AbstractListModel {
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
            return entries.get(index);
        }

        private void arm(int index) {
            disarm();
            if (index < 0) return;
            ButtonModel entryModel = entries.get(index).getModel();
            entryModel.setArmed(true);
            entryModel.setRollover(true);
            armedEntry = index;
            fireContentsChanged(this, armedEntry, armedEntry);
        }

        private void disarm() {
            if (armedEntry >= 0) {
                entries.get(armedEntry).getModel().setArmed(false);
                fireContentsChanged(this, armedEntry, armedEntry);
                armedEntry = -1;
            }
        }

        public void toggle(int index) {
            if (armedEntry >= 0 && armedEntry != index) disarm();
            CipherSuiteListEntry entry = entries.get(index);
            ButtonModel entryModel = entry.getModel();
            entryModel.setArmed(false);
            entryModel.setRollover(false);
            entry.setSelected(!entry.isSelected());
            fireContentsChanged(this, index, index);
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
            cipherSuiteList.setSelectionModel(new CipherSuiteListSelectionModel());
            cipherSuiteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            cipherSuiteList.setCellRenderer(new CipherSuiteListCellRenderer());
            cipherSuiteList.addMouseListener(new MouseAdapter() {
                public void mouseExited(MouseEvent e) {
                    cipherSuiteListModel.disarm();
                }

                public void mousePressed(MouseEvent e) {
                    cipherSuiteListModel.disarm();
                    int selectedIndex = cipherSuiteList.locationToIndex(e.getPoint());
                    cipherSuiteListModel.arm(selectedIndex);
                }

                public void mouseReleased(MouseEvent e) {
                    int selectedIndex = cipherSuiteList.locationToIndex(e.getPoint());
                    cipherSuiteListModel.toggle(selectedIndex);
                }
            });
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
        tabbedPane.setEnabledAt(TAB_SSL, isSslProto(getSelectedProtocol()));
        tabbedPane.setEnabledAt(TAB_FTP, isFtpProto(getSelectedProtocol()));
    }

    private void modelToView() {
        nameField.setText(connector.getName());
        portField.setText(String.valueOf(connector.getPort()));
        // TODO
        enableOrDisableComponents();
    }

    private void viewToModel() {
        connector.setName(nameField.getText());
        connector.setPort(Integer.parseInt(portField.getText()));
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
