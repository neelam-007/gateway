/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;

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

    public void delete(Group group) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public String save(Group group) throws SaveException {
        throw new UnsupportedOperationException();
    }

    public void update(Group group) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public String save(Group group, Set userHeaders) throws SaveException {
        throw new UnsupportedOperationException();
    }

    public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
        throw new UnsupportedOperationException();
    }

    public Class getImpClass() {
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

    public Set getGroupHeaders(User user) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Set getGroupHeaders(String userId) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public Set getUserHeaders(Group group) throws FindException {
        throw new UnsupportedOperationException();
    }

    public Set getUserHeaders(String groupId) throws FindException {
        throw new UnsupportedOperationException();
    }

    public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
        throw new UnsupportedOperationException();
    }

    public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
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
