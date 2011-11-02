package com.l7tech.security.xml;

import com.ibm.xml.enc.EncryptionEngine;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.ibm.xml.enc.type.KeySize;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.RandomUtil;
import org.jetbrains.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;

/**
 * An EncryptionEngine that attempts to use AES-GCM.
 */
public class AesGcmEncryptionEngine extends EncryptionEngine {
    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;  // GCM always uses 12 byte IV
    private static final int AUTH_TAG_BYTES = 16;  // xenc 1.1 specific a 128 bit auth tag for GCM
    private final String algUri;
    private final KeySize keySize;
    private final Cipher cipher;
    private int cipherMode = 0;
    private Key key;
    private byte[] ivToWrite;
    private byte[] ivToCollect;
    private int ivToCollectPosition;
    private int ivBytesRemainingToCollect;

    public AesGcmEncryptionEngine(int keyBits, String uri) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        this.algUri = uri;
        this.keySize = new KeySize();
        keySize.setSize(keyBits);
        this.cipher = JceProvider.getInstance().getAesGcmCipher();
    }

    @Override
    public EncryptionMethod getEncryptionMethod() {
        EncryptionMethod encryptionMethod = new EncryptionMethod();
        encryptionMethod.setAlgorithm(algUri);
        encryptionMethod.setKeySize(keySize);
        encryptionMethod.setOAEPParams(null);
        throw new UnsupportedOperationException("Not implemented");
        //return encryptionMethod;
    }

    @Override
    public Provider getProvider() {
        throw new UnsupportedOperationException("Not implemented");
        //return JceProvider.getInstance().getPreferredProvider(AES_GCM_NO_PADDING);
    }

    @Override
    public AlgorithmParameters getParameters() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void init(int cipherMode, Key key, Object params) throws InvalidAlgorithmParameterException, InvalidKeyException {
        this.cipherMode = cipherMode;
        this.key = key;

        try {
            switch (cipherMode) {
                case Cipher.ENCRYPT_MODE:
                    this.ivToWrite = new byte[IV_BYTES];
                    RandomUtil.nextBytes(ivToWrite);
                    final AlgorithmParameterSpec spec = JceProvider.getInstance().generateAesGcmParameterSpec(AUTH_TAG_BYTES, ivToWrite);
                    cipher.init(cipherMode, key, spec);
                    if (ivToWrite.length != 12) throw new IllegalStateException("IV must be 12 bytes long, found: " + ivToWrite.length);
                    break;

                case Cipher.DECRYPT_MODE:
                    this.ivToCollect = new byte[IV_BYTES];
                    this.ivToCollectPosition = 0;
                    this.ivBytesRemainingToCollect = IV_BYTES;
                    break;

                default:
                    throw new IllegalStateException("Mode not supported for AES-GCM: " + cipherMode);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidAlgorithmParameterException("AES-GCM not supported with current crypto provider", e);
        }
    }

    @Override
    public byte[] update(byte[] bytes, int offset, int len) {
        try{
        switch (cipherMode) {
            case Cipher.ENCRYPT_MODE:
                byte[] ciphertext = cipher.update(bytes, offset, len);
                if (ivToWrite != null) {
                    ciphertext = ciphertext == null ? ivToWrite : ArrayUtils.concat(ivToWrite, ciphertext);
                    ivToWrite = null;
                }
                return ciphertext;

            case Cipher.DECRYPT_MODE:
                if (ivBytesRemainingToCollect > 0) {
                    if (len >= ivBytesRemainingToCollect) {
                        // Collect last of the IV, initialize the cipher, and fall through to the first actual cipher update
                        System.arraycopy(bytes, offset, ivToCollect, ivToCollectPosition, ivBytesRemainingToCollect);
                        len -= ivBytesRemainingToCollect;
                        offset += ivBytesRemainingToCollect;
                        ivBytesRemainingToCollect = 0;

                        cipher.init(Cipher.DECRYPT_MODE, key, JceProvider.getInstance().generateAesGcmParameterSpec(AUTH_TAG_BYTES, ivToCollect));
                    } else {
                        System.arraycopy(bytes, offset, ivToCollect, ivToCollectPosition, len);
                        ivBytesRemainingToCollect -= len;
                        ivToCollectPosition += len;
                        len = 0;
                        // all input consumed, don't have the IV yet, cipher not initialized yet; return null (signalling not enough input yet given to produce a block)
                    }
                    if (len < 1)
                        return null;
                }
                return cipher.update(bytes, offset, len);

            default:
                throw new IllegalStateException("Mode not supported for AES-GCM: " + cipherMode);
        }
        } catch (InvalidAlgorithmParameterException e) {
            throw new IllegalStateException("Decryption internal error", e);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("Decryption key invalid", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Decryption internal error", e);
        }
    }

    @Override
    public byte[] doFinal() throws BadPaddingException, IllegalBlockSizeException {
        return doFinal(null);
    }

    @Override
    public byte[] doFinal(@Nullable byte[] bytes) throws BadPaddingException, IllegalBlockSizeException {
        byte[] first = bytes == null ? null : update(bytes);
        byte[] last = cipher.doFinal();

        // TODO triple check that the cipher object is consuming and verifying the auth tag for us
        // TODO repeat this triple check for each new JceProvider we support with GCM

        return ArrayUtils.concat(first != null ? first : new byte[0], last != null ? last : new byte[0]);
    }

    @Override
    public byte[] wrap(Key key) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException {
        throw new NoSuchAlgorithmException("Key wrapping not supported using AES-GCM");
    }

    @Override
    public Key unwrap(byte[] bytes, EncryptionMethod encryptionMethod, String s) throws BadPaddingException, IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, StructureException {
        throw new NoSuchAlgorithmException("Key unwrapping not supported using AES-GCM");
    }
}
