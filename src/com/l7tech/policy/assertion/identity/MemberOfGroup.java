/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.identity;

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

    public String getGroupName() {
        return _groupName;
    }

    public void setGroupName(String groupName) {
        _groupName = groupName;
    }

    public MemberOfGroup(long providerOid, String groupName) {
        super(providerOid);
        _groupName = groupName;
    }

    protected String _groupName;
}
