package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.*;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 *
 */
public class InternalGroupManagerServer extends HibernateEntityManager implements GroupManager {
    public InternalGroupManagerServer() {
        super();
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            Group out = (Group)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid) );
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public void delete(Group group) throws DeleteException {
        try {
            _manager.delete( getContext(), group );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public long save(Group group) throws SaveException {
        try {
            return _manager.save( getContext(), group );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( Group group ) throws UpdateException {
        try {
            _manager.update( getContext(), group );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public EntityHeader groupToHeader( Group group ) {
        return new EntityHeader( group.getOid(), EntityType.GROUP, group.getName(), group.getDescription() );
    }

    public Group headerToGroup( EntityHeader header ) throws FindException {
        return findByPrimaryKey( header.getStrId() );
    }

    public Set groupsToHeaders(Set groups) {
        Group group;
        EntityHeader header;
        Set result = Collections.EMPTY_SET;
        for (Iterator i = groups.iterator(); i.hasNext();) {
            group = (Group) i.next();
            if ( result == Collections.EMPTY_SET ) result = new HashSet();
            header = groupToHeader( group );
            result.add( header );
        }
        return result;
    }

    public Set headersToGroups(Set headers) throws FindException {
        Group group;
        EntityHeader header;
        Set result = Collections.EMPTY_SET;
        for (Iterator i = headers.iterator(); i.hasNext();) {
            header = (EntityHeader) i.next();
            if ( header.getType() == EntityType.GROUP ) {
                group = headerToGroup( header );
                if ( result == Collections.EMPTY_SET ) result = new HashSet();
                result.add( group );
            } else {
                IllegalArgumentException iae = new IllegalArgumentException( "EntityHeader " + header + " doesn't represent a Group!" );
                LogManager.getInstance().getSystemLogger().throwing( getClass().getName(), "headersToGroups", iae );
                throw iae;
            }
        }
        return result;
    }


    public String getTableName() {
        return "internal_group";
    }

    public Class getImpClass() {
        return Group.class;
    }

    public Class getInterfaceClass() {
        return Group.class;
    }
}
