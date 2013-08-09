package com.l7tech.console.panels;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    @Override
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
    private static final String RES_VALIDATE_URL_TEXT = "validate.url.text";
    private static final String RES_VALIDATE_URL_TITLE = "validate.url.title";
    private static final String RES_VALIDATE_URLREGEX_TEXT = "validate.urlregex.text";
    private static final String RES_VALIDATE_URLREGEX_TITLE = "validate.urlregex.title";
    private static final String RES_VALIDATE_SIGNER_TEXT = "validate.signer.text";
    private static final String RES_VALIDATE_SIGNER_TITLE = "validate.signer.title";

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
    private final boolean readOnly;
    private boolean wasOk;
    private boolean valid = true;

    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField urlTextField;
    private JCheckBox allowIssuerSignatureCheckBox;
    private JComboBox typeComboBox;
    private JTextField urlRegexTextField;
    private JPanel certPanel;
    private TrustedCertsPanel trustedCertsPanel;

    /**
     * Initialize the dialog
     */
    private void init() {
        initUI();
        modelToUI();
    }

    /**
     * Initialize the dialog UI
     */
    private void initUI() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);
        setContentPane(contentPane);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{okButton, cancelButton});

        typeComboBox.setModel(new DefaultComboBoxModel(RevocationCheckPolicyItem.Type.values()));
        typeComboBox.setRenderer(new TextListCellRenderer<RevocationCheckPolicyItem.Type>(new RevocationCheckPolicyItemAccessor()));
        typeComboBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableControls();
            }
        });
        urlTextField.setDocument(new MaxLengthDocument(255));
        urlRegexTextField.setDocument(new MaxLengthDocument(255));


        trustedCertsPanel = new TrustedCertsPanel( readOnly, MAXIMUM_ITEMS, new TrustedCertsPanel.TrustedCertListenerSupport(this){
            @Override
            public void notifyError() {
                valid = false;
            }
        }, policies );
        certPanel.add( trustedCertsPanel, BorderLayout.CENTER );

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
        okButton.setEnabled(enableControls);

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
            if ( !allowIssuerSignatureCheckBox.isSelected() && trustedCertsPanel.getCertificateGoids().length==0 ) {
                JOptionPane.showMessageDialog(this,
                        resources.getString(RES_VALIDATE_SIGNER_TEXT),
                        resources.getString(RES_VALIDATE_SIGNER_TITLE),
                        JOptionPane.ERROR_MESSAGE);
            } else if ( type.isUrlSpecified() ) {
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
     * Update the UI from the model data
     */
    private void modelToUI() {
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
        List<Goid> signers = revocationCheckPolicyItem.getTrustedSigners();
        trustedCertsPanel.setCertificateGoids(signers.toArray(new Goid[signers.size()]));
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
        revocationCheckPolicyItem.setTrustedSigners(Arrays.<Goid>asList(trustedCertsPanel.getCertificateGoids()));
    }

    /**
     * Label text accessor for RevocationCheckPolicyItem.Types
     */
    private static final class RevocationCheckPolicyItemAccessor implements Functions.Unary<String, RevocationCheckPolicyItem.Type> {
        @Override
        public String call( final RevocationCheckPolicyItem.Type type ) {
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
