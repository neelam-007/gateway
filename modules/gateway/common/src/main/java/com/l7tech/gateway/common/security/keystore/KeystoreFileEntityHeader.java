package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;

/**
 * User: dlee
 * Date: Feb 4, 2009
 */
public class KeystoreFileEntityHeader extends EntityHeader {
    private final String keyStoreType;
    private final boolean readonly;

    public KeystoreFileEntityHeader(Goid goid, String name, String keyStoreType, boolean readonly) {
        super(goid, EntityType.SSG_KEYSTORE, name, name);
        this.keyStoreType = keyStoreType;
        this.readonly = readonly;
    }

    public KeystoreFileEntityHeader(final KeystoreFileEntityHeader header) {
        this(header.getGoid(), header.getName(), header.getKeyStoreType(), header.isReadonly());
    }

    public boolean isReadonly() {
        return readonly;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }
}
