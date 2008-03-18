/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.ncesval.console;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.TargetMessagePanel;
import com.l7tech.common.security.CertificateValidationType;
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.Resolver;
import com.l7tech.common.util.ResolvingComparator;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.ncesval.NcesValidatorAssertion;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.CertificateInfo;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** @author alex */
public class NcesValidatorAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<NcesValidatorAssertion> {

    //- PUBLIC

    public NcesValidatorAssertionPropertiesDialog(Dialog owner) {
        super(owner, "NCES Validator Properties", true);
        initialize();
    }

    public NcesValidatorAssertionPropertiesDialog(Frame owner) {
        super(owner, "NCES Validator Properties", true);
        initialize();
    }

    public boolean isConfirmed() {
        return ok;
    }

    public void setData(NcesValidatorAssertion assertion) {
        this.assertion = assertion;
        targetMessagePanel.setModel(assertion);
        samlCheckbox.setSelected(assertion.isSamlRequired());
        validationOptionComboBox.setSelectedItem(assertion.getCertificateValidationType());
        try {
            trustedCertificates.clear();
            populateTrustedCerts(trustedCertificates, assertion.getTrustedCertificateInfo());
        } catch (FindException fe) {
            logger.log( Level.WARNING, "Error loading trusted certificates", fe );
        }
        try {
            trustedIssuerCertificates.clear();
            populateTrustedCerts(trustedIssuerCertificates, assertion.getTrustedIssuerCertificateInfo());
        } catch (FindException fe) {
            logger.log( Level.WARNING, "Error loading trusted certificates", fe );
        }
        sortTrustedCerts();
    }

    public NcesValidatorAssertion getData(NcesValidatorAssertion assertion) {
        targetMessagePanel.updateModel(assertion);
        assertion.setSamlRequired(samlCheckbox.isSelected());
        assertion.setCertificateValidationType((CertificateValidationType) validationOptionComboBox.getSelectedItem());
        assertion.setTrustedCertificateInfo(getTrustedSigners(trustedCertificates));
        assertion.setTrustedIssuerCertificateInfo(getTrustedSigners(trustedIssuerCertificates));

        return assertion;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(NcesValidatorAssertionPropertiesDialog.class.getName());

    private static final String RES_VALTYPE_PREFIX = "validation.option.";
    private static final String RES_VALTYPE_DEFAULT = "default";

    private JCheckBox samlCheckbox;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;

    private TargetMessagePanel targetMessagePanel;
    private JComboBox validationOptionComboBox;

    private JTable trustedCertsTable;
    private JButton addCertButton;
    private JButton removeCertButton;
    private JButton propertiesCertButton;
    private JButton createCertButton;

    private JTable trustedCertIssuersTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton createButton;

    private volatile boolean ok = false;
    private NcesValidatorAssertion assertion;
    private Collection<RevocationCheckPolicy> policies;
    private final java.util.List<TrustedCert> trustedCertificates = new ArrayList<TrustedCert>();
    private final java.util.List<TrustedCert> trustedIssuerCertificates = new ArrayList<TrustedCert>();

    private void initialize() {
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = true;
                targetMessagePanel.updateModel(assertion);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        targetMessagePanel.addPropertyChangeListener("valid", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                okButton.setEnabled(Boolean.TRUE.equals(evt.getNewValue()));
            }
        });

        addCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onAdd(true);
            }
        });

        removeCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onRemove(true);
            }
        });

        propertiesCertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onProperties(true);
            }
        });

        createCertButton.setAction(new NewTrustedCertificateAction(new CertListener(){
            public void certSelected( CertEvent ce) {
                addTrustedCert(true, ce.getCert());
            }
        }, createButton.getText()));

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onAdd(false);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onRemove(false);
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onProperties(false);
            }
        });

        createButton.setAction(new NewTrustedCertificateAction(new CertListener(){
            public void certSelected( CertEvent ce) {
                addTrustedCert(false, ce.getCert());
            }
        }, createButton.getText()));

        DefaultComboBoxModel model = new DefaultComboBoxModel(
            new Object[]{null, CertificateValidationType.PATH_VALIDATION, CertificateValidationType.REVOCATION}
        );
        validationOptionComboBox.setModel(model);
        validationOptionComboBox.setRenderer(new CertificateValidationTypeRenderer());

        trustedCertsTable.setModel(new TrustedCertTableModel(trustedCertificates));
        trustedCertsTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        Utilities.setDoubleClickAction( trustedCertsTable, propertiesCertButton);

        trustedCertIssuersTable.setModel(new TrustedCertTableModel(trustedIssuerCertificates));
        trustedCertIssuersTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        Utilities.setDoubleClickAction( trustedCertIssuersTable, propertiesButton);

        add(mainPanel);
    }

    /**
     * Add a trusted cert to the list
     */
    private void onAdd(final boolean isCertificate) {
        CertSearchPanel sp = new CertSearchPanel(this);
        sp.addCertListener(new CertListener(){
            public void certSelected(CertEvent ce) {
                addTrustedCert(isCertificate, ce.getCert());
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    /**
     * Remove the selected trusted cert from the list
     */
    private void onRemove(final boolean isCertificate) {
        JTable certTable;
        java.util.List<TrustedCert> certificates;
        if ( isCertificate ) {
            certTable = trustedCertsTable;
            certificates = trustedCertificates;
        } else {
            certTable = trustedCertIssuersTable;
            certificates = trustedIssuerCertificates;
        }

        int row = certTable.getSelectedRow();
        if (row >= 0) {
            certificates.remove(row);
            ((AbstractTableModel) certTable.getModel()).fireTableDataChanged();
        }
    }

    /**
     * View properties of the selected trusted cert
     */
    private void onProperties(final boolean isCertificate) {
        JTable certTable;
        java.util.List<TrustedCert> certificates;
        if ( isCertificate ) {
            certTable = trustedCertsTable;
            certificates = trustedCertificates;
        } else {
            certTable = trustedCertIssuersTable;
            certificates = trustedIssuerCertificates;
        }

        int row = certTable.getSelectedRow();
        if (row >= 0) {
            CertPropertiesWindow cpw = new CertPropertiesWindow(this, certificates.get(row), false, getRevocationCheckPolicies());
            DialogDisplayer.display(cpw);
        }
    }

    private Collection<RevocationCheckPolicy> getRevocationCheckPolicies() {
        if ( policies == null ) {
            try {
                policies = getTrustedCertAdmin().findAllRevocationCheckPolicies();
            } catch (FindException fe) {
                logger.log( Level.WARNING, "Error loading revocation check policies", fe);
            }
        }
        return policies;
    }

    /**
     * Get a list of oids from a list of trusted certs
     */
    private CertificateInfo[] getTrustedSigners( java.util.List<TrustedCert> trustedCerts) {
        java.util.List<CertificateInfo> signers = new ArrayList<CertificateInfo>();

        for ( TrustedCert cert : trustedCerts ) {
            try {
                signers.add( new CertificateInfo(cert.getCertificate()) );
            } catch ( CertificateException ce ) {
                logger.log( Level.WARNING, "Error processing trusted certificate '"+cert.getName()+"'.", ce );
            }
        }

        return signers.toArray( new CertificateInfo[signers.size()] );
    }

    /**
     * Add a trusted cert to the table.
     */
    private void addTrustedCert( final boolean isCertificate, final TrustedCert trustedCert ) {
        JTable certTable;
        java.util.List<TrustedCert> certificates;
        if ( isCertificate ) {
            certTable = trustedCertsTable;
            certificates = trustedCertificates;
        } else {
            certTable = trustedCertIssuersTable;
            certificates = trustedIssuerCertificates;
        }

        if ( !isTrusted( getTrustedSigners(certificates), trustedCert) ) {
            certificates.add(trustedCert);
            sortTrustedCerts();
            ((AbstractTableModel) certTable.getModel()).fireTableDataChanged();
        }
    }

    /**
     * Sort trusted certs by name
     */
    @SuppressWarnings( { "unchecked" } )
    private void sortTrustedCerts() {
        Comparator comparator = new ResolvingComparator(new Resolver<TrustedCert, String>(){
            public String resolve(TrustedCert key) {
                String name = key.getName();
                if (name == null)
                    name = "";
                return name.toLowerCase();
            }
        }, false);

        Collections.sort(trustedCertificates, comparator);
        Collections.sort(trustedIssuerCertificates, comparator);
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /**
     * Populate the given list of trusted certs using the given list of oids
     */
    private void populateTrustedCerts( java.util.List<TrustedCert> certs, CertificateInfo[] certificateInfos ) throws FindException {
        if ( certificateInfos != null ) {
            TrustedCertAdmin tca = getTrustedCertAdmin();

            if ( certificateInfos.length < 10 ) {
                for ( CertificateInfo info : certificateInfos ) {
                    if ( info != null ) {
                        TrustedCert cert = tca.findCertBySubjectDn( info.getSubjectDn() );
                        if ( cert != null ) {
                            certs.add(cert);
                        }
                    }
                }
            } else {
                java.util.List<TrustedCert> trustedCertificates = tca.findAllCerts();
                for ( TrustedCert cert : trustedCertificates ) {
                    if ( isTrusted(certificateInfos, cert) ) {
                        certs.add(cert);
                    }
                }
            }
        }
    }

    /**
     * Is the given trustedCert one of the given certificateInfos
     */
    private boolean isTrusted( final CertificateInfo[] certificateInfos, final TrustedCert trustedCert ) {
        boolean trusted = false;

        for ( CertificateInfo info : certificateInfos ) {
            if ( info != null && info.getSubjectDn().equals( trustedCert.getSubjectDn() )) {
                trusted = true;
                break;
            }
        }

        return trusted;
    }

    /**
     * TableModel for trusted certs backed by a list.
     */
    private static final class TrustedCertTableModel extends AbstractTableModel {
        private static final String[] columnNames = new String[]{
                "Name",
                "Issued By",
                "Expiration Date"
        };
        private final java.util.List<TrustedCert> trustedCertificates;

        private TrustedCertTableModel( java.util.List<TrustedCert> trustedCertificates) {
            this.trustedCertificates = trustedCertificates;
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return trustedCertificates.size();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            TrustedCert trustedCert = trustedCertificates.get(rowIndex);
            Object value = null;

            try {
                switch ( columnIndex ) {
                    case 0:
                        value = trustedCert.getName();
                        break;
                    case 1:
                        value = CertUtils.extractIssuerNameFromClientCertificate(trustedCert.getCertificate());
                        break;
                    case 2:
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                        value = sdf.format(trustedCert.getCertificate().getNotAfter());
                        break;
                }
            } catch ( CertificateException ce) {
                value = "";
            }

            return value;
        }
    }

    /**
     * Renderer for CertificateValidationType
     */
    private final class CertificateValidationTypeRenderer extends JLabel implements ListCellRenderer {
        private Map<String,String> names = new HashMap<String,String>();

        public CertificateValidationTypeRenderer() {
            names.put("validation.option.CERTIFICATE_ONLY","Validate Certificate Path");
            names.put("validation.option.PATH_VALIDATION","Validate Certificate Path");
            names.put("validation.option.REVOCATION","Revocation Checking");
            names.put("validation.option.default","Use Default");
        }

        public Component getListCellRendererComponent( JList list,
                                                       Object value,
                                                       int index,
                                                       boolean isSelected,
                                                       boolean cellHasFocus)
        {
            CertificateValidationType type = (CertificateValidationType) value;

            String labelKey = RES_VALTYPE_PREFIX + RES_VALTYPE_DEFAULT;
            if (type != null) {
                labelKey = RES_VALTYPE_PREFIX + type.name();
            }

            setText(names.get(labelKey));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
                setOpaque(true);
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
                setOpaque(false);
            }

            setEnabled(list.isEnabled());
            setFont(list.getFont());

            return this;
        }
    }

    public static void main(String[] args) {
        NcesValidatorAssertionPropertiesDialog dlg = new NcesValidatorAssertionPropertiesDialog(new JFrame());
        dlg.setData(new NcesValidatorAssertion());
        dlg.pack();
        dlg.setVisible(true);
    }
}
