package com.l7tech.identity;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    User findByPrimaryKey( String identifier ) throws FindException;
    User findByLogin( String login ) throws FindException;
    void delete( User user ) throws DeleteException;
    void delete( String identifier ) throws DeleteException;
    String save( User user ) throws SaveException;
    String save( UserBean user ) throws SaveException;
    void update( User user ) throws UpdateException;
    void update( UserBean user ) throws UpdateException;

    EntityHeader userToHeader(User user);
    User headerToUser(EntityHeader header);
}
