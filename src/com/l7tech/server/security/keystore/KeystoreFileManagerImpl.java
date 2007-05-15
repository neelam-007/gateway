package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.EntityType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

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
}
