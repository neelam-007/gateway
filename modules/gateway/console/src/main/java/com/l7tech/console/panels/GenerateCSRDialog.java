package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.X509GeneralName;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dialog;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * This is the generate CSR dialog. It allows a user to enter a DN and select a hash function for the CSR
 */
public class GenerateCSRDialog extends JDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.GenerateCSRDialog");

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField dnTextField;
    private JComboBox<SigHash> signatureHashComboBox;
    private JLabel sanLabel;
    private JTable sanTable;
    private JButton addSanButton;
    private JButton editSanButton;
    private JButton removeSanButton;
    private boolean okD = false;
    private String selectedHash;
    private String csrSubjectDN;
    private List<X509GeneralName> csrSAN = new ArrayList<>();

    private SimpleTableModel<NameValuePair> sanTableModel;

    public GenerateCSRDialog(Dialog owner, String defaultDN) {
        super(owner, resources.getString("dialog.title"), true);

        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);

        sanTableModel = TableUtil.configureTable(sanTable,
                TableUtil.column(resources.getString("sanTable.type.column.name"), 100, 250, 99999, Functions.propertyTransform(NameValuePair.class, "key")),
                TableUtil.column(resources.getString("sanTable.type.column.name"), 100, 250, 99999, Functions.propertyTransform(NameValuePair.class, "value")));
        sanTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ListSelectionModel selectionModel = sanTable.getSelectionModel();
        selectionModel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableComponents();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        final SigHash defaultSignatureHash;
        Collection<SigHash> hashes = new ArrayList<>(Arrays.asList(
                defaultSignatureHash = new SigHash("Auto", null),
                new SigHash("SHA-1", "SHA1"),
                new SigHash("SHA-256", "SHA256"),
                new SigHash("SHA-384", "SHA384"),
                new SigHash("SHA-512", "SHA512")
        ));
        signatureHashComboBox.setModel(new DefaultComboBoxModel<>(hashes.toArray(new SigHash[hashes.size()])));
        signatureHashComboBox.setSelectedItem(defaultSignatureHash);

        dnTextField.setText(defaultDN);

        dnTextField.setCaretPosition(0);

        sanTableModel.setRows(new ArrayList<NameValuePair>(Collections.<NameValuePair>emptyList()));

        addSanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEditMappingDialog(resources.getString("addMapping.dialog.title"), new NameValuePair(), new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair nameValuePair) {
                        sanTableModel.addRow(nameValuePair);
                    }
                });
            }
        });

        editSanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int rowIndex = sanTable.getSelectedRow();
                final NameValuePair mapping = sanTableModel.getRowObject(rowIndex);
                if (mapping != null) showEditMappingDialog(resources.getString("editMapping.dialog.title"), mapping, new Functions.UnaryVoid<NameValuePair>() {
                    @Override
                    public void call(NameValuePair nameValuePair) {
                        sanTableModel.setRowObject(rowIndex, nameValuePair);
                    }
                });
            }
        });

        removeSanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NameValuePair row = sanTableModel.getRowObject(sanTable.getSelectedRow());

                Object[] options = {resources.getString("removeMapping.dialog.button.ok"), resources.getString("removeMapping.dialog.button.cancel")};
                int result = JOptionPane.showOptionDialog(GenerateCSRDialog.this, MessageFormat.format(resources.getString("removeMapping.message")  ,row.right),
                        resources.getString("removeMapping.dialog.title"), 0, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
                if (result == 0) {
                    sanTableModel.removeRowAt(sanTable.getSelectedRow());
                }
            }
        });

        enableDisableComponents();
    }

    private void onOK() {
        final String dnres = dnTextField.getText();
        //checks if the subject dn is valid.
        try {
            new X500Principal(dnres);
        } catch (IllegalArgumentException e) {
            DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("error.invalidSubject.message"),dnres),
                    resources.getString("error.invalidSubject.dialog.title"), JOptionPane.ERROR_MESSAGE, null);
            return;
        }

        selectedHash = ((SigHash) signatureHashComboBox.getSelectedItem()).algorithm;
        csrSubjectDN = dnres;

        List<NameValuePair> pairs = sanTableModel.getRows();

        for(NameValuePair pair : pairs) {
            try {
                X509GeneralName generalName = CertUtils.convertToX509GeneralName(pair);
                if(generalName != null) csrSAN.add(generalName);
            } catch (IllegalArgumentException e) {
                DialogDisplayer.showMessageDialog(this, MessageFormat.format(resources.getString("error.invalidSan.message"),pair.right),
                        resources.getString("error.invalidSan.dialog.title"), JOptionPane.ERROR_MESSAGE, null);
                return;
            }
        }

        okD = true;

        dispose();
    }

    private void onCancel() {
        okD = false;
        dispose();
    }

    /**
     * Returned true if the operation was ok'd.
     *
     * @return true if the operation was ok'd.
     */
    public boolean isOkD() {
        return okD;
    }

    /**
     * Returns the selected signature hash algorithm to use
     *
     * @return The signature hash to use
     */
    public String getSelectedHash() {
        return selectedHash;
    }

    /**
     * Returns the subject DN
     * @return The selected subject DN
     */
    public String getCsrSubjectDN() {
        return csrSubjectDN;
    }

    /**
     * Returns the subject alternative names
     * @return
     */
    public List<X509GeneralName> getCsrSAN() {
        return csrSAN;
    }

    private void showEditMappingDialog(final String title, NameValuePair initialValue, final Functions.UnaryVoid<NameValuePair> actionIfConfirmed) {
        final X509GeneralNamePanel panel = new X509GeneralNamePanel(initialValue);
        final OkCancelDialog<NameValuePair> dlg = new OkCancelDialog<NameValuePair>(this, title, true, panel);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    NameValuePair value = dlg.getValue();
                    if (value != null)
                        actionIfConfirmed.call(value);
                }
            }
        });
    }

    private void enableDisableComponents() {
        boolean sANnRecordSelected = sanTable.getSelectedRowCount() > 0;
        editSanButton.setEnabled(sANnRecordSelected);
        removeSanButton.setEnabled(sANnRecordSelected);
    }

    private static class SigHash {
        private final String label;
        private final String algorithm;

        private SigHash(String label, String algorithm) {
            this.label = label;
            this.algorithm = algorithm;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
