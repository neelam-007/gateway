package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.*;
import com.l7tech.common.util.Functions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.TransactionStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Query;

import java.util.logging.Logger;
import java.sql.SQLException;

/**
 * Hibernate manager for read/write access to the keystore table.
 *
 * @author mike@layer7-tech.com
 */
@Transactional(propagation= Propagation.REQUIRED)
public class KeystoreFileManagerImpl
        extends HibernateEntityManager<KeystoreFile, EntityHeader>
        implements KeystoreFileManager {
    private final Logger logger = Logger.getLogger(KeystoreFileManagerImpl.class.getName());

    private final String HQL_FIND_BY_NAME =
            "from " + getTableName() +
                    " in class " + KeystoreFile.class.getName() +
                    " where " + getTableName() + ".name = ?";

    public KeystoreFileManagerImpl() {}

    public Class getImpClass() {
        return KeystoreFile.class;
    }

    public Class getInterfaceClass() {
        return KeystoreFile.class;
    }

    public String getTableName() {
        return "keystore";
    }

    public EntityType getEntityType() {
        return EntityType.PRIVATE_KEY;
    }

    public void updateDataBytes(final long id, final Functions.Unary<byte[], byte[]> mutator) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    try {
                        KeystoreFile keystoreFile = findByPrimaryKey(id);
                        byte[] bytesBefore = keystoreFile.getDatabytes();
                        byte[] bytesAfter = mutator.call(bytesBefore);
                        keystoreFile.setDatabytes(bytesAfter);
                        save(keystoreFile);
                        return null;
                    } catch (FindException e) {
                        throw new HibernateException(e);
                    } catch (SaveException e) {
                        throw new HibernateException(e);
                    }
                }
            });
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }
}
