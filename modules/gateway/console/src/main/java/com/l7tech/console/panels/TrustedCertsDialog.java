package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Simple dialog that can be used to designate a subset of zero or more TrustedCert instances to work with in a particular context.
 */
public class TrustedCertsDialog extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTable certsTable;
    private JButton addButton;
    private JButton removeButton;
    private JRadioButton trustAllTrustedCertificatesRadioButton;
    private JRadioButton trustOnlyTheSpecifiedRadioButton;

    private SimpleTableModel<EntityHeader> certsTableModel;
    private List<EntityHeader> trustedCerts;
    private boolean confirmed = false;
    
    public TrustedCertsDialog(Window owner, String title, ModalityType modalityType, @Nullable Collection<EntityHeader> trustedCerts, boolean readOnly) {
        super(owner, title, modalityType);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        
        this.trustedCerts = trustedCerts == null ? null : new ArrayList<EntityHeader>(trustedCerts);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TrustedCertsDialog.this.trustedCerts = trustAllTrustedCertificatesRadioButton.isSelected() ? null : new ArrayList<EntityHeader>(certsTableModel.getRows()); 
                
                confirmed = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();                
            }
        });
        
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final CertSearchPanel sp = new CertSearchPanel(TrustedCertsDialog.this, false, false);
                sp.addCertListener(new CertListener() {
                    @Override
                    public void certSelected(CertEvent ce) {
                        TrustedCert cert = ce.getCert();
                        if (cert != null) {
                            EntityHeader header = new EntityHeader(cert.getOid(), EntityType.TRUSTED_CERT, cert.getName(), null);
                            if (certsTableModel.getRowIndex(header) >= 0) {
                                DialogDisplayer.showMessageDialog(TrustedCertsDialog.this, "The selected certificate is already present in the table.", "Certificate Already Added", JOptionPane.ERROR_MESSAGE, null);
                            } else {
                                certsTableModel.addRow(header);
                            }
                        }
                    }
                });
                sp.pack();
                Utilities.centerOnScreen(sp);
                DialogDisplayer.display(sp);
            }
        });
        
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                certsTableModel.removeRowAt(certsTable.getSelectedRow());
            }
        });
        
        RunOnChangeListener enableDisableListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {                
                enableOrDisableComponents();
            }
        });
        trustAllTrustedCertificatesRadioButton.addActionListener(enableDisableListener);
        trustOnlyTheSpecifiedRadioButton.addActionListener(enableDisableListener);

        certsTableModel = TableUtil.configureTable(certsTable, 
                TableUtil.column("Name", 10, 400, 99999, Functions.propertyTransform(EntityHeader.class, "name")));
        if (this.trustedCerts == null) {
            certsTableModel.setRows(Collections.<EntityHeader>emptyList());
            trustAllTrustedCertificatesRadioButton.setSelected(true);
            trustOnlyTheSpecifiedRadioButton.setSelected(false);
        } else {
            certsTableModel.setRows(this.trustedCerts);
            trustAllTrustedCertificatesRadioButton.setSelected(false);
            trustOnlyTheSpecifiedRadioButton.setSelected(true);
        }
        certsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton, addButton, removeButton});
        Utilities.enableGrayOnDisabled(addButton, removeButton, certsTable);

        okButton.setEnabled(!readOnly);
        
        enableOrDisableComponents();
    }

    private void enableOrDisableComponents() {        
        boolean custom = trustOnlyTheSpecifiedRadioButton.isSelected();
        Utilities.setEnabled(addButton, custom);
        Utilities.setEnabled(removeButton, custom);
        Utilities.setEnabled(certsTable, custom);
    }

    public List<EntityHeader> getTrustedCerts() {
        return trustedCerts;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Show a dialog that offers to (re)configure a set of trusted certs.
     *
     * @param owner the owner dialog.  required.
     * @param title the title, or null to use a default.
     * @param modalityType the modality type, or null to use the system default for modal dialogs.
     * @param readOnly if true, the OK button will never be enabled.
     * @param trustedCerts the initial trusted cert config, with null meaning to use all of the system trusted certs.
     * @param confirmCallback a confirmation callback that will be invoked only if the dialog is confirmed.
     *                        Its argument will be the new trusted cert configuration, with null meaning to use all the system trusted certs.
     */
    public static void show(Window owner, String title, ModalityType modalityType, boolean readOnly, @Nullable Collection<EntityHeader> trustedCerts, final Functions.UnaryVoid<List<EntityHeader>> confirmCallback) {
        if (title == null)
            title = "Trusted Certificate Configuration";
        if (modalityType == null)
            modalityType = JDialog.DEFAULT_MODALITY_TYPE;
        final TrustedCertsDialog dlg = new TrustedCertsDialog(owner, title, modalityType, trustedCerts, readOnly);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed() && confirmCallback != null) {
                    confirmCallback.call(dlg.getTrustedCerts());
                }
            }
        });
    }
}
