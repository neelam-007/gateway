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
public class OrganizationManagerImp extends ProviderSpecificEntityManager implements OrganizationManager {
    public OrganizationManagerImp( PersistenceContext context ) {
        super( context );
    }

    public OrganizationManagerImp() {
        super();
    }

    public Organization findByPrimaryKey(long oid) throws FindException {
        try {
            return (Organization)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Organization organization) throws DeleteException {
        try {
            _manager.delete( getContext(), organization );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Organization organization) throws SaveException {
        try {
            return _manager.save( getContext(), organization );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Organization organization ) throws UpdateException {
        try {
            _manager.update( getContext(), organization );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public String getTableName() {
        return "organization";
    }

    public Class getImpClass() {
        return OrganizationImp.class;
    }

    public Class getInterfaceClass() {
        return Organization.class;
    }

}
