/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal.imp;

import com.l7tech.identity.internal.Country;
import com.l7tech.identity.internal.CountryManager;
import com.l7tech.objectmodel.*;

import java.sql.SQLException;

/**
 * @author alex
 * @version $Revision$
 */
public class CountryManagerImp extends HibernateEntityManager implements CountryManager {
    public static final Class IMPCLASS = CountryImp.class;

    public CountryManagerImp( PersistenceContext context ) {
        super( context );
    }

    public CountryManagerImp() {
        super();
    }

    public Country findByPrimaryKey(long oid) throws FindException {
        try {
            return (Country)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch (SQLException se) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Country country) throws DeleteException {
        try {
            _manager.delete( getContext(), country );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Country country) throws SaveException {
        try {
            return _manager.save( getContext(), country );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Country country ) throws UpdateException {
        try {
            _manager.update( getContext(), country );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public Class getImpClass() {
        return CountryImp.class;
    }

    public Class getInterfaceClass() {
        return Country.class;
    }

    public String getTableName() {
        return "country";
    }

}
