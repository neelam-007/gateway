/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.*;

/**
 * @author alex
 */
public interface PersistentIdentityProvider<UT extends PersistentUser, GT extends PersistentGroup, UMT extends PersistentUserManager<UT>, GMT extends PersistentGroupManager<UT, GT>>
        extends AuthenticatingIdentityProvider<UT, GT, UMT, GMT>
{
}
