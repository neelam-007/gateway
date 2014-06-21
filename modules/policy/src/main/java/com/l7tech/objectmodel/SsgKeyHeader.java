package com.l7tech.objectmodel;

/**
 * @author jbufu
 */
public class SsgKeyHeader extends ZoneableEntityHeader {

    private Goid keystoreId;
    private String alias;

    public SsgKeyHeader(String id, Goid keystoreId, String alias, String name) {
        super(id, EntityType.SSG_KEY_ENTRY, name, "", null);
        this.keystoreId = keystoreId;
        this.alias = alias;
    }

    public Goid getKeystoreId() {
        return keystoreId;
    }

    public void setKeystoreId(Goid keystoreId) {
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
