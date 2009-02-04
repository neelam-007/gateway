package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

/**
 * @author jbufu
 */
public class SsgKeyHeader extends EntityHeader {

    private long keystoreId;
    private String alias;

    public SsgKeyHeader(String id, long keystoreId, String alias, String name) {
        super(id, EntityType.SSG_KEY_ENTRY, name, "");
        this.keystoreId = keystoreId;
        this.alias = alias;
    }

    public SsgKeyHeader(SsgKeyEntry ssgKey) {
        this(ssgKey.getId(), ssgKey.getKeystoreId(), ssgKey.getAlias(), ssgKey.getName());
    }

    public long getKeystoreId() {
        return keystoreId;
    }

    public void setKeystoreId(long keystoreId) {
        this.keystoreId = keystoreId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return keystoreId + ":" + alias;
    }
}
