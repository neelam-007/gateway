/*
 * Created on 7-May-2003
 */
package com.l7tech.ssg.objectmodel.imp;

import com.l7tech.ssg.objectmodel.ObjectIdentity;

/**
 * @author alex
 */
public class ObjectIdentityImp implements ObjectIdentity {

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
}
