package com.l7tech.server.search.objects;

import com.l7tech.search.Dependency;

/**
 * Represents a dependent assertion.
 *
 * @author Victor Kazakov
 */
public class DependentAssertion extends DependentObject {

    private String descriptiveName;

    public DependentAssertion(String name, String descriptiveName) {
        super(name, Dependency.DependencyType.ASSERTION);
        this.descriptiveName = descriptiveName;
    }

    public String getDescriptiveName() {
        return descriptiveName;
    }
}
