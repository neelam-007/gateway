package com.l7tech.security.prov.ncipher;

import com.l7tech.security.prov.GcmCipher;
import com.l7tech.util.ExceptionUtils;
import com.ncipher.nfast.connect.utils.Channel;
import com.ncipher.nfast.connect.utils.EasyConnection;
import com.ncipher.nfast.marshall.*;
import com.ncipher.provider.km.nCipherKM;
import com.ncipher.provider.nCKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.Closeable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A GcmCipher implementation coded against the low-level nCore API.  Not threadsafe.
 */
class NcipherGcmCipher implements GcmCipher, Closeable {
    private static final Logger logger = Logger.getLogger(NcipherGcmCipher.class.getName());

    // module number
    private final long moduleId;

    // connection to hardserver
    private EasyConnection conn;

    // nCipher key ID, either borrowed from an existing nCKey, or else representing a transient session key we imported for this operation
    private M_KeyID keyId;

    // true if we created the keyId and must destroy it when we are finished with it
    private boolean keyIdNeedsToBeDestroyed;

    // channel for encryption operation, when open
    private Channel channel;

    // holds reference to JCE key we were initialized with, so it doesn't get GC'ed until we are finished using it
    @SuppressWarnings({"UnusedDeclaration"})
    private Key key;

    NcipherGcmCipher(long moduleId) {
        this.moduleId = moduleId;
    }

    @Override
    public void init(int cipherMode, Key key, AlgorithmParameterSpec spec) throws InvalidKeyException, InvalidAlgorithmParameterException {
        close();

        final M_IV iv = prepareIv(spec);

        int ncMode;
        if (cipherMode == Cipher.ENCRYPT_MODE) {
            ncMode = M_ChannelMode.Encrypt;
        } else if (cipherMode == Cipher.DECRYPT_MODE) {
            ncMode = M_ChannelMode.Decrypt;
        } else {
            throw new InvalidAlgorithmParameterException("Invalid cipher mode: " + cipherMode);
        }

        prepareKey(key);
        // From this point on, we should call close() if there's a fatal error, to avoid leaking the key ID for the finalizer to clean up

        try {
            channel = conn.openChannel(ncMode, keyId, M_Mech.RijndaelmGCM, iv, false, false);
        } catch (Exception e) {
            close();
            throw new InvalidAlgorithmParameterException("Unable to initialize nCipher channel for GCM encryption: " + ExceptionUtils.getMessage(e), e);
        }

    }

    @Override
    public byte[] update(byte[] bytes, int offset, int len) {
        if (channel == null) throw new IllegalStateException("Not initialized");

        if (offset != 0 || len != bytes.length) {
            // If only a subset of the chunk is being processed, it must be truncated
            byte[] newbytes = new byte[len];
            System.arraycopy(bytes, offset, newbytes, 0, len);
            bytes = newbytes;
        }

        try {
            return channel.update(bytes, false, false);
        } catch (Exception e) {
            throw new RuntimeException("Error while performing AES-GCM with nCipher: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public byte[] doFinal() throws IllegalBlockSizeException, BadPaddingException {
        if (channel == null) throw new IllegalStateException("Not initialized");
        try {
            return channel.update(new byte[0], true, false);
        } catch (Exception e) {
            throw new RuntimeException("Error while performing AES-GCM with nCipher: " + ExceptionUtils.getMessage(e), e);
        } finally {
            close();
        }
    }

    private EasyConnection connection() throws InvalidAlgorithmParameterException {
        if (conn == null) {
            conn = new EasyConnection(nCipherKM.getConnection());
        }
        return conn;
    }

    public void close() {
        if (conn != null) {
            if (keyId != null && keyIdNeedsToBeDestroyed) {
                try {
                    conn.destroy(keyId);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Unable to destroy nCipher key ID: " + ExceptionUtils.getMessage(e), e);
                }
                keyId = null;
                keyIdNeedsToBeDestroyed = false;
            }

            EasyConnection c = conn;
            conn = null;
        }
        key = null;
    }

    private static M_IV prepareIv(AlgorithmParameterSpec spec) throws InvalidAlgorithmParameterException {
        if (!(spec instanceof IvParameterSpec)) {
            throw new InvalidAlgorithmParameterException("Spec must be IvParameterSpec");
        }
        IvParameterSpec ivspec = (IvParameterSpec) spec;
        byte[] ivBytes = ivspec.getIV();
        if (ivBytes == null) throw new InvalidAlgorithmParameterException("IV is null");
        if (ivBytes.length != 12) throw new InvalidAlgorithmParameterException("IV must be 12 bytes long");
        M_Mech_IV ivData = new M_Mech_IV_GenericGCM128(new M_ByteBlock(ivBytes), new M_ByteBlock(new byte[0]), 16L);
        return new M_IV(M_Mech.RijndaelmGCM, ivData);
    }

    // If this method returns normally, this.keyId is a valid key identifier of an nCore session symmetric key
    // As a side-effect, the local field "conn" will have been populated if necessary
    private void prepareKey(Key key) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (key instanceof nCKey) {
            this.keyIdNeedsToBeDestroyed = false;
            this.keyId = ((nCKey)key).getKeyID();
        } else if (key instanceof SecretKey) {
            final byte[] bytes = key.getEncoded();
            if (bytes == null || bytes.length < 1)
                throw new InvalidKeyException("Unable to import symmetric key into nCipher session -- missing or empty encoded key bytes");
            this.keyIdNeedsToBeDestroyed = true;
            this.keyId = importSymmetricKey(bytes);
        } else {
            throw new InvalidKeyException("Unable to import symmetric key into nCipher session -- key is of unsupported type " + key.getClass());
        }
        this.key = key;
    }

    private M_KeyID importSymmetricKey(byte[] secretBytes) throws InvalidAlgorithmParameterException, InvalidKeyException {
        if (keyId != null)
            throw new IllegalStateException("A key ID is already prepared");
        EasyConnection conn = connection();
        M_KeyType_Data ktdata = new M_KeyType_Data_Random(new M_ByteBlock(secretBytes));
        M_KeyData keyData = new M_KeyData(M_KeyType.Rijndael, ktdata);
        M_ACL acl = conn.makeSimpleACL(M_Act_Details_OpPermissions.perms_Encrypt | M_Act_Details_OpPermissions.perms_Decrypt);
        M_Cmd_Args_Import impargs = new M_Cmd_Args_Import(new M_ModuleID(moduleId), keyData, acl, new M_AppData(new byte[64]));
        try {
            M_Reply rep = conn.transactChecked(new M_Command(M_Cmd.Import, 0, impargs));
            return ((M_Cmd_Reply_Import)rep.reply).key;
        } catch (Exception e) {
            throw new InvalidKeyException("Unable to import AES symmetric key into nCipher session: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
