package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.UserRoleAssignment;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import java.util.HashMap;
import java.util.Map;

/**
 * User: megery
 * Date: Jul 28, 2006
 * Time: 5:08:25 PM
 */
class UserHolder {

    private static IdentityAdmin identityAdmin;
    private static Map<Long, String> idpNames;

    private final UserRoleAssignment userRoleAssignment;
    private final User user;
    private final String provName;

    private void initIdpMap() {
        if (identityAdmin == null) {
            identityAdmin = Registry.getDefault().getIdentityAdmin();
            idpNames = new HashMap<Long, String>();

            EntityHeader[] hs;
            try {
                hs = identityAdmin.findAllIdentityProviderConfig();
            } catch (FindException e) {
                throw new RuntimeException(e);
            }

            for (EntityHeader h : hs) {
                idpNames.put(h.getOid(), h.getName());
            }
        }
    }

    static void reset() {
        identityAdmin = null;
        idpNames = null;
    }

    static class NoSuchUserException extends Exception {
        final UserRoleAssignment assignment;

        public NoSuchUserException(UserRoleAssignment ura) {
            this.assignment = ura;
        }
    }

    public UserHolder(UserRoleAssignment ura) throws FindException, NoSuchUserException {
        initIdpMap();
        this.userRoleAssignment = ura;
        User u = identityAdmin.findUserByID(ura.getProviderId(), ura.getUserId());
        if (u == null) throw new NoSuchUserException(ura);
        String name = idpNames.get(u.getProviderId());
        if (name == null) name = "Unknown Identity Provider #" + u.getProviderId();
        this.user = u;
        this.provName = name;
    }

    public UserRoleAssignment getUserRoleAssignment() {
        return userRoleAssignment;
    }

    public User getUser() {
        return user;
    }

    public String toString() {
        String name = user.getName();
        if (name == null) name = user.getLogin();
        if (name == null) name = user.getId();
        StringBuilder sb = new StringBuilder(name);
        sb.append(" [").append(provName).append("]");
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserHolder that = (UserHolder) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;

        return true;
    }

    public int hashCode() {
        return (user != null ? user.hashCode() : 0);
    }
}
