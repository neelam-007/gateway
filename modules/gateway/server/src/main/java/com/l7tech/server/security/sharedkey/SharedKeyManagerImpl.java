package com.l7tech.server.security.sharedkey;

import com.l7tech.objectmodel.FindException;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Functions;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionCallback;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidParameterSpecException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allow SSG access to a symmetric key shared throughout the cluster. This key is
 * stored in the database, itself secured with the SSL keystore (encrypted with pub
 * key and decrypted with the private key).
 *
 * The shared key is initially created the first time it is requested. This is done
 * atomically through the cluster.
 *
 * Whenever the SSL keystore, the shared key will be re-encrypted using the new SSL
 * keystore. This will be handled by the Config Wizard.
 * 
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 18, 2007<br/>
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SharedKeyManagerImpl extends HibernateDaoSupport implements SharedKeyManager {
    private final Logger logger = Logger.getLogger(SharedKeyManagerImpl.class.getName());

    public static final String CLUSTER_WIDE_IDENTIFIER = "%ClusterWidePBE%";
    public static final String CIPHER = "PBEWithSHA1AndDESede";

    private final PlatformTransactionManager transactionManager;
    private SecretKey sharedKeyDecryptionKey;

    /**
     * Create a SharedKeyManager that will decrypt the shared key using a symmetric key derived from the
     * specified passphrase.
     * @param clusterPassphrase passphrase to be used to create PBE key to decrypt cluster shared key
     *                          (or to encrypt it if a new one needs to be created).
     */
    public SharedKeyManagerImpl(final char[] clusterPassphrase,
                                final PlatformTransactionManager transactionManager) {
        if ( clusterPassphrase == null || clusterPassphrase.length==0 ) throw new IllegalArgumentException("clusterPassphrase is required");
        if ( transactionManager == null ) throw new IllegalArgumentException("transactionManager is required");
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
            this.sharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(clusterPassphrase));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize decryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
        }
        this.transactionManager = transactionManager;
    }

    /**
     * For tests only.
     */
    SharedKeyManagerImpl(final char[] clusterPassphrase) {
        if ( clusterPassphrase == null || clusterPassphrase.length==0 ) throw new IllegalArgumentException("clusterPassphrase is required");
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER);
            this.sharedKeyDecryptionKey = skf.generateSecret(new PBEKeySpec(clusterPassphrase));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Unable to initialize decryption key for cluster shared key: " + ExceptionUtils.getMessage(e), e);
        }
        this.transactionManager = null;
    }

    /**
     * Get the shared key.
     *
     * @return byte[64] containing the shared symmetric key (raw, unencrypted)
     * @throws com.l7tech.objectmodel.FindException if there was a problem finding or creating the key
     */
    public byte[] getSharedKey() throws FindException {
        final Collection<SharedKeyRecord> res = selectSharedKeyRecordsByEncodingId();

        if (res != null && res.size() > 0) {
            SharedKeyRecord keyRecord = (SharedKeyRecord)res.toArray()[0];
            try {
                logger.fine("Shared key found, attempting to decrypt it");
                return decryptKey(keyRecord.getB64edKey());
            } catch (Exception e) {
                throw new FindException("could not decrypt shared key: " + ExceptionUtils.getMessage(e), e);
            }
        }

        return generateAndSaveNewKey();
    }

    private byte[] generateAndSaveNewKey() throws FindException {
        logger.info("Shared key does not yet exist, attempting to create one");
        final SharedKeyRecord sharedKeyToSave = new SharedKeyRecord();
        final byte[] theKey = generate64RandomBytes();

        try {
            sharedKeyToSave.setEncodingID(CLUSTER_WIDE_IDENTIFIER);
            sharedKeyToSave.setB64edKey(encryptKey(theKey));
        } catch (Exception e) {
            throw new FindException("could not encrypt new shared key: " + ExceptionUtils.getMessage(e), e);
        }

        logger.info("new shared created, saving it");

        try {
            transactionIfAvailable(new Functions.Nullary<Object>(){
                public Object call() {
                    saveSharedKeyRecord(sharedKeyToSave);
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new FindException("Unable to save new key: " + ExceptionUtils.getMessage(e), e);
        }

        logger.info("new shared key saved, returning it");
        return theKey;
    }

    @SuppressWarnings({"unchecked"})
    private <T> T transactionIfAvailable( final Functions.Nullary<T> func ) {
        if ( transactionManager != null ) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setPropagationBehavior( TransactionTemplate.PROPAGATION_REQUIRES_NEW );
            return (T) template.execute(new TransactionCallback(){
                public Object doInTransaction(final TransactionStatus transactionStatus) {
                   return func.call();
                }
            });
        } else {
            return func.call();
        }
    }

    protected void saveSharedKeyRecord(final SharedKeyRecord sharedKeyToSave) throws DataAccessException {
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.save(sharedKeyToSave);
                session.flush();
                return null;
            }
        });
    }

    protected Collection<SharedKeyRecord> selectSharedKeyRecordsByEncodingId() {
        // try to get record from the database
        final String query = " from shared_keys in class " + SharedKeyRecord.class.getName() +
                             " where shared_keys.encodingID = ?";
        //noinspection unchecked
        return (Collection<SharedKeyRecord>)getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Query q = session.createQuery(query);
                q.setString(0, CLUSTER_WIDE_IDENTIFIER);
                return q.list();
            }});
    }

    private String encryptKey(byte[] toEncrypt) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException, IOException, InvalidParameterSpecException
    {
        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, sharedKeyDecryptionKey);
        byte[] cipherBytes = cipher.doFinal(toEncrypt);
        PBEParameterSpec pbeSpec = cipher.getParameters().getParameterSpec(PBEParameterSpec.class);
        byte[] salt = pbeSpec.getSalt();
        int itc = pbeSpec.getIterationCount();
        return "$PBE1$" + HexUtils.encodeBase64(salt, true) + "$" + itc + "$" + HexUtils.encodeBase64(cipherBytes, true) + "$";
    }

    private byte[] decryptKey(String b64edEncKey) throws IOException, GeneralSecurityException
    {
        Pattern pattern = Pattern.compile("^\\$PBE1\\$([^$]+)\\$(\\d+)\\$([^$]+)\\$$");
        Matcher matcher = pattern.matcher(b64edEncKey);
        if (!matcher.matches())
            throw new IOException("Invalid shared key format: " + b64edEncKey);
        String b64edSalt = matcher.group(1);
        String strIterationCount = matcher.group(2);
        String b64edCiphertext = matcher.group(3);

        byte[] saltbytes;
        int iterationCount;
        byte[] cipherbytes;
        try {
            saltbytes = HexUtils.decodeBase64(b64edSalt);
            iterationCount = Integer.parseInt(strIterationCount);
            cipherbytes = HexUtils.decodeBase64(b64edCiphertext);
        } catch (NumberFormatException nfe) {
            throw new IOException("Invalid shared key format: " + b64edEncKey, nfe);
        }

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, sharedKeyDecryptionKey, new PBEParameterSpec(saltbytes, iterationCount));
        return cipher.doFinal(cipherbytes);
    }

    private byte[] generate64RandomBytes() {
        // this should only be called once the first time the key is created
        byte[] output = new byte[64];
        (new SecureRandom()).nextBytes(output);
        return output;
    }
}
