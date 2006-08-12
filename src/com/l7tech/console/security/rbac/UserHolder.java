package com.l7tech.console.security.rbac;

import com.l7tech.common.security.rbac.UserRoleAssignment;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.util.Registry;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.HashMap;

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

            EntityHeader[] hs = new EntityHeader[0];
            try {
                hs = identityAdmin.findAllIdentityProviderConfig();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            } catch (FindException e) {
                throw new RuntimeException(e);
            }

            for (EntityHeader h : hs) {
                idpNames.put(h.getOid(), h.getName());
            }
        }
    }

    public UserHolder(UserRoleAssignment ura) throws RemoteException, FindException {
        initIdpMap();
        this.userRoleAssignment = ura;
        this.user = identityAdmin.findUserByID(ura.getProviderId(), ura.getUserId());
        String name = idpNames.get(user.getProviderId());
        if (name == null) name = "Unknown Identity Provider #" + user.getProviderId();
        provName = name;
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
