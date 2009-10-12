package com.l7tech.security.xml;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>SignerInfo</code> is the simple holder for a public
 * private key and the certificate.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SignerInfo implements Serializable {
    private static final long serialVersionUID = 2335356651146783430L;
    private static final Logger logger = Logger.getLogger(SignerInfo.class.getName());

    private transient PrivateKey privateKey;
    private X509Certificate[] certificateChain;
    private String[] subjectDns;

    /**
     * Constructs a signer info from the given private key and certificate.
     * The public key is retrieved from the certificate.
     * 
     * @param privateKey the private key.
     * @param certificateChain the certificate chain; the first element contains the relevant public key.
     */
    public SignerInfo(PrivateKey privateKey, X509Certificate[] certificateChain) {
        this.privateKey = privateKey;
        this.certificateChain = certificateChain;
        populateSubjectDns();
    }

    /**
     * Constructs a signer info from the given private key and certificate.
     * The public key is retrieved from the certificate.
     *
     * @param certWithPrivateKey a Pair holding the subject cert and private key.  Required.
     */
    public SignerInfo(Pair<X509Certificate, PrivateKey> certWithPrivateKey) {
        if (certWithPrivateKey.left == null) throw new IllegalArgumentException("A certificate is required.");
        this.privateKey = certWithPrivateKey.right;
        this.certificateChain = new X509Certificate[] { certWithPrivateKey.left };
        populateSubjectDns();
    }

    /**
     * Returns a reference to the public key
     * 
     * @return a reference to the public key.
     */
    public PublicKey getPublic() {
        return certificateChain[0].getPublicKey();
    }

    /**
     * Returns a reference to the private key
     * 
     * @return a reference to the private key, or null if the private key is not available here.
     */
    public PrivateKey getPrivate() {
        return privateKey;
    }

    protected void setPrivate(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @return the certificate chain for this private key.  Always contains at least one certificate.
     *         The zeroth entry is the target certificate, containing the public key corresponding to this entry's
     *         private key.  Entry #1, if it exists, contains the public key that was used to sign Entry #0, and so on.
     */
    public X509Certificate[] getCertificateChain() {
        return certificateChain;
    }

    protected void setCertificateChain(X509Certificate[] chain) {
        this.certificateChain = chain;
        populateSubjectDns();
    }

    public String[] getCertificateChainSubjectDns() {
        return subjectDns;
    }

    /**
     * Convenience metho that returns the first cert in the cert chain.
     * Equivalent to getCertificateChain[0].
     *
     * @return the certificate for this private key.  Never null.
     */
    public X509Certificate getCertificate() {
        return getCertificateChain()[0];
    }

    /**
     * Convenience method that returns the Subject DN of the first cert in the cert chain.
     * Equivalent to getCertificate().getSubjectDN().toString().
     *
     * @return the Subject DN of the first cert in the cert chain.
     */
    public String getSubjectDN() {
        return getCertificateChainSubjectDns()[0];
    }

    public SignerInfo getSignerInfo() {
        return this;
    }

    private void populateSubjectDns() {
        if (certificateChain == null) {
            subjectDns = null;
            return;
        }

        subjectDns = new String[certificateChain.length];
        for (int i = 0; i < certificateChain.length; i++) {
            X509Certificate certificate = certificateChain[i];
            subjectDns[i] = certificate.getSubjectDN().getName();
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        if (((certificateChain == null) != (subjectDns == null)) ||
            (certificateChain != null && certificateChain.length != subjectDns.length)) {
            throw new IOException("certificateChain is out of sync with subjectDns");
        }

        if (subjectDns == null) {
            out.writeBoolean(false);
            return;
        }

        out.writeBoolean(true);
        out.writeObject(subjectDns);
        for (X509Certificate cert : certificateChain) {
            byte[] encoded;
            try {
                encoded = cert.getEncoded();
            } catch (CertificateEncodingException e) {
                throw new IOException("Unable to encode certificate for output to client: " + ExceptionUtils.getMessage(e), e);
            }

            out.writeObject(encoded);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        boolean haveChain = in.readBoolean();
        if (!haveChain) {
            subjectDns = null;
            certificateChain = null;
            return;
        }

        subjectDns = (String[])in.readObject();
        certificateChain = new X509Certificate[subjectDns.length];
        for (int i = 0; i < subjectDns.length; i++) {
            byte[] certBytes = (byte[])in.readObject();
            try {
                certificateChain[i] = CertUtils.decodeCert(certBytes);
            } catch (CertificateException e) {
                logger.log(Level.WARNING, "Unable to deserialize certificate: " + ExceptionUtils.getMessage(e), e);
                // Leave it as null
            }
        }        
    }
}
