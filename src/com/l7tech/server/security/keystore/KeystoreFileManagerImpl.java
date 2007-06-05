package com.l7tech.server.security.keystore;

import com.l7tech.common.util.Functions;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.UpdateException;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Hibernate manager for read/write access to the keystore table.
 *
 * @author mike@layer7-tech.com
 */
@Transactional(propagation= Propagation.REQUIRED)
public class KeystoreFileManagerImpl
        extends HibernateEntityManager<KeystoreFile, EntityHeader>
        implements KeystoreFileManager
{
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
                        KeystoreFile keystoreFile = (KeystoreFile)session.load(KeystoreFile.class, id, LockMode.UPGRADE);
                        byte[] bytesBefore = keystoreFile.getDatabytes();
                        byte[] bytesAfter = mutator.call(bytesBefore);
                        keystoreFile.setDatabytes(bytesAfter);
                        update(keystoreFile);
                        return null;
                    } catch (UpdateException e) {
                        throw new HibernateException(e);
                    }
                }
            });
        } catch (Exception e) {
            throw new UpdateException(e);
        }
    }
}
