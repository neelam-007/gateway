package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.identity.User;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

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

    public Group findByName( String name ) throws FindException {
        try {
            List groups = _manager.find( getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".name = ?", name, String.class );
            switch ( groups.size() ) {
            case 0:
                return null;
            case 1:
                Group g = (Group)groups.get(0);
                g.setProviderId( IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID );
                return g;
            default:
                String err = "Found more than one group with the name " + name;
                LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
                throw new FindException( err );
            }
        } catch ( SQLException se ) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, null, se);
            throw new FindException( se.toString(), se );
        }

    }

    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List groups = _manager.find( getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".name like ?", searchString, String.class );
            Collection output = new ArrayList();
            LogManager.getInstance().getSystemLogger().log(Level.INFO, "search for " + searchString + " returns " + groups.size() + " groups.");
            for (Iterator i = groups.iterator(); i.hasNext();) {
                output.add(groupToHeader((Group)i.next()));
            }
            return output;
        } catch (SQLException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "exception searching groups with pattern " + searchString, e);
            throw new FindException(e.toString(), e);
        }
    }

    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            Group out = (Group)_manager.findByPrimaryKey( getContext(), getImpClass(), Long.parseLong(oid) );
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        } catch ( NumberFormatException nfe ) {
            throw new FindException( "Can't find groups with non-numeric OIDs!", nfe );
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
