package com.l7tech.console.panels;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Dialog;
import java.awt.Frame;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Locale;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.security.cert.CertificateException;
import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;

import com.l7tech.common.security.RevocationCheckPolicyItem;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.RevocationCheckPolicy;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.MaxLengthDocument;
import com.l7tech.common.gui.widgets.TextListCellRenderer;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ValidationUtils;
import com.l7tech.common.util.ResolvingComparator;
import com.l7tech.common.util.Resolver;
import com.l7tech.common.util.Functions;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

/**
 * Properties dialog for Revocation Checking Policy Items.
 *
 * @author Steve Jones
 */
public class RevocationCheckPolicyItemPropertiesDialog extends JDialog {

    //- PUBLIC

    /**
     * Create a new RevocationCheckPolicyItemPropertiesDialog to edit the given item.
     *
     * @param parent The parent Dialog
     * @param readOnly True if this dialog is read-only
     * @param revocationCheckPolicyItem The item to edit
     * @param policies Collection of all current RevocationCheckPolicies (used when viewing trusted certs, may be null)
     */
    public RevocationCheckPolicyItemPropertiesDialog(Dialog parent,
                                                     boolean readOnly,
                                                     RevocationCheckPolicyItem revocationCheckPolicyItem,
                                                     Collection<RevocationCheckPolicy> policies) {
        super(parent, resources.getString(RES_TITLE), true);
        this.readOnly = readOnly;
        this.revocationCheckPolicyItem = revocationCheckPolicyItem;
        this.trustedCertificates = new ArrayList();
        this.policies = policies;
        init();
    }

    /**
     * Create a new RevocationCheckPolicyItemPropertiesDialog to edit the given item.
     *
     * @param parent The parent Frame
     * @param readOnly True if this dialog is read-only
     * @param revocationCheckPolicyItem The item to edit
     * @param policies Collection of all current RevocationCheckPolicies (used when viewing trusted certs, may be null)
     */
    public RevocationCheckPolicyItemPropertiesDialog(Frame parent,
                                                     boolean readOnly,
                                                     RevocationCheckPolicyItem revocationCheckPolicyItem,
                                                     Collection<RevocationCheckPolicy> policies) {
        super(parent, resources.getString(RES_TITLE), true);
        this.readOnly = readOnly;
        this.revocationCheckPolicyItem = revocationCheckPolicyItem;
        this.trustedCertificates = new ArrayList();
        this.policies = policies;
        init();
    }

    /**
     * Did this dialog exit successfully?
     *
     * @return True if the dialog was exited with 'OK'
     */
    public boolean wasOk() {
        return wasOk;
    }

    /**
     * Show/hide the dialog.
     *
     * <p>Note that this will do nothing if the dialog could not be initialized.</p>
     *
     * @param visible True to show
     */
    public void setVisible(boolean visible) {
        if (!visible || valid)
            super.setVisible(visible);
    }

    //- PRIVATE

    /**
     * Resource bundle for this dialog
     */
    private static final ResourceBundle resources =
            ResourceBundle.getBundle("com.l7tech.console.resources.RevocationCheckPolicyItemPropertiesDialog", Locale.getDefault());

    /**
     * Resource bundle keys
     */
    private static final String RES_TITLE = "dialog.title";
    private static final String RES_ITEM_CRLFROMCERT = "item.crlfromcert.text";
    private static final String RES_ITEM_CRLFROMURL = "item.crlfromurl.text";
    private static final String RES_ITEM_OCSPFROMCERT = "item.ocspfromcert.text";
    private static final String RES_ITEM_OCSPFROMURL = "item.ocspfromurl.text";    
    private static final String RES_TABLE_COLUMN_NAME = "table.column.name";
    private static final String RES_TABLE_COLUMN_ISSUEDBY = "table.column.issuedby";
    private static final String RES_TABLE_COLUMN_EXPIRES = "table.column.expires";
    private static final String RES_VALIDATE_URL_TEXT = "validate.url.text";
    private static final String RES_VALIDATE_URL_TITLE = "validate.url.title";
    private static final String RES_VALIDATE_URLREGEX_TEXT = "validate.urlregex.text";
    private static final String RES_VALIDATE_URLREGEX_TITLE = "validate.urlregex.title";
    private static final String RES_ERROR_LOAD_TEXT = "error.load.text";
    private static final String RES_ERROR_LOAD_TITLE = "error.load.title";

