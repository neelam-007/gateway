package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

import javax.security.auth.x500.X500Principal;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The dialog displays the contents of a CSR and allows user to set/modify some settings of signing certificate.
 *
 * @author: ghuang
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

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField subjectDnTextField;
    private JTextField expiryAgeTextField;
    private JComboBox hashAlgComboBox;
    private JButton fullDetailsButton;
    private JTextField publicKeyDetailsTextField;

    private Functions.Nullary<Boolean> precheckingShortKeyFunc;
    private String publicKeyDetails;
    private boolean confirmed;

    public SigningCertificatePropertiesDialog(Frame owner, Functions.Nullary<Boolean> precheckingShortKeyFunc) {
        super(owner, resources.getString("dialog.title"));
        this.precheckingShortKeyFunc = precheckingShortKeyFunc;
        initialize();
    }

    /**
     * The constructor will process the given raw CSR bytes given and get the contents of the CSR.
     * @param owner parent of this dialog
     * @param csrBytes raw CSR bytes
     * @param precheckingShortKeyFunc: the function to check if the CA key is a short key for signature alorightm
     */
    public SigningCertificatePropertiesDialog(Frame owner, byte[] csrBytes, Functions.Nullary<Boolean> precheckingShortKeyFunc) {
        this(owner, precheckingShortKeyFunc);
        modelToView(csrBytes);
    }

    /**
     * The constructor will be given the contents of the CSR.
     * @param owner parent of this dialog
     * @param csrProps CSR properties
     * @param precheckingShortKeyFunc: the function to check if the CA key is a short key for signature alorightm
     */
    public SigningCertificatePropertiesDialog(Frame owner, Map<String, String> csrProps, Functions.Nullary<Boolean> precheckingShortKeyFunc) {
        this(owner, precheckingShortKeyFunc);
        modelToView(csrProps);
    }

    public boolean isConfirmed() {
        return confirmed;
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

        final InputValidator inputValidator = new InputValidator(this, resources.getString("error.dialog.title"));
        inputValidator.constrainTextFieldToNumberRange(resources.getString("text.cert.expiry.age"), expiryAgeTextField, MIN_CERTIFICATE_EXPIRY, MAX_CERTIFICATE_EXPIRY);
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (validatePropertiesAndReportErrors()) {
                    confirmed = true;
                    dispose();
                }
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

        pack();
    }

    private boolean validatePropertiesAndReportErrors() {
        boolean valid = false;

        // Check Subject DN
        final String subjectDn = getSubjectDn();
        try {
            if (subjectDn.isEmpty()) throw new IllegalArgumentException(resources.getString("error.message.empty.subject.dn"));
            new X500Principal(subjectDn);
            valid = true;
        } catch (IllegalArgumentException e) {
            showErrorMessage(
                resources.getString("error.dialog.title"),
                MessageFormat.format(resources.getString("error.message.invalid.subject.dn"), ExceptionUtils.getMessage(e)));
        }
        if (!valid) return valid;

        // The CA Key is short or not
        final boolean[] isShort = {false};
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
                        if (option == JOptionPane.NO_OPTION) {
                            isShort[0] = true;
                        }
                    }
                }
            );
        }
        return !isShort[0];
    }

    private void showErrorMessage(String title, String msg) {
        logger.log(Level.WARNING, msg);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
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
     * @param csrBytes given CSR raw bytes
     */
    private void modelToView(final byte[] csrBytes) {
        byte[] decodedCsrBytes;
        try {
            decodedCsrBytes = CertUtils.csrPemToBinary(csrBytes);
        } catch (IOException e) {
            // Try as DER
            decodedCsrBytes = csrBytes;
        }
        PKCS10CertificationRequest pkcs10 = new PKCS10CertificationRequest(decodedCsrBytes);
        CertificationRequestInfo certReqInfo = pkcs10.getCertificationRequestInfo();

        @SuppressWarnings({"deprecation"})
        String subjectDn = certReqInfo.getSubject().toString(true, X509Name.DefaultSymbols);
        subjectDnTextField.setText(subjectDn);

        final PublicKey publicKey;
        try {
            publicKey = BouncyCastleRsaSignerEngine.getPublicKey(pkcs10);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get the public key from the CSR: " + ExceptionUtils.getMessage(e));
            return;
        }

        final String briefDetails;
        if (publicKey instanceof RSAPublicKey) {
            final RSAPublicKey rsa = (RSAPublicKey) publicKey;
            final BigInteger modulus = rsa.getModulus();

            briefDetails = "RSA, " + modulus.bitLength() + " bits";

            publicKeyDetails = "Key type: RSA public key\n" +
                "Key size: " + modulus.bitLength() + " bits\n" +
                "Modulus: " + modulus.toString(16) + "\n" +
                "Public exponent: " + rsa.getPublicExponent().toString(16);
        } else if (publicKey instanceof ECPublicKey) {
            final ECPublicKey ec = (ECPublicKey) publicKey;
            final ECParameterSpec params = ec.getParams();
            final ECPoint w = ec.getW();
            final String curveName = CertUtils.guessEcCurveName(publicKey);

            briefDetails = "EC" + (curveName == null? "" : ", " + curveName);

            publicKeyDetails = "Key type: EC public key\n" +
                (curveName == null? "" : "Curve name: " + curveName + "\n") + // If curve name is know, then display curve name.
                "Curve size: " + params.getCurve().getField().getFieldSize() + " bits\n" +
                "Curve point-W (X): " + w.getAffineX() + "\n" +
                "Curve point-W (Y): " + w.getAffineY();
        } else {
            briefDetails = publicKey.getAlgorithm();
            publicKeyDetails = publicKey.toString();
        }

        publicKeyDetailsTextField.setText(briefDetails);
    }

    /**
     * Display the contents of the CSR
     *
     * @param csrProps given the properties of the CSR
     */
    private void modelToView(final Map<String, String> csrProps) {
        if (csrProps == null || csrProps.isEmpty()) return;

        subjectDnTextField.setText(csrProps.get(TrustedCertAdmin.CSR_PROP_SUBJECT_DN));

        final String keyType = csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_TYPE);
        if (keyType == null) return;

        final String briefDetails;
        if (keyType.startsWith("RSA")) {
            final String keySize = csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_SIZE);
            briefDetails = "RSA, " + keySize + " bits";

            publicKeyDetails = "Key type: RSA public key\n" +
                "Key size: " + keySize + " bits\n" +
                "Modulus: " + csrProps.get(TrustedCertAdmin.CSR_PROP_MODULUS) + "\n" +
                "Public exponent: " + csrProps.get(TrustedCertAdmin.CSR_PROP_EXPONENT);
        } else if (keyType.startsWith("EC")) {
            final String curveName = csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_NAME);
            briefDetails = "EC" + (curveName == null? "" : ", " + curveName);

            publicKeyDetails = "Key type: EC public key\n" +
                (curveName == null? "" : "Curve name: " + curveName + "\n") + // If curve name is know, then display curve name.
                "Curve size: " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_SIZE) + " bits\n" +
                "Curve point-W (X): " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_POINT_W_X) + "\n" +
                "Curve point-W (Y): " + csrProps.get(TrustedCertAdmin.CSR_PROP_CURVE_POINT_W_Y);
        } else {
            briefDetails = csrProps.get(TrustedCertAdmin.CSR_PROP_KEY_TYPE);
            fullDetailsButton.setEnabled(false); // No full details available for this case.
        }

        publicKeyDetailsTextField.setText(briefDetails);
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

            pack();
        }
    }
}