package com.l7tech.ssg.objectmodel;

public interface ObjectIdentity {
    String getClassName();
    String getTableName();
    int getClassSeed();
    int getServerSeed();
    long getKeySeed();
    int getKeyBatchSize();

    void setClassName( String className );
    void setTableName( String tableName );
    void setClassSeed( int classSeed );
    void setServerSeed( int serverSeed );
    void setKeySeed( long keySeed );
    void setKeyBatchSize( int keyBatchSize );
}
