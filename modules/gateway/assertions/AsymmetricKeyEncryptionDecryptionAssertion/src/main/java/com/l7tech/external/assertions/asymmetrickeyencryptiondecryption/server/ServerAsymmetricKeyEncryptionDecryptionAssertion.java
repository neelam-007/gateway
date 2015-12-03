package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
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
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Server side implementation of the AsymmetricKeyEncryptionDecryptionAssertion.
 *
 * @see com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion
 */
public class ServerAsymmetricKeyEncryptionDecryptionAssertion extends AbstractServerAssertion<AsymmetricKeyEncryptionDecryptionAssertion> {

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

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {

        Map<String, Object> refVariablesMap = context.getVariableMap(variablesUsed, getAudit());
        String inputText = ExpandVariables.process(Syntax.SYNTAX_PREFIX + assertion.getInputVariable() + Syntax.SYNTAX_SUFFIX, refVariablesMap, getAudit(), true);

        byte[] inputData = HexUtils.decodeBase64(inputText);

        byte[] outputData = null;
        Key key = null;
        int mode = assertion.getMode();

        try {

            //check for valid mode only javax.crypto.Cipher.ENCRYPT_MODE (== 1) and  javax.crypto.Cipher.DECRYPT_MODE (== 2)
            //allowed
            if (mode != Cipher.ENCRYPT_MODE && mode != Cipher.DECRYPT_MODE)
                throw new Exception("Invalid mode selected.");

            //retrieve the key to use for encryption/decryption
            //if mode == javax.crypto.Cipher.ENCRYPT_MODE (== 1) then get public key
            //if mode == javax.crypto.Cipher.DECRYPT_MODE (== 2) then get private key
            key = getKey(mode, assertion.getKeyName(), assertion.getKeyGoid());

            //encrypt/decrypt data
            outputData = transform(key, inputData, mode, assertion.getModePaddingOption());

            //assign to context variable
            context.setVariable( assertion.getOutputVariable(), HexUtils.encodeBase64(outputData));
        } catch (Exception e) {
            getAudit().logAndAudit(Messages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    /**
     * @param key the public key if doing encryption, the private key if doing decryption
     * @param inputData data to encrypt and decrypt
     * @param mode will either be javax.crypto.Cipher.ENCRYPT_MODE (== 1) or javax.crypto.Cipher.DECRYPT_MODE (== 2) as selected through the gui
     * @return the result of the encryption/decryption
     */
    private byte[] transform(Key key, byte[] inputData, int mode, RsaModePaddingOption modePaddingOption) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException {

        byte[] result = null;
        Cipher cipher = null;

        if (!key.getAlgorithm().contains("RSA"))
            throw new NoSuchAlgorithmException("Only RSA Keys are supported.");

        if (modePaddingOption == RsaModePaddingOption.NO_MODE_NO_PADDING)
            cipher = Cipher.getInstance("RSA");
        else if (modePaddingOption == RsaModePaddingOption.ECB_NO_PADDING)
            cipher = JceProvider.getInstance().getRsaNoPaddingCipher();
        else if (modePaddingOption == RsaModePaddingOption.ECB_PKCS1_PADDING)
            cipher = JceProvider.getInstance().getRsaPkcs1PaddingCipher();
        else if (modePaddingOption == RsaModePaddingOption.ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING)
            cipher = JceProvider.getInstance().getRsaOaepPaddingCipher();

        // set initialize the cypher to eithed encrypt/decrypt, depending on what was selected, with the specified key.
        cipher.init(mode, key);
        // do the transformation
        result = cipher.doFinal(inputData);

        return result;
    }

    /**
     *
     * @param mode will either be javax.crypto.Cipher.ENCRYPT_MODE (== 1) or javax.crypto.Cipher.DECRYPT_MODE (== 2) as selected through the gui
     * @param keyName the name of the trusted certificate or the alias for private keys
     * @param keyGoid the oid of the trusted certificate of the keystore id for private keys
     * @return the public or private key
     */
    private Key getKey(int mode, String keyName, Goid keyGoid) throws FindException, UnrecoverableKeyException, KeyStoreException {

        Key key = null;

        if (mode == Cipher.ENCRYPT_MODE) {
            //get the public key from the trusted certificate manager
            TrustedCert trustedCertificate = trustedCertManager.findByPrimaryKey(keyGoid);

            X509Certificate certificate = trustedCertificate.getCertificate();
            if (certificate == null)
                throw new FindException("Certificate; " + keyName + ", not found.");

            key = certificate.getPublicKey();
        } else if (mode == Cipher.DECRYPT_MODE) {
            //get the private key from the private key store
            SsgKeyEntry ssgKeyEntry = keyStoreManager.lookupKeyByKeyAlias(keyName, keyGoid);
            key = ssgKeyEntry.getPrivateKey();
        }

        return key;
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
