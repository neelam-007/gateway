/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

import com.l7tech.identity.*;
import com.l7tech.policy.assertion.AssertionStatus;

import java.security.Principal;
import java.util.Set;

/**
 * Asserts that the requestor is a member of a particular group.
 *
 * @author alex
 * @version $Revision$
 */
public class MemberOfGroup extends IdentityAssertion {
    public MemberOfGroup() {
        super();
    }

    public void setGroup( Group group ) {
        _group = group;
    }

    public Group getGroup() {
        return _group;
    }

    public MemberOfGroup( IdentityProvider provider, Group group ) {
        super( provider );
        _group = group;
    }

    public AssertionStatus doCheckPrincipal( Principal p ) {
        if ( !(p instanceof User) ) throw new IllegalArgumentException( "Authenticated Principal is not a User!" );
        User u = (User)p;
        Set groups = u.getGroups();
        if ( groups.contains( _group ) )
            return AssertionStatus.NONE;
        else {
            return AssertionStatus.AUTH_FAILED;
        }
    }

    protected Group _group;
}
