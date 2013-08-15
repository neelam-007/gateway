package com.l7tech.server.admin;

import com.l7tech.objectmodel.IdentityHeader;

import java.security.Principal;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 23, 2008
 * Time: 5:49:02 PM
 * To change this template use File | Settings | File Templates.
 *
 * Store an individual group membership for a user
 */
public class GroupPrincipal implements Principal {

    private final String name;
    private final IdentityHeader groupHeader;
    
    public GroupPrincipal(String name, IdentityHeader groupHeader){
        this.name = name;
        this.groupHeader = new IdentityHeader(groupHeader.getProviderGoid(), groupHeader);
    }

    public IdentityHeader getGroupHeader() {
        return new IdentityHeader(groupHeader.getProviderGoid(), groupHeader);
    }

    public String getName() {
        return name;
    }

}
