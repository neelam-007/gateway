/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Entity;

import java.io.Serializable;

/**
 * @author alex
 */
public class EntityImp implements Entity, Serializable {
    public long getOid() { return _oid; }

	public void setOid( long oid ) { _oid = oid; }

    public EntityImp() {
        _oid = DEFAULT_OID;
        _loadTime = System.currentTimeMillis();
    }

    public long getLoadTime() {
        return _loadTime;
    }

	protected long _oid;

    protected transient long _loadTime;
}
