/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.ObjectIdentity;

/**
 * @author alex
 */
public class ObjectIdentityImp implements ObjectIdentity {
    public long getOid() {
        return _oid;
    }

    public String getClassName() {
        return _className;
    }

    public String getTableName() {
        return _tableName;
    }

    public int getClassSeed() {
        return _classSeed;
    }

    public int getServerSeed() {
        return _serverSeed;
    }

    public long getKeySeed() {
        return _keySeed;
    }

    public int getKeyBatchSize() {
        return _keyBatchSize;
    }

    public void setOid(long oid) {
        _oid = oid;
    }

    public void setClassName(String className) {
        _className = className;
    }

    public void setTableName(String tableName) {
        _tableName = tableName;
    }

    public void setClassSeed(int classSeed) {
        _classSeed = classSeed;
    }

    public void setServerSeed(int serverSeed) {
        _serverSeed = serverSeed;
    }

    public void setKeySeed(long keySeed) {
        _keySeed = keySeed;
    }

    public void setKeyBatchSize(int keyBatchSize) {
        _keyBatchSize = keyBatchSize;
    }

    private long _oid;
    private String _className;
    private String _tableName;
    private int _classSeed;
    private int _serverSeed;
    private long _keySeed;
    private int _keyBatchSize;
}
