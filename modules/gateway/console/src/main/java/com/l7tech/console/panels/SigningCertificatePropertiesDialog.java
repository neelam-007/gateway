package com.l7tech.console.panels;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.prov.bc.BouncyCastleRsaSignerEngine;
import com.l7tech.util.ExceptionUtils;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PKCS10CertificationRequest;

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

    private String publicKeyDetails;
    private boolean confirmed;

    public SigningCertificatePropertiesDialog(Frame owner, byte[] csrBytes) {
        super(owner, resources.getString("dialog.title"));
        initialize();
        modelToView(csrBytes);
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
                confirmed = true;
                dispose();
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

        String subjectDn = certReqInfo.getSubject().toString(true, X509Name.DefaultSymbols);
        subjectDnTextField.setText(subjectDn);

        final PublicKey publicKey;
        try {
            publicKey = BouncyCastleRsaSignerEngine.getPublicKey(pkcs10);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get the public key from the CSR: " + ExceptionUtils.getMessage(e));
            return;
        }

        final String shortDetails;
        if (publicKey instanceof RSAPublicKey) {
            final RSAPublicKey rsa = (RSAPublicKey) publicKey;
            final BigInteger modulus = rsa.getModulus();

            shortDetails = "RSA, " + modulus.bitLength() + " bits";

            publicKeyDetails = "Key type: RSA public key\n" +
                "Key size: " + modulus.bitLength() + " bits\n" +
                "Modulus: " + modulus.toString(16) + "\n" +
                "Public exponent: " + rsa.getPublicExponent();
        } else if (publicKey instanceof ECPublicKey) {
            final ECPublicKey ec = (ECPublicKey) publicKey;
            final ECParameterSpec params = ec.getParams();
            final ECPoint w = ec.getW();
            final String curveName = CertUtils.guessEcCurveName(publicKey);

            shortDetails = "EC" + (curveName == null? "" : ", " + curveName);

            publicKeyDetails = "Key type: EC public key\n" +
                (curveName == null? "" : "Curve name: " + curveName + "\n") + // If curve name is know, then display curve name.
                "Curve size: " + params.getCurve().getField().getFieldSize() + " bits\n" +
                "Curve point-W (X): " + w.getAffineX() + "\n" +
                "Curve point-W (Y): " + w.getAffineY();
        } else {
            shortDetails = publicKey.getAlgorithm();
            publicKeyDetails = publicKey.toString();
        }

        publicKeyDetailsTextField.setText(shortDetails);
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