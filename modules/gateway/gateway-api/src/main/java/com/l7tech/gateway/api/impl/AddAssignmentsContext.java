package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;
import com.l7tech.gateway.api.RbacRoleAssignmentMO;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used in role add assignments operation. It contains a list of assignmentMO's
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "AddAssignmentsContext")
@XmlType(name = "AddAssignmentsContextType", propOrder = {"assignments"})
public class AddAssignmentsContext extends ManagedObject {
    private List<RbacRoleAssignmentMO> assignments = new ArrayList<>();

    /**
     * The list of assignments to add to the role.
     *
     * @return The list of assignments to add to the role
     */
    @XmlElementWrapper(name = "assignments", required = false)
    @XmlElement(name = "assignment", required = false)
    public List<RbacRoleAssignmentMO> getAssignments() {
        return assignments;
    }

    /**
     * Sets the list of assignments to add to the role
     *
     * @param assignments The list of assignments to add to the role.
     */
    public void setAssignments(List<RbacRoleAssignmentMO> assignments) {
        this.assignments = assignments;
    }
}
