package com.l7tech.objectmodel;

public interface Entity {
    public static final long DEFAULT_OID = -1L;

    int getVersion();
    void setVersion( int version );
    long getOid();
    void setOid( long oid );
    long getLoadTime();
}
