package com.l7tech.identity;

import com.l7tech.objectmodel.*;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    public User findByPrimaryKey( String oid ) throws FindException;
    public User findByLogin( String login ) throws FindException;
    public void delete( User user ) throws DeleteException;
    public long save( User user ) throws SaveException;
    public void update( User user ) throws UpdateException;
    public EntityHeader userToHeader(User user);
    public User headerToUser(EntityHeader header);
}
