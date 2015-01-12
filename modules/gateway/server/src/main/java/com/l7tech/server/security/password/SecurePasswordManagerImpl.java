package com.l7tech.server.security.password;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.security.sharedkey.SharedKeyManager;
import com.l7tech.util.MasterPasswordManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of {@link SecurePasswordManager}.
 */
@Transactional(propagation=Propagation.SUPPORTS, rollbackFor=Throwable.class)
public class SecurePasswordManagerImpl extends HibernateEntityManager<SecurePassword, EntityHeader> implements SecurePasswordManager {
    private final SharedKeyManager sharedKeyManager;
    private final AtomicReference<MasterPasswordManager> encryptor = new AtomicReference<MasterPasswordManager>();

    public SecurePasswordManagerImpl(SharedKeyManager skm) {
        this.sharedKeyManager = skm;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SecurePassword.class;
    }

    @Override
    public String encryptPassword(char[] plaintext) throws FindException {
        return getEncryptor().encryptPassword(plaintext);
    }

    @Override
    public char[] decryptPassword(String encodedPassword) throws FindException, ParseException {
        return getEncryptor().decryptPassword(encodedPassword);
    }

    private MasterPasswordManager getEncryptor() throws FindException {
        MasterPasswordManager ret = encryptor.get();
        if (ret == null) {
            synchronized (encryptor) {
                ret = encryptor.get();
                if (ret == null) {
                    ret = new MasterPasswordManager( sharedKeyManager.getSharedKey(), true );
                    encryptor.set(ret);
                }
            }
        }
        return ret;
    }
}
