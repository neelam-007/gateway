package com.l7tech.server.ems;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;

import java.util.Collection;

/**
 * Identity Management interface for the EMS
 */
public interface EmsAccountManager {

    int getUserCount() throws FindException;

    Collection<InternalUser> getUserPage( int start, int count, String property, boolean ascending ) throws FindException;

    InternalUser findByLogin( String login ) throws FindException;

    String save( InternalUser user ) throws SaveException;

    void update( InternalUser user ) throws UpdateException;

}
