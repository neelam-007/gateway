package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.ManagedObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * This is used in role remove assignments operation. It contains a list of assignmentId's
 *
 * @author Victor Kazakov
 */
@XmlRootElement(name = "RemoveAssignmentsContext")
@XmlType(name = "RemoveAssignmentsContextType", propOrder = {"assignmentIds"})
public class RemoveAssignmentsContext extends ManagedObject {
    private List<String> assignmentIds = new ArrayList<>();

    /**
     * This is the list of assignment Id's to remove from the role
     *
     * @return The list of assignment Id's to remove from the role.
     */
    @XmlElementWrapper(name = "assignments", required = false)
    @XmlElement(name = "assignmentId", required = false)
    public List<String> getAssignmentIds() {
        return assignmentIds;
    }

    /**
     * Sets the list of assignment Id's to remove from the role
     *
     * @param assignmentIds The list of assignment Id's to remove from the role.
     */
    public void setAssignmentIds(List<String> assignmentIds) {
        this.assignmentIds = assignmentIds;
    }
}
