package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The dialog displays the contents of a CSR and allows user to set/modify some settings of signing certificate.
 *
 * @author ghuang
 */
public class SigningCertificatePropertiesDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.SigningCertificatePropertiesDialog");
    private static final Logger logger = Logger.getLogger(SigningCertificatePropertiesDialog.class.getName());
    private static final String CLUSTER_PROP_PKIX_CSR_DEFAULT_EXPIRY_AGE = "pkix.csr.defaultExpiryAge";
    private static final int MIN_CERTIFICATE_EXPIRY = 1;
    private static final int MAX_CERTIFICATE_EXPIRY = 100000;
    private static final String[] HASH_FUNC_LIST = new String[] {
        "Automatic",
        "SHA-1",
        "SHA-256",
        "SHA-384",
        "SHA-512"
    };

    //a set of supported Subject Alternative Name types
    private static final Set<String> SUPPORTED_SAN_TYPES = new HashSet<>();

    static {
        try {
            String[] types = resources.getString("san.supported.types").split(",");
            Arrays.stream(types).forEach(x -> SUPPORTED_SAN_TYPES.add(x.trim()));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to retrieve Subject Alternative Name upported types.");
        }
    }


    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField subjectDnTextField;
    private JTextField expiryAgeTextField;
    private JComboBox hashAlgComboBox;
    private JButton fullDetailsButton;
    private JTextField publicKeyDetailsTextField;
    private JTable sansTable;
    private JLabel sansLabel;
    private JScrollPane sansScrollPane;
    private JPanel settingsPane;

    private Functions.Nullary<Boolean> precheckingShortKeyFunc;
    private Functions.Nullary<Void> postTaskFunc;
    private String publicKeyDetails;

    private SimpleTableModel<NameValuePair> sanTableModel;
    /**
     * The constructor will be given the contents of the CSR.
     * @param owner parent of this dialog
     * @param csrProps CSR properties
     * @param precheckingShortKeyFunc: the function to check if the CA key is a short key for signature alorightm
     */
    public SigningCertificatePropertiesDialog(Frame owner, Map<String, Object> csrProps, Functions.Nullary<Boolean> precheckingShortKeyFunc) {
        super(owner, resources.getString("dialog.title"));
        this.precheckingShortKeyFunc = precheckingShortKeyFunc;
        initialize();
        modelToView(csrProps);
        DialogDisplayer.pack(this);
    }

    public void setPostTaskFunc(Functions.Nullary<Void> postTaskFunc) {
        this.postTaskFunc = postTaskFunc;
    }

    public String getSubjectDn() {
        String dn = subjectDnTextField.getText();
        return dn == null? "" : dn.trim();
    }

    public int getExpiryAge() {
        return Integer.parseInt(expiryAgeTextField.getText());
    }

    public String getHashAlg() {
        String hashAlg = (String) hashAlgComboBox.getSelectedItem();

        // Convert SHA-XXX to SHAXXX
        if (hashAlg != null && hashAlg.startsWith("SHA-")) {
            hashAlg = "SHA" + hashAlg.substring(4);
        }
        return hashAlg;
    }

    private void initialize() {
        // Initialize GUI components
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(cancelButton);
        Utilities.setEscKeyStrokeDisposes(this);

        sanTableModel = TableUtil.configureTable(sansTable,
                TableUtil.column(resources.getString("sanTable.type.column.name"), 100, 100, 99999, Functions.propertyTransform(NameValuePair.class, "key")),
                TableUtil.column(resources.getString("sanTable.name.column.name"), 100, 250, 99999, Functions.propertyTransform(NameValuePair.class, "value")));
        sanTableModel.setRows(new ArrayList<NameValuePair>(Collections.<NameValuePair>emptyList()));
        sansTable.setEnabled(false);
        sansLabel.setVisible(false);
        sansScrollPane.setVisible(false);

        final InputValidator inputValidator = new InputValidator(this, resources.getString("error.dialog.title"));
        inputValidator.constrainTextFieldToNumberRange(resources.getString("text.cert.expiry.age"), expiryAgeTextField, MIN_CERTIFICATE_EXPIRY, MAX_CERTIFICATE_EXPIRY);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validatePropertiesAndExecutePostTask();
            }
        });

        expiryAgeTextField.setText(getDefaultExpiryAge());

        hashAlgComboBox.setModel(new DefaultComboBoxModel(HASH_FUNC_LIST));

        fullDetailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PublicKeyDetailsDialog detailsDialog = new PublicKeyDetailsDialog(publicKeyDetails);
                Utilities.centerOnScreen(detailsDialog);
                DialogDisplayer.display(detailsDialog);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void validatePropertiesAndExecutePostTask() {
        // Check Subject DN
        final String subjectDn = getSubjectDn();
        try {
            if (subjectDn.isEmpty()) throw new IllegalArgumentException(resources.getString("error.message.empty.subject.dn"));
            new X500Principal(subjectDn);
        } catch (IllegalArgumentException e) {
            showErrorMessage(
                resources.getString("error.dialog.title"),
                MessageFormat.format(resources.getString("error.message.invalid.subject.dn"), ExceptionUtils.getMessage(e)));
            return;
        }

        // The CA Key is short or not
        final String hashAlg = getHashAlg();
        if (precheckingShortKeyFunc.call() && hashAlg != null && !hashAlg.startsWith("SHA1") && !hashAlg.equals("Automatic")) {
            final String[] options = {"Yes", "No"};
            DialogDisplayer.showOptionDialog(
                SigningCertificatePropertiesDialog.this,
                MessageFormat.format(resources.getString("short.key.warning.message"), hashAlgComboBox.getSelectedItem()),
                resources.getString("warning.dialog.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[1],
                new  DialogDisplayer.OptionListener(){
                    @Override
                    public void reportResult( final int option ) {
                        if (option == JOptionPane.YES_OPTION) {
                            SigningCertificatePropertiesDialog.this.dispose();
                            postTaskFunc.call();
                        }
                    }
                }
            );
        } else {
            SigningCertificatePropertiesDialog.this.dispose();
            postTaskFunc.call();
        }
    }

    private void showErrorMessage(String title, String msg) {
        logger.log(Level.WARNING, msg);
        DialogDisplayer.showMessageDialog(this, msg.length() > 128 ? msg.substring(0, 125) + "..." : msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private String getDefaultExpiryAge() {
        final Registry reg = Registry.getDefault();
        String expiryAge = "730"; // days

        if (reg.isAdminContextPresent()) {
            try {
                ClusterProperty prop = reg.getClusterStatusAdmin().findPropertyByName(CLUSTER_PROP_PKIX_CSR_DEFAULT_EXPIRY_AGE);
                if (prop != null) expiryAge = prop.getValue();
            } catch (FindException e) {
                logger.warning("Unable to get the cluster property, '" + CLUSTER_PROP_PKIX_CSR_DEFAULT_EXPIRY_AGE + "'");
            }
        }

        return expiryAge;
    }

    /**
     * Display the contents of the CSR
     *
     * @param csrProps given the properties of the CSR
     */
    private void modelToView(final Map<String, Object> csrProps) {
        if (csrProps == null || csrProps.isEmpty()) return;


        subjectDnTextField.setText((String)csrProps.get(TrustedCertAdmin.CSR_PROP_SUBJECT_DN));
        subjectDnTextField.setCaretPosition(0);
        if(csrProps.containsKey(TrustedCertAdmin.CSR_PROP_SUBJECT_ALTERNATIVE_NAMES)) {
            List<NameValuePair> sansList = ((List<NameValuePair>) csrProps.get(TrustedCertAdmin.CSR_PROP_SUBJECT_ALTERNATIVE_NAMES))
                    .stream()
                    .filter(x -> SUPPORTED_SAN_TYPES.contains(x.left))
                    //.map(x -> x.left.equals(X509GeneralName.Type.otherName.getUserFriendlyName()) ? new NameValuePair(x.left, "<binary>") : new NameValuePair(x.left, x.right))
                    .collect(Collectors.toList());
            if(sansList.size() > 0) {
                sanTableModel.setRows(sansList);
                sansScrollPane.setVisible(true);
                sansLabel.setVisible(true);
            }
            else {
                resizeDialogWithNoSans();
            }
        }
        else {
            resizeDialogWithNoSans();
        }

        final String keyType = (String)csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_TYPE);
        if (keyType == null) return;

        final String briefDetails;
        if (keyType.startsWith("RSA")) {
            final String keySize = (String)csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_SIZE);
            briefDetails = "RSA, " + keySize + " bits";

            publicKeyDetails = "Key type: RSA public key\n" +
                "Key size: " + keySize + " bits\n" +
                "Modulus: " + csrProps.get(TrustedCertAdmin.CSR_PROP_MODULUS) + "\n" +
                "Public exponent: " + csrProps.get(TrustedCertAdmin.CSR_PROP_EXPONENT);
        } else if (keyType.startsWith("EC")) {
            final String curveName = (String)csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_NAME);
            briefDetails = "EC" + (curveName == null? "" : ", " + curveName);

            publicKeyDetails = "Key type: EC public key\n" +
                (curveName == null? "" : "Curve name: " + curveName + "\n") + // If curve name is know, then display curve name.
                "Curve size: " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_SIZE) + " bits\n" +
                "Curve point-W (X): " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_POINT_W_X) + "\n" +
                "Curve point-W (Y): " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_POINT_W_Y);
        } else {
            briefDetails = (String)csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_TYPE);
            fullDetailsButton.setEnabled(false); // No full details available for this case.
        }

        publicKeyDetailsTextField.setText(briefDetails);
    }

    private void resizeDialogWithNoSans() {
        sansScrollPane.setSize(0,0);
        sansLabel.setSize(0,0);
        mainPanel.setPreferredSize(new Dimension(590, 185));
    }

    /**
     * The dialog displays the details of a public key.
     */
    private class PublicKeyDetailsDialog extends JDialog {
        private JButton closeButton;
        private JTextArea textArea1;
        private JPanel mainPanel;

        PublicKeyDetailsDialog(final String publicKeyDetails) {
            setTitle(resources.getString("public.key.details.dialog.title"));
            setContentPane(mainPanel);
            setModal(true);
            getRootPane().setDefaultButton(closeButton);
            Utilities.setEscKeyStrokeDisposes(PublicKeyDetailsDialog.this);

            textArea1.setText(publicKeyDetails);

            closeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

            DialogDisplayer.pack(PublicKeyDetailsDialog.this);
        }
    }
}