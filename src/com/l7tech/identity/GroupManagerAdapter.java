/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Set;
import java.util.Collection;

/**
 * @author alex
 * @version $Revision$
 */
public class GroupManagerAdapter implements GroupManager {
    public Group findByPrimaryKey(String oid) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Group findByName(String name) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void delete(Group group) throws DeleteException {
        throw new UnsupportedOperationException();
    }

    public long save(Group group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    public void update(Group group) throws UpdateException {
        throw new UnsupportedOperationException();
    }

    public EntityHeader groupToHeader(Group group) {
        throw new UnsupportedOperationException();
    }

    public Group headerToGroup(EntityHeader header) throws FindException {
        throw new UnsupportedOperationException();
    }

    public boolean isMember( User user, Group group ) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void addUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUsers(Group group, Set users) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void addUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUser(User user, Set groups) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void addUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void removeUser(User user, Group group) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public Set getGroups(User user) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Set getUsers(Group group) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection findAllHeaders() throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection findAll() throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new UnsupportedOperationException();
    }
}
