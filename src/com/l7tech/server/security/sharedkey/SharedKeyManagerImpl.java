package com.l7tech.server.security.sharedkey;

import com.l7tech.common.util.EncryptionUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.KeystoreUtils;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataAccessException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private KeystoreUtils keystore;
    public static final String CIPHER = "RSA/ECB/PKCS1Padding";

    /**
     * Don't call this, you are supposed to get an instance of this through the
     * web application context
     */
    public SharedKeyManagerImpl() {}

    public KeystoreUtils getKeystore() {
        return keystore;
    }

    public void setKeystore(KeystoreUtils keystore) {
        this.keystore = keystore;
    }

    /**
     * Get the shared key
     * @return byte[64] containing the shared symmetric key (raw, unencrypted)
     * @throws com.l7tech.objectmodel.FindException if there was a problem finding or creating the key
     */
    public byte[] getSharedKey() throws FindException {
        final String currentSSLPubID;
        try {
            currentSSLPubID = getCurrentSSLPubID();
        } catch (IOException e) {
            throw new FindException("could not get current SSL key id", e);
        }
        // try to get record from the database
        final String query = " from shared_keys in class " + SharedKeyRecord.class.getName() +
                             " where shared_keys.encodingID = ?";
        final Collection res = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Query q = session.createQuery(query);
                q.setString(0, currentSSLPubID);
                return q.list();
            }});

        if (res != null && res.size() > 0) {
            SharedKeyRecord keyRecord = (SharedKeyRecord)res.toArray()[0];
            try {
                logger.fine("Shared key found, attempting to decrypt it");
                return decryptKey(keyRecord.getB64edKey());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Cannot decrypt shared key", e);
                throw new FindException("could not decrypt shared key", e);
            }
        } else {
            logger.info("Shared key does not yet exist, attempting to create one");
            final SharedKeyRecord sharedKeyToSave = new SharedKeyRecord();
            byte[] theKey = initializeKeyFirstTime();

            try {
                sharedKeyToSave.setEncodingID(currentSSLPubID);
                sharedKeyToSave.setB64edKey(encryptKey(theKey));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Could not encrypt new shared key", e);
                throw new FindException("could not encrypt new shared key", e);
            }
            logger.info("new shared created, saving it");
            try {
                getHibernateTemplate().execute(new HibernateCallback() {
                    public Object doInHibernate(Session session) throws HibernateException, SQLException {
                        session.save(sharedKeyToSave);
                        session.flush();
                        return null;
                    }
                });
            } catch (DataAccessException e) {
                throw new FindException("Unable to save new key", e);
            }
            logger.info("new shared key saved, returning it");
            return theKey;
        }
    }

    private String getCurrentSSLPubID() throws IOException {
        return EncryptionUtil.computeCustomRSAPubKeyID((RSAPublicKey)(keystore.getSslSignerInfo().getPublic()));
    }

    private String encryptKey(byte[] toEncrypt) throws NoSuchAlgorithmException, NoSuchPaddingException,
                                                       InvalidKeyException, BadPaddingException,
                                                       IllegalBlockSizeException, IOException {
        return EncryptionUtil.rsaEncAndB64(toEncrypt, keystore.getSslSignerInfo().getPublic());
    }

    private byte[] decryptKey(String b64edEncKey) throws IOException, KeyStoreException,
                                                         NoSuchAlgorithmException, NoSuchPaddingException,
                                                         InvalidKeyException, BadPaddingException,
                                                         IllegalBlockSizeException {
        return EncryptionUtil.deB64AndRsaDecrypt(b64edEncKey, keystore.getSSLPrivateKey());
    }

    private byte[] initializeKeyFirstTime() {
        // this should only be called once the first time the key is created
        byte[] output = new byte[64];
        (new SecureRandom()).nextBytes(output);
        return output;
    }
}
