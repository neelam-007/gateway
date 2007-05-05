package com.l7tech.server.security.keystore.software;

import com.l7tech.cluster.ClusterProperty;
import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.security.keystore.JdkKeyStoreBackedSsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyEntry;
import com.l7tech.server.security.keystore.SsgKeyStore;

import javax.naming.ldap.LdapName;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

/**
 * A KeyFinder that works with PKCS#12 files read from the database.
 */
public class DatabasePkcs12SsgKeyStore extends JdkKeyStoreBackedSsgKeyStore implements SsgKeyStore {
    protected static final Logger logger = Logger.getLogger(DatabasePkcs12SsgKeyStore.class.getName());
    private static final long refreshTime = 5 * 60 * 1000;

    private final int id;
    private final String name;
    private final ClusterPropertyManager cpm;
    private final char[] password;

    private KeyStore cachedKeystore = null;
    private long lastLoaded = 0;

    public DatabasePkcs12SsgKeyStore(int id, String name, ClusterPropertyManager cpm, char[] password) {
        this.id = id;
        this.name = name;
        this.cpm = cpm;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SsgKeyStoreType getType() {
        return SsgKeyStoreType.PKCS12_SOFTWARE;
    }

    protected synchronized KeyStore keyStore() throws KeyStoreException {
        if (cachedKeystore == null || System.currentTimeMillis() - lastLoaded > refreshTime) {
            cachedKeystore = loadFromDatabase();
            lastLoaded = System.currentTimeMillis();
        }
        return cachedKeystore;
    }

    protected Logger getLogger() {
        return logger;
    }

    private KeyStore loadFromDatabase() throws KeyStoreException {
        try {
            String b64 = cpm.getProperty(getClusterPropertyName());
            byte[] bytes = HexUtils.decodeBase64(b64);
            InputStream inputStream = new ByteArrayInputStream(bytes);
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(inputStream, password);
            return keystore;
        } catch (FindException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to load software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    private String getClusterPropertyName() {
        return "keystore.pkcs12." + getName();
    }

    private void storeToDatabase(KeyStore keystore) throws KeyStoreException {
        BufferPoolByteArrayOutputStream outputStream = new BufferPoolByteArrayOutputStream();
        try {
            keystore.store(outputStream, password);
            String b64 = HexUtils.encodeBase64(outputStream.toByteArray());
            cpm.save(new ClusterProperty(getClusterPropertyName(), b64));
        } catch (IOException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } catch (SaveException e) {
            throw new KeyStoreException("Unable to save software database keystore named " + name + ": " + ExceptionUtils.getMessage(e), e);
        } finally {
            outputStream.close();
        }
    }


    public synchronized void storePrivateKeyEntry(SsgKeyEntry entry) throws KeyStoreException {
        super.storePrivateKeyEntry(entry);
        storeToDatabase(cachedKeystore);
    }

    public synchronized KeyPair generateRsaKeyPair(int keyBits) throws InvalidAlgorithmParameterException {
        return super.generateRsaKeyPair(keyBits);
    }

    public synchronized CertificateRequest makeCsr(LdapName dn, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        return super.makeCsr(dn, keyPair);
    }
}
