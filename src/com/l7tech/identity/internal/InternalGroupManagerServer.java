package com.l7tech.identity.internal;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdProvConfManagerServer;
import com.l7tech.logging.LogManager;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 24, 2003
 */
public class InternalGroupManagerServer extends HibernateEntityManager implements GroupManager {
    public InternalGroupManagerServer() {
        super();
        logger = LogManager.getInstance().getSystemLogger();
    }

    public Group findByName(String name) throws FindException {
        try {
            List groups = _manager.find(getContext(), "from " + getTableName() + " in class " + getImpClass().getName() + " where " + getTableName() + ".name = ?", name, String.class);
            switch (groups.size()) {
                case 0:
                    return null;
                case 1:
                    Group g = (Group)groups.get(0);
                    g.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
                    return g;
                default:
                    String err = "Found more than one group with the name " + name;
                    logger.log(Level.SEVERE, err);
                    throw new FindException(err);
            }
        } catch (SQLException se) {
            logger.log(Level.SEVERE, null, se);
            throw new FindException(se.toString(), se);
        }

    }


    public Group findByPrimaryKey(String oid) throws FindException {
        try {
            Group out = (Group)_manager.findByPrimaryKey(getContext(), getImpClass(), Long.parseLong(oid));
            out.setProviderId(IdProvConfManagerServer.INTERNALPROVIDER_SPECIAL_OID);
            return out;
        } catch (SQLException se) {
            throw new FindException(se.toString(), se);
        } catch (NumberFormatException nfe) {
            throw new FindException("Can't find groups with non-numeric OIDs!", nfe);
        }
    }

    /**
     * Search for the group headers using the given search string.
     * 
     * @param searchString the search string (supports '*' wildcards)
     * @return the never <b>null</b> collection of entitites
     * @throws FindException thrown if an SQL error is encountered
     * @see InternalGroupManagerServer
     * @see InternalUserManagerServer
     */
    public Collection search(String searchString) throws FindException {
        // replace wildcards to match stuff understood by mysql
        // replace * with % and ? with _
        // note. is this portable?
        searchString = searchString.replace('*', '%');
        searchString = searchString.replace('?', '_');
        try {
            List results = PersistenceManager.find(getContext(),
              allHeadersQuery + " where " + getTableName() + ".name like ?",
              searchString, String.class);
            List headers = new ArrayList();
            for (Iterator i = results.iterator(); i.hasNext();) {
                Object[] row = (Object[])i.next();
                final long id = ((Long)row[0]).longValue();
                headers.add(new EntityHeader(id, EntityType.fromInterface(getInterfaceClass()), row[1].toString(), EMPTY_STRING));
            }
            return Collections.unmodifiableList(headers);
        } catch (SQLException e) {
            final String msg = "Error while searching for " + getInterfaceClass() + " instances.";
            logger.log(Level.SEVERE, msg, e);
            throw new FindException(msg, e);
        }
    }

    public void delete(Group group) throws DeleteException {
        try {
            // it is not allowed to delete the admin group
            if (Group.ADMIN_GROUP_NAME.equals(group.getName())) {
                logger.severe("an attempt to delete the admin group was made.");
                throw new DeleteException("Cannot delete administrator group.");
            }
            _manager.delete(getContext(), group);
        } catch (SQLException se) {
            throw new DeleteException(se.toString(), se);
        }
    }

    public long save(Group group) throws SaveException {
        try {
            // check that no existing group have same name
            Group existingGrp = null;
            try {
                existingGrp = findByName(group.getName());
            } catch (FindException e) {
                existingGrp = null;
            }
            if (existingGrp != null) {
                throw new SaveException("This group cannot be saved because an existing group already uses the name '" + group.getName() + "'");
            }
            return _manager.save(getContext(), group);
        } catch (SQLException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    public void update(Group group) throws UpdateException {
        try {
            // if this is the admin group, make sure that we are not removing all memberships
            if (Group.ADMIN_GROUP_NAME.equals(group.getName())) {
                if (group.getMembers().size() < 1) {
                    logger.severe("Blocked update on admin group because all members were deleted.");
                    throw new UpdateException("Cannot update admin group with no memberships!");
                }
            }
            Group originalGroup = findByPrimaryKey(Long.toString(group.getOid()));
            originalGroup.copyFrom(group);
            _manager.update(getContext(), originalGroup);
        } catch (FindException e) {
            throw new UpdateException("Update called on group that does not already exist", e);
        } catch (SQLException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    public EntityHeader groupToHeader(Group group) {
        return new EntityHeader(group.getOid(), EntityType.GROUP, group.getName(), group.getDescription());
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getStrId());
    }

    public Set groupsToHeaders(Set groups) {
        Group group;
        EntityHeader header;
        Set result = new HashSet();
        for (Iterator i = groups.iterator(); i.hasNext();) {
            group = (Group)i.next();
            header = groupToHeader(group);
            result.add(header);
        }
        return result;
    }

    public Set headersToGroups(Set headers) throws FindException {
        Group group;
        EntityHeader header;
        Set result = new HashSet();
        for (Iterator i = headers.iterator(); i.hasNext();) {
            header = (EntityHeader)i.next();
            if (header.getType() == EntityType.GROUP) {
                group = headerToGroup(header);
                result.add(group);
            } else {
                IllegalArgumentException iae = new IllegalArgumentException("EntityHeader " + header + " doesn't represent a Group!");
                logger.throwing(getClass().getName(), "headersToGroups", iae);
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

    private Logger logger = null;
}
