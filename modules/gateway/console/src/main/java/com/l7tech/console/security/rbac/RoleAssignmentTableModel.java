package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.util.ExceptionUtils;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Jul 24, 2008
 * Time: 6:35:38 PM
 * To change this template use File | Settings | File Templates.
 *
 * Table model to show Identity Provider and User / Group which belong to a Role
 */
public class RoleAssignmentTableModel  extends AbstractTableModel {

    private Role role;
    private List<RoleAssignment> roleAssignments = new ArrayList<RoleAssignment>();
    private Map<RoleAssignment, IdentityHolder> roleAssignmentToIdentityHolder = new HashMap<RoleAssignment, IdentityHolder>();
    public final static String IDENTITY_PROVIDER = "Identity Provider";
    public final static String USER_GROUPS = "User / Group";
    
    private String columnNames [] = new String[]{IDENTITY_PROVIDER, USER_GROUPS};
    private static final Logger logger = Logger.getLogger(RoleAssignmentTableModel.class.getName());

    /**
     * Comparator used to compare users and groups in the USER_GROUPS column in a role assignee table
     */
    public final static Comparator USER_GROUP_COMPARATOR = new Comparator(){
            public int compare(Object o1, Object o2) {
                Identity i1 = (Identity) o1;
                Identity i2 = (Identity) o2;
                if(!i1.getProviderId().equals(i2.getProviderId())){
                    return i1.getProviderId().compareTo(i2.getProviderId());
                }

                if(i1 instanceof User && i2 instanceof Group) return -1;
                if(i1 instanceof Group && i2 instanceof User) return 1;

                return i1.getName().compareToIgnoreCase(i2.getName());
            }
        };

    public RoleAssignmentTableModel(Role role) throws FindException, DuplicateObjectException{
        if(role == null) return;//can function without any roles - to show an empty table
        this.role = role;
        for(RoleAssignment ra: this.role.getRoleAssignments()){
            try {
                addRoleAssignment(ra);
            } catch (FindException fe) {
                logger.log(Level.FINE, "Cannot find user for role: " + fe.getMessage(), ExceptionUtils.getDebugException(fe));
            }
        }
    }

    /*Allow the table model to be set on the table before any Roles are available*/
    public RoleAssignmentTableModel(){
        
    }

    public List<RoleAssignment> getRoleAssignments(){
        return this.roleAssignments;
    }
    
    public void addRoleAssignment(RoleAssignment ra) throws FindException, DuplicateObjectException {
        IdentityHolder iH = null;
        try{
            iH = new IdentityHolder(ra);
        }catch(IdentityHolder.NoSuchUserException nsue){
            throw new FindException("Can't assign deleted user", nsue);
        }

        //check if ra is a duplicate
        for (RoleAssignment roleAssignement : roleAssignmentToIdentityHolder.keySet()) {
            if (roleAssignement.getProviderId().equals(ra.getProviderId())
                    && roleAssignement.getIdentityId().equals(ra.getIdentityId())
                    && roleAssignement.getRole().getOid() == ra.getRole().getOid()) {
                String userType = ra.getEntityType();
                throw new DuplicateObjectException("The "+userType+" \"" + iH.getIdentity().getName() + "\" is already assigned to this role");
            }
        }
        roleAssignments.add(ra);
        roleAssignmentToIdentityHolder.put(ra, iH);
        this.fireTableDataChanged();
    }

    public void removeRoleAssignment(int index){
        RoleAssignment ra = roleAssignments.get(index);
        roleAssignments.remove(ra);
        roleAssignmentToIdentityHolder.remove(ra);
        this.fireTableDataChanged();
    }

    public String getColumnName(int column) {
        return this.columnNames[column];
    }

    public int getRowCount() {
        return this.roleAssignments.size();
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public Class getColumnClass(int column) {
        if(roleAssignments.size() == 0){
            return super.getColumnClass(column);
        }
        if ((column >= 0) && (column < getColumnCount())) {
            return getValueAt(0, column).getClass();
        } else {
            return Object.class;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        RoleAssignment ra = roleAssignments.get(rowIndex);
        if(ra == null) return "Cannot find identity";
        IdentityHolder iH = roleAssignmentToIdentityHolder.get(ra);
        if(iH == null) return "Cannot find identity";

        if(columnNames[columnIndex].equals(IDENTITY_PROVIDER)){
            return iH.getProvName();            
        }else if(columnNames[columnIndex].equals(USER_GROUPS)){
            return iH.getIdentity();
        }

        return null;
    }

    public Role getRole(){
        return this.role;
    }
}
