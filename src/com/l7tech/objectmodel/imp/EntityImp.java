/*
 * Created on 7-May-2003
 */
package com.l7tech.objectmodel.imp;

import com.l7tech.objectmodel.Entity;
import cirrus.hibernate.*;

import java.io.Serializable;

/**
 * @author alex
 */
public class EntityImp implements Entity {
    public long getOid() { return _oid; }

	public void setOid( long oid ) { _oid = oid; }

    public EntityImp() {
        _oid = DEFAULT_OID;
    }

	protected long _oid;
}
