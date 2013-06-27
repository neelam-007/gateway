package com.l7tech.objectmodel;

@Deprecated // deprecated by com.l7tech.objectmodel.GoidEntity
public interface PersistentEntity extends Entity {
    public static final long DEFAULT_OID = -1L;

    int getVersion();
    void setVersion( int version );
    long getOid();
    void setOid( long oid );
}
