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
public class EntityImp implements Entity, Lifecycle, Validatable {
    public long getOid() { return _oid; }

	public void setOid( long oid ) { _oid = oid; }

    public EntityImp() {
    }

	protected long _oid;

    public boolean onSave(Session session) throws CallbackException {
        return false;
    }

    public boolean onUpdate(Session session) throws CallbackException {
        return false;
    }

    public boolean onDelete(Session session) throws CallbackException {
        return false;
    }

    public void onLoad(Session session, Serializable serializable) {
    }

    public void validate() throws ValidationFailure {
    }
}
