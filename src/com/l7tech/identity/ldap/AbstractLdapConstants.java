/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.ldap;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractLdapConstants {
    public abstract String descriptionAttribute();

    public abstract String groupObjectClass();
    public abstract String groupNameAttribute();
    public abstract String groupMemberAttribute();

    public abstract String userObjectClass();
    public abstract String userLoginAttribute();
    public abstract String userNameAttribute();
    public abstract String userEmailAttribute();
    public abstract String userFirstnameAttribute();
    public abstract String userLastnameAttribute();
    public abstract String userPasswordAttribute();

}
