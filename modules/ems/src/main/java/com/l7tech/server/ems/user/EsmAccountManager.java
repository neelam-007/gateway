package com.l7tech.server.ems.user;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;

import java.util.Collection;

/**
 * Identity Management interface for the EMS
 */
public interface EsmAccountManager {

    int getUserCount() throws FindException;

    Collection<InternalUser> getUserPage( int start, int count, String property, boolean ascending ) throws FindException;

    InternalUser findByLogin( String login ) throws FindException;

    InternalUser findByPrimaryKey(final String identifier) throws FindException;

    String save( InternalUser user ) throws SaveException;

    void update( InternalUser user ) throws UpdateException;

    void delete( String login ) throws DeleteException;
}
