package com.l7tech.identity;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    User findByPrimaryKey( String oid ) throws FindException;
    User findByLogin( String login ) throws FindException;
    void delete( User user ) throws DeleteException;
    long save( User user ) throws SaveException;
    void update( User user ) throws UpdateException;

    EntityHeader userToHeader(User user);
    User headerToUser(EntityHeader header);
}
