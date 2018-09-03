package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptDecryptUtils;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion.KeySource;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.prov.ProviderUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.util.HexUtils;
import org.springframework.context.ApplicationContext;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the AsymmetricKeyEncryptionDecryptionAssertion.
 *
 * @see com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion
 */
public class ServerAsymmetricKeyEncryptionDecryptionAssertion extends AbstractServerAssertion<AsymmetricKeyEncryptionDecryptionAssertion> {

    private static final Logger logger = Logger.getLogger(ServerAsymmetricKeyEncryptionDecryptionAssertion.class.getName());

    private final String[] variablesUsed;
    private final AsymmetricKeyEncryptionDecryptionAssertion assertion;

    @Inject
    @Named("ssgKeyStoreManager")
    private SsgKeyStoreManager keyStoreManager;

    @Inject
    @Named("trustedCertManager")
    private TrustedCertManager trustedCertManager;

    public ServerAsymmetricKeyEncryptionDecryptionAssertion( final AsymmetricKeyEncryptionDecryptionAssertion assertion, ApplicationContext context ) throws PolicyAssertionException {
        super(assertion);

        this.assertion = assertion;
        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) {

        Map<String, Object> refVariablesMap = context.getVariableMap(variablesUsed, getAudit());
        String inputText = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getInputVariable() + Syntax.SYNTAX_SUFFIX, refVariablesMap, getAudit(), true);

        byte[] inputData = HexUtils.decodeBase64(inputText);

        final boolean isEncrypt = assertion.getMode() == Cipher.ENCRYPT_MODE;
        final KeySource keySource = assertion.getKeySource();

        Key key;
        try {

            //retrieve the key to use for encryption/decryption
            if (keySource == KeySource.FROM_VALUE) {
                final String keyValue = ExpandVariables.process(assertion.getRsaKeyValue(), refVariablesMap, getAudit(), true);
                key = getRSAKey(keyValue, isEncrypt);
            } else {
                // By default get key from store
                key = getKey(isEncrypt, assertion.getKeyName(), assertion.getKeyGoid());
            }

            //encrypt/decrypt data
            final byte[] outputData = transform(key, inputData, isEncrypt, assertion.getAlgorithm());

            //assign to context variable
            context.setVariable(assertion.getOutputVariable(), HexUtils.encodeBase64(outputData));
        } catch (Exception e) {
            getAudit().logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    /**
     * @param key the public key if doing encryption, the private key if doing decryption
     * @param inputData data to encrypt and decrypt
     * @param isEncrypt is encrypt mode selected
     * @param algorithm algorithm to encrypt/decrypt with
     * @return the result of the encryption/decryption
     */
    private byte[] transform(Key key, byte[] inputData, boolean isEncrypt, String algorithm)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {

        byte[] result;
        Cipher cipher;

        if (!key.getAlgorithm().contains("RSA"))
            throw new NoSuchAlgorithmException("Only RSA Keys are supported.");

        cipher = JceProvider.getCipher(algorithm, JceProvider.getInstance().getProviderFor("Cipher.RSA"));

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Using Provider: {0} for algorithm: {1}", new Object[]{cipher.getProvider().getName(), algorithm} );
        }

        // set initialize the cypher to either encrypt/decrypt, depending on what was selected, with the specified key.
        final int mode = isEncrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
        cipher.init(mode, key);
        // do the transformation
        result = cipher.doFinal(inputData);

        // US265438: When the encryption/decryption transformation uses "RSA/ECB/NoPadding", some providers add padding
        // zeros at the beginning of the decryption output bytes, if the output byte length is less than the key size in
        // bytes. In order to keep this back compatibility, this story added padding zeros if the current provider does
        // not pad leading zeros.
        if (mode == Cipher.DECRYPT_MODE && algorithm.equals("RSA/ECB/NoPadding")) {
            final int keySizeInBytes = ((RSAKey)key).getModulus().bitLength() / 8;
            result = ProviderUtil.paddingDecryptionOutputUsingRsaEcbNoPadding(result, keySizeInBytes);
        }

        return result;
    }

    /**
     * @param isEncrypt is encrypt mode selected
     * @param keyName the name of the trusted certificate or the alias for private keys
     * @param keyGoid the oid of the trusted certificate of the keystore id for private keys
     * @return the public or private key
     */
    private Key getKey(final boolean isEncrypt, String keyName, Goid keyGoid) throws FindException,
            UnrecoverableKeyException, KeyStoreException {
        Key key;

        //if mode == javax.crypto.Cipher.ENCRYPT_MODE (== 1) then get public key
        //if mode == javax.crypto.Cipher.DECRYPT_MODE (== 2) then get private key
        if (isEncrypt) {
            if (keyGoid == null) {
                throw new IllegalArgumentException("Invalid key provided");
            }
            //get the public key from the trusted certificate manager
            TrustedCert trustedCertificate = trustedCertManager.findByPrimaryKey(keyGoid);

            X509Certificate certificate = trustedCertificate.getCertificate();
            if (certificate == null)
                throw new FindException("Certificate; " + keyName + ", not found.");

            key = certificate.getPublicKey();
        } else {
            if (keyGoid == null || keyName == null) {
                throw new IllegalArgumentException("Invalid key provided");
            }
            //get the private key from the private key store
            SsgKeyEntry ssgKeyEntry = keyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid);
            key = ssgKeyEntry.getPrivateKey();
        }

        return key;
    }

    /**
     * @param keyString base64 encoded Public or Private RSA key string
     * @param isEncrypt Is encrypt mode selected
     * @return the public or the private key
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private Key getRSAKey(final String keyString, final boolean isEncrypt) throws InvalidKeySpecException,
            NoSuchAlgorithmException, IOException, CertificateException {

        if (keyString != null && !keyString.trim().isEmpty()) {
            if (isEncrypt) {
                return AsymmetricKeyEncryptDecryptUtils.parsePublicKeyOrCertificate(keyString);
            } else {
                return AsymmetricKeyEncryptDecryptUtils.parsePrivateKey(keyString);
            }
        } else {
            throw new IllegalArgumentException("Invalid base64 key string provided.");
        }
    }

    /**
     * Used to set the KeyStoreManager for testing purpose only.
     * @param keyStoreManager
     */
    void setKeyStoreManager(SsgKeyStoreManager keyStoreManager){
        this.keyStoreManager = keyStoreManager;
    }

    /**
     * Used to set the TrustedCertManager for testing purposes only
     * @param trustedCertManager
     */
    void setTrustedCertManager(TrustedCertManager trustedCertManager){
        this.trustedCertManager = trustedCertManager;
    }
}
