package com.l7tech.adminws.security;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 3, 2003
 *
 */
public class Principal implements java.security.Principal {

    public Principal(com.l7tech.identity.User user) {
        if (user != null) {
            this.name = user.getLogin();
            this.oid = user.getOid();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    private String name = null;
    private long oid = 0;
}
