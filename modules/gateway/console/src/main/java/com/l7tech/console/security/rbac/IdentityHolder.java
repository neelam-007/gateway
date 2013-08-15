package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.identity.Identity;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;

import java.util.HashMap;
import java.util.Map;

/**
 * User: megery
 * Date: Jul 28, 2006
 * Time: 5:08:25 PM
 */
class IdentityHolder {

    private static IdentityAdmin identityAdmin;
    private static Map<Goid, String> idpNames;

    private final RoleAssignment roleAssignment;
    private final Identity identity;
    private final String provName;

    private void initIdpMap() {
        if (identityAdmin == null) {
            identityAdmin = Registry.getDefault().getIdentityAdmin();
            idpNames = new HashMap<Goid, String>();

            EntityHeader[] hs;
            try {
                hs = identityAdmin.findAllIdentityProviderConfig();
            } catch (FindException e) {
                throw new RuntimeException(e);
            }

            for (EntityHeader h : hs) {
                idpNames.put(h.getGoid(), h.getName());
            }
        }
    }

    static void reset() {
        identityAdmin = null;
        idpNames = null;
    }

    static class NoSuchUserException extends Exception {
        final RoleAssignment assignment;

        public NoSuchUserException(RoleAssignment ura) {
            this.assignment = ura;
        }
    }

    public IdentityHolder(RoleAssignment ra) throws FindException, NoSuchUserException {
        initIdpMap();
        this.roleAssignment = ra;
        Identity id = null;
        if(ra.getEntityType().equals(EntityType.USER.getName())){
            id = identityAdmin.findUserByID(ra.getProviderId(), ra.getIdentityId());
        }else if(ra.getEntityType().equals(EntityType.GROUP.getName())){
            id = identityAdmin.findGroupByID(ra.getProviderId(), ra.getIdentityId());
        }else{
            throw new RuntimeException(ra.getEntityType()+" is not supported by IdentityHolder");
        }

        if (id == null) throw new NoSuchUserException(ra);

        String name = idpNames.get(id.getProviderId());
        if (name == null) name = "Unknown Identity Provider #" + id.getProviderId();
        this.identity = id;
        this.provName = name;
    }

    /*Get the name of the identity provider this Identity belongs to*/
    public String getProvName(){
        return this.provName;
    }
    
    public RoleAssignment getUserRoleAssignment() {
        return roleAssignment;
    }

    public Identity getIdentity() {
        return identity;
    }

    public String toString() {
        if(identity instanceof User){
            User u = (User)identity;
            String name = u.getName();
            if (name == null) name = u.getLogin();
            if (name == null) name = u.getId();
            StringBuilder sb = new StringBuilder(name);
            sb.append(" [").append(provName).append("]");
            return sb.toString();
        }else if(identity instanceof Group){
            Group g = (Group)identity;
            String name = g.getName();
            if (name == null) name = g.getDescription();
            if (name == null) name = g.getId();
            StringBuilder sb = new StringBuilder(name);
            sb.append(" [").append(provName).append("]");
            return sb.toString();
        }
        return null;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentityHolder that = (IdentityHolder) o;

        if (identity != null ? !identity.equals(that.identity) : that.identity != null) return false;

        return true;
    }

    public int hashCode() {
        return (identity != null ? identity.hashCode() : 0);
    }
}
