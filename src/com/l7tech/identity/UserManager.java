package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public interface UserManager extends EntityManager {
    User findByPrimaryKey( String identifier ) throws FindException;
    User findByLogin( String login ) throws FindException;
    void delete( User user ) throws DeleteException, ObjectNotFoundException;
    void delete( String identifier ) throws DeleteException, ObjectNotFoundException;
    String save( User user ) throws SaveException;
    void update( User user ) throws UpdateException, ObjectNotFoundException;
    String save( User user, Set groupHeaders ) throws SaveException;
    void update( User user, Set groupHeaders ) throws UpdateException, ObjectNotFoundException;
    Collection search(String searchString) throws FindException;
    EntityHeader userToHeader(User user);
    Class getImpClass();
}
