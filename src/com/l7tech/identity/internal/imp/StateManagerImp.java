/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.*;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;

/**
 * @author alex
 */
public class StateManagerImp extends HibernateEntityManager implements StateManager {
    public StateManagerImp( PersistenceContext context ) {
        super( context );
    }

    public StateManagerImp() {
        super();
    }

    public State findByPrimaryKey(long oid) throws FindException {
        try {
            return (State)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }

    }

    public void delete(State state) throws DeleteException {
        try {
            _manager.delete( getContext(), state );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(State state) throws SaveException {
        try {
            return _manager.save( getContext(), state );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( State state ) throws UpdateException {
        try {
            _manager.update( getContext(), state );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public String getTableName() {
        return "state";
    }

    public Class getImpClass() {
        return StateImp.class;
    }

    public Class getInterfaceClass() {
        return State.class;
    }

}