    /**
     * Maximum allowed number of trusted certificates 
     */
    private static final int MAXIMUM_ITEMS = 20;

    /**
     * Permitted schemes for URLs
     */
    private static final Collection<String> URL_SCHEMES_CRL = Arrays.asList("http", "https", "ldap", "ldaps");
    private static final Collection<String> URL_SCHEMES_OCSP = Arrays.asList("http", "https");

    private final Collection<RevocationCheckPolicy> policies;
    private final RevocationCheckPolicyItem revocationCheckPolicyItem;
    private final List<TrustedCert> trustedCertificates;
    private final boolean readOnly;
    private boolean wasOk;
    private boolean valid;

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField urlTextField;
    private JTable trustedCertsTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton createButton;
    private JCheckBox allowIssuerSignatureCheckBox;
    private JComboBox typeComboBox;
    private JTextField urlRegexTextField;

    /**
     * Initialize the dialog
     */
    private void init() {
        initUI();
        try {
            modelToUI();
            valid = true;
        } catch (FindException fe) {
            JOptionPane.showMessageDialog(getParent(),
                    resources.getString(RES_ERROR_LOAD_TEXT),
                    resources.getString(RES_ERROR_LOAD_TITLE),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Initialize the dialog UI
     */
    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);
        setContentPane(contentPane);

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onAdd();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onRemove();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                onProperties();
            }
        });

        createButton.setAction(new NewTrustedCertificateAction(new CertListener(){
            public void certSelected(CertEvent ce) {
                addTrustedCert(ce.getCert());
            }
        }, createButton.getText()));

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        typeComboBox.setModel(new DefaultComboBoxModel(RevocationCheckPolicyItem.Type.values()));
        typeComboBox.setRenderer(new TextListCellRenderer(new RevocationCheckPolicyItemAccessor()));
        typeComboBox.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                enableOrDisableControls();
            }
        });
        urlTextField.setDocument(new MaxLengthDocument(255));
        urlRegexTextField.setDocument(new MaxLengthDocument(255));
        
        trustedCertsTable.setModel(new TrustedCertTableModel(trustedCertificates));
        trustedCertsTable.getTableHeader().setReorderingAllowed(false);
        trustedCertsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trustedCertsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableControls();
            }
        });
        Utilities.setDoubleClickAction(trustedCertsTable, propertiesButton);        

        enableOrDisableControls();
        pack();
        Utilities.centerOnParentWindow(this);
    }

    /**
     * Enable/disable controls based on permissions and selections
     */
    private void enableOrDisableControls() {
        boolean enableControls = !readOnly;

        // disable if readonly
        typeComboBox.setEnabled(enableControls);
        urlTextField.setEnabled(enableControls);
        urlRegexTextField.setEnabled(enableControls);
        allowIssuerSignatureCheckBox.setEnabled(enableControls);
        addButton.setEnabled(enableControls);
        removeButton.setEnabled(enableControls);
        createButton.setEnabled(enableControls);
        okButton.setEnabled(enableControls);

        boolean contextualEnabled = false;

        int row = trustedCertsTable.getSelectedRow();
        if ( row >= 0 ) {
            contextualEnabled = true;
        }

        addButton.setEnabled(enableControls && trustedCertsTable.getRowCount() < MAXIMUM_ITEMS);
        createButton.setEnabled(enableControls && trustedCertsTable.getRowCount() < MAXIMUM_ITEMS);
        removeButton.setEnabled(enableControls && contextualEnabled);
        propertiesButton.setEnabled(contextualEnabled);

        RevocationCheckPolicyItem.Type selectedType = (RevocationCheckPolicyItem.Type) typeComboBox.getSelectedItem();
        if( selectedType==null || selectedType.isUrlSpecified() ) {
            urlTextField.setEnabled(enableControls);
            urlRegexTextField.setEnabled(false);
        } else {
            urlTextField.setEnabled(false);
            urlRegexTextField.setEnabled(enableControls);
        }
    }

    /**
     * Add a trusted cert to the list
     */
    private void onAdd() {
        CertSearchPanel sp = new CertSearchPanel(this);
        sp.addCertListener(new CertListener(){
            public void certSelected(CertEvent ce) {
                addTrustedCert(ce.getCert());
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    /**
     * Remove the selected trusted cert from the list
     */
    private void onRemove() {
        int row = trustedCertsTable.getSelectedRow();
        if (row >= 0) {
            trustedCertificates.remove(row);
            ((AbstractTableModel)trustedCertsTable.getModel()).fireTableDataChanged();
        }
    }

    /**
     * View properties of the selected trusted cert
     */
    private void onProperties() {
        int row = trustedCertsTable.getSelectedRow();
        if (row >= 0) {
            CertPropertiesWindow cpw = new CertPropertiesWindow(this, trustedCertificates.get(row), false, policies);
            DialogDisplayer.display(cpw);
        }
    }

    /**
     * Handle OK
     */
    private void onOK() {
        if (isItemValid()) {
            wasOk = true;
            uiToModel();
            dispose();
        }
    }

    /**
     * Ensure that the URL or URL Regex is valid (depending on type)
     */
    private boolean isItemValid() {
        boolean valid = false;

        RevocationCheckPolicyItem.Type type = (RevocationCheckPolicyItem.Type) typeComboBox.getSelectedItem();
        if( type != null ) {
            if ( type.isUrlSpecified() ) {
                String url = urlTextField.getText();
                Collection<String> schemes = URL_SCHEMES_CRL;
                if (type.equals(RevocationCheckPolicyItem.Type.OCSP_FROM_URL)) {
                    schemes = URL_SCHEMES_OCSP;
                }
                if ( ValidationUtils.isValidUrl(url, false, schemes) ) {
                    valid = true;
                } else {
                    JOptionPane.showMessageDialog(this,
                            resources.getString(RES_VALIDATE_URL_TEXT),
                            resources.getString(RES_VALIDATE_URL_TITLE),
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                boolean validRegex = false;
                String errorDetail = "";
                try {
                     Pattern.compile(urlRegexTextField.getText());
                    validRegex = true;
                } catch (PatternSyntaxException pse) {
                    errorDetail = pse.getMessage();
                }
                if ( validRegex ) {
                    valid = true;
                } else {
                    JOptionPane.showMessageDialog(this,
                            MessageFormat.format(resources.getString(RES_VALIDATE_URLREGEX_TEXT), errorDetail),
                            resources.getString(RES_VALIDATE_URLREGEX_TITLE),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        return valid;
    }

    /**
     * Add a trusted cert to the table.
     */
    private void addTrustedCert(TrustedCert trustedCert) {
        if ( trustedCertificates.size() < MAXIMUM_ITEMS &&
             !getTrustedSigners(trustedCertificates).contains(trustedCert.getOid()) ) {
            trustedCertificates.add(trustedCert);
            sortTrustedCerts();
            ((AbstractTableModel)trustedCertsTable.getModel()).fireTableDataChanged();
        }
    }

    /**
     * Sort trusted certs by name
     */
    private void sortTrustedCerts() {
        Collections.sort(trustedCertificates, new ResolvingComparator(new Resolver<TrustedCert, String>(){
            public String resolve(TrustedCert key) {
                String name = key.getName();
                if (name == null)
                    name = "";
                return name.toLowerCase();
            }
        }, false));        
    }

    /**
     * Update the UI from the model data
     */
    private void modelToUI() throws FindException {
        if (revocationCheckPolicyItem.getType() != null) {
            typeComboBox.setSelectedItem(revocationCheckPolicyItem.getType());
        } else {
            typeComboBox.setSelectedItem(RevocationCheckPolicyItem.Type.CRL_FROM_CERTIFICATE);
        }
        if ( revocationCheckPolicyItem.getType()==null || revocationCheckPolicyItem.getType().isUrlSpecified() ) {
            urlTextField.setText(revocationCheckPolicyItem.getUrl());
            urlRegexTextField.setText(".*");
        } else {
            urlTextField.setText("");
            urlRegexTextField.setText(revocationCheckPolicyItem.getUrl());
        }
        allowIssuerSignatureCheckBox.setSelected(revocationCheckPolicyItem.isAllowIssuerSignature());
        populateTrustedCerts(trustedCertificates, revocationCheckPolicyItem.getTrustedSigners());
        sortTrustedCerts();
    }

    /**
     * Update the model data from the UI 
     */
    private void uiToModel() {
        revocationCheckPolicyItem.setType((RevocationCheckPolicyItem.Type)typeComboBox.getSelectedItem());
        if ( revocationCheckPolicyItem.getType().isUrlSpecified() ) {
            revocationCheckPolicyItem.setUrl(urlTextField.getText());
        } else {
            revocationCheckPolicyItem.setUrl(urlRegexTextField.getText());
        }
        revocationCheckPolicyItem.setAllowIssuerSignature(allowIssuerSignatureCheckBox.isSelected());
        revocationCheckPolicyItem.setTrustedSigners(getTrustedSigners(trustedCertificates));
    }

    /**
     * Populate the given list of trusted certs using the given list of oids
     */
    private void populateTrustedCerts(List<TrustedCert> certs, List<Long> oids) throws FindException {
        TrustedCertAdmin tca = getTrustedCertAdmin();

        if ( oids.size() < 10 ) {
            for ( Long oid : oids ) {
                TrustedCert cert = tca.findCertByPrimaryKey(oid);
                if ( cert != null ) {
                    certs.add(cert);
                }
            }
        } else {
            List<TrustedCert> trustedCertificates = tca.findAllCerts();
            for ( TrustedCert cert : trustedCertificates ) {
                if ( oids.contains(cert.getOid()) ) {
                    certs.add(cert);
                }
            }
        }
    }

    /**
     * Get a list of oids from a list of trusted certs 
     */
    private List<Long> getTrustedSigners(List<TrustedCert> trustedCerts) {
        List<Long> signers = new ArrayList();

        for ( TrustedCert cert : trustedCerts ) {
            signers.add(cert.getOid());
        }
        
        return signers;
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
     * TableModel for trusted certs backed by a list.
     */
    private static final class TrustedCertTableModel extends AbstractTableModel {
        private static final String[] columnNames = new String[]{
                resources.getString(RES_TABLE_COLUMN_NAME),
                resources.getString(RES_TABLE_COLUMN_ISSUEDBY),
                resources.getString(RES_TABLE_COLUMN_EXPIRES)
        };
        private final List<TrustedCert> trustedCertificates;

        private TrustedCertTableModel(List<TrustedCert> trustedCertificates) {
            this.trustedCertificates = trustedCertificates;    
        }

        public int getColumnCount() {
            return 3;
        }

        public int getRowCount() {
            return trustedCertificates.size();
        }

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
            } catch (CertificateException ce) {
                value = "";
            }

            return value;
        }
    }

    /**
     * Label text accessor for RevocationCheckPolicyItem.Types
     */
    private static final class RevocationCheckPolicyItemAccessor implements Functions.Unary<String, Object> {
        public String call(Object value) {
            RevocationCheckPolicyItem.Type type = (RevocationCheckPolicyItem.Type) value;

            String labelKey = RES_ITEM_CRLFROMCERT;
            switch(type) {
                case CRL_FROM_CERTIFICATE:
                    labelKey = RES_ITEM_CRLFROMCERT;
                    break;
                case CRL_FROM_URL:
                    labelKey = RES_ITEM_CRLFROMURL;
                    break;
                case OCSP_FROM_CERTIFICATE:
                    labelKey = RES_ITEM_OCSPFROMCERT;
                    break;
                case OCSP_FROM_URL:
                    labelKey = RES_ITEM_OCSPFROMURL;
                    break;
            }

            return resources.getString(labelKey);
        }
    }
}
