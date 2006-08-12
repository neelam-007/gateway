/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.PersistentEntity;

import java.io.Serializable;

/**
 * @author alex
 */
public class PersistentEntityImp implements PersistentEntity, Serializable {
    public long getOid() { return _oid; }

    public String getId() { return Long.toString(_oid); }

    public void setOid( long oid ) { _oid = oid; }

    public PersistentEntityImp() {
        _oid = DEFAULT_OID;
        _loadTime = System.currentTimeMillis();
    }

    public int getVersion() { return _version; }
    public void setVersion(int version) { _version = version; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PersistentEntityImp entityImp = (PersistentEntityImp)o;

        if (_oid != entityImp._oid) return false;

        return true;
    }

    public int hashCode() {
        return (int)(_oid ^ (_oid >>> 32));
    }

    protected int _version;
	protected long _oid;
    protected transient long _loadTime;
}
