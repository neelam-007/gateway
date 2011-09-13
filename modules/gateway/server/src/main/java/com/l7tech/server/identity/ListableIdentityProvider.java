package com.l7tech.server.identity;

import com.l7tech.identity.*;

/**
 * An identity provider that can browse users and groups stored within it.
 */
public interface ListableIdentityProvider<UT extends User, GT extends Group, UMT extends UserManager<UT>, GMT extends GroupManager<UT, GT>> extends AuthenticatingIdentityProvider<UT, GT, UMT, GMT> {
}
