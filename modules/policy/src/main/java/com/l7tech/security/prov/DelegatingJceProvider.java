package com.l7tech.security.prov;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Map;

/**
 * A JceProvider that always delegates all method invocation to a specified delegate JceProvider.
 */
public class DelegatingJceProvider extends JceProvider {
    protected final JceProvider delegate;

    /**
     * Create a JceProvider that will forward all method invocations to the specified delegate provider.
     *
     * @param delegate the delegate to which all method calls should be forwarded.  Required.
     */
    public DelegatingJceProvider(JceProvider delegate) {
        if (delegate == null)
            throw new IllegalArgumentException("delegate is required");
        this.delegate = delegate;
    }

    @Override
    public GcmCipher getAesGcmCipherWrapper() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return delegate.getAesGcmCipherWrapper();
    }

    @Override
    public Cipher getAesGcmCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return delegate.getAesGcmCipher();
    }

    @Override
    public String getAesGcmCipherName() {
        return delegate.getAesGcmCipherName();
    }

    @Override
    public AlgorithmParameterSpec generateAesGcmParameterSpec(int authTagLenBytes, @NotNull byte[] iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        return delegate.generateAesGcmParameterSpec(authTagLenBytes, iv);
    }

    @Override
    public SecureRandom getSecureRandom() {
        return delegate.getSecureRandom();
    }

    @Override
    public SecureRandom newSecureRandom() {
        return delegate.newSecureRandom();
    }

    @Override
    public void setDebugOptions(Map<String, String> options) {
        delegate.setDebugOptions(options);
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public boolean isFips140ModeEnabled() {
        return delegate.isFips140ModeEnabled();
    }

    @Override
    public RsaSignerEngine createRsaSignerEngine(PrivateKey caKey, X509Certificate[] caCertChain) {
        return delegate.createRsaSignerEngine(caKey, caCertChain);
    }

    @Override
    public KeyPair generateRsaKeyPair() {
        return delegate.generateRsaKeyPair();
    }

    @Override
    public KeyPair generateRsaKeyPair(int keybits) {
        return delegate.generateRsaKeyPair(keybits);
    }

    @Override
    public KeyPair generateEcKeyPair(String curveName, SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return delegate.generateEcKeyPair(curveName, random);
    }

    @Override
    protected KeyPair tryGenerateEcKeyPair(String curveName, SecureRandom random) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        return delegate.tryGenerateEcKeyPair(curveName, random);
    }

    @Override
    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws SignatureException, InvalidKeyException {
        return delegate.makeCsr(username, keyPair);
    }

    @Override
    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return delegate.getRsaNoPaddingCipher();
    }

    @Override
    public String getRsaNoPaddingCipherName() {
        return delegate.getRsaNoPaddingCipherName();
    }

    @Override
    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return delegate.getRsaOaepPaddingCipher();
    }

    @Override
    protected String getRsaOaepPaddingCipherName() {
        return delegate.getRsaOaepPaddingCipherName();
    }

    @Override
    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return delegate.getRsaPkcs1PaddingCipher();
    }

    @Override
    protected String getRsaPkcs1PaddingCipherName() {
        return delegate.getRsaPkcs1PaddingCipherName();
    }

    @Override
    public MessageDigest getMessageDigest(String messageDigest) throws NoSuchAlgorithmException {
        return delegate.getMessageDigest(messageDigest);
    }

    @Override
    public Signature getSignature(String alg) throws NoSuchAlgorithmException {
        return delegate.getSignature(alg);
    }

    @Override
    public KeyPairGenerator getKeyPairGenerator(String algorithm) throws NoSuchAlgorithmException {
        return delegate.getKeyPairGenerator(algorithm);
    }

    @Override
    public KeyFactory getKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        return delegate.getKeyFactory(algorithm);
    }

    @Override
    public KeyStore getKeyStore(String kstype) throws KeyStoreException {
        return delegate.getKeyStore(kstype);
    }

    @Override
    public Provider getBlockCipherProvider() {
        return delegate.getBlockCipherProvider();
    }

    @Override
    public Provider getProviderFor(String service) {
        return delegate.getProviderFor(service);
    }

    @Override
    public void prepareSslContext( @NotNull SSLContext sslContext ) {
        delegate.prepareSslContext( sslContext );
    }
}
