package com.l7tech.server.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;

/**
 * Interface that provides the ability to do CRUD operations on KeystoreFile rows in the database.
 */
public interface KeystoreFileManager extends EntityManager<KeystoreFile, EntityHeader> {
}
