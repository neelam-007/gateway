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

    public PersistentEntityImp() {
        _oid = DEFAULT_OID;
        _loadTime = System.currentTimeMillis();
        _locked = false;
    }

    protected PersistentEntityImp(final PersistentEntityImp entity) {
        this();
        setOid(entity.getOid());
        setVersion(entity.getVersion());
    }

    public long getOid() {
        return _oid;
    }

    public Long getOidAsLong() {
        return _oidObject;
    }

    public String getId() {
        return Long.toString(_oid);
    }

    public void setOid( long oid ) {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked entity");
        _oid = oid;
        _oidObject = new Long(oid);
    }

    public int getVersion() {
        return _version;
    }

    public void setVersion(int version) {
        if ( isLocked() ) throw new IllegalStateException("Cannot update locked entity");
        _version = version;
    }

    @SuppressWarnings( { "RedundantIfStatement" } )
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PersistentEntityImp entityImp = (PersistentEntityImp)o;

        if (_oid != entityImp._oid) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return (int)(_oid ^ (_oid >>> 32));
    }

    protected void lock() {
        _locked = true;
    }

    protected boolean isLocked() {
        return _locked;
    }

    protected int _version;
	protected long _oid;
    protected Long _oidObject;
    protected final transient long _loadTime;
    protected transient boolean _locked; // read-only when locked
}
