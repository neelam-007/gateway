package com.l7tech.server.admin;

import com.l7tech.objectmodel.IdentityHeader;

import java.security.Principal;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 23, 2008
 * Time: 5:49:02 PM
 * To change this template use File | Settings | File Templates.
 *
 * Store group membership for a user
 */
public class GroupPrincipal implements Principal {

    private String name;

    private Set<IdentityHeader> groupHeaders;

    public Set<IdentityHeader> getGroupHeaders() {
        return groupHeaders;
    }

    public void setGroupHeaders(Set<IdentityHeader> groupHeaders) {
        this.groupHeaders = groupHeaders;
    }

    public GroupPrincipal(String name){
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    private final long timestamp = System.currentTimeMillis();
    
}
